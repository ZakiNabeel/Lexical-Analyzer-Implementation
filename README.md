# Lexical Analyzer Implementation

**Course:** CS4031 - Compiler Construction  
**Assignment:** 01 - Lexical Analyzer  
**Section:** CS-D  

## 👥 Team Members
- **Roll No:** 23I-0604
- **Roll No:** 23I-0508

---

## 📘 Language Specification
**Name:** CustomLang  
**File Extension:** `.lang`

This project implements a lexical analyzer (scanner) for *CustomLang*, a custom programming language designed for this assignment. The scanner identifies and categorizes tokens such as keywords, identifiers, literals, and operators.

---

## 🔑 Keywords
The following words are reserved and cannot be used as identifiers. They are case-sensitive.

| Keyword | Meaning |
| :--- | :--- |
| `start` | Begins the program execution block. |
| `finish` | Ends the program execution block. |
| `declare` | Used to declare a new variable. |
| `output` | Prints a value or string to the console. |
| `input` | Reads a value from the user. |
| `condition` | Starts a conditional (if) statement. |
| `else` | The alternative block of a conditional statement. |
| `loop` | Initiates a loop structure. |
| `break` | Exits the current loop immediately. |
| `continue` | Skips the rest of the loop body and proceeds to the next iteration. |
| `function` | Defines a new function. |
| `return` | Returns a value from a function. |

---

## 🏷️ Identifiers
Identifiers are used for variable and function names.
* **Rule:** Must start with an **Uppercase Letter** (`A-Z`).
* **Body:** Can contain lowercase letters (`a-z`), digits (`0-9`), and underscores (`_`).
* **Length:** Maximum **31 characters**.

**Examples:**
* ✅ Valid: `Count`, `Total_Sum`, `X`, `Var2`
* ❌ Invalid: `count` (starts with lowercase), `2var` (starts with digit), `my-var` (hyphen not allowed)

---

## 🔢 Literals

| Type | Format | Examples |
| :--- | :--- | :--- |
| **Integer** | Optional sign `+` or `-`, followed by digits. | `42`, `-10`, `+500` |
| **Float** | Integer part, dot `.`, 1-6 decimal digits. Optional scientific notation (`e` or `E`). | `3.14`, `-0.001`, `1.5E-2` |
| **String** | Enclosed in double quotes. Supports escapes `\n`, `\t`, `\r`, `\"`, `\\`. | `"Hello World"`, `"Line\nBreak"` |
| **Char** | Enclosed in single quotes. Single character or escape. | `'A'`, `'\n'`, `'+'` |
| **Boolean** | Exact words `true` or `false`. | `true`, `false` |

---

## ➕ Operators & Precedence
Operators are listed from **highest precedence** to **lowest**.

| Category | Operators |
| :--- | :--- |
| **Unary / Inc/Dec** | `++`, `--`, `!` |
| **Exponentiation** | `**` |
| **Multiplicative** | `*`, `/`, `%` |
| **Additive** | `+`, `-` |
| **Relational** | `<`, `>`, `<=`, `>=`, `==`, `!=` |
| **Logical AND** | `&&` |
| **Logical OR** | `||` |
| **Assignment** | `=`, `+=`, `-=`, `*=`, `/=` |

---

## 💬 Comments
The scanner supports two types of comments, which are ignored during processing (but line numbers are tracked).

1.  **Single-line:** Starts with `##` and continues to the end of the line.
    ```python
    ## This is a comment
    declare X ## variable declaration
    ```

2.  **Multi-line:** Enclosed between `#*` and `*#`.
    ```python
    #* This is a multi-line comment 
       spanning two lines 
    *#
    ```

---

## 💻 Sample Programs

### Sample 1: Basic Logic (`test1.lang`)
```text
start
declare X
declare Total_sum_2024
output "Hello\nWorld"
condition true
loop
X = 42
X += 5
finish
Sample 2: Complex Math (test2.lang)
Plaintext
start
declare X
declare Y
X = 10
Y = 3
output X**Y
output (X + Y) * (X - Y) / Y % 2
condition X >= 10 && Y != 0
output "OK"
finish
Sample 3: Comments (test5.lang)
Plaintext
start
## Single line comment
declare X
#* Multi-line 
   comment block 
*#
output "Done"
finish
🚀 Compilation and Execution


## Prerequisites
Java Development Kit (JDK) installed.

JFlex library (provided as jflex-full-1.9.1.jar in the root directory).

## Option 1: Running the Manual Scanner
The manual scanner is a pure Java implementation using if/else and loops.

Compile:

Bash
javac -d bin src/*.java
Run:

Bash
java -cp bin ManualScanner tests/test1.lang


## Option 2: Running the JFlex Scanner
The JFlex scanner uses a generated table-based approach.

Generate the Scanner (Yylex.java):

Bash
java -jar jflex-full-1.9.1.jar src/Scanner.flex
Compile:

Bash
javac -d bin src/*.java
Run:

Bash
java -cp bin JFlexRunner tests/test1.lang
