# Compilador-Rodrigo-M

[![Compilation Status](https://compiler-tester.insper-comp.com.br/svg/Digao0/Compilador-Rodrigo-M)](https://compiler-tester.insper-comp.com.br/svg/Digao0/Compilador-Rodrigo-M)

Projeto de compilador para a materia Logica Computacional.

Diagrama sintatico atual:
![image](imgs/image4.png)

```ebnf
PROGRAM = { STATEMENT } ;

STATEMENT =
      "local", IDENTIFIER, TYPE, [ "=", BOOLEXPRESSION ], EOL
    | IDENTIFIER, "=", BOOLEXPRESSION, EOL
    | "print", "(", BOOLEXPRESSION, ")", EOL
    | "if", "(", BOOLEXPRESSION, ")", "then", BLOCK, [ "else", BLOCK ], "end"
    | "while", "(", BOOLEXPRESSION, ")", "do", BLOCK, "end"
    | "do", BLOCK, "end"
    | EOL ;

BLOCK = { STATEMENT } ;
BOOLEXPRESSION = BOOLTERM, { "or", BOOLTERM } ;
BOOLTERM = RELEXPRESSION, { "and", RELEXPRESSION } ;
RELEXPRESSION = CONCATEXPRESSION, [ ("==" | "<" | ">"), CONCATEXPRESSION ] ;
CONCATEXPRESSION = EXPRESSION, { "..", EXPRESSION } ;
EXPRESSION = TERM, { ("+" | "-"), TERM } ;
TERM = FACTOR, { ("*" | "/"), FACTOR } ;

FACTOR =
      ("+" | "-" | "not"), FACTOR
    | "(", BOOLEXPRESSION, ")"
    | NUMBER
    | STRING
    | BOOLEAN
    | IDENTIFIER
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
`git tag v0.1.0`
`git push origin v0.1.1`
