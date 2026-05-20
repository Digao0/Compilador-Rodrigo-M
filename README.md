# Compilador-Rodrigo-M

[![Compilation Status](https://compiler-tester.insper-comp.com.br/svg/Digao0/Compilador-Rodrigo-M)](https://compiler-tester.insper-comp.com.br/svg/Digao0/Compilador-Rodrigo-M)

Projeto de compilador para a materia Logica Computacional.

## Versão 1.0 - Suporte a Funções e Escopo de Variáveis

Diagrama sintatico atual:
![image](imgs/image4.png)

```ebnf
PROGRAM = { FUNCDEC | STATEMENT } ;

FUNCDEC = "function", IDENTIFIER, "(", [IDENTIFIER, TYPE, {",", IDENTIFIER, TYPE}], ")", [TYPE], "\n", {STATEMENT}, "end" ;

STATEMENT =
      "local", IDENTIFIER, TYPE, ["=", BOOLEXPRESSION]
    | IDENTIFIER, "=", BOOLEXPRESSION
    | IDENTIFIER, "(", [BOOLEXPRESSION, {",", BOOLEXPRESSION}], ")"
    | "print", "(", BOOLEXPRESSION, ")"
    | "return", BOOLEXPRESSION
    | "if", "(", BOOLEXPRESSION, ")", "then", {STATEMENT}, ["else", {STATEMENT}], "end"
    | "while", "(", BOOLEXPRESSION, ")", "do", {STATEMENT}, "end"
    | "do", {STATEMENT}, "end"
    | "\n" ;

BOOLEXPRESSION = BOOLTERM, { "or", BOOLTERM } ;
BOOLTERM = RELEXPRESSION, { "and", RELEXPRESSION } ;
RELEXPRESSION = EXPRESSION, [("==" | "<" | ">"), EXPRESSION] ;
EXPRESSION = TERM, { ("+" | "-"), TERM } ;
TERM = FACTOR, { ("*" | "/"), FACTOR } ;

FACTOR =
      ("+" | "-" | "not"), FACTOR
    | "(", BOOLEXPRESSION, ")"
    | NUMBER
    | STRING
    | BOOLEAN
    | IDENTIFIER, ["(", [BOOLEXPRESSION, {",", BOOLEXPRESSION}], ")"]
    | "read", "(", ")" ;

TYPE = "number" | "string" | "boolean" ;
BOOLEAN = "true" | "false" ;
STRING = '"', { CHARACTER }, '"' ;
NUMBER = DIGIT, { DIGIT } ;
IDENTIFIER = (LETTER | "_"), { LETTER | DIGIT | "_" } ;
DIGIT = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
LETTER = "a" | ... | "z" | "A" | ... | "Z" ;
```


How the server will run your program:
`kotlinc -script main.kts [arguments]`

Para lancar tags:
`git tag v1.0.0`
`git push origin v1.0.0`


