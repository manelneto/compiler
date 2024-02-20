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
        (EXTENDS parentClassName=ID)?
        LCURLY
        varDecl* // nomes ?
        methodDecl* // nomes ?
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (param (COMMA param)*)? RPAREN
        LCURLY varDecl* stmt* RETURN expr SEMI RCURLY #returningMethod
    | (PUBLIC {$isPublic=true;})?
        STATIC VOID MAIN
        LPAREN STRING LSQUARE RSQUARE name=ID RPAREN
        LCURLY varDecl* stmt* RCURLY #nonReturningMethod
    ;

type
    : name=INT LSQUARE RSQUARE #intArray
    | name=INT DOT DOT DOT #vararg
    | name=BOOL #bool
    | name=INT #int // <- o teste falha ?
    | name=ID #customType
    | name=STRING #string // <- prof
    ;

param
    : type name=ID
    ;

stmt
    : LCURLY stmt* RCURLY #stmtBlock
    | IF LPAREN expr RPAREN stmt ELSE stmt #ifElse
    | WHILE LPAREN expr RPAREN stmt #while
    | expr SEMI #simpleExpr
    | name=ID EQUALS expr SEMI #assignment
    | name=ID LSQUARE expr RSQUARE EQUALS expr SEMI #arrayAssignment
    ;

expr
    : LPAREN expr RPAREN #exprBlock
    | NOT expr #unaryOp
    | expr op=(MUL | DIV) expr #binaryOp
    | expr op=(ADD | SUB) expr #binaryOp
    | expr op=LT expr #binaryOp
    | expr op=AND expr #binaryOp
    | expr LSQUARE expr RSQUARE #index
    | expr DOT LENGTH #length
    | expr DOT name=ID LPAREN (expr (COMMA expr)*)? RPAREN #methodCall
    | NEW INT LSQUARE expr RSQUARE #newArray
    | NEW name=ID LPAREN RPAREN #newObject
    | LSQUARE (expr (COMMA expr)*)? RSQUARE #array
    | INTEGER #integer
    | TRUE #boolean
    | FALSE #boolean
    | name=ID #identifier
    | THIS #this
    ;
