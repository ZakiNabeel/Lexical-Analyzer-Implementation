import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ErrorHandler {

    public static class LexError {
        public final String type;
        public final int line;
        public final int col;
        public final String lexeme;
        public final String reason;

        public LexError(String type, int line, int col, String lexeme, String reason) {
            this.type = type;
            this.line = line;
            this.col = col;
            this.lexeme = lexeme;
            this.reason = reason;
        }

        @Override
        public String toString() {
            return "[ERROR] " + type + " at Line " + line + ", Col " + col +
                    " | Lexeme: \"" + lexeme + "\" | Reason: " + reason;
        }
    }

    private final List<LexError> errors = new ArrayList<>();

    public void report(String type, int line, int col, String lexeme, String reason) {
        errors.add(new LexError(type, line, col, lexeme, reason));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<LexError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public void printErrors() {
        for (LexError e : errors) System.out.println(e);
    }
}