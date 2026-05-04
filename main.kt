import java.io.File
import kotlin.system.exitProcess

object Code {
    private val instructions = mutableListOf<String>()

    fun reset() {
        instructions.clear()
    }

    fun append(code: String) {
        instructions.add(code)
    }

    fun dump(filename: String) {
        val header = """
            section .data
              format_out: db "%d", 10, 0 ; format do printf
              format_in: db "%d", 0 ; format do scanf
              scan_int: dd 0; 32-bits integer

            section .text
              extern printf ; usar _printf para Windows
              extern scanf ; usar _scanf para Windows
              ; extern _ExitProcess@4 ; usar para Windows
              global _start ; inicio do programa

            _start:
              push ebp ; guarda o EBP
              mov ebp, esp ; zera a pilha

              ; aqui comeca o codigo gerado:
        """.trimIndent()

        val footer = """

              ; aqui termina o codigo gerado

              mov esp, ebp ; reestabelece a pilha
              pop ebp

              ; chamada da interrupcao de saida (Linux)
              mov eax, 1
              xor ebx, ebx
              int 0x80
              ; Para Windows:
              ; push dword 0
              ; call _ExitProcess@4
        """.trimIndent()

        File(filename).writeText(header + "\n" + instructions.joinToString("\n") + "\n" + footer + "\n")
    }
}

abstract class Node {
    val id: Int = newId()
    abstract val Value: Any
    abstract val children: List<Node>
    abstract fun evaluate(st: ST): Variable?
    abstract fun generate(st: ST): Variable?

    companion object {
        private var currentId = 0

        fun newId(): Int {
            currentId += 1
            return currentId
        }
    }
}

data class Variable(val value: Any, val type: String, val shift: Int = 0)

class ST(
    val table: MutableMap<String, Variable> = mutableMapOf(),
    private var nextShift: Int = 0
) {
    fun getter(name: String): Variable =
        table[name] ?: throw Exception("[Semantic] simbolo '$name' nao foi declarado")

    fun setter(name: String, variable: Variable) {
        val existing = table[name]
            ?: throw Exception("[Semantic] variavel '$name' nao foi declarada")
        if (existing.type != variable.type) {
            throw Exception(
                "[Semantic] tipo incompativel para '$name': esperado '${existing.type}', recebido '${variable.type}'"
            )
        }
        table[name] = variable.copy(shift = existing.shift)
    }

    fun create_variable(name: String, type: String, initValue: Any? = null): Variable {
        if (table.containsKey(name)) {
            throw Exception("[Semantic] variavel '$name' ja foi declarada")
        }

        val defaultValue: Any = when (type) {
            "number" -> 0
            "string" -> ""
            "boolean" -> false
            else -> throw Exception("[Semantic] tipo desconhecido: '$type'")
        }

        nextShift += 4
        val variable = Variable(initValue ?: defaultValue, type, nextShift)
        table[name] = variable
        return variable
    }
}

class IntVal(override val Value: Int) : Node() {
    override val children: List<Node> = emptyList()

    override fun evaluate(st: ST): Variable = Variable(Value, "number")

    override fun generate(st: ST): Variable {
        Code.append("  mov eax, $Value")
        return Variable(Value, "number")
    }
}

class BoolVal(override val Value: Boolean) : Node() {
    override val children: List<Node> = emptyList()

    override fun evaluate(st: ST): Variable = Variable(Value, "boolean")

    override fun generate(st: ST): Variable {
        Code.append("  mov eax, ${if (Value) 1 else 0}")
        return Variable(Value, "boolean")
    }
}

class StringVal(override val Value: String) : Node() {
    override val children: List<Node> = emptyList()

    override fun evaluate(st: ST): Variable = Variable(Value, "string")

    override fun generate(st: ST): Variable {
        throw Exception("[Codegen] strings nao possuem geracao de codigo")
    }
}

class Identifier(override val Value: String) : Node() {
    override val children: List<Node> = emptyList()

    override fun evaluate(st: ST): Variable = st.getter(Value as String)

    override fun generate(st: ST): Variable {
        val variable = st.getter(Value)
        if (variable.type == "string") {
            throw Exception("[Codegen] strings nao possuem geracao de codigo")
        }
        Code.append("  mov eax, [ebp-${variable.shift}] ; recupera $Value")
        return variable
    }
}

class VarDec(
    val identifier: Node,
    val expr: Node? = null,
    override val Value: Any
) : Node() {
    override val children: List<Node> =
        if (expr == null) listOf(identifier) else listOf(identifier, expr)

    override fun evaluate(st: ST): Variable? {
        val name = identifier.Value as String
        val type = Value as String

        if (expr == null) {
            st.create_variable(name, type)
            return null
        }

        val result = expr.evaluate(st)!!
        if (result.type != type) {
            throw Exception(
                "[Semantic] tipo incompativel na declaracao de '$name': esperado '$type', recebido '${result.type}'"
            )
        }

        st.create_variable(name, type, result.value)
        return null
    }

    override fun generate(st: ST): Variable? {
        val name = identifier.Value as String
        val type = Value as String
        if (type == "string") {
            throw Exception("[Codegen] strings nao possuem geracao de codigo")
        }

        if (expr == null) {
            val variable = st.create_variable(name, type)
            Code.append("  sub esp, 4 ; var $name $type [EBP-${variable.shift}]")
            Code.append("  mov eax, 0")
            Code.append("  mov [ebp-${variable.shift}], eax ; $name = 0")
            return null
        }

        val result = expr.generate(st)!!
        if (result.type != type) {
            throw Exception(
                "[Semantic] tipo incompativel na declaracao de '$name': esperado '$type', recebido '${result.type}'"
            )
        }

        val variable = st.create_variable(name, type)
        Code.append("  sub esp, 4 ; var $name $type [EBP-${variable.shift}]")
        Code.append("  mov [ebp-${variable.shift}], eax ; $name = ${expr.Value}")
        return null
    }
}

class BinOp(override val Value: Char, val left: Node, val right: Node) : Node() {
    override val children: List<Node> = listOf(left, right)

    override fun evaluate(st: ST): Variable {
        val l = left.evaluate(st)!!
        val r = right.evaluate(st)!!

        return when (Value) {
            '+' -> {
                requireType(l, "number", "+")
                requireType(r, "number", "+")
                Variable((l.value as Int) + (r.value as Int), "number")
            }
            '-' -> {
                requireType(l, "number", "-")
                requireType(r, "number", "-")
                Variable((l.value as Int) - (r.value as Int), "number")
            }
            '*' -> {
                requireType(l, "number", "*")
                requireType(r, "number", "*")
                Variable((l.value as Int) * (r.value as Int), "number")
            }
            '/' -> {
                requireType(l, "number", "/")
                requireType(r, "number", "/")
                val divisor = r.value as Int
                if (divisor == 0) {
                    throw Exception("[Semantic] divisao por zero")
                }
                Variable((l.value as Int) / divisor, "number")
            }
            '.' -> Variable(toConcatString(l) + toConcatString(r), "string")
            '<' -> Variable(compareValues(l, r, "<") < 0, "boolean")
            '>' -> Variable(compareValues(l, r, ">") > 0, "boolean")
            '=' -> {
                if (l.type != r.type) {
                    throw Exception("[Semantic] '==' requer operandos do mesmo tipo")
                }
                Variable(l.value == r.value, "boolean")
            }
            '&' -> {
                requireType(l, "boolean", "and")
                requireType(r, "boolean", "and")
                Variable((l.value as Boolean) && (r.value as Boolean), "boolean")
            }
            '|' -> {
                requireType(l, "boolean", "or")
                requireType(r, "boolean", "or")
                Variable((l.value as Boolean) || (r.value as Boolean), "boolean")
            }
            else -> throw Exception("[Semantic] operador desconhecido '$Value'")
        }
    }

    override fun generate(st: ST): Variable {
        if (Value == '.') {
            throw Exception("[Codegen] concatenacao de strings nao possui geracao de codigo")
        }

        val r = right.generate(st)!!
        Code.append("  push eax")
        val l = left.generate(st)!!
        Code.append("  pop ecx")

        return when (Value) {
            '+' -> {
                requireType(l, "number", "+")
                requireType(r, "number", "+")
                Code.append("  add eax, ecx")
                Variable(0, "number")
            }
            '-' -> {
                requireType(l, "number", "-")
                requireType(r, "number", "-")
                Code.append("  sub eax, ecx")
                Variable(0, "number")
            }
            '*' -> {
                requireType(l, "number", "*")
                requireType(r, "number", "*")
                Code.append("  imul ecx")
                Variable(0, "number")
            }
            '/' -> {
                requireType(l, "number", "/")
                requireType(r, "number", "/")
                Code.append("  cdq")
                Code.append("  idiv ecx")
                Variable(0, "number")
            }
            '<' -> {
                requireType(l, "number", "<")
                requireType(r, "number", "<")
                Code.append("  cmp eax, ecx")
                Code.append("  mov eax, 0")
                Code.append("  mov ecx, 1")
                Code.append("  cmovl eax, ecx")
                Variable(false, "boolean")
            }
            '>' -> {
                requireType(l, "number", ">")
                requireType(r, "number", ">")
                Code.append("  cmp eax, ecx")
                Code.append("  mov eax, 0")
                Code.append("  mov ecx, 1")
                Code.append("  cmovg eax, ecx")
                Variable(false, "boolean")
            }
            '=' -> {
                if (l.type != r.type || l.type == "string") {
                    throw Exception("[Semantic] '==' requer operandos do mesmo tipo")
                }
                Code.append("  cmp eax, ecx")
                Code.append("  mov eax, 0")
                Code.append("  mov ecx, 1")
                Code.append("  cmove eax, ecx")
                Variable(false, "boolean")
            }
            '&' -> {
                requireType(l, "boolean", "and")
                requireType(r, "boolean", "and")
                Code.append("  and eax, ecx")
                Variable(false, "boolean")
            }
            '|' -> {
                requireType(l, "boolean", "or")
                requireType(r, "boolean", "or")
                Code.append("  or eax, ecx")
                Variable(false, "boolean")
            }
            else -> throw Exception("[Semantic] operador desconhecido '$Value'")
        }
    }

    private fun requireType(variable: Variable, expected: String, op: String) {
        if (variable.type != expected) {
            throw Exception("[Semantic] '$op' requer operando do tipo '$expected'")
        }
    }

    private fun compareValues(left: Variable, right: Variable, op: String): Int =
        when {
            left.type == "number" && right.type == "number" ->
                (left.value as Int).compareTo(right.value as Int)
            left.type == "string" && right.type == "string" ->
                (left.value as String).compareTo(right.value as String)
            else ->
                throw Exception("[Semantic] '$op' requer dois operandos number ou dois operandos string")
        }

    private fun toConcatString(variable: Variable): String =
        when (variable.type) {
            "number" -> (variable.value as Int).toString()
            "string" -> variable.value as String
            "boolean" -> (variable.value as Boolean).toString()
            else -> throw Exception("[Semantic] tipo incompativel para concatenacao: '${variable.type}'")
        }
}

class UnOp(override val Value: Char, val child: Node) : Node() {
    override val children: List<Node> = listOf(child)

    override fun evaluate(st: ST): Variable {
        val result = child.evaluate(st)!!

        return when (Value) {
            '+' -> {
                if (result.type != "number") {
                    throw Exception("[Semantic] unario '+' requer number")
                }
                result
            }
            '-' -> {
                if (result.type != "number") {
                    throw Exception("[Semantic] unario '-' requer number")
                }
                Variable(-(result.value as Int), "number")
            }
            '!' -> {
                if (result.type != "boolean") {
                    throw Exception("[Semantic] 'not' requer boolean")
                }
                Variable(!(result.value as Boolean), "boolean")
            }
            else -> throw Exception("[Semantic] operador unario desconhecido '$Value'")
        }
    }

    override fun generate(st: ST): Variable {
        val result = child.generate(st)!!

        return when (Value) {
            '+' -> {
                if (result.type != "number") {
                    throw Exception("[Semantic] unario '+' requer number")
                }
                result
            }
            '-' -> {
                if (result.type != "number") {
                    throw Exception("[Semantic] unario '-' requer number")
                }
                Code.append("  neg eax")
                Variable(0, "number")
            }
            '!' -> {
                if (result.type != "boolean") {
                    throw Exception("[Semantic] 'not' requer boolean")
                }
                Code.append("  cmp eax, 0")
                Code.append("  mov eax, 0")
                Code.append("  mov ecx, 1")
                Code.append("  cmove eax, ecx")
                Variable(false, "boolean")
            }
            else -> throw Exception("[Semantic] operador unario desconhecido '$Value'")
        }
    }
}

class Print(val child: Node, override val Value: Any = "") : Node() {
    override val children: List<Node> = listOf(child)

    override fun evaluate(st: ST): Variable? {
        println(child.evaluate(st)!!.value)
        return null
    }

    override fun generate(st: ST): Variable? {
        val result = child.generate(st)!!
        if (result.type == "string") {
            throw Exception("[Codegen] strings nao possuem geracao de codigo")
        }
        Code.append("  push eax")
        Code.append("  push format_out")
        Code.append("  call printf")
        Code.append("  add esp, 8")
        return null
    }
}

class Assignment(val left: Node, val right: Node, override val Value: Any = "") : Node() {
    override val children: List<Node> = listOf(left, right)

    override fun evaluate(st: ST): Variable? {
        val name = left.Value as String
        val result = right.evaluate(st)!!
        st.setter(name, result)
        return null
    }

    override fun generate(st: ST): Variable? {
        val name = left.Value as String
        val variable = st.getter(name)
        if (variable.type == "string") {
            throw Exception("[Codegen] strings nao possuem geracao de codigo")
        }

        val result = right.generate(st)!!
        if (result.type != variable.type) {
            throw Exception(
                "[Semantic] tipo incompativel para '$name': esperado '${variable.type}', recebido '${result.type}'"
            )
        }

        Code.append("  mov [ebp-${variable.shift}], eax ; $name = ${right.Value}")
        return null
    }
}

class Block(
    override val children: List<Node> = emptyList(),
    override val Value: Any = ""
) : Node() {
    override fun evaluate(st: ST): Variable? {
        for (child in children) {
            child.evaluate(st)
        }
        return null
    }

    override fun generate(st: ST): Variable? {
        for (child in children) {
            child.generate(st)
        }
        return null
    }
}

class NoOp(override val Value: Any = "") : Node() {
    override val children: List<Node> = emptyList()

    override fun evaluate(st: ST): Variable? = null

    override fun generate(st: ST): Variable? = null
}

class If(
    val condition: Node,
    val thenBlock: Node,
    val elseBlock: Node? = null,
    override val Value: Any = ""
) : Node() {
    override val children: List<Node> =
        if (elseBlock == null) listOf(condition, thenBlock) else listOf(condition, thenBlock, elseBlock)

    override fun evaluate(st: ST): Variable? {
        val result = condition.evaluate(st)!!
        if (result.type != "boolean") {
            throw Exception("[Semantic] condicao do if deve ser boolean")
        }

        return if (result.value as Boolean) {
            thenBlock.evaluate(st)
        } else {
            elseBlock?.evaluate(st)
        }
    }

    override fun generate(st: ST): Variable? {
        val result = condition.generate(st)!!
        if (result.type != "boolean") {
            throw Exception("[Semantic] condicao do if deve ser boolean")
        }

        val elseNode = elseBlock
        Code.append("  cmp eax, 0")
        if (elseNode == null) {
            Code.append("  je exit_$id")
            thenBlock.generate(st)
            Code.append("exit_$id:")
            return null
        }

        Code.append("  je else_$id")
        thenBlock.generate(st)
        Code.append("  jmp exit_$id")
        Code.append("else_$id:")
        elseNode.generate(st)
        Code.append("exit_$id:")
        return null
    }
}

class While(val condition: Node, val body: Node, override val Value: Any = "") : Node() {
    override val children: List<Node> = listOf(condition, body)

    override fun evaluate(st: ST): Variable? {
        var result = condition.evaluate(st)!!
        if (result.type != "boolean") {
            throw Exception("[Semantic] condicao do while deve ser boolean")
        }

        while (result.value as Boolean) {
            body.evaluate(st)
            result = condition.evaluate(st)!!
            if (result.type != "boolean") {
                throw Exception("[Semantic] condicao do while deve ser boolean")
            }
        }

        return null
    }

    override fun generate(st: ST): Variable? {
        Code.append("loop_$id:")
        val result = condition.generate(st)!!
        if (result.type != "boolean") {
            throw Exception("[Semantic] condicao do while deve ser boolean")
        }

        Code.append("  cmp eax, 0")
        Code.append("  je exit_$id")
        body.generate(st)
        Code.append("  jmp loop_$id")
        Code.append("exit_$id:")
        return null
    }
}

class Read(override val Value: Any = "") : Node() {
    override val children: List<Node> = emptyList()

    override fun evaluate(st: ST): Variable {
        val input = readLine() ?: ""
        return try {
            Variable(input.toInt(), "number")
        } catch (_: NumberFormatException) {
            Variable(input, "string")
        }
    }

    override fun generate(st: ST): Variable {
        Code.append("  push scan_int")
        Code.append("  push format_in")
        Code.append("  call scanf")
        Code.append("  add esp, 8")
        Code.append("  mov eax, dword [scan_int]")
        return Variable(0, "number")
    }
}

class Prepro {
    companion object {
        fun filter(source: String): String = source.replace(Regex("--.*"), "")
    }
}

class Token(val type: String, val Value: Any)

class Lexer(val source: String, var position: Int = 0, var next: Token? = null) {
    private val keywords = mapOf(
        "print" to "PRINT",
        "and" to "AND",
        "or" to "OR",
        "not" to "NOT",
        "if" to "IF",
        "while" to "WHILE",
        "else" to "ELSE",
        "read" to "READ",
        "then" to "THEN",
        "do" to "DO",
        "end" to "END_BLOCK",
        "local" to "VAR",
        "true" to "BOOL",
        "false" to "BOOL",
        "string" to "TYPE",
        "number" to "TYPE",
        "boolean" to "TYPE"
    )

    fun selectNext() {
        while (position < source.length && source[position] in listOf(' ', '\t', '\r')) {
            position++
        }

        if (position >= source.length) {
            next = Token("EOF", "")
            return
        }

        val char = source[position]
        when {
            char == '\n' -> {
                next = Token("BREAK", "\n")
                position++
            }
            char == '+' -> {
                next = Token("PLUS", '+')
                position++
            }
            char == '-' -> {
                next = Token("MINUS", '-')
                position++
            }
            char == '*' -> {
                next = Token("MULT", '*')
                position++
            }
            char == '/' -> {
                next = Token("DIV", '/')
                position++
            }
            char == '(' -> {
                next = Token("OPEN_PAR", '(')
                position++
            }
            char == ')' -> {
                next = Token("CLOSE_PAR", ')')
                position++
            }
            char == '.' && position + 1 < source.length && source[position + 1] == '.' -> {
                next = Token("CONCAT", "..")
                position += 2
            }
            char == '=' && position + 1 < source.length && source[position + 1] == '=' -> {
                next = Token("EQ", "==")
                position += 2
            }
            char == '=' -> {
                next = Token("ASSIGN", '=')
                position++
            }
            char == '>' -> {
                next = Token("GT", '>')
                position++
            }
            char == '<' -> {
                next = Token("LT", '<')
                position++
            }
            char == '"' -> {
                position++
                val builder = StringBuilder()
                while (position < source.length && source[position] != '"') {
                    if (source[position] == '\n') {
                        throw Exception("[Lexer] string nao fechada antes do fim da linha")
                    }
                    builder.append(source[position])
                    position++
                }

                if (position >= source.length) {
                    throw Exception("[Lexer] string nao fechada antes do EOF")
                }

                position++
                next = Token("STR", builder.toString())
            }
            char.isDigit() -> {
                val builder = StringBuilder()
                while (position < source.length && source[position].isDigit()) {
                    builder.append(source[position])
                    position++
                }
                next = Token("INT", builder.toString().toInt())
            }
            char.isLetter() || char == '_' -> {
                val builder = StringBuilder()
                while (
                    position < source.length &&
                    (source[position].isLetter() || source[position].isDigit() || source[position] == '_')
                ) {
                    builder.append(source[position])
                    position++
                }

                val word = builder.toString()
                val type = keywords[word]
                if (type == null) {
                    next = Token("IDEN", word)
                } else {
                    val value: Any = if (type == "BOOL") word == "true" else word
                    next = Token(type, value)
                }
            }
            else -> throw Exception("[Lexer] caractere inesperado: '$char'")
        }
    }
}

class Parser(val lexer: Lexer) {
    fun run(): Node {
        lexer.selectNext()
        val root = parseProgram()
        if (lexer.next!!.type != "EOF") {
            throw Exception("[Parser] entrada invalida")
        }
        return root
    }

    fun parseProgram(): Node {
        val statements = mutableListOf<Node>()
        while (lexer.next!!.type != "EOF") {
            statements.add(parseStatement())
        }
        return Block(statements)
    }

    fun parseBlock(): Node {
        val statements = mutableListOf<Node>()
        while (lexer.next!!.type !in listOf("END_BLOCK", "ELSE", "EOF")) {
            statements.add(parseStatement())
        }
        return Block(statements)
    }

    fun parseStatement(): Node {
        val current = lexer.next!!
        return when (current.type) {
            "BREAK" -> {
                lexer.selectNext()
                NoOp()
            }
            "VAR" -> parseVarDec()
            "IDEN" -> parseAssignment()
            "PRINT" -> parsePrint()
            "DO" -> parseDoBlock()
            "IF" -> parseIf()
            "WHILE" -> parseWhile()
            else -> throw Exception("[Parser] statement invalido")
        }
    }

    private fun parseVarDec(): Node {
        lexer.selectNext()
        if (lexer.next!!.type != "IDEN") {
            throw Exception("[Parser] esperado identificador apos 'local'")
        }
        val identifier = Identifier(lexer.next!!.Value as String)
        lexer.selectNext()

        if (lexer.next!!.type != "TYPE") {
            throw Exception("[Parser] esperado tipo apos identificador na declaracao")
        }
        val type = lexer.next!!.Value as String
        lexer.selectNext()

        var expr: Node? = null
        if (lexer.next!!.type == "ASSIGN") {
            lexer.selectNext()
            expr = parseBoolExpression()
        }

        if (lexer.next!!.type != "BREAK") {
            throw Exception("[Parser] esperado fim de linha apos declaracao")
        }
        lexer.selectNext()

        return VarDec(identifier, expr, type)
    }

    private fun parseAssignment(): Node {
        val identifier = Identifier(lexer.next!!.Value as String)
        lexer.selectNext()

        if (lexer.next!!.type != "ASSIGN") {
            throw Exception("[Parser] esperado '=' apos identificador")
        }
        lexer.selectNext()

        val expr = parseBoolExpression()
        if (lexer.next!!.type != "BREAK") {
            throw Exception("[Parser] esperado fim de linha")
        }
        lexer.selectNext()

        return Assignment(identifier, expr)
    }

    private fun parsePrint(): Node {
        lexer.selectNext()
        if (lexer.next!!.type != "OPEN_PAR") {
            throw Exception("[Parser] esperado '(' apos 'print'")
        }
        lexer.selectNext()

        val expr = parseBoolExpression()

        if (lexer.next!!.type != "CLOSE_PAR") {
            throw Exception("[Parser] esperado ')'")
        }
        lexer.selectNext()

        if (lexer.next!!.type != "BREAK") {
            throw Exception("[Parser] esperado fim de linha")
        }
        lexer.selectNext()

        return Print(expr)
    }

    private fun parseDoBlock(): Node {
        lexer.selectNext()
        val block = parseBlock()
        if (lexer.next!!.type != "END_BLOCK") {
            throw Exception("[Parser] esperado 'end'")
        }
        lexer.selectNext()
        return block
    }

    private fun parseIf(): Node {
        lexer.selectNext()
        if (lexer.next!!.type != "OPEN_PAR") {
            throw Exception("[Parser] esperado '(' apos 'if'")
        }
        lexer.selectNext()

        val condition = parseBoolExpression()

        if (lexer.next!!.type != "CLOSE_PAR") {
            throw Exception("[Parser] esperado ')'")
        }
        lexer.selectNext()

        if (lexer.next!!.type != "THEN") {
            throw Exception("[Parser] esperado 'then'")
        }
        lexer.selectNext()

        val thenBlock = parseBlock()
        var elseBlock: Node? = null

        if (lexer.next!!.type == "ELSE") {
            lexer.selectNext()
            elseBlock = parseBlock()
        }

        if (lexer.next!!.type != "END_BLOCK") {
            throw Exception("[Parser] esperado 'end'")
        }
        lexer.selectNext()

        return If(condition, thenBlock, elseBlock)
    }

    private fun parseWhile(): Node {
        lexer.selectNext()
        if (lexer.next!!.type != "OPEN_PAR") {
            throw Exception("[Parser] esperado '(' apos 'while'")
        }
        lexer.selectNext()

        val condition = parseBoolExpression()

        if (lexer.next!!.type != "CLOSE_PAR") {
            throw Exception("[Parser] esperado ')'")
        }
        lexer.selectNext()

        if (lexer.next!!.type != "DO") {
            throw Exception("[Parser] esperado 'do'")
        }
        lexer.selectNext()

        val body = parseBlock()

        if (lexer.next!!.type != "END_BLOCK") {
            throw Exception("[Parser] esperado 'end'")
        }
        lexer.selectNext()

        return While(condition, body)
    }

    fun parseBoolExpression(): Node {
        var result = parseBoolTerm()
        while (lexer.next!!.type == "OR") {
            lexer.selectNext()
            result = BinOp('|', result, parseBoolTerm())
        }
        return result
    }

    fun parseBoolTerm(): Node {
        var result = parseRelExpression()
        while (lexer.next!!.type == "AND") {
            lexer.selectNext()
            result = BinOp('&', result, parseRelExpression())
        }
        return result
    }

    fun parseRelExpression(): Node {
        var result = parseConcatExpression()
        if (lexer.next!!.type in listOf("EQ", "GT", "LT")) {
            val op = lexer.next!!.type
            lexer.selectNext()
            val right = parseConcatExpression()
            result = when (op) {
                "EQ" -> BinOp('=', result, right)
                "GT" -> BinOp('>', result, right)
                "LT" -> BinOp('<', result, right)
                else -> throw Exception("[Parser] operador relacional invalido")
            }
        }
        return result
    }

    fun parseConcatExpression(): Node {
        var result = parseExpression()
        while (lexer.next!!.type == "CONCAT") {
            lexer.selectNext()
            result = BinOp('.', result, parseExpression())
        }
        return result
    }

    fun parseExpression(): Node {
        var result = parseTerm()
        while (lexer.next!!.type == "PLUS" || lexer.next!!.type == "MINUS") {
            val op = lexer.next!!.type
            lexer.selectNext()
            result = when (op) {
                "PLUS" -> BinOp('+', result, parseTerm())
                "MINUS" -> BinOp('-', result, parseTerm())
                else -> throw Exception("[Parser] operador invalido")
            }
        }
        return result
    }

    fun parseTerm(): Node {
        var result = parseFactor()
        while (lexer.next!!.type == "MULT" || lexer.next!!.type == "DIV") {
            val op = lexer.next!!.type
            lexer.selectNext()
            result = when (op) {
                "MULT" -> BinOp('*', result, parseFactor())
                "DIV" -> BinOp('/', result, parseFactor())
                else -> throw Exception("[Parser] operador invalido")
            }
        }
        return result
    }

    fun parseFactor(): Node {
        val current = lexer.next!!
        return when (current.type) {
            "INT" -> {
                lexer.selectNext()
                IntVal(current.Value as Int)
            }
            "BOOL" -> {
                lexer.selectNext()
                BoolVal(current.Value as Boolean)
            }
            "STR" -> {
                lexer.selectNext()
                StringVal(current.Value as String)
            }
            "IDEN" -> {
                lexer.selectNext()
                Identifier(current.Value as String)
            }
            "PLUS" -> {
                lexer.selectNext()
                UnOp('+', parseFactor())
            }
            "MINUS" -> {
                lexer.selectNext()
                UnOp('-', parseFactor())
            }
            "NOT" -> {
                lexer.selectNext()
                UnOp('!', parseFactor())
            }
            "OPEN_PAR" -> {
                lexer.selectNext()
                val result = parseBoolExpression()
                if (lexer.next!!.type != "CLOSE_PAR") {
                    throw Exception("[Parser] parenteses nao fechado")
                }
                lexer.selectNext()
                result
            }
            "READ" -> {
                lexer.selectNext()
                if (lexer.next!!.type != "OPEN_PAR") {
                    throw Exception("[Parser] esperado '(' apos 'read'")
                }
                lexer.selectNext()
                if (lexer.next!!.type != "CLOSE_PAR") {
                    throw Exception("[Parser] esperado ')'")
                }
                lexer.selectNext()
                Read()
            }
            else -> throw Exception("[Parser] factor invalido - token '${current.type}'")
        }
    }
}

fun main(args: Array<String>) {
    try {
        if (args.isEmpty()) {
            throw Exception("[Main] nenhum arquivo fornecido")
        }

        val inputFile = File(args[0])
        val source = inputFile.readText() + "\n"
        val filtered = Prepro.filter(source)
        val root = Parser(Lexer(filtered)).run()
        val outputFile = File(inputFile.parentFile ?: File("."), inputFile.nameWithoutExtension + ".asm")

        Code.reset()
        root.generate(ST())
        Code.dump(outputFile.path)
    } catch (e: Exception) {
        System.err.println(e.message ?: "[Error]")
        exitProcess(1)
    }
}
