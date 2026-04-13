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
        } else if (Value == '<') {
            return if (left < right) 1 else 0
        } else if (Value == '>') {
            return if (left > right) 1 else 0
        } else if (Value == '=') { 
            return if (left == right) 1 else 0
        } else if (Value == '&') {
            return if (left != 0 && right != 0) 1 else 0
        } else if (Value == '|') {
            return if (left != 0 || right != 0) 1 else 0
        }
        
        else {throw Exception("[Semantic] Entrada invalida - binop fora do alfabeto") }
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
        } else if (Value == '!') {
            return if (num == 0) 1 else 0
        }
        else {throw Exception("[Semantic] Entrada invalida - unop fora do alfabeto")}
    }   
}

class Identifier(override val Value: String) : Node {
    override val children: List<Node> = emptyList()

    override fun evaluate(st: ST): Int {
        return st.getter(Value)
    }
}

class Print(val child: Node, override val Value: Any = "") : Node {
    override val children: List<Node> = listOf(child)

    override fun evaluate(st: ST) : Int?{
        println(child.evaluate(st))
        return null
    }
}

class Assignment(val left : Node, val right : Node, override val Value: Any = "") : Node {
    override val children: List<Node> = listOf(left, right)

    override fun evaluate(st: ST) : Int?{
        val nome = children[0].Value as String
        val valor = children[1].evaluate(st) as Int
        st.setter(nome,valor)
        return null
    }
}

class Block(override val children: List<Node> = emptyList(), override val Value: Any = "") : Node {

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

class If(val condition: Node, val thenBlock: Node, val elseBlock: Node? = null,override val Value: Any = "") : Node {
    override val children: List<Node> =
        if (elseBlock != null) listOf(condition, thenBlock, elseBlock)
        else listOf(condition, thenBlock)
    
    override fun evaluate(st: ST): Int? {
        val cond = condition.evaluate(st)!!

        return if (cond != 0) { // TRUE -> != 0
            thenBlock.evaluate(st)
        } else {
            elseBlock?.evaluate(st)
        }
    }
}

class While(val condition: Node, val body: Node, override val Value: Any = "") : Node {
    override val children: List<Node> = listOf(condition, body)

    override fun evaluate(st: ST): Int? {

        while (condition.evaluate(st)!! != 0) {
            body.evaluate(st)
        }

        return null
    }
}

class Read(override val Value: Any = "") : Node {
    override val children: List<Node> = emptyList()

    override fun evaluate(st: ST): Int {
        return readLine()!!.toInt()
    }
}

class Token(val type: String, val Value: Any){ //tipos validos: ASSIGN, END, PRINT ,MULT, DIV, OPEN_PAR, CLOSE_PAR, XOR, INT, PLUS, MINUS, EOF ex: (PLUS, '+')
}                                                                                 

class Lexer(val source: String, var position: Int = 0, var next: Token? = null){ //para iniciar o token como nulo ? = null
    
    val keywords = mapOf(
    "print" to "PRINT",
    "and" to "AND",
    "or" to "OR",
    "not" to "NOT",
    "if" to "IF",
    "while" to "WHILE",
    "else" to "ELSE",
    "read" to "READ",
    "then" to "OPEN_IF_BRA",
    "do" to "OPEN_BRA",
    "end" to "CLOSE_BRA"
    )

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
        } else if (char == '=' && position + 1 < source.length && source[position + 1] == '='){
            next = Token("EQ","==")
            position += 2
        
        } else if (char == '='){
            next = Token("ASSIGN","=")
            position++

        } else if (char == '\n'){
            next = Token("END", "\n")
            position++
        
        } else if (char == '>'){
            next = Token("GT",">")
            position++

        } else if (char == '<'){
            next = Token("LT","<")
            position++
          
        } else if (char.isLetter()){
            letra += char
            position++

            while (position < source.length && (source[position].isLetter() || source[position].isDigit() || source[position] == '_')){
                letra += source[position]
                position++
            }

            val tokenType = keywords[letra]

            if (tokenType != null) {
                next = Token(tokenType,letra)
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

    fun parseBlock(): Node {
        val statements = mutableListOf<Node>()

        while (lexer.next!!.type != "CLOSE_BRA" && lexer.next!!.type != "ELSE") {
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
            val expr = parseBoolExpression()

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
            val expr = parseBoolExpression()

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

        else if (cur.type == "IF") {
            lexer.selectNext()

            if (lexer.next!!.type != "OPEN_PAR") {
                throw Exception("[Parser] Esperado '(' após IF")
            }

            lexer.selectNext()
            val condition = parseBoolExpression() // trocar

            if (lexer.next!!.type != "CLOSE_PAR") {
                throw Exception("[Parser] Esperado ')'")
            }

            lexer.selectNext()

            if (lexer.next!!.type != "OPEN_IF_BRA") {
                throw Exception("[Parser] Esperado THEN")
            }

            lexer.selectNext()

            val thenBlock = parseBlock()

            var elseBlock: Node? = null

            if (lexer.next!!.type == "ELSE") {
                lexer.selectNext()
                elseBlock = parseBlock()
            }

            if (lexer.next!!.type != "CLOSE_BRA") {
                throw Exception("[Parser] Esperado END")
            }

            lexer.selectNext()

            return If(condition, thenBlock, elseBlock)
        }

        else if (cur.type == "WHILE") {
        lexer.selectNext()

        if (lexer.next!!.type != "OPEN_PAR") {
            throw Exception("[Parser] Esperado '(' após WHILE")
        }

        lexer.selectNext()
        val condition = parseBoolExpression()

        if (lexer.next!!.type != "CLOSE_PAR") {
            throw Exception("[Parser] Esperado ')'")
        }

        lexer.selectNext()

        if (lexer.next!!.type != "OPEN_BRA") { // DO
            throw Exception("[Parser] Esperado DO")
        }

        lexer.selectNext()

        val body = parseBlock()

        if (lexer.next!!.type != "CLOSE_BRA") { // END
            throw Exception("[Parser] Esperado END")
        }

        lexer.selectNext()

        return While(condition, body)
    }
                

        throw Exception("[Parser] Statement invalido")

    }

    fun parseBoolExpression(): Node {
        var result = parseBoolTerm()

        while (lexer.next!!.type == "OR") {
            lexer.selectNext()
            val right = parseBoolTerm()
            result = BinOp('|', result, right)
        }

        return result
    }

    fun parseBoolTerm(): Node {
        var result = parseRelExpression()

        while (lexer.next!!.type == "AND") {
            lexer.selectNext()
            val right = parseRelExpression()
            result = BinOp('&', result, right)
        }

        return result
    }

    fun parseRelExpression(): Node {
        var result = parseExpression()

        val cur = lexer.next!!

        if (cur.type == "EQ" || cur.type == "GT" || cur.type == "LT") {
            lexer.selectNext()
            val right = parseExpression()

            result = when (cur.type) {
                "EQ" -> BinOp('=', result, right)
                "GT" -> BinOp('>', result, right)
                "LT" -> BinOp('<', result, right)
                else -> throw Exception("Operador relacional inválido")
            }
        }

        return result
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
            val exp = parseBoolExpression()

            val current = lexer.next ?: throw Exception("[Parser] parentesis nao fechado")
            if (current.type != "CLOSE_PAR"){
                throw Exception("[Parser] Parentesis nao fechado")
            }

            lexer.selectNext()
            return exp
        
        } else if (factor.type == "NOT") {
            lexer.selectNext()
            return UnOp('!', parseFactor())
        
        } else if (factor.type == "READ") {
            lexer.selectNext()

            if (lexer.next!!.type != "OPEN_PAR") {
                throw Exception("Esperado ( após READ")
            }

            lexer.selectNext()

            if (lexer.next!!.type != "CLOSE_PAR") {
                throw Exception("Esperado )")
            }

            lexer.selectNext()

            return Read()
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