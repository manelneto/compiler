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
ELLIPSIS : '...' ;

CLASS : 'class' ;
INT : 'int' ;
BOOLEAN : 'boolean' ;
PUBLIC : 'public' ;
STATIC : 'static' ;
VOID : 'void' ;
RETURN : 'return' ;
IMPORT : 'import' ;
EXTENDS : 'extends' ;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;
NEW : 'new' ;
THIS : 'this' ;

TRUE : 'true' ;
FALSE : 'false' ;
INTEGER : ('0' | [1-9][0-9]*) ;
ID : ([a-zA-Z_$][0-9a-zA-Z_$]*);

WS : [ \t\n\r\f]+ -> skip ;
MULTICOMMENTS : '/*' .*? '*/' -> skip ;
SINGLECOMMENT : '//' .*? [\n] -> skip ;

program
    : importDecl* classDecl EOF
    ;

importDecl
    : IMPORT name+=ID (DOT name+=ID)* SEMI
    ;

classDecl locals[boolean isSubclass=false]
    : CLASS name=ID
        (EXTENDS parentClassName=ID {$isSubclass=true;})?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

methodDecl locals[boolean isPublic=false, boolean isVoid=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (param (COMMA param)*)? RPAREN
        LCURLY varDecl* stmt+ RCURLY
    | (PUBLIC {$isPublic=true;})?
        STATIC VOID name=ID
        LPAREN ID LSQUARE RSQUARE paramName=ID RPAREN
        LCURLY varDecl* stmt* RCURLY {$isVoid=true;}
    ;

type locals[boolean isArray=false, boolean isEllipsis=false]
    : name=INT LSQUARE RSQUARE {$isArray=true;}
    | name=INT ELLIPSIS {$isArray=true; $isEllipsis=true;}
    | name=BOOLEAN
    | name=INT
    | name=ID
    ;

param
    : type paramName=ID
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
    | expr LSQUARE expr RSQUARE #ArrayAccess
    | expr DOT name=ID LPAREN (expr (COMMA expr)*)? RPAREN #FunctionCall
    | expr DOT name=ID #Length
    | NOT expr #UnaryExpr
    | NEW name=ID LPAREN RPAREN #NewObject
    | NEW INT LSQUARE expr RSQUARE #NewArray
    | expr op=(MUL | DIV) expr #BinaryExpr
    | expr op=(ADD | SUB) expr #BinaryExpr
    | expr op=LT expr #BinaryExpr
    | expr op=AND expr #BinaryExpr
    | LSQUARE (expr (COMMA expr)*)? RSQUARE #Array
    | value=INTEGER #IntegerLiteral
    | value=TRUE #BooleanLiteral
    | value=FALSE #BooleanLiteral
    | name=ID #VarRefExpr
    | name=THIS #This
    ;
