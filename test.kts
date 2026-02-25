val equacao = args[0]
var soma = 0
var numero = ""


var ops_validas = listOf('+', '-')
var ops = mutableListOf<Char>()
var nu = mutableListOf<Int>()

if (equacao.isEmpty()) {
    throw Exception("Entrada invalida") //checa string vazia
}

var cont = 0
while (cont < equacao.length){
    var char = equacao[cont]

    if (char.isDigit()){

        while (cont < equacao.length && equacao[cont].isDigit()) {
            numero += equacao[cont]
            cont++
        }

        nu.add(numero.toInt())
        numero = ""
    } else if (char in ops_validas){
        if (nu.isEmpty()) {
            throw Exception("Entrada invalida")
        }
        ops.add(char)
        cont++
    } else if (char == ' '){
        cont++
    } else {
        throw Exception("Entrada invalida")
    }

}

if (nu.size != ops.size + 1){
    throw Exception("Entrada invalida") //checa se tem numero a mais ou operador a mais
}

soma = nu[0]
for (i in ops.indices){
    if (ops[i] == '+'){
        soma += nu[i + 1]
    } else if (ops[i] == '-'){
        soma -= nu[i + 1]
    }
}

println(soma)


