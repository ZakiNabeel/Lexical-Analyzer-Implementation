import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.*;

public class ManualScanner {

    // 3.1 Keywords (case-sensitive)
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "start", "finish", "loop", "condition", "declare", "output", "input",
            "function", "return", "break", "continue", "else"
    ));

    // 3.7 Boolean literals (case-sensitive)
    private static final Set<String> BOOLEANS = new HashSet<>(Arrays.asList("true", "false"));

    // Multi-char operators (must be checked before single-char ops)
    private static final String[] MULTI_OPS = {
            "**", "==", "!=", "<=", ">=", "&&", "||",
            "++", "--",
            "+=", "-=", "*=", "/="
    };

    private static final Set<Character> SINGLE_OPS = new HashSet<>(Arrays.asList(
            '+', '-', '*', '/', '%', '<', '>', '!', '='
    ));

    private static final Set<Character> PUNCTUATORS = new HashSet<>(Arrays.asList(
            '(', ')', '{', '}', '[', ']', ',', ';', ':'
    ));

    private final ErrorHandler errorHandler = new ErrorHandler();
    private final SymbolTable symbolTable = new SymbolTable();
    private final Map<TokenType, Integer> counts = new EnumMap<>(TokenType.class);

    private int commentsRemoved = 0;

    private static class Cursor {
        int i = 0;
        int line = 1;
        int col = 1;
    }

    private static class Match {
        final TokenType type;
        final int len;
        final int priority;
        Match(TokenType type, int len, int priority) {
            this.type = type;
            this.len = len;
            this.priority = priority;
        }
    }

    private static class Priority {
        // smaller number means higher priority
        static final int MULTI_OP = 1;
        static final int KEYWORD = 2;
        static final int BOOLEAN = 3;
        static final int IDENTIFIER = 4;
        static final int FLOAT = 5;
        static final int INT = 6;
        static final int STRING = 7;
        static final int CHAR = 7;
        static final int SINGLE_OP = 8;
        static final int PUNCT = 9;
    }

    public List<Token> scan(String src) {
        List<Token> tokens = new ArrayList<>();
        Cursor c = new Cursor();

        while (c.i < src.length()) {

            // Skip whitespace, but track line and col
            if (isWhitespace(src.charAt(c.i))) {
                consumeWhitespace(src, c);
                continue;
            }

            // Multi-line comment: #* ... *#
            if (startsWith(src, c.i, "#*")) {
                int startLine = c.line, startCol = c.col;
                int end = src.indexOf("*#", c.i + 2);
                if (end == -1) {
                    String lex = src.substring(c.i);
                    errorHandler.report("UNCLOSED_MULTILINE_COMMENT", startLine, startCol, lex,
                            "No closing *# found.");
                    advanceByString(lex, c);
                    break;
                } else {
                    String lex = src.substring(c.i, end + 2);
                    advanceByString(lex, c);
                    commentsRemoved++;
                    continue;
                }
            }

            // Single-line comment: ## ... endline
            if (startsWith(src, c.i, "##")) {
                int start = c.i;
                int nl = findLineEnd(src, c.i);
                String lex = src.substring(start, nl);
                advanceByString(lex, c);
                commentsRemoved++;
                continue;
            }

            int tokenLine = c.line;
            int tokenCol = c.col;

            // Special handling: if starts with quote, try parse string/char first so we can report precise errors
            if (src.charAt(c.i) == '"') {
                ParseResult pr = parseString(src, c.i);
                if (!pr.ok) {
                    errorHandler.report("MALFORMED_LITERAL", tokenLine, tokenCol, pr.lexeme, pr.reason);
                    advanceByString(pr.lexeme, c);
                    continue;
                }
            }
            if (src.charAt(c.i) == '\'' || src.charAt(c.i) == '’') {
                ParseResult pr = parseChar(src, c.i);
                if (!pr.ok) {
                    errorHandler.report("MALFORMED_LITERAL", tokenLine, tokenCol, pr.lexeme, pr.reason);
                    advanceByString(pr.lexeme, c);
                    continue;
                }
            }

            Match best = null;

            // Priority order selection (longest match, then priority)
            best = pickBest(best, matchMultiOp(src, c.i), Priority.MULTI_OP);
            best = pickBest(best, matchKeyword(src, c.i), Priority.KEYWORD);
            best = pickBest(best, matchBoolean(src, c.i), Priority.BOOLEAN);
            best = pickBest(best, matchIdentifier(src, c.i), Priority.IDENTIFIER);
            best = pickBest(best, matchFloat(src, c.i), Priority.FLOAT);
            best = pickBest(best, matchInt(src, c.i), Priority.INT);
            best = pickBest(best, matchString(src, c.i), Priority.STRING);
            best = pickBest(best, matchChar(src, c.i), Priority.CHAR);
            best = pickBest(best, matchSingleOp(src, c.i), Priority.SINGLE_OP);
            best = pickBest(best, matchPunctuator(src, c.i), Priority.PUNCT);

            if (best == null || best.len <= 0) {
                // Recovery: invalid character or invalid identifier-like chunk
                char bad = src.charAt(c.i);

                if (Character.isLetterOrDigit(bad) || bad == '_' || bad == ' ') {
                    int j = c.i;
                    while (j < src.length()) {
                        char ch = src.charAt(j);
                        if (isWhitespace(ch) || isDelimiter(ch)) break;
                        j++;
                    }
                    String lex = src.substring(c.i, j);
                    errorHandler.report("INVALID_TOKEN", tokenLine, tokenCol, lex,
                            "Unrecognised token starting here.");
                    advanceByString(lex, c);
                } else {
                    errorHandler.report("INVALID_CHARACTER", tokenLine, tokenCol, String.valueOf(bad),
                            "Character does not belong to any token.");
                    advanceByString(String.valueOf(bad), c);
                }
                continue;
            }

            String lexeme = src.substring(c.i, c.i + best.len);

            // Identifier max length 31 check (strict)
            if (best.type == TokenType.IDENTIFIER && lexeme.length() > 31) {
                errorHandler.report("INVALID_IDENTIFIER", tokenLine, tokenCol, lexeme,
                        "Identifier exceeds max length 31.");
                advanceByString(lexeme, c);
                continue;
            }

            Token tok = new Token(best.type, lexeme, tokenLine, tokenCol);
            tokens.add(tok);
            counts.merge(tok.type, 1, Integer::sum);

            if (tok.type == TokenType.IDENTIFIER) {
                symbolTable.recordIdentifier(tok.lexeme, tok.line, tok.col);
            }

            advanceByString(lexeme, c);
        }

        Token eof = new Token(TokenType.EOF, "EOF", 1, 1);
        tokens.add(eof);
        counts.merge(TokenType.EOF, 1, Integer::sum);

        return tokens;
    }

    // ---------- Matching ----------

    private static Match pickBest(Match current, Match candidate, int candidatePriority) {
        if (candidate == null || candidate.len <= 0) return current;
        Match c = new Match(candidate.type, candidate.len, candidatePriority);

        if (current == null) return c;

        if (c.len > current.len) return c;
        if (c.len == current.len && c.priority < current.priority) return c;

        return current;
    }

    private static Match matchMultiOp(String s, int i) {
        for (String op : MULTI_OPS) {
            if (startsWith(s, i, op)) return new Match(TokenType.OPERATOR, op.length(), Priority.MULTI_OP);
        }
        return null;
    }

    private static Match matchSingleOp(String s, int i) {
        char ch = s.charAt(i);
        if (SINGLE_OPS.contains(ch)) return new Match(TokenType.OPERATOR, 1, Priority.SINGLE_OP);
        return null;
    }

    private static Match matchPunctuator(String s, int i) {
        char ch = s.charAt(i);
        if (PUNCTUATORS.contains(ch)) return new Match(TokenType.PUNCTUATOR, 1, Priority.PUNCT);
        return null;
    }

    private static Match matchKeyword(String s, int i) {
        for (String kw : KEYWORDS) {
            if (startsWith(s, i, kw) && isWordBoundary(s, i, kw.length())) {
                return new Match(TokenType.KEYWORD, kw.length(), Priority.KEYWORD);
            }
        }
        return null;
    }

    private static Match matchBoolean(String s, int i) {
        for (String b : BOOLEANS) {
            if (startsWith(s, i, b) && isWordBoundary(s, i, b.length())) {
                return new Match(TokenType.BOOLEAN_LITERAL, b.length(), Priority.BOOLEAN);
            }
        }
        return null;
    }

    private static Match matchIdentifier(String s, int i) {
        char first = s.charAt(i);
        if (!(first >= 'A' && first <= 'Z')) return null;

        int j = i + 1;
        int usedExtra = 0;

        while (j < s.length() && usedExtra < 30) {
            char ch = s.charAt(j);
            // Spec conflict in PDF: it mentions underscore in rules, and regex line can look odd.
            // We accept lowercase, digits, underscore, and space (space is uncommon but was shown in the given regex).
            if (isLower(ch) || Character.isDigit(ch) || ch == '_') {
                j++;
                usedExtra++;
            } else break;
        }

        // avoid consuming trailing spaces if present
        while (j > i + 1 && s.charAt(j - 1) == ' ') j--;

        return new Match(TokenType.IDENTIFIER, j - i, Priority.IDENTIFIER);
    }

    private static Match matchInt(String s, int i) {
        int j = i;

        if (s.charAt(j) == '+' || s.charAt(j) == '-') {
            if (j + 1 >= s.length() || !Character.isDigit(s.charAt(j + 1))) return null;
            j++;
        }

        int startDigits = j;
        while (j < s.length() && Character.isDigit(s.charAt(j))) j++;

        if (j == startDigits) return null;

        // if next char is '.', it is probably a float, so do not match int here
        if (j < s.length() && s.charAt(j) == '.') return null;

        return new Match(TokenType.INT_LITERAL, j - i, Priority.INT);
    }

    private static Match matchFloat(String s, int i) {
        int j = i;

        if (s.charAt(j) == '+' || s.charAt(j) == '-') {
            if (j + 1 >= s.length() || !Character.isDigit(s.charAt(j + 1))) return null;
            j++;
        }

        int intStart = j;
        while (j < s.length() && Character.isDigit(s.charAt(j))) j++;
        if (j == intStart) return null;

        if (j >= s.length() || s.charAt(j) != '.') return null;
        j++;

        int fracCount = 0;
        while (j < s.length() && Character.isDigit(s.charAt(j)) && fracCount < 6) {
            j++;
            fracCount++;
        }
        if (fracCount < 1) return null;

        // if more digits exist beyond 6 decimals, we treat float match as failed (so error handler can catch)
        if (j < s.length() && Character.isDigit(s.charAt(j))) return null;

        // optional exponent
        if (j < s.length() && (s.charAt(j) == 'e' || s.charAt(j) == 'E')) {
            int k = j + 1;
            if (k < s.length() && (s.charAt(k) == '+' || s.charAt(k) == '-')) k++;
            int expStart = k;
            while (k < s.length() && Character.isDigit(s.charAt(k))) k++;
            if (k == expStart) return null;
            j = k;
        }

        return new Match(TokenType.FLOAT_LITERAL, j - i, Priority.FLOAT);
    }

    private static Match matchString(String s, int i) {
        ParseResult pr = parseString(s, i);
        if (!pr.ok) return null;
        return new Match(TokenType.STRING_LITERAL, pr.len, Priority.STRING);
    }

    private static Match matchChar(String s, int i) {
        ParseResult pr = parseChar(s, i);
        if (!pr.ok) return null;
        return new Match(TokenType.CHAR_LITERAL, pr.len, Priority.CHAR);
    }

    // ---------- String and char parsing with error recovery ----------

    private static class ParseResult {
        final boolean ok;
        final int len;
        final String lexeme;
        final String reason;

        ParseResult(boolean ok, int len, String lexeme, String reason) {
            this.ok = ok;
            this.len = len;
            this.lexeme = lexeme;
            this.reason = reason;
        }
    }

    private static ParseResult parseString(String s, int i) {
        if (s.charAt(i) != '"') return new ParseResult(false, 0, "", "Not a string.");

        int j = i + 1;
        while (j < s.length()) {
            char ch = s.charAt(j);

            if (ch == '\n' || ch == '\r') {
                String lex = s.substring(i, j);
                return new ParseResult(false, lex.length(), lex, "Unterminated string literal (newline encountered).");
            }

            if (ch == '"') {
                String lex = s.substring(i, j + 1);
                return new ParseResult(true, lex.length(), lex, "");
            }

            if (ch == '\\') {
                if (j + 1 >= s.length()) {
                    String lex = s.substring(i);
                    return new ParseResult(false, lex.length(), lex, "Unterminated string literal (EOF after escape).");
                }
                char esc = s.charAt(j + 1);
                if (!(esc == '"' || esc == '\\' || esc == 'n' || esc == 't' || esc == 'r')) {
                    String lex = s.substring(i, Math.min(j + 2, s.length()));
                    return new ParseResult(false, lex.length(), lex, "Invalid escape sequence: \\" + esc);
                }
                j += 2;
                continue;
            }

            j++;
        }

        String lex = s.substring(i);
        return new ParseResult(false, lex.length(), lex, "Unterminated string literal (EOF reached).");
    }

    private static ParseResult parseChar(String s, int i) {
        char q = s.charAt(i);
        if (!(q == '\'' || q == '’')) return new ParseResult(false, 0, "", "Not a char literal.");

        int j = i + 1;
        if (j >= s.length()) {
            String lex = s.substring(i);
            return new ParseResult(false, lex.length(), lex, "Unterminated char literal (EOF reached).");
        }

        char ch = s.charAt(j);
        if (ch == '\n' || ch == '\r') {
            String lex = s.substring(i, j);
            return new ParseResult(false, lex.length(), lex, "Unterminated char literal (newline encountered).");
        }

        if (ch == '\\') {
            if (j + 1 >= s.length()) {
                String lex = s.substring(i);
                return new ParseResult(false, lex.length(), lex, "Unterminated char literal (EOF after escape).");
            }
            char esc = s.charAt(j + 1);
            if (!(esc == '\'' || esc == '’' || esc == '\\' || esc == 'n' || esc == 't' || esc == 'r')) {
                String lex = s.substring(i, Math.min(j + 2, s.length()));
                return new ParseResult(false, lex.length(), lex, "Invalid escape sequence in char: \\" + esc);
            }
            j += 2;
        } else {
            j += 1;
        }

        if (j < s.length() && (s.charAt(j) == '\'' || s.charAt(j) == '’')) {
            String lex = s.substring(i, j + 1);
            return new ParseResult(true, lex.length(), lex, "");
        }

        String lex = s.substring(i, Math.min(i + 4, s.length()));
        return new ParseResult(false, lex.length(), lex, "Malformed char literal (missing closing quote or too long).");
    }

    // ---------- Cursor utilities ----------

    private static boolean startsWith(String s, int i, String prefix) {
        return i + prefix.length() <= s.length() && s.startsWith(prefix, i);
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    private static void consumeWhitespace(String s, Cursor c) {
        while (c.i < s.length() && isWhitespace(s.charAt(c.i))) {
            char ch = s.charAt(c.i);
            if (ch == '\r') {
                if (c.i + 1 < s.length() && s.charAt(c.i + 1) == '\n') c.i++;
                c.i++;
                c.line++;
                c.col = 1;
            } else if (ch == '\n') {
                c.i++;
                c.line++;
                c.col = 1;
            } else {
                c.i++;
                c.col++;
            }
        }
    }

    private static int findLineEnd(String s, int i) {
        int j = i;
        while (j < s.length() && s.charAt(j) != '\n' && s.charAt(j) != '\r') j++;
        return j;
    }

    private static void advanceByString(String lex, Cursor c) {
        for (int k = 0; k < lex.length(); k++) {
            char ch = lex.charAt(k);
            if (ch == '\r') {
                if (k + 1 < lex.length() && lex.charAt(k + 1) == '\n') k++;
                c.i++;
                c.line++;
                c.col = 1;
            } else if (ch == '\n') {
                c.i++;
                c.line++;
                c.col = 1;
            } else {
                c.i++;
                c.col++;
            }
        }
    }

    private static boolean isLower(char c) {
        return c >= 'a' && c <= 'z';
    }

    private static boolean isDelimiter(char c) {
        return SINGLE_OPS.contains(c) || PUNCTUATORS.contains(c) || c == '#';
    }

    private static boolean isWordBoundary(String s, int i, int len) {
        int end = i + len;
        if (end >= s.length()) return true;
        char nxt = s.charAt(end);
        // boundary means next char is not part of a word-like token
        return !(Character.isLetterOrDigit(nxt) || nxt == '_');
    }

    // ---------- Main runner ----------

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java ManualScanner <file.lang>");
            return;
        }

        String src = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);

        ManualScanner scanner = new ManualScanner();
        List<Token> tokens = scanner.scan(src);

        for (Token t : tokens) {
            System.out.println(t);
        }

        scanner.printStats(tokens);
        scanner.symbolTable.print();

        if (scanner.errorHandler.hasErrors()) {
            System.out.println("\n--- LEXICAL ERRORS ---");
            scanner.errorHandler.printErrors();
        }
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
}