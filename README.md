# Compilador-Rodrigo-M

[![Compilation Status](https://compiler-tester.insper-comp.com.br/svg/Digao0/Compilador-Rodrigo-M)](https://compiler-tester.insper-comp.com.br/svg/Digao0/Compilador-Rodrigo-M)

Projeto de compilador para materia Logica Computacional

Diagrama Sintático atual : 
![image](imgs/image2.png)

```ebnf

EXPRESSION = TERM, { ("+" | "-"), TERM } ;
TERM = FACTOR, { ("*" | "/"), FACTOR } ;
FACTOR = ("+" | "-"), FACTOR | "(", EXPRESSION, ")" | NUMBER ;
NUMBER = DIGIT, {DIGIT} ;
DIGIT = 0 | 1 | ... | 9 ;

```

How the server will run your program:
$ kotlinc -script main.kts [arguments]

Para lancar tags:
$ git tag v0.1.0
$ git push origin v0.1.1