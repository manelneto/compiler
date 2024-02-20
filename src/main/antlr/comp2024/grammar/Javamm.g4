grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LSQUARE : '[' ;
RSQUARE : ']' ;
MUL : '*' ;
ADD : '+' ;
DIV : '/' ;
SUB : '-' ;
AND : '&&' ;
LT : '<' ;
NOT : '!' ;
DOT : '.' ;
COMMA : ',' ;

CLASS : 'class' ;
INT : 'int' ;
STRING : 'String' ;
BOOL : 'boolean' ;
PUBLIC : 'public' ;
STATIC : 'static' ;
VOID : 'void' ;
MAIN : 'main' ;
RETURN : 'return' ;
IMPORT : 'import' ;
EXTENDS : 'extends' ;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;
LENGTH : 'length' ;
NEW : 'new' ;
THIS : 'this' ;

TRUE : 'true' ;
FALSE : 'false' ;
INTEGER : [0-9]+ ;
ID : [a-zA-Z][0-9a-zA-Z_]* ; // falta o dolar

WS : [ \t\n\r\f]+ -> skip ;

program
    : importDecl* classDecl EOF
    ;

importDecl
    : IMPORT name=ID (DOT name=ID)* SEMI
    ;

classDecl
    : CLASS name=ID
        (EXTENDS name=ID)?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (param (COMMA param)*)? RPAREN
        LCURLY varDecl* stmt* RETURN expr SEMI RCURLY
    | (PUBLIC {$isPublic=true;})?
        STATIC VOID MAIN
        LPAREN STRING LSQUARE RSQUARE name=ID RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

type
    : name=INT LSQUARE RSQUARE
    | name=INT DOT DOT DOT
    | name=BOOL
    | name=INT
    | name=ID
    | name=STRING // <- prof
    ;

param
    : type name=ID
    ;

stmt
    : LCURLY stmt* RCURLY
    | IF LPAREN expr RPAREN stmt ELSE stmt
    | WHILE LPAREN expr RPAREN stmt
    | expr SEMI
    | name=ID EQUALS expr SEMI
    | name=ID LSQUARE expr RSQUARE EQUALS expr SEMI
    ;

expr
    : LPAREN expr RPAREN
    | NOT expr
    | expr op=(MUL | DIV) expr
    | expr op=(ADD | SUB) expr
    | expr op=LT expr
    | expr op=AND expr
    | expr LSQUARE expr RSQUARE
    | expr DOT LENGTH
    | expr DOT name=ID LPAREN (expr (COMMA expr)*)? RPAREN
    | NEW INT LSQUARE expr RSQUARE
    | NEW name=ID LPAREN RPAREN
    | LSQUARE (expr (COMMA expr)*)? RSQUARE
    | INTEGER
    | TRUE
    | FALSE
    | name=ID
    | THIS
    ;



