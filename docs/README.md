# Language Scanner - Assignment 1

## Team
- Roll No: 23I-0604 
- Roll No: 23I-0508
- Section: CS-D

## Language
- Name: Java/JFlex
- Extension: .lang

## Keywords
start, finish, loop, condition, declare, output, input, function, return, break, continue, else

## Identifiers
- Must start with uppercase letter A-Z
- Followed by lowercase letters, digits, underscore (and space if you keep it)
- Max length 31 characters
Examples:
- Valid:
- Invalid:

## Literals
### Integers
Examples:

### Floats
Examples:

### Strings
Supported escapes: \", \\, \n, \t, \r

### Characters
Supported escapes: \', \\, \n, \t, \r

## Operators
- Arithmetic: + - * / % **
- Relational: == != <= >= < >
- Logical: && || !
- Assignment: = += -= *= /=
- Inc/Dec: ++ --

## Comments
- Single-line: ## ...
- Multi-line: #* ... *#

## How to run
### Manual scanner
javac src/*.java
java -cp src ManualScanner tests/test1.lang

### JFlex scanner
jflex src/Scanner.flex
javac src/*.java
(java runner you write for Yylex, or integrate a main)

## Sample Programs
1)
2)
3)