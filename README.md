# Lexical Analyzer Implementation

Implementation of a Lexical Analyzer (Scanner) for a custom programming language. The lexical analyzer is the first phase of a compiler that reads source code and breaks it into meaningful units called tokens.

## Features

- **Manual Scanner** (`ManualScanner.java`) — a hand-written lexical analyzer in Java that performs tokenization using priority-based longest-match rules.
- **JFlex Scanner** (`Scanner.flex`) — a JFlex specification that generates an equivalent scanner automatically.
- **Error Handling** — reports lexical errors (invalid tokens, unterminated strings, malformed literals) with line and column information.
- **Symbol Table** — tracks identifiers, their first occurrence, and usage frequency.
- **Token Statistics** — prints token counts, lines processed, and comments removed.

## Token Types

| Category | Examples |
|---|---|
| Keywords | `start`, `finish`, `loop`, `condition`, `declare`, `output`, `input`, `function`, `return`, `break`, `continue`, `else` |
| Identifiers | Must start with an uppercase letter (`A`–`Z`), followed by lowercase letters, digits, or underscores (max 31 characters) |
| Integer literals | `42`, `+5`, `-3` |
| Float literals | `3.14`, `2.0E-3` (up to 6 decimal digits, optional exponent) |
| String literals | `"Hello\nWorld"` (supports `\"`, `\\`, `\n`, `\t`, `\r` escapes) |
| Character literals | `'A'`, `'\n'` |
| Boolean literals | `true`, `false` |
| Operators | `+`, `-`, `*`, `/`, `%`, `**`, `==`, `!=`, `<=`, `>=`, `<`, `>`, `&&`, `\|\|`, `!`, `=`, `++`, `--`, `+=`, `-=`, `*=`, `/=` |
| Punctuators | `(`, `)`, `{`, `}`, `[`, `]`, `,`, `;`, `:` |
| Comments | Single-line `## ...`, Multi-line `#* ... *#` |

## Project Structure

```
src/
  ManualScanner.java   # Hand-written scanner
  Scanner.flex         # JFlex scanner specification
  Token.java           # Token data class
  TokenType.java       # Token type enum
  ErrorHandler.java    # Lexical error reporting
  SymbolTable.java     # Identifier tracking
docs/
  README.md            # Language and team details
  LanguageGrammar.txt  # Token grammar summary
tests/
  test1.lang           # All valid tokens
  test2.lang           # Complex expressions, operator combinations
  test3.lang           # String and char escapes
  test4.lang           # Lexical errors and malformed literals
  test5.lang           # Single-line and multi-line comments
  TestResults.txt      # Test results summary
bin/                   # Compiled class files
```

## How to Run

### Manual Scanner

```bash
javac src/*.java -d bin
java -cp bin ManualScanner tests/test1.lang
```

### JFlex Scanner

```bash
jflex src/Scanner.flex
javac src/*.java -d bin
```