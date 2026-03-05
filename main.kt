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

        var result = parseFactor()

        while (true){
            val cur = lexer.next ?: throw Exception("[Parser] operacao esperada nula") //cur -> token atual 
            
            if (cur.type != "MULT" && cur.type != "DIV") {break}

            var op = cur.type
            lexer.selectNext()

            var num = parseFactor() 

            if (op == "MULT"){
                result = result * num
            } else {
                result = result / num
            } 
        }
    
        return result        

    }

    fun parseFactor(): Int{

        val factor = lexer.next ?: throw Exception("[Parser] token no factor nulo") 

        if (factor.type == "INT"){
            lexer.selectNext()
            val num = factor.Value as Int
            return num

        } else if (factor.type == "PLUS"){
            lexer.selectNext()
            return parseFactor()

        } else if (factor.type == "MINUS"){
            lexer.selectNext()
            return parseFactor() * -1

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

