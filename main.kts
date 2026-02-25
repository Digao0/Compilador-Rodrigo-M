//imports


//variaveis
val equacao = args[0]
var soma = 0
var numero = ""
var ops_validas = listOf('+', '-')
var ops = mutableListOf<Char>()
var nu = mutableListOf<Int>()

class Token(val type: String, val Value: Any){
}

class Lexer(val source: String, val postion: Int = 0, val next: Token){
    
    fun selectNext() {
        //lê o próximo token e atualiza o atributo next
    } 

    
}

class Parser(val lexer: Lexer){

    fun parseExpression(): Int {
        //consome os tokens do Lexer e analisa se a sintaxe está aderente à gramática proposta. retorna o resultado numérico da expressão analisada.
        //Ao final verificar se terminou de consumir toda a cadeia (o token deve ser EOF).
    }

    fun run(code: String): Int{
        //recebe o código fonte como argumento, inicializa um objeto Lexer em lex, posiciona no primeiro token e retorna o resultado do
    }

}

