import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ManualScanner {

    // --- DFA STATES ---
    private static final int S_START = 0;
    private static final int S_ID = 1;
    private static final int S_LOWER_WORD = 2; // Keywords/Booleans start with lowercase
    private static final int S_INT = 3;
    private static final int S_FLOAT = 4;
    private static final int S_FLOAT_EXP_START = 5;
    private static final int S_FLOAT_EXP_SIGN = 6;
    private static final int S_FLOAT_EXP_DIGIT = 7;
    private static final int S_OP = 8;              
    private static final int S_OP_MULTI = 9;        
    private static final int S_PUNCT = 10;
    private static final int S_STRING_START = 11;   
    private static final int S_STRING_CONTENT = 12; 
    private static final int S_STRING_ESC = 13;     
    private static final int S_STRING_END = 14;     
    private static final int S_CHAR_START = 15;     
    private static final int S_CHAR_CONTENT = 16;
    private static final int S_CHAR_ESC = 17;
    private static final int S_CHAR_END = 18;
    
    private static final int S_ERROR = -1;

    // --- KEYWORDS & BOOLEANS ---
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "start", "finish", "loop", "condition", "declare", "output", "input",
            "function", "return", "break", "continue", "else"
    ));
    private static final Set<String> BOOLEANS = new HashSet<>(Arrays.asList("true", "false"));

    // --- FIELDS ---
    private final ErrorHandler errorHandler = new ErrorHandler();
    private final SymbolTable symbolTable = new SymbolTable();
    private final Map<TokenType, Integer> counts = new EnumMap<>(TokenType.class);
    private int commentsRemoved = 0;

    // --- MAIN SCAN METHOD ---
    public List<Token> scan(String src) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        int line = 1;
        int col = 1;
        int len = src.length();

        while (pos < len) {
            char c = src.charAt(pos);

            // 1. Skip Whitespace
            if (isWhitespace(c)) {
                if (c == '\n') { line++; col = 1; }
                else { col++; }
                pos++;
                continue;
            }

            // 2. BONUS: Nested Multi-line Comments (#*)
            if (pos + 1 < len && src.substring(pos, pos + 2).equals("#*")) {
                int[] res = scanMultiLineComment(src, pos, line, col);
                pos = res[0];
                line = res[1];
                col = res[2];
                commentsRemoved++; 
                continue;
            }

            // 3. FIX: Single-line Comments (##)
            // Explicitly handled here to ensure they are skipped before the DFA sees them
            if (pos + 1 < len && src.substring(pos, pos + 2).equals("##")) {
                int[] res = scanSingleLineComment(src, pos, line, col);
                pos = res[0];
                line = res[1];
                col = res[2];
                commentsRemoved++;
                continue;
            }

            // 4. DFA DRIVER
            int startPos = pos;
            int startLine = line;
            int startCol = col;
            
            int currentState = S_START;
            int lastAcceptingState = -1;
            int lastAcceptingPos = -1;
            int currentPos = pos;
            
            // We track furthest state to give better error messages
            int furthestState = S_START;

            while (currentPos < len) {
                char inputChar = src.charAt(currentPos);
                int next = nextState(currentState, inputChar);
                
                if (next == S_ERROR) break; 
                
                currentState = next;
                furthestState = currentState;
                
                if (isAccepting(currentState)) {
                    lastAcceptingState = currentState;
                    lastAcceptingPos = currentPos;
                }
                
                currentPos++;
            }

            // 5. Token Generation
            if (lastAcceptingState != -1) {
                int tokenLen = (lastAcceptingPos - startPos) + 1;
                String lexeme = src.substring(startPos, startPos + tokenLen);
                
                TokenType type = mapStateToType(lastAcceptingState);
                
                // Refine Identifiers (Keywords/Booleans/Validation)
                if (type == TokenType.IDENTIFIER) {
                    if (KEYWORDS.contains(lexeme)) {
                        type = TokenType.KEYWORD;
                    } else if (BOOLEANS.contains(lexeme)) {
                        type = TokenType.BOOLEAN_LITERAL;
                    } else {
                        // Check Identifier Rules (Must start with Uppercase)
                        if (!Character.isUpperCase(lexeme.charAt(0))) {
                             errorHandler.report("INVALID_IDENTIFIER", startLine, startCol, lexeme, "Must start with Uppercase");
                        } else {
                             symbolTable.recordIdentifier(lexeme, startLine, startCol);
                        }
                    }
                }

                Token token = new Token(type, lexeme, startLine, startCol);
                tokens.add(token);
                counts.put(type, counts.getOrDefault(type, 0) + 1);

                // Update Position
                for (int k = 0; k < tokenLen; k++) {
                    if (src.charAt(startPos + k) == '\n') { line++; col = 1; }
                    else col++;
                }
                pos = startPos + tokenLen;

            } else {
                // ERROR RECOVERY - Improved
                String badChar = String.valueOf(src.charAt(pos));
                
                // Check if we failed inside a string or char to give a better error
                if (furthestState == S_STRING_CONTENT || furthestState == S_STRING_ESC || furthestState == S_STRING_START) {
                    errorHandler.report("MALFORMED_LITERAL", startLine, startCol, "\"", "Unclosed string literal");
                } else if (furthestState == S_CHAR_CONTENT || furthestState == S_CHAR_ESC || furthestState == S_CHAR_START) {
                    errorHandler.report("MALFORMED_LITERAL", startLine, startCol, "'", "Unclosed character literal");
                } else {
                    errorHandler.report("INVALID_CHARACTER", line, col, badChar, "No token starts with this character");
                }
                
                pos++;
                col++;
            }
        }

        Token eof = new Token(TokenType.EOF, "EOF", line, col);
        tokens.add(eof);
        counts.put(TokenType.EOF, counts.getOrDefault(TokenType.EOF, 0) + 1);
        
        return tokens;
    }

    // --- TRANSITION FUNCTION ---
    private int nextState(int state, char c) {
        switch (state) {
            case S_START:
                if (Character.isDigit(c)) return S_INT;
                if (Character.isUpperCase(c)) return S_ID; 
                if (Character.isLowerCase(c)) return S_LOWER_WORD; 
                if (c == '"') return S_STRING_START;
                if (c == '\'') return S_CHAR_START;
                
                // Note: # is handled in the main loop (comments)
                
                if ("*=!<>+ -".indexOf(c) != -1) return S_OP_MULTI;
                if (c == '&' || c == '|') return S_OP_MULTI; 
                if ("(){}[],;:".indexOf(c) != -1) return S_PUNCT;
                if ("/%".indexOf(c) != -1) return S_OP;
                return S_ERROR;

            case S_ID: 
                if (Character.isLetterOrDigit(c) || c == '_') return S_ID;
                return S_ERROR;
            case S_LOWER_WORD: 
                if (Character.isLetterOrDigit(c) || c == '_') return S_LOWER_WORD;
                return S_ERROR;

            case S_INT:
                if (Character.isDigit(c)) return S_INT;
                if (c == '.') return S_FLOAT;
                return S_ERROR;
            case S_FLOAT:
                if (Character.isDigit(c)) return S_FLOAT;
                if (c == 'e' || c == 'E') return S_FLOAT_EXP_START;
                return S_ERROR;
            case S_FLOAT_EXP_START:
                if (c == '+' || c == '-') return S_FLOAT_EXP_SIGN;
                if (Character.isDigit(c)) return S_FLOAT_EXP_DIGIT;
                return S_ERROR;
            case S_FLOAT_EXP_SIGN:
            case S_FLOAT_EXP_DIGIT:
                if (Character.isDigit(c)) return S_FLOAT_EXP_DIGIT;
                return S_ERROR;

            case S_STRING_START:
            case S_STRING_CONTENT:
                if (c == '"') return S_STRING_END;
                if (c == '\\') return S_STRING_ESC;
                return S_STRING_CONTENT; 
            case S_STRING_ESC:
                return S_STRING_CONTENT;

            case S_CHAR_START:
                if (c == '\\') return S_CHAR_ESC;
                if (c == '\'') return S_ERROR; 
                return S_CHAR_CONTENT;
            case S_CHAR_CONTENT:
                if (c == '\'') return S_CHAR_END;
                return S_ERROR; 
            case S_CHAR_ESC:
                return S_CHAR_CONTENT;

            case S_OP_MULTI:
                if (c == '=' || c == '+' || c == '-' || c == '*' || c == '&' || c == '|') return S_OP;
                return S_ERROR;
                
            default:
                return S_ERROR;
        }
    }

    private boolean isAccepting(int state) {
        return state == S_ID || state == S_LOWER_WORD || state == S_INT || 
               state == S_FLOAT || state == S_FLOAT_EXP_DIGIT || 
               state == S_OP || state == S_OP_MULTI || state == S_PUNCT || 
               state == S_STRING_END || state == S_CHAR_END;
    }

    private TokenType mapStateToType(int state) {
        if (state == S_ID || state == S_LOWER_WORD) return TokenType.IDENTIFIER;
        if (state == S_INT) return TokenType.INT_LITERAL;
        if (state == S_FLOAT || state == S_FLOAT_EXP_DIGIT) return TokenType.FLOAT_LITERAL;
        if (state == S_STRING_END) return TokenType.STRING_LITERAL;
        if (state == S_CHAR_END) return TokenType.CHAR_LITERAL;
        if (state == S_PUNCT) return TokenType.PUNCTUATOR;
        return TokenType.OPERATOR;
    }

    // --- COMMENT HANDLERS ---
    
    // Single Line: ## ...
    private int[] scanSingleLineComment(String src, int pos, int line, int col) {
        pos += 2; col += 2; // skip ##
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '\n') {
                return new int[]{pos, line, col};
            }
            pos++; col++;
        }
        return new int[]{pos, line, col};
    }

    // Multi Line: #* ... *#
    private int[] scanMultiLineComment(String src, int pos, int line, int col) {
        int depth = 1;
        pos += 2; col += 2; // skip #*
        while (pos < src.length()) {
            if (pos + 1 < src.length() && src.charAt(pos) == '#' && src.charAt(pos + 1) == '*') {
                depth++;
                pos += 2; col += 2;
                continue;
            }
            if (pos + 1 < src.length() && src.charAt(pos) == '*' && src.charAt(pos + 1) == '#') {
                depth--;
                pos += 2; col += 2;
                if (depth == 0) return new int[]{pos, line, col};
                continue;
            }
            if (src.charAt(pos) == '\n') { line++; col = 1; }
            else { col++; }
            pos++;
        }
        errorHandler.report("UNCLOSED_COMMENT", line, col, "EOF", "Multi-line comment not closed");
        return new int[]{pos, line, col};
    }

    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private void printStats(List<Token> tokens) {
        int maxLine = 1;
        for (Token t : tokens) maxLine = Math.max(maxLine, t.line);

        System.out.println("\n--- STATISTICS ---");
        System.out.println("Total tokens: " + tokens.size());
        System.out.println("Lines processed: " + maxLine);
        System.out.println("Comments removed: " + commentsRemoved);

        System.out.println("\nToken counts:");
        for (TokenType tt : TokenType.values()) {
            int v = counts.getOrDefault(tt, 0);
            if (v > 0) System.out.println("  " + tt + ": " + v);
        }
    }
    
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
             System.out.println("Usage: java ManualScanner <file.lang>");
             return;
        }
        String src = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        ManualScanner scanner = new ManualScanner();
        List<Token> tokens = scanner.scan(src);
        
        for (Token t : tokens) System.out.println(t);
        
        scanner.printStats(tokens);
        scanner.symbolTable.print();

        if (scanner.errorHandler.hasErrors()) {
            System.out.println("\n--- LEXICAL ERRORS ---");
            scanner.errorHandler.printErrors();
        }
    }
}