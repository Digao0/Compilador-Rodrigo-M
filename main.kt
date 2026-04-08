//imports
import java.io.File

interface Node{
    val Value: Any
    val children : List<Node>
    fun evaluate(st: ST): Int? 
}

class Variable(val Value: Int) {
}

class ST (val table: MutableMap<String, Variable> = mutableMapOf()){
    fun getter(variavel: String): Int {
        val letra = table[variavel] ?: throw Exception("[Semantic] simbolo nao esta na tabela")
        return letra.Value 
    }

    fun setter(string: String, variavel: Int) {
        table[string] = Variable(variavel)
    }
}

//nós
class IntVal(override val Value: Int) : Node{
    override val children: List<Node> = emptyList()

    override fun evaluate(st: ST): Int {
        return Value
    }
}

class BinOp(override val Value: Char, val left : Node, val right : Node) : Node {
    override val children: List<Node> = listOf(left, right)  

    override fun evaluate(st: ST): Int {  
        val left = children[0].evaluate(st) as Int
        val right = children[1].evaluate(st) as Int

        if (Value == '+'){
            return left + right
        } else if (Value == '-') {
            return left - right
        } else if (Value == '*') {
            return left * right
        } else if (Value == '/'){
            if (right != 0){
                return left / right
            } else {throw Exception("[Semantic] Divisao por 0")}
        } else {throw Exception("[Semantic] Entrada invalida - binop fora do alfabeto") }
    }
}

class UnOp(override val Value: Char, val child: Node) : Node {
    override val children: List<Node> = listOf(child)

    override fun evaluate(st: ST): Int {
        val num = children[0].evaluate(st) as Int

        if (Value == '-'){
            return -num
        } else if (Value == '+') {
            return num
        } else {throw Exception("[Semantic] Entrada invalida - unop fora do alfabeto")}
    }   
}

class Identifier(override val Value: String) : Node {
    override val children: List<Node> = emptyList()

    override fun evaluate(st: ST): Int {
        return st.getter(Value)
    }
}

class Print(override val Value: Any = "", val child: Node) : Node {
    override val children: List<Node> = listOf(child)

    override fun evaluate(st: ST) : Int?{
        println(child.evaluate(st))
        return null
    }
}

class Assignment(override val Value: Any = "", val left : Node, val right : Node) : Node {
    override val children: List<Node> = listOf(left, right)

    override fun evaluate(st: ST) : Int?{
        val nome = children[0].Value as String
        val valor = children[1].evaluate(st) as Int
        st.setter(nome,valor)
        return null
    }
}

class Block(override val Value: Any = "", override val children: List<Node> = emptyList()) : Node {

    override fun evaluate(st: ST): Int?{
        for (child in children){
            child.evaluate(st)
        } 
        return null  
    }
}

class NoOp(override val Value: Any = "") : Node {
    override val children: List<Node> = emptyList()

    override fun evaluate(st: ST): Int? {
        return null
    }
}


class Prepro(){
    companion object { //para criar metodos estaticos
        fun filter(codigo: String): String {
            return codigo.replace(Regex("--.*"),"")
        }
    }
}

class Token(val type: String, val Value: Any){ //tipos validos: ASSIGN, END, PRINT ,MULT, DIV, OPEN_PAR, CLOSE_PAR, XOR, INT, PLUS, MINUS, EOF ex: (PLUS, '+')
}                                                                                 

class Lexer(val source: String, var position: Int = 0, var next: Token? = null){ //para iniciar o token como nulo ? = null
    
    fun selectNext() {
        var numero = ""
        var letra = ""
        //lê o próximo token e atualiza o atributo next
        while (position < source.length && source[position] == ' '){
            position++
        }
        if (position == source.length){
            next = Token("EOF","")
            return 
        }

        var char = source[position]
        
        if (char == '+'){
            next = Token("PLUS", '+')
            position++ 

        } else if (char == '-'){
            next = Token("MINUS", '-')
            position++ 

        } else if (char == '*' && position + 1 < source.length && source[position + 1] == '*') {
            next = Token("POWER", "**")
            position += 2
            
        } else if (char == '*'){
            next = Token("MULT",'*')
            position++

        } else if (char == '/'){
            next = Token("DIV",'/')
            position++

        } else if (char == '('){
            next = Token("OPEN_PAR",'(')
            position++

        } else if (char == ')'){
            next = Token("CLOSE_PAR",')')
            position++

        } else if (char == '^' ){  
            next = Token("XOR", '^')
            position++
            
        } else if (char.isDigit()){
            numero += char
            position++
                //char = source[position]
            while (position < source.length && source[position].isDigit()){
                numero += source[position]
                position++
                //char = source[position]
            }
            next = Token("INT",numero.toInt())
            //numero = ""
        } else if (char == '='){
            next = Token("ASSIGN","=")
            position++

        } else if (char == '\n'){
            next = Token("END", "\n")
            position ++

        } else if (char.isLetter()){
            letra += char
            position++

            while (position < source.length && (source[position].isLetter() || source[position].isDigit() || source[position] == '_')){
                letra += source[position]
                position++
            }

            if (letra == "print") {
                next = Token("PRINT","print")
            } else {next = Token("IDEN",letra)}

        }
        else {
            throw Exception("[Lexer] Entrada invalida - char fora do alfabeto")
        }

    } 
    
}

class Parser(val lexer: Lexer){

    fun parseProgram(): Node {

        val statements = mutableListOf<Node>()

        while (lexer.next!!.type != "EOF"){
            statements.add(parseStatement())
        }
        
        return Block(statements)

    }

    fun parseStatement(): Node {
        
        val cur = lexer.next!!

        if (cur.type == "IDEN") {
            val id = Identifier(cur.Value as String)
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

        var resultNode = parseTerm()

        while (true){
            val cur = lexer.next ?: throw Exception("[Parser] operacao esperada nula") //cur -> token atual 
            
            if (cur.type != "PLUS" && cur.type != "MINUS" && cur.type != "XOR") {break}

            var op = cur.type
            lexer.selectNext()

            var numNode = parseTerm() 

            if (op == "PLUS"){
                resultNode = BinOp('+', resultNode, numNode)
            } else if (op == "MINUS"){
                resultNode = BinOp('-', resultNode, numNode)
            } else {
                throw Exception("[Parser] operacao desconhecida")
            }
        }
    
        return resultNode        

    }

    fun parseTerm(): Node{

        var resultNode = parseFactor()

        while (true){
            val cur = lexer.next ?: throw Exception("[Parser] operacao esperada nula") //cur -> token atual 
            
            if (cur.type != "MULT" && cur.type != "DIV") {break}

            var op = cur.type
            lexer.selectNext()

            var numNode = parseFactor() 

            if (op == "MULT"){
                resultNode = BinOp('*', resultNode, numNode)
            } else {
                resultNode = BinOp('/', resultNode, numNode)
            } 
        }
    
        return resultNode        

    }

    fun parseFactor(): Node{

        val factor = lexer.next ?: throw Exception("[Parser] token no factor nulo") 

        if (factor.type == "INT"){
            lexer.selectNext()
            return IntVal(factor.Value as Int)

        } else if (factor.type == "IDEN"){
            lexer.selectNext()
            return Identifier(factor.Value as String)

        } else if (factor.type == "PLUS"){
            lexer.selectNext()
            return UnOp('+', parseFactor())

        } else if (factor.type == "MINUS"){
            lexer.selectNext()
            return UnOp('-', parseFactor())

        } else if (factor.type == "OPEN_PAR"){
            lexer.selectNext()
            val exp = parseExpression()

            val current = lexer.next ?: throw Exception("[Parser] parentesis nao fechado")
            if (current.type != "CLOSE_PAR"){
                throw Exception("[Parser] Parentesis nao fechado")
            }

            lexer.selectNext()
            return exp
        } 

        throw Exception("[Parser] Factor invalido")

    }

    fun run(): Node{ //retorna toda a arvore
        lexer.selectNext()
        val tree = parseProgram()
        if (lexer.next!!.type != "EOF"){
            throw Exception("[Parser] Entrada invalida - Nao termina em EOF")
        }
        return tree
    }

}


fun main(args: Array<String>) {

    if (args.isEmpty()) {
        throw Exception("[Lexer] Entrada vazia")
    }

    val nome_arquivo = args[0]
    val conteudo = File(nome_arquivo).readText() + "\n"

    val codigoFiltrado = Prepro.filter(conteudo) 
     
    val lex = Lexer(codigoFiltrado)
    val pars = Parser(lex)

    val root = pars.run()

    val st = ST()
    root.evaluate(st)
}