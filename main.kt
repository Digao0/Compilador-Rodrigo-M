import java.io.File

// ================= PREPRO =================
class PrePro {
    companion object {
        fun filter(code: String): String {
            val regex = Regex("--.*")
            return code.lines().joinToString("\n") {
                it.replace(regex, "")
            }
        }
    }
}

// ================= SYMBOL TABLE =================
class Variable(var value: Int)

class SymbolTable {
    private val table = mutableMapOf<String, Variable>()

    fun set(name: String, value: Int) {
        table[name] = Variable(value)
    }

    fun get(name: String): Int {
        return table[name]?.value
            ?: throw Exception("[Semantic] Variavel nao definida: $name")
    }
}

// ================= AST =================
interface Node {
    val Value: Any
    val children: List<Node>

    fun evaluate(st: SymbolTable): Int?
}

class IntVal(override val Value: Int) : Node {
    override val children = emptyList<Node>()

    override fun evaluate(st: SymbolTable): Int {
        return Value
    }
}

class BinOp(override val Value: Char, val left: Node, val right: Node) : Node {
    override val children = listOf(left, right)

    override fun evaluate(st: SymbolTable): Int {
        val l = left.evaluate(st)!!
        val r = right.evaluate(st)!!

        return when (Value) {
            '+' -> l + r
            '-' -> l - r
            '*' -> l * r
            '/' -> {
                if (r == 0) throw Exception("[Semantic] Divisao por 0")
                l / r
            }
            else -> throw Exception("[Semantic] BinOp invalido")
        }
    }
}

class UnOp(override val Value: Char, val child: Node) : Node {
    override val children = listOf(child)

    override fun evaluate(st: SymbolTable): Int {
        val v = child.evaluate(st)!!
        return if (Value == '-') -v else v
    }
}

class Identifier(override val Value: String) : Node {
    override val children = emptyList<Node>()

    override fun evaluate(st: SymbolTable): Int {
        return st.get(Value)
    }
}

class Assignment(val id: Identifier, val expr: Node) : Node {
    override val Value: Any = "="
    override val children = listOf(id, expr)

    override fun evaluate(st: SymbolTable): Int? {
        val value = expr.evaluate(st)!!
        st.set(id.Value, value)
        return null
    }
}

class Print(val expr: Node) : Node {
    override val Value: Any = "print"
    override val children = listOf(expr)

    override fun evaluate(st: SymbolTable): Int? {
        println(expr.evaluate(st))
        return null
    }
}

class Block(val statements: List<Node>) : Node {
    override val Value: Any = "block"
    override val children = statements

    override fun evaluate(st: SymbolTable): Int? {
        for (stmt in children) {
            stmt.evaluate(st)
        }
        return null
    }
}

class NoOp : Node {
    override val Value: Any = ""
    override val children = emptyList<Node>()

    override fun evaluate(st: SymbolTable): Int? {
        return null
    }
}

// ================= TOKEN =================
class Token(val type: String, val value: Any)

// ================= LEXER =================
class Lexer(val source: String, var position: Int = 0, var next: Token? = null) {

    fun selectNext() {
        while (position < source.length && source[position] == ' ') {
            position++
        }

        if (position >= source.length) {
            next = Token("EOF", "")
            return
        }

        val char = source[position]

        when {
            char == '+' -> {
                next = Token("PLUS", '+'); position++
            }
            char == '-' -> {
                next = Token("MINUS", '-'); position++
            }
            char == '*' -> {
                next = Token("MULT", '*'); position++
            }
            char == '/' -> {
                next = Token("DIV", '/'); position++
            }
            char == '(' -> {
                next = Token("OPEN_PAR", '('); position++
            }
            char == ')' -> {
                next = Token("CLOSE_PAR", ')'); position++
            }
            char == '=' -> {
                next = Token("ASSIGN", '='); position++
            }
            char == '\n' -> {
                next = Token("END", '\n'); position++
            }
            char.isDigit() -> {
                var num = ""
                while (position < source.length && source[position].isDigit()) {
                    num += source[position]
                    position++
                }
                next = Token("INT", num.toInt())
            }
            char.isLetter() -> {
                var ident = ""
                while (position < source.length &&
                    (source[position].isLetterOrDigit() || source[position] == '_')) {
                    ident += source[position]
                    position++
                }

                if (ident == "print") {
                    next = Token("PRINT", ident)
                } else {
                    next = Token("IDEN", ident)
                }
            }
            else -> throw Exception("[Lexer] Caractere invalido: $char")
        }
    }
}

// ================= PARSER =================
class Parser(val lexer: Lexer) {

    fun parseProgram(): Node {
        val statements = mutableListOf<Node>()

        while (lexer.next!!.type != "EOF") {
            statements.add(parseStatement())
        }

        return Block(statements)
    }

    fun parseStatement(): Node {
        val cur = lexer.next!!

        if (cur.type == "IDEN") {
            val id = Identifier(cur.value as String)
            lexer.selectNext()

            if (lexer.next!!.type != "ASSIGN") {
                throw Exception("[Parser] Esperado '='")
            }

            lexer.selectNext()
            val expr = parseExpression()

            if (lexer.next!!.type != "END") {
                throw Exception("[Parser] Esperado fim de linha")
            }

            lexer.selectNext()
            return Assignment(id, expr)
        }

        else if (cur.type == "PRINT") {
            lexer.selectNext()

            if (lexer.next!!.type != "OPEN_PAR") {
                throw Exception("[Parser] Esperado '('")
            }

            lexer.selectNext()
            val expr = parseExpression()

            if (lexer.next!!.type != "CLOSE_PAR") {
                throw Exception("[Parser] Esperado ')'")
            }

            lexer.selectNext()

            if (lexer.next!!.type != "END") {
                throw Exception("[Parser] Esperado fim de linha")
            }

            lexer.selectNext()

            return Print(expr)
        }

        else if (cur.type == "END") {
            lexer.selectNext()
            return NoOp()
        }

        throw Exception("[Parser] Statement invalido")
    }

    fun parseExpression(): Node {
        var result = parseTerm()

        while (lexer.next!!.type == "PLUS" || lexer.next!!.type == "MINUS") {
            val op = lexer.next!!
            lexer.selectNext()

            val right = parseTerm()

            result = if (op.type == "PLUS") {
                BinOp('+', result, right)
            } else {
                BinOp('-', result, right)
            }
        }

        return result
    }

    fun parseTerm(): Node {
        var result = parseFactor()

        while (lexer.next!!.type == "MULT" || lexer.next!!.type == "DIV") {
            val op = lexer.next!!
            lexer.selectNext()

            val right = parseFactor()

            result = if (op.type == "MULT") {
                BinOp('*', result, right)
            } else {
                BinOp('/', result, right)
            }
        }

        return result
    }

    fun parseFactor(): Node {
        val token = lexer.next!!

        return when (token.type) {
            "INT" -> {
                lexer.selectNext()
                IntVal(token.value as Int)
            }
            "PLUS" -> {
                lexer.selectNext()
                UnOp('+', parseFactor())
            }
            "MINUS" -> {
                lexer.selectNext()
                UnOp('-', parseFactor())
            }
            "OPEN_PAR" -> {
                lexer.selectNext()
                val expr = parseExpression()

                if (lexer.next!!.type != "CLOSE_PAR") {
                    throw Exception("[Parser] Parentesis nao fechado")
                }

                lexer.selectNext()
                expr
            }
            "IDEN" -> {
                lexer.selectNext()
                Identifier(token.value as String)
            }
            else -> throw Exception("[Parser] Factor invalido")
        }
    }

    fun run(code: String): Node {
        lexer.selectNext()
        val tree = parseProgram()

        if (lexer.next!!.type != "EOF") {
            throw Exception("[Parser] Nao terminou em EOF")
        }

        return tree
    }
}

// ================= MAIN =================
fun main(args: Array<String>) {

    if (args.isEmpty()) {
        throw Exception("Arquivo nao fornecido")
    }

    val filename = args[0]
    val rawCode = File(filename).readText() + "\n"

    val code = PrePro.filter(rawCode)

    val lexer = Lexer(code)
    val parser = Parser(lexer)

    val root = parser.run(code)

    val st = SymbolTable()
    root.evaluate(st)
}