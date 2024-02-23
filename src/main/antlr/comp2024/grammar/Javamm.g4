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
THREEDOTS : '...' ;

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
ID : [a-zA-Z][0-9a-zA-Z_$]* ; //dolar

WS : [ \t\n\r\f]+ -> skip ;
MULTICOMMENTS : '/*' (ID | WS)* '*/' -> skip ;
SINGLECOMMENT : '//' (ID | [ \t\r\f])* [\n] -> skip ;

program
    : importDecl* classDecl EOF
    ;

importDecl
    : IMPORT name=ID (DOT name=ID)* SEMI // name 2 vezes? como tratar tudo?
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
        LCURLY varDecl* stmt* RETURN expr SEMI RCURLY // #ReturningMethod
    | (PUBLIC {$isPublic=true;})?
        STATIC VOID MAIN
        LPAREN STRING LSQUARE RSQUARE name=ID RPAREN
        LCURLY varDecl* stmt* RCURLY // #NonReturningMethod
    ;

type
    : name=INT LSQUARE RSQUARE // #IntArrayType
    | name=INT THREEDOTS // #VarargTyoe
    | name=BOOL // #BoolType
    | name=INT // #IntType // <- o teste falha ?
    | name=ID // #CustomType
    | name=STRING // #StringType // <- prof
    ;

param
    : type name=ID
    ;

stmt
    : LCURLY stmt* RCURLY #StmtBlock
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfElseStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | expr SEMI #SimpleStmt
    | name=ID EQUALS expr SEMI #AssignStmt
    | name=ID LSQUARE expr RSQUARE EQUALS expr SEMI #ArrayAssignStmt
    | RETURN expr SEMI #ReturnStmt
    ;

expr
    : LPAREN expr RPAREN #ParenExpr
    | NOT expr #UnaryExpr
    | expr op=(MUL | DIV) expr #BinaryExpr
    | expr op=(ADD | SUB) expr #BinaryExpr
    | expr op=LT expr #BinaryExpr
    | expr op=AND expr #BinaryExpr
    | expr LSQUARE expr RSQUARE #Index
    | expr DOT LENGTH #Length
    | expr DOT name=ID LPAREN (expr (COMMA expr)*)? RPAREN #MethodCall
    | NEW INT LSQUARE expr RSQUARE #NewArray
    | NEW name=ID LPAREN RPAREN #NewObject
    | LSQUARE (expr (COMMA expr)*)? RSQUARE #Array
    | value=INTEGER #IntegerLiteral
    | value=TRUE #BooleanLiteral
    | value=FALSE #BooleanLiteral
    | name=ID #VarRefExpr
    | THIS #This
    ;
