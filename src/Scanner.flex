%%
%public
%class Yylex
%unicode
%line
%column
%type Token

%{
  // User code section
  private Token token(TokenType type, String lexeme) {
    // JFlex line/column are 0-based. Convert to 1-based.
    return new Token(type, lexeme, yyline + 1, yycolumn + 1);
  }
%}

/* Macros */
DIGIT       = [0-9]
LOWER       = [a-z]
UPPER       = [A-Z]
SIGN        = [+\-]
IDBODY      = ({LOWER}|{DIGIT}|[_ ])

/* Literals */
INT         = {SIGN}?{DIGIT}+
FLOAT       = {SIGN}?{DIGIT}+\.{DIGIT}{1,6}([eE]{SIGN}?{DIGIT}+)?

/* Strings and chars */
ESC         = \\([\"\\ntr])
STRCHAR     = [^\"\\\n\r] | {ESC}
STRING      = \"({STRCHAR})*\"

CHARESC     = \\([\'\\ntr])
CHARCHAR    = [^\'\\\n\r] | {CHARESC}
CHAR        = \'({CHARCHAR})\'

/* Comments */
SLCOMMENT   = "##"[^\n\r]*
MLCOMMENT   = "#*"([^*]|\*+[^*#])*\*+"#"

/* Whitespace */
WS          = [ \t\r\n]+

%%

/* Priority order rules */

/* 1) Comments */
{MLCOMMENT}      { /* skip */ }
{SLCOMMENT}      { /* skip */ }

/* 2) Multi-char operators */
"**"             { return token(TokenType.OPERATOR, yytext()); }
"=="             { return token(TokenType.OPERATOR, yytext()); }
"!="             { return token(TokenType.OPERATOR, yytext()); }
"<="             { return token(TokenType.OPERATOR, yytext()); }
">="             { return token(TokenType.OPERATOR, yytext()); }
"&&"             { return token(TokenType.OPERATOR, yytext()); }
"||"             { return token(TokenType.OPERATOR, yytext()); }
"++"             { return token(TokenType.OPERATOR, yytext()); }
"--"             { return token(TokenType.OPERATOR, yytext()); }
"+="             { return token(TokenType.OPERATOR, yytext()); }
"-="             { return token(TokenType.OPERATOR, yytext()); }
"*="             { return token(TokenType.OPERATOR, yytext()); }
"/="             { return token(TokenType.OPERATOR, yytext()); }

/* 3) Keywords */
"start"          { return token(TokenType.KEYWORD, yytext()); }
"finish"         { return token(TokenType.KEYWORD, yytext()); }
"loop"           { return token(TokenType.KEYWORD, yytext()); }
"condition"      { return token(TokenType.KEYWORD, yytext()); }
"declare"        { return token(TokenType.KEYWORD, yytext()); }
"output"         { return token(TokenType.KEYWORD, yytext()); }
"input"          { return token(TokenType.KEYWORD, yytext()); }
"function"       { return token(TokenType.KEYWORD, yytext()); }
"return"         { return token(TokenType.KEYWORD, yytext()); }
"break"          { return token(TokenType.KEYWORD, yytext()); }
"continue"       { return token(TokenType.KEYWORD, yytext()); }
"else"           { return token(TokenType.KEYWORD, yytext()); }

/* 4) Booleans */
"true"           { return token(TokenType.BOOLEAN_LITERAL, yytext()); }
"false"          { return token(TokenType.BOOLEAN_LITERAL, yytext()); }

/* 5) Identifier */
{UPPER}{IDBODY}{0,30}   { return token(TokenType.IDENTIFIER, yytext()); }

/* 6) Float then int */
{FLOAT}          { return token(TokenType.FLOAT_LITERAL, yytext()); }
{INT}            { return token(TokenType.INT_LITERAL, yytext()); }

/* 7) String and char */
{STRING}         { return token(TokenType.STRING_LITERAL, yytext()); }
{CHAR}           { return token(TokenType.CHAR_LITERAL, yytext()); }

/* 8) Single-char operators */
[+\-*/%<>=!]     { return token(TokenType.OPERATOR, yytext()); }

/* 9) Punctuators */
[(){}\\[\\],;:]  { return token(TokenType.PUNCTUATOR, yytext()); }

/* 10) Whitespace */
{WS}             { /* skip */ }

/* 11) Error */
.                { return token(TokenType.OPERATOR, yytext()); /* replace with error handling later */ }