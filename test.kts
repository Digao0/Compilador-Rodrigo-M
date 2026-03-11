
//extra feito
//imports


fun checa_num(type: String){
    if (type != "INT"){
        throw Exception("[Parser] Entrada invalida - esperado numero")
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

    fun intPow(base: Int, exp: Int): Int {
        if (exp < 0) throw Exception("[Semantic] Expoente negativo nao suportado")

        var result = 1
        repeat(exp) {
            result *= base
        }
        return result

    }

    fun parseExpression(): Int {

        var result = parseTerm()

        while (true){
            val cur = lexer.next ?: throw Exception("[Parser] operacao esperada nula") //cur -> token atual 
            
            if (cur.type != "PLUS" && cur.type != "MINUS" && cur.type != "XOR") {break}

            var op = cur.type
            lexer.selectNext()

            var num = parseTerm() 

            if (op == "PLUS"){
                result += num
            } else if (op == "MINUS"){
                result -= num
            } else {
                result = result xor num
            }
        }
    
        return result        

    }

    fun parseTerm(): Int{

        var result = parseUnary()

        while (true){
            val cur = lexer.next ?: throw Exception("[Parser] operacao esperada nula") //cur -> token atual 
            
            if (cur.type != "MULT" && cur.type != "DIV") {break}

            var op = cur.type
            lexer.selectNext()

            var num = parseUnary() 

            if (op == "MULT"){
                result = result * num
            } else {
                result = result / num
            } 
        }
    
        return result        

    }

    fun parseUnary(): Int {

        val token = lexer.next ?: throw Exception("[Parser] token nulo em unary")

        if (token.type == "PLUS") {
            lexer.selectNext()
            return parseUnary()
        }

        if (token.type == "MINUS") {
            lexer.selectNext()
            return -parseUnary()
        }

        return parsePower()
    }

    fun parsePower(): Int {

        val token = lexer.next ?: throw Exception("[Parser] token nulo em power")

        var base: Int

        if (token.type == "INT") {
            lexer.selectNext()
            base = token.Value as Int

        } else if (token.type == "OPEN_PAR") {
            lexer.selectNext()
            base = parseExpression()

            val current = lexer.next ?: throw Exception("[Parser] parentesis nao fechado")
            if (current.type != "CLOSE_PAR") {
                throw Exception("[Parser] parentesis nao fechado")
            }

            lexer.selectNext()

        } else {
            throw Exception("[Parser] fator invalido")
        }

        // lidando com potencia
        val nextToken = lexer.next
        if (nextToken != null && nextToken.type == "POWER") {
            lexer.selectNext()
            val exponent = parsePower()
            base = intPow(base, exponent)
        }

        return base

    }

    fun run(code: String): Int{
        //recebe o código fonte como argumento, inicializa um objeto Lexer em lex, posiciona no primeiro token e retorna o resultado do parseExpression(). 
        //Ao final verificar se terminou de consumir toda a cadeia (o token deve ser EOF).
        lexer.selectNext()
        val somaFinal = parseExpression()
        if (lexer.next!!.type != "EOF"){
            throw Exception("[Parser] Entrada invalida - Nao termina em EOF")
        }
        return somaFinal
    }

}


fun main(args: Array<String>) {

    if (args.isEmpty()) {
        throw Exception("[Lexer] Entrada vazia")
    }

    val equacao = args[0]

    val lex = Lexer(equacao)
    val pars = Parser(lex)
    val result = pars.run(equacao)
    println(result)
}


