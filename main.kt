//imports
/* 
fun checa_num(type: String){
    if (type != "INT"){
        throw Exception("[Parser] Entrada invalida - esperado numero")
    }    
}
*/

interface Node{
    val Value: Any
    val children : List<Node>

    fun evaluate(): Int 
}

class IntVal(override val Value: Int) : Node{
    override val children: List<Node> = emptyList()

    override fun evaluate(): Int {
        return Value
    }
}

class BinOp(override val Value: Char, val left : Node, val right : Node) : Node{
    override val children: List<Node> = listOf(left, right)  

    override fun evaluate(): Int {
        if (Value == '+'){
            return children[0].evaluate() + children[1].evaluate()
        } else if (Value == '-') {
            return children[0].evaluate() - children[1].evaluate()
        } else if (Value == '*') {
            return children[0].evaluate() * children[1].evaluate()
        } else if (Value == '/'){
            return children[0].evaluate() / children[1].evaluate()
        } else {throw Exception("[Semantic] Entrada invalida - binop fora do alfabeto") }
    }
}

class UnOp(override val Value: Char, val child: Node) : Node {
    override val children: List<Node> = listOf(child)

    override fun evaluate(): Int {
        if (Value == '-'){
            return -children[0].evaluate()
        } else if (Value == '+') {
            return children[0].evaluate()
        } else {throw Exception("[Semantic] Entrada invalida - unop fora do alfabeto")}
    }   
}





class Token(val type: String, val Value: Any){ //tipos validos: MULT, DIV, OPEN_PAR, CLOSE_PAR, XOR, INT, PLUS, MINUS, EOF ex: (PLUS, '+')
}

class Lexer(val source: String, var position: Int = 0, var next: Token? = null){ //para iniciar o token como nulo ? = null
    
    fun selectNext() {
        var numero = ""
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
            
        }else if (char.isDigit()){
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
        } else {
            throw Exception("[Lexer] Entrada invalida - char fora do alfabeto")
        }

    } 

    
}

class Parser(val lexer: Lexer){

    fun parseExpression(): Node {

        var resultNode = parseTerm()

        while (true){
            val cur = lexer.next ?: throw Exception("[Parser] operacao esperada nula") //cur -> token atual 
            
            if (cur.type != "PLUS" && cur.type != "MINUS" && cur.type != "XOR") {break}

            var op = cur.type
            lexer.selectNext()

            var numNode = parseTerm() 

            if (op == "PLUS"){
                result = BinOp('+', resultNode, numNode)
            } else if (op == "MINUS"){
                result = BinOp('-', resultNode, numNode)
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
                result = BinOp('*', resultNode, numNode)
            } else {
                result = BinOp('/', resultNode, numNode)
            } 
        }
    
        return resultNode        

    }

    fun parseFactor(): Node{

        val factor = lexer.next ?: throw Exception("[Parser] token no factor nulo") 

        if (factor.type == "INT"){
            lexer.selectNext()
            return IntVal(factor.Value as Int)

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

    fun run(code: String): Node{ //retorna toda a arvore
        //recebe o código fonte como argumento, inicializa um objeto Lexer em lex, posiciona no primeiro token e retorna o resultado do parseExpression(). 
        //Ao final verificar se terminou de consumir toda a cadeia (o token deve ser EOF).
        lexer.selectNext()
        val tree = parseExpression()
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

    val equacao = args[0]

    val lex = Lexer(equacao)
    val pars = Parser(lex)
    val root = pars.run(equacao)
    println(root.evaluate())
}