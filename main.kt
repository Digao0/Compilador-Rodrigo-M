import java.io.File


interface Node {
    val Value: Any
    val children: List<Node>
    fun evaluate(st: ST): Variable?
}


class Variable(val value: Any, val type: String)

class ST(val table: MutableMap<String, Variable> = mutableMapOf()) {

   
    fun getter(variavel: String): Variable =
        table[variavel] ?: throw Exception("[Semantic] símbolo '$variavel' não está na tabela")

    fun setter(name: String, variavel: Variable) {
        val existing = table[name]
            ?: throw Exception("[Semantic] variável '$name' não foi declarada — use 'local'")
        if (existing.type != variavel.type)
            throw Exception("[Semantic] tipo incompatível para '$name': esperado '${existing.type}', recebido '${variavel.type}'")
        table[name] = variavel
    }


    fun create_variable(name: String, type: String, initValue: Any? = null) {
        if (table.containsKey(name))
            throw Exception("[Semantic] variável '$name' já foi declarada")
        val default: Any = when (type) {
            "number"  -> 0
            "string"  -> ""
            "boolean" -> false
            else      -> throw Exception("[Semantic] tipo desconhecido: '$type'")
        }
        table[name] = Variable(initValue ?: default, type)
    }
}


class IntVal(override val Value: Int) : Node {
    override val children: List<Node> = emptyList()
    override fun evaluate(st: ST) = Variable(Value, "number")
}

class BoolVal(override val Value: Boolean) : Node {
    override val children: List<Node> = emptyList()
    override fun evaluate(st: ST) = Variable(Value, "boolean")
}

class StringVal(override val Value: String) : Node {
    override val children: List<Node> = emptyList()
    override fun evaluate(st: ST) = Variable(Value, "string")
}

class Identifier(override val Value: String) : Node {
    override val children: List<Node> = emptyList()
    override fun evaluate(st: ST): Variable = st.getter(Value)
}


class VarDec(
    val identifier: Node,
    val expr: Node? = null,
    override val Value: Any = ""
) : Node {
    override val children: List<Node> =
        if (expr != null) listOf(identifier, expr) else listOf(identifier)

    override fun evaluate(st: ST): Variable? {
        val type = Value as String
        val name = identifier.Value as String
        if (expr != null) {
            val v = expr.evaluate(st)!!
            if (v.type != type)
                throw Exception("[Semantic] tipo incompatível na declaração de '$name': esperado '$type', recebido '${v.type}'")
            st.create_variable(name, type, v.value)
        } else {
            st.create_variable(name, type)
        }
        return null
    }
}


class BinOp(override val Value: Char, val left: Node, val right: Node) : Node {
    override val children: List<Node> = listOf(left, right)

    override fun evaluate(st: ST): Variable {
        val l = children[0].evaluate(st)!!
        val r = children[1].evaluate(st)!!

        return when (Value) {
            '+' -> when {
                l.type == "number"  && r.type == "number"  ->
                    Variable((l.value as Int) + (r.value as Int), "number")
                l.type == "string"  && r.type == "string"  ->
                    Variable((l.value as String) + (r.value as String), "string")
                else -> throw Exception("[Semantic] '+' requer dois 'number' ou dois 'string'")
            }
            '-' -> {
                checkBoth(l, r, "number", "-")
                Variable((l.value as Int) - (r.value as Int), "number")
            }
            '*' -> {
                checkBoth(l, r, "number", "*")
                Variable((l.value as Int) * (r.value as Int), "number")
            }
            '/' -> {
                checkBoth(l, r, "number", "/")
                val divisor = r.value as Int
                if (divisor == 0) throw Exception("[Semantic] divisão por zero")
                Variable((l.value as Int) / divisor, "number")
            }
            '<' -> {
                checkBoth(l, r, "number", "<")
                Variable((l.value as Int) < (r.value as Int), "boolean")
            }
            '>' -> {
                checkBoth(l, r, "number", ">")
                Variable((l.value as Int) > (r.value as Int), "boolean")
            }
            '=' -> {
                if (l.type != r.type)
                    throw Exception("[Semantic] '==' requer operandos do mesmo tipo")
                Variable(l.value == r.value, "boolean")
            }
            '&' -> {
                checkBoth(l, r, "boolean", "and")
                Variable((l.value as Boolean) && (r.value as Boolean), "boolean")
            }
            '|' -> {
                checkBoth(l, r, "boolean", "or")
                Variable((l.value as Boolean) || (r.value as Boolean), "boolean")
            }
            else -> throw Exception("[Semantic] BinOp: operador desconhecido '$Value'")
        }
    }

    private fun checkBoth(l: Variable, r: Variable, expected: String, op: String) {
        if (l.type != expected || r.type != expected)
            throw Exception("[Semantic] '$op' requer dois operandos do tipo '$expected'")
    }
}


class UnOp(override val Value: Char, val child: Node) : Node {
    override val children: List<Node> = listOf(child)

    override fun evaluate(st: ST): Variable {
        val v = children[0].evaluate(st)!!
        return when (Value) {
            '-' -> {
                if (v.type != "number") throw Exception("[Semantic] unário '-' requer 'number'")
                Variable(-(v.value as Int), "number")
            }
            '+' -> {
                if (v.type != "number") throw Exception("[Semantic] unário '+' requer 'number'")
                v
            }
            '!' -> {
                if (v.type != "boolean") throw Exception("[Semantic] 'not' requer 'boolean'")
                Variable(!(v.value as Boolean), "boolean")
            }
            else -> throw Exception("[Semantic] UnOp: operador desconhecido '$Value'")
        }
    }
}


class Print(val child: Node, override val Value: Any = "") : Node {
    override val children: List<Node> = listOf(child)

    override fun evaluate(st: ST): Variable? {
        println(child.evaluate(st)!!.value)
        return null
    }
}


class Assignment(val left: Node, val right: Node, override val Value: Any = "") : Node {
    override val children: List<Node> = listOf(left, right)

    override fun evaluate(st: ST): Variable? {
        val name  = children[0].Value as String
        val valor = children[1].evaluate(st)!!
        st.setter(name, valor)
        return null
    }
}


class Block(override val children: List<Node> = emptyList(), override val Value: Any = "") : Node {
    override fun evaluate(st: ST): Variable? {
        for (child in children) child.evaluate(st)
        return null
    }
}


class NoOp(override val Value: Any = "") : Node {
    override val children: List<Node> = emptyList()
    override fun evaluate(st: ST): Variable? = null
}


class If(
    val condition: Node,
    val thenBlock: Node,
    val elseBlock: Node? = null,
    override val Value: Any = ""
) : Node {
    override val children: List<Node> =
        if (elseBlock != null) listOf(condition, thenBlock, elseBlock)
        else listOf(condition, thenBlock)

    override fun evaluate(st: ST): Variable? {
        val cond = condition.evaluate(st)!!
        if (cond.type != "boolean")
            throw Exception("[Semantic] condição do 'if' deve ser 'boolean', recebido '${cond.type}'")
        return if (cond.value as Boolean) thenBlock.evaluate(st) else elseBlock?.evaluate(st)
    }
}

class While(val condition: Node, val body: Node, override val Value: Any = "") : Node {
    override val children: List<Node> = listOf(condition, body)

    override fun evaluate(st: ST): Variable? {
        var cond = condition.evaluate(st)!!
        if (cond.type != "boolean")
            throw Exception("[Semantic] condição do 'while' deve ser 'boolean', recebido '${cond.type}'")
        while (cond.value as Boolean) {
            body.evaluate(st)
            cond = condition.evaluate(st)!!
        }
        return null
    }
}


class Read(override val Value: Any = "") : Node {
    override val children: List<Node> = emptyList()

    override fun evaluate(st: ST): Variable {
        val input = readLine()!!
        return try {
            Variable(input.toInt(), "number")
        } catch (e: NumberFormatException) {
            Variable(input, "string")
        }
    }
}


class Prepro {
    companion object {
        fun filter(codigo: String): String = codigo.replace(Regex("--.*"), "")
    }
}


class Token(val type: String, val Value: Any)


class Lexer(val source: String, var position: Int = 0, var next: Token? = null) {

    val keywords = mapOf(
        "print"   to "PRINT",
        "and"     to "AND",
        "or"      to "OR",
        "not"     to "NOT",
        "if"      to "IF",
        "while"   to "WHILE",
        "else"    to "ELSE",
        "read"    to "READ",
        "then"    to "OPEN_IF_BRA",
        "do"      to "OPEN_BRA",
        "end"     to "CLOSE_BRA",
        "local"   to "VAR",
        "true"    to "BOOL",
        "false"   to "BOOL",
        "string"  to "TYPE",
        "number"  to "TYPE",
        "boolean" to "TYPE"
    )

    fun selectNext() {
        
        while (position < source.length && source[position] == ' ') position++

        if (position == source.length) { next = Token("EOF", ""); return }

        val char = source[position]

        when {
            char == '+' -> { next = Token("PLUS",  '+'); position++ }
            char == '-' -> { next = Token("MINUS", '-'); position++ }

            char == '*' && position + 1 < source.length && source[position + 1] == '*' -> {
                next = Token("POWER", "**"); position += 2
            }
            char == '*' -> { next = Token("MULT", '*'); position++ }
            char == '/' -> { next = Token("DIV",  '/'); position++ }
            char == '(' -> { next = Token("OPEN_PAR",  '('); position++ }
            char == ')' -> { next = Token("CLOSE_PAR", ')'); position++ }
            char == '^' -> { next = Token("XOR", '^'); position++ }

            char == '=' && position + 1 < source.length && source[position + 1] == '=' -> {
                next = Token("EQ", "=="); position += 2
            }
            char == '=' -> { next = Token("ASSIGN", '='); position++ }

            char == '\n' -> { next = Token("END", "\n"); position++ }
            char == '>'  -> { next = Token("GT", '>'); position++ }
            char == '<'  -> { next = Token("LT", '<'); position++ }

           
            char == '"' -> {
                position++ 
                val sb = StringBuilder()
                while (position < source.length && source[position] != '"') {
                    if (source[position] == '\n')
                        throw Exception("[Lexer] string não fechada antes do fim da linha")
                    sb.append(source[position])
                    position++
                }
                if (position >= source.length)
                    throw Exception("[Lexer] string não fechada antes do EOF")
                position++ 
                next = Token("STR", sb.toString())
            }

            char.isDigit() -> {
                val sb = StringBuilder()
                while (position < source.length && source[position].isDigit()) {
                    sb.append(source[position]); position++
                }
                next = Token("INT", sb.toString().toInt())
            }

            char.isLetter() || char == '_' -> {
                val sb = StringBuilder()
                while (position < source.length &&
                    (source[position].isLetter() || source[position].isDigit() || source[position] == '_')
                ) {
                    sb.append(source[position]); position++
                }
                val word = sb.toString()
                val tokenType = keywords[word]
                if (tokenType != null) {
                    
                    val tokenValue: Any = if (tokenType == "BOOL") (word == "true") else word
                    next = Token(tokenType, tokenValue)
                } else {
                    next = Token("IDEN", word)
                }
            }

            else -> throw Exception("[Lexer] caractere inesperado: '$char'")
        }
    }
}


class Parser(val lexer: Lexer) {

    fun run(): Node {
        lexer.selectNext()
        val tree = parseProgram()
        if (lexer.next!!.type != "EOF")
            throw Exception("[Parser] entrada inválida — não termina em EOF")
        return tree
    }

    fun parseProgram(): Node {
        val stmts = mutableListOf<Node>()
        while (lexer.next!!.type != "EOF") stmts.add(parseStatement())
        return Block(stmts)
    }

    fun parseBlock(): Node {
        val stmts = mutableListOf<Node>()
        while (lexer.next!!.type != "CLOSE_BRA" && lexer.next!!.type != "ELSE")
            stmts.add(parseStatement())
        return Block(stmts)
    }

    fun parseStatement(): Node {
        val cur = lexer.next!!

        
        if (cur.type == "VAR") {
            lexer.selectNext()
            if (lexer.next!!.type != "TYPE")
                throw Exception("[Parser] esperado tipo após 'local'")
            val type = lexer.next!!.Value as String
            lexer.selectNext()

            if (lexer.next!!.type != "IDEN")
                throw Exception("[Parser] esperado identificador após o tipo")
            val id = Identifier(lexer.next!!.Value as String)
            lexer.selectNext()

            var expr: Node? = null
            if (lexer.next!!.type == "ASSIGN") {
                lexer.selectNext()
                expr = parseBoolExpression()
            }

            if (lexer.next!!.type != "END")
                throw Exception("[Parser] esperado fim de linha após declaração")
            lexer.selectNext()

            return VarDec(id, expr, type)
        }

       
        if (cur.type == "IDEN") {
            val id = Identifier(cur.Value as String)
            lexer.selectNext()

            if (lexer.next!!.type != "ASSIGN")
                throw Exception("[Parser] esperado '=' após identificador")
            lexer.selectNext()

            val expr = parseBoolExpression()

            if (lexer.next!!.type != "END")
                throw Exception("[Parser] esperado fim de linha")
            lexer.selectNext()

            return Assignment(id, expr)
        }

        if (cur.type == "PRINT") {
            lexer.selectNext()
            if (lexer.next!!.type != "OPEN_PAR")
                throw Exception("[Parser] esperado '(' após 'print'")
            lexer.selectNext()

            val expr = parseBoolExpression()

            if (lexer.next!!.type != "CLOSE_PAR")
                throw Exception("[Parser] esperado ')'")
            lexer.selectNext()

            if (lexer.next!!.type != "END")
                throw Exception("[Parser] esperado fim de linha")
            lexer.selectNext()

            return Print(expr)
        }

    
        if (cur.type == "END") {
            lexer.selectNext()
            return NoOp()
        }

   
        if (cur.type == "OPEN_BRA") {
            lexer.selectNext()
            val block = parseBlock()
            if (lexer.next!!.type != "CLOSE_BRA")
                throw Exception("[Parser] esperado 'end'")
            lexer.selectNext()
            return block
        }

    
        if (cur.type == "IF") {
            lexer.selectNext()
            if (lexer.next!!.type != "OPEN_PAR")
                throw Exception("[Parser] esperado '(' após 'if'")
            lexer.selectNext()

            val condition = parseBoolExpression()

            if (lexer.next!!.type != "CLOSE_PAR")
                throw Exception("[Parser] esperado ')'")
            lexer.selectNext()

            if (lexer.next!!.type != "OPEN_IF_BRA")
                throw Exception("[Parser] esperado 'then'")
            lexer.selectNext()

            val thenBlock = parseBlock()

            var elseBlock: Node? = null
            if (lexer.next!!.type == "ELSE") {
                lexer.selectNext()
                elseBlock = parseBlock()
            }

            if (lexer.next!!.type != "CLOSE_BRA")
                throw Exception("[Parser] esperado 'end'")
            lexer.selectNext()

            return If(condition, thenBlock, elseBlock)
        }


        if (cur.type == "WHILE") {
            lexer.selectNext()
            if (lexer.next!!.type != "OPEN_PAR")
                throw Exception("[Parser] esperado '(' após 'while'")
            lexer.selectNext()

            val condition = parseBoolExpression()

            if (lexer.next!!.type != "CLOSE_PAR")
                throw Exception("[Parser] esperado ')'")
            lexer.selectNext()

            if (lexer.next!!.type != "OPEN_BRA")
                throw Exception("[Parser] esperado 'do'")
            lexer.selectNext()

            val body = parseBlock()

            if (lexer.next!!.type != "CLOSE_BRA")
                throw Exception("[Parser] esperado 'end'")
            lexer.selectNext()

            return While(condition, body)
        }

        throw Exception("[Parser] statement inválido")
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
        var result = parseExpression()
        val cur = lexer.next!!
        if (cur.type in listOf("EQ", "GT", "LT")) {
            lexer.selectNext()
            val right = parseExpression()
            result = when (cur.type) {
                "EQ" -> BinOp('=', result, right)
                "GT" -> BinOp('>', result, right)
                "LT" -> BinOp('<', result, right)
                else -> throw Exception("[Parser] operador relacional inválido")
            }
        }
        return result
    }

    fun parseExpression(): Node {
        var result = parseTerm()
        while (true) {
            val cur = lexer.next!!
            if (cur.type != "PLUS" && cur.type != "MINUS") break
            lexer.selectNext()
            result = when (cur.type) {
                "PLUS"  -> BinOp('+', result, parseTerm())
                "MINUS" -> BinOp('-', result, parseTerm())
                else    -> throw Exception("[Parser] operação desconhecida")
            }
        }
        return result
    }

    fun parseTerm(): Node {
        var result = parseFactor()
        while (true) {
            val cur = lexer.next!!
            if (cur.type != "MULT" && cur.type != "DIV") break
            lexer.selectNext()
            result = when (cur.type) {
                "MULT" -> BinOp('*', result, parseFactor())
                else   -> BinOp('/', result, parseFactor())
            }
        }
        return result
    }

    fun parseFactor(): Node {
        val factor = lexer.next!!

        return when (factor.type) {
            "INT" -> {
                lexer.selectNext()
                IntVal(factor.Value as Int)
            }
            "BOOL" -> {
                lexer.selectNext()
                BoolVal(factor.Value as Boolean)
            }
            "STR" -> {
                lexer.selectNext()
                StringVal(factor.Value as String)
            }
            "IDEN" -> {
                lexer.selectNext()
                Identifier(factor.Value as String)
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
                val exp = parseBoolExpression()
                if (lexer.next!!.type != "CLOSE_PAR")
                    throw Exception("[Parser] parêntesis não fechado")
                lexer.selectNext()
                exp
            }
            "READ" -> {
                lexer.selectNext()
                if (lexer.next!!.type != "OPEN_PAR")
                    throw Exception("[Parser] esperado '(' após 'read'")
                lexer.selectNext()
                if (lexer.next!!.type != "CLOSE_PAR")
                    throw Exception("[Parser] esperado ')'")
                lexer.selectNext()
                Read()
            }
            else -> throw Exception("[Parser] factor inválido — token: '${factor.type}'")
        }
    }
}


fun main(args: Array<String>) {
    if (args.isEmpty()) throw Exception("[Main] nenhum arquivo fornecido")

    val source = File(args[0]).readText() + "\n"
    val filtered = Prepro.filter(source)

    val lexer  = Lexer(filtered)
    val parser = Parser(lexer)
    val root   = parser.run()

    root.evaluate(ST())
}