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

## Features Implementadas

### Declaração de Funções
- Funções com argumentos tipados
- Tipos de retorno opcionais
- Funções sem retorno (void)
- Exemplo:
```
function soma(x number, y number) number
  return x + y
end

function main()
  print(soma(3, 4))
end

main()
```

### Escopo de Variáveis
- Variáveis globais
- Variáveis locais em funções
- Variáveis em blocos (do...end)
- Busca recursiva de variáveis até escopo global
- Exemplo:
```
local b number = 5 -- Global

function main()
  local a number = 3
  do
    local b number = 10 -- Shadowing da global
    print(b) -- Imprime 10
  end
  print(b) -- Imprime 5 (acessa a global)
end

main()
```

## Exemplos de Uso

### Exemplo 1: Função Simples com Retorno
```
function soma(x number, y number) number
  return x + y
end

function main()
  local resultado number
  resultado = soma(3, 4)
  print(resultado)  -- Imprime 7
end

main()
```

### Exemplo 2: Escopo de Variáveis
```
local b number = 5  -- Variável global

function main()
  local a number = 3
  do
    local b number = 10  -- Shadowing: sobrescreve a global neste bloco
    print(b)  -- Imprime 10 (usa a local)
  end
  print(a)  -- Imprime 3 (local de main)
  print(b)  -- Imprime 5 (volta para a global)
end

main()
```

### Exemplo 3: Múltiplos Argumentos
```
function calcula(a number, b number, c number) number
  return a + b * c
end

function main()
  print(calcula(2, 3, 4))  -- Imprime 14 (2 + 3*4)
end

main()
```

## Mudanças Implementadas

### Lexer
- ✅ Adicionado token `FUNC` para palavra-chave `function`
- ✅ Adicionado token `RETURN` para palavra-chave `return`
- ✅ Adicionado token `COMMA` para separador de argumentos `,`

### Parser
- ✅ `parseFuncDeclaration()`: Analisa declarações de funções com argumentos tipados e tipo de retorno opcional
- ✅ `parseProgram()`: Agora suporta tanto funções quanto statements globais
- ✅ `parseStatement()`: Suporta statements de `return`
- ✅ `parseAssignment()`: Distingue entre atribuição `=` e chamada de função `(`
- ✅ `parseFactor()`: Suporta chamadas de função em expressões
- ✅ `parseFunctionArguments()`: Analisa argumentos separados por vírgulas

### Symbol Table (ST)
- ✅ Adicionado atributo `parent` para suportar lista ligada reversa de escopos
- ✅ `getter()`: Busca recursiva até encontrar a variável em qualquer escopo pai
- ✅ `setter()`: Busca recursiva para encontrar e modificar a variável
- ✅ `create_variable()`: Parâmetro `isFunction` para marcar funções

### Nós da AST

#### Node Return
- Retorna o valor/tipo do evaluate() do seu único filho (expressão)

#### Node Block
- Retorna valor quando um filho executado for um `Return`
- Cria nova SymbolTable encadeada ao executar um Block como filho
- Permite escopo aninhado corretamente

#### Node FuncDec
- Armazena referência do próprio nó na SymbolTable com flag `isFunction=true`
- Armazena tipo de retorno no atributo `Value`

#### Node FuncCall
- Recupera declaração da função na SymbolTable
- Valida número de argumentos
- Cria nova SymbolTable com parent apontando para escopo de chamada
- Declara argumentos na nova SymbolTable
- Executa body e captura return se houver
- Verifica tipos dos argumentos

#### Nodes If e While
- Retornam valor se o Block interno retornar (para suportar return)

How the server will run your program:
`kotlinc -script main.kts [arguments]`

Para lancar tags:
`git tag v1.0.0`
`git push origin v1.0.0`


