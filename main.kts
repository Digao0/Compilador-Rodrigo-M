//imports


//variaveis
val equacao = args[0]
var soma = 0
var numero = ""
var ops_validas = listOf('+', '-')
var ops = mutableListOf<Char>()
var nu = mutableListOf<Int>()

class Token(val type: String, val Value: Any){ //tipos validos: INT, PLUS, MINUS, EOF ex: (PLUS, '+')
}

class Lexer(val source: String, var position: Int = 0, var next: Token? = null){
    
    fun selectNext() {
        //lê o próximo token e atualiza o atributo next
        if (postion > source.length){
            throw Exception("Entrada invalida - char out of bounds")
        }
        var char = source[position]
        while (char == '' && position <= source.length){
            position++
            char = source[position]
        }

        if (position == source.length){
            next = Token("EOF","") 
        } else if (char == '+'){
            next = Token("PLUS", '+')  
        } else if (char == '-'){
            next = Token("MINUS", '-') 
        } else if (char.isDigit()){
                numero += char
                position++
                //char = source[position]
            while (char.isDigit()){
                numero += char
                position++
                //char = source[position]
            }
            next = Token("INT",numero.toInt())
            numero = ""
        } else {
            throw Exception("Entrada invalida - char fora do alfabeto")
        }

    } 

    
}

class Parser(val lexer: Lexer){

    fun parseExpression(): Int {
        //consome os tokens do Lexer e analisa se a sintaxe está aderente à gramática proposta. retorna o resultado numérico da expressão analisada.
        while (lexer.next.type != "EOF"){
            if (lexer.next.type != "INT"){
                throw Exception("Entrada invalida - nao inicia em numero")
            }
            var result = lexer.next.Value
            lexer.selectNext()
            while (lexer.next.type == "PLUS" || lexer.next.type == "MINUS") {
                var op = lexer.next.type
                lexer.selectNext()
                var num = lexer.next.Value
                if (lexer.next.type != "INT"){
                    throw Exception("Entrada invalida - numero nao sucede operacao")
                }
                if (op == "PLUS"){
                    result += num
                } else {result -= num}
                lexer.selectNext() 
            }
        }
        return result


    }

    fun run(val code: String): Int{
        //recebe o código fonte como argumento, inicializa um objeto Lexer em lex, posiciona no primeiro token e retorna o resultado do parseExpression(). 
        //Ao final verificar se terminou de consumir toda a cadeia (o token deve ser EOF).
        lexer.selectNext()
        val somaFinal = parseExpression()
        if (lexer.next.type != "EOF"){
            throw Exception("Entrada invalida - Nao termina em EOF")
        }
        return somaFinal
    }

}

if (equacao.isEmpty()) {
    throw Exception("Entrada vazia") //checa string vazia
}

var lex = Lexer(equacao)
var pars = Parser(lex)
pars.run(equacao)

