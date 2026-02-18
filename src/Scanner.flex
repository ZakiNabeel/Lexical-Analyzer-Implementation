%%
%public
%class Yylex
%unicode
%line
%column
%type Token

/* --- 1. Custom Code & Variables --- */
%{
  // NEW: Depth counter for nested comments
  private int commentDepth = 0;

  private Token token(TokenType type, String lexeme) {
    // JFlex line/column are 0-based. Convert to 1-based.
    return new Token(type, lexeme, yyline + 1, yycolumn + 1);
  }

  public int getLine() { return yyline + 1; }
  public int getColumn() { return yycolumn + 1; }
%}

/* --- 2. States --- */
// NEW: Define a state for handling nested comments
%state COMMENT

/* --- 3. Macros --- */
DIGIT       = [0-9]
LOWER       = [a-z]
UPPER       = [A-Z]
SIGN        = [+\-]
IDBODY      = ({LOWER}|{DIGIT}|[_])

/* Literals */
INT         = {SIGN}?{DIGIT}+
FLOAT       = {SIGN}?{DIGIT}+\.{DIGIT}{1,6}([eE]{SIGN}?{DIGIT}+)?

/* Strings and chars */
// FIX: Removed \n and \r from excluded list to allow multi-line strings
ESC         = \\([\"\\ntr])
STRCHAR     = [^\"\\\\] | {ESC} 
STRING      = \"({STRCHAR})*\"

CHARESC     = \\([\'\\ntr])
CHARCHAR    = [^\'\\\n\r] | {CHARESC}
CHAR        = \'({CHARCHAR})\'

/* Error Detection Macros */
TOO_LONG_FLOAT  = {SIGN}?{DIGIT}+\.{DIGIT}{7}{DIGIT}*([eE]{SIGN}?{DIGIT}+)?
MALFORMED_FLOAT = {DIGIT}+\.{DIGIT}+\.{DIGIT}+
INVALID_ID      = {LOWER}[a-zA-Z0-9_]*
UNCLOSED_STR    = \"[^\n\r\"]*
MALFORMED_CHAR  = \'[^\'\n\r]{2}[^\'\n\r]*\'
UNCLOSED_CHAR   = \'[^\'\n\r]*
SLCOMMENT       = "##"[^\n\r]*

/* Whitespace */
WS          = [ \t\r\n]+

%%

/* --- 4. Rules --- */

/* 1) Nested Multi-line Comments (State-Based) */
// NEW: Logic to handle #* ... *# with nesting
<YYINITIAL> "#*" { 
    yybegin(COMMENT); 
    commentDepth = 1; 
}

<COMMENT> {
  "#*" { commentDepth++; }
  "*#" { 
      commentDepth--; 
      if (commentDepth == 0) yybegin(YYINITIAL); 
  }
  <<EOF>> { 
      System.out.println("[ERROR] UNCLOSED_MULTILINE_COMMENT at Line " + getLine() + ", Col " + getColumn() + 
                         " | Reason: No closing *# found (EOF reached).");
      return token(TokenType.EOF, "EOF"); // Stop processing
  }
  [^] { /* ignore content inside comments */ }
}

/* Single Line Comments */
<YYINITIAL> {SLCOMMENT} { /* skip */ }

/* 2) Multi-char operators */
<YYINITIAL> {
  "**" | "==" | "!=" | "<=" | ">=" |
  "&&" | "||" | "++" | "--" | "+=" | "-=" | "*=" |
  "/=" { return token(TokenType.OPERATOR, yytext()); }
}

/* 3) Keywords */
<YYINITIAL> {
  "start" | "finish" | "loop" | "condition" |
  "declare" | "output" | "input" | "function" | "return" | "break" | "continue" |
  "else" { return token(TokenType.KEYWORD, yytext()); }
}

/* 4) Booleans */
<YYINITIAL> {
  "true" | "false" { return token(TokenType.BOOLEAN_LITERAL, yytext()); }
}

/* 5) Identifier */
<YYINITIAL> {
  {UPPER}{IDBODY}{0,30} { return token(TokenType.IDENTIFIER, yytext()); }
}

/* 6) Float then int */
<YYINITIAL> {
  {FLOAT} { return token(TokenType.FLOAT_LITERAL, yytext()); }
  {INT}   { return token(TokenType.INT_LITERAL, yytext()); }
}

/* 7) String and char */
<YYINITIAL> {
  {STRING} { return token(TokenType.STRING_LITERAL, yytext()); }
  {CHAR}   { return token(TokenType.CHAR_LITERAL, yytext()); }
}

/* 8) Single-char operators */
<YYINITIAL> {
  [+\-*/%<>=!] { return token(TokenType.OPERATOR, yytext()); }
}

/* 9) Punctuators */
<YYINITIAL> {
  [(){}\\[\\],;:] { return token(TokenType.PUNCTUATOR, yytext()); }
}

/* 10) Whitespace */
<YYINITIAL> {
  {WS} { /* skip */ }
}

/* 11) Error Handling Rules */
<YYINITIAL> {
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

  /* Fallback Error */
  . { 
      System.out.println("[ERROR] INVALID_CHARACTER at Line " + getLine() + ", Col " + getColumn() + 
                         " | Lexeme: \"" + yytext() + "\"");
  }
}