//imports

//variaveis
//var soma = 0
//var ops_validas = listOf('+', '-')
//var ops = mutableListOf<Char>()
//var nu = mutableListOf<Int>()
val equacao = args[0]

fun checa_num(type: String){
    if (type != "INT"){
        throw Exception("Entrada invalida - esperado numero")
    }    
}

class Token(val type: String, val Value: Any){ //tipos validos: INT, PLUS, MINUS, EOF ex: (PLUS, '+')
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
        } else if (char.isDigit()){
                numero += char
                position++
                //char = source[position]
            while (source[position].isDigit() && position < source.length){
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
        //consome os tokens do Lexer e analisa se a sintaxe está aderente à gramática proposta. retorna o resultado numérico da expressão analisada.
        //var result: Int
        var result = 0
        
        val first = lexer.next ?: throw Exception("[Parser] Primeiro token nulo") //cur -> token atual
        checa_num(first.type)
        result = first.Value as Int
        lexer.selectNext()

        //if (cur.type == "INT"){throw Exception("numero seguido de numero")}

        while (true){
            val cur = lexer.next ?: throw Exception("[Parser] operacao esperada nula") //cur -> token atual 
            
            if (cur.type != "PLUS" && cur.type != "MINUS") {break}
            var op = cur.type
            lexer.selectNext()

            var prox = lexer.next ?: throw Exception("[Parser] Unexpected EOF")
            checa_num(prox.type)
            var num = prox.Value as Int 

            if (op == "PLUS"){
                result += num
            } else {result -= num}

            lexer.selectNext() //apos somar/sub procura o proximo operador

        }
    
        return result
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

if (equacao.isEmpty()) {
    throw Exception("[Semantic] Entrada vazia") //checa string vazia
}

var lex = Lexer(equacao)
var pars = Parser(lex)
pars.run(equacao)

