import java.io.FileReader;
import java.io.IOException;

public class JFlexRunner {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java JFlexRunner <file.lang>");
            return;
        }

        try {
            Yylex scanner = new Yylex(new FileReader(args[0]));
            Token token;
            
            System.out.println("--- JFlex Scanner Output ---");
            while ((token = scanner.yylex()) != null && token.type != TokenType.EOF) {
                System.out.println(token);
            }
            // Corrected: Use getLine() and getColumn()
            System.out.println(new Token(TokenType.EOF, "EOF", scanner.getLine(), scanner.getColumn()));
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Error e) {
            System.err.println("Lexical Error: " + e.getMessage());
        }
    }
}