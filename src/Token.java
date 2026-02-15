public class Token {
    public final TokenType type;
    public final String lexeme;
    public final int line;
    public final int col;

    public Token(TokenType type, String lexeme, int line, int col) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.col = col;
    }

    @Override
    public String toString() {
        // Required format example: <KEYWORD, "start", Line: 1, Col: 1>
        return "<" + type + ", \"" + lexeme + "\", Line: " + line + ", Col: " + col + ">";
    }
}