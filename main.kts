//println("hello kotlin")

//variaveis
//var inicio = true
//var flag_space = false
//var current_op = '+'
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


/* 
while (cont < equacao.length) {
    val char = equacao[cont]

    if (inicio){                       // || = or 
        if (char.isDigit() == false && char != ' '){  // && = and
            inicio = false
            throw Exception("Entrada invalida") //verifica se eh numero no inicio ou depois de sinal
        }
    }

    if (flag_space && char.isDigit() == true){
        throw Exception("Entrada invalida") //verifica se tem numero depois de um espaco
    }
    
    if (char.isDigit() == true){
        inicio = false
        numero += char
    } else if (char == '+'){
        soma += numero.toInt()
        numero = ""
        inicio = true
        flag_space = false
    } else if (char == '-'){
        soma -= numero.toInt()
        numero = ""
        inicio = true
        flag_space = false
    } else if (char == ' '){
        if (!inicio){
            flag_space = true
        } else {
            cont++
            continue
        }    
    } else {
        throw Exception("Entrada invalida")
    }

    if (cont == equacao.length - 1) {

        if (char == '+' || char == '-') {
            throw Exception("Expressão não pode terminar com operador")
        } else {
            soma += numero.toInt()
        }

        break  // sai do loop
    }

    cont ++
}
    
println(soma) */


/* 

for (char in equacao) {

    cont++

    if (inicio){                       // || = or 
        if (char.isDigit() == false){  // && = and
            inicio = false
            throw Exception("Entrada invalida") //verifica se eh numero no inicio ou depois de sinal
        }
    }
    
    if (char.isDigit() == true){
        numero += char
    } else if (char == '+'){
        soma += numero.toInt()
        numero = ""
        inicio = true
    } else if (char == '-'){
        soma -= numero.toInt()
        numero = ""
        inicio = true
    } else if (char == ' '){
        continue
    } else {
        throw Exception("Entrada invalida")
    }

    if (char == equacao.last()){ //mudar
        soma += numero.toInt()
    }
}

println(soma)

*/ //tentativa de resolver a questao usando for, mas nao consegui


