%%
%public
%class Yylex
%unicode
%line
%column
%type Token

%{
  private Token token(TokenType type, String lexeme) {
    // JFlex line/column are 0-based. Convert to 1-based.
    return new Token(type, lexeme, yyline + 1, yycolumn + 1);
  }

  // Helper methods for accessing line and column from Runner
  public int getLine() { return yyline + 1; }
  public int getColumn() { return yycolumn + 1; }
%}

/* Macros */
DIGIT       = [0-9]
LOWER       = [a-z]
UPPER       = [A-Z]
SIGN        = [+\-]
IDBODY      = ({LOWER}|{DIGIT}|[_])

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

/* Error Detection Macros (Fixed for JFlex syntax and \r formatting) */

// Float with 7+ decimals (Fixed syntax: {7} followed by more digits)
TOO_LONG_FLOAT  = {SIGN}?{DIGIT}+\.{DIGIT}{7}{DIGIT}*([eE]{SIGN}?{DIGIT}+)?

// Float with multiple dots (e.g. 1.2.3)
MALFORMED_FLOAT = {DIGIT}+\.{DIGIT}+\.{DIGIT}+

// Identifier starting with lowercase
INVALID_ID      = {LOWER}[a-zA-Z0-9_]*

// String hitting newline/return (Added \r to fix display bug)
UNCLOSED_STR    = \"[^\n\r\"]*

// Char with >1 character (e.g. 'TooLong') (Fixed syntax)
MALFORMED_CHAR  = \'[^\'\n\r]{2}[^\'\n\r]*\'

// Unclosed Char (e.g. 'A )
UNCLOSED_CHAR   = \'[^\'\n\r]*

// Unclosed Multi-line Comment
UNCLOSED_COMMENT = "#*"([^*]|\*+[^*#])*

/* Comments */
SLCOMMENT   = "##"[^\n\r]*
MLCOMMENT   = "#*"([^*]|\*+[^*#])*\*+"#"

/* Whitespace */
WS          = [ \t\r\n]+

%%

/* 1) Comments */
{MLCOMMENT}      { /* skip */ }
{SLCOMMENT}      { /* skip */ }

/* 2) Multi-char operators */
"**" | "==" | "!=" | "<=" | ">=" | "&&" | "||" | "++" | "--" | "+=" | "-=" | "*=" | "/=" { 
    return token(TokenType.OPERATOR, yytext()); 
}

/* 3) Keywords */
"start" | "finish" | "loop" | "condition" | "declare" | "output" | "input" | "function" | "return" | "break" | "continue" | "else" { 
    return token(TokenType.KEYWORD, yytext()); 
}

/* 4) Booleans */
"true" | "false" { return token(TokenType.BOOLEAN_LITERAL, yytext()); }

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

/* 11) Error Handling Rules */

{TOO_LONG_FLOAT} { 
    System.out.println("[ERROR] MALFORMED_LITERAL at Line " + getLine() + ", Col " + getColumn() + 
                       " | Lexeme: \"" + yytext() + "\" | Reason: Float exceeds 6 decimal places.");
}

{MALFORMED_FLOAT} { 
    System.out.println("[ERROR] MALFORMED_LITERAL at Line " + getLine() + ", Col " + getColumn() + 
                       " | Lexeme: \"" + yytext() + "\" | Reason: Multiple decimal points.");
}

{INVALID_ID} { 
    System.out.println("[ERROR] INVALID_IDENTIFIER at Line " + getLine() + ", Col " + getColumn() + 
                       " | Lexeme: \"" + yytext() + "\" | Reason: Identifiers must start with Uppercase.");
}

{UNCLOSED_STR} {
    System.out.println("[ERROR] MALFORMED_LITERAL at Line " + getLine() + ", Col " + getColumn() + 
                       " | Lexeme: \"" + yytext() + "\" | Reason: Unclosed string literal.");
}

{MALFORMED_CHAR} {
    System.out.println("[ERROR] MALFORMED_LITERAL at Line " + getLine() + ", Col " + getColumn() + 
                       " | Lexeme: \"" + yytext() + "\" | Reason: Character literal too long.");
}

{UNCLOSED_CHAR} {
    System.out.println("[ERROR] MALFORMED_LITERAL at Line " + getLine() + ", Col " + getColumn() + 
                       " | Lexeme: \"" + yytext() + "\" | Reason: Unclosed character literal.");
}

{UNCLOSED_COMMENT} {
    System.out.println("[ERROR] UNCLOSED_MULTILINE_COMMENT at Line " + getLine() + ", Col " + getColumn() + 
                       " | Lexeme: \"" + yytext() + "\" | Reason: No closing *# found.");
}

/* 12) Fallback Error */
. { 
    System.out.println("[ERROR] INVALID_CHARACTER at Line " + getLine() + ", Col " + getColumn() + 
                       " | Lexeme: \"" + yytext() + "\"");
}