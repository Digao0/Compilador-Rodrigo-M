//println("hello kotlin")

//variaveis
val equacao = args[0]
var inicio = true
var soma = 0
var numero = ""
var cont = 0
var flag_space = false

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
    
println(soma)


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


