# MedScript Compiler (Lexical + Syntax + Semantic Analysis)

A mini "compiler-like" application that converts **doctor prescription shorthand** into
structured **JSON** with **compiler-style diagnostics**.

## Features
- **Lexical analysis**: implemented using rules matching a JFlex specification (`jflex/MedLexer.flex`).
- **Syntax analysis**: recursive-descent parser (builds an AST).
- **Semantic analysis**: validations (dose limits, required fields, allergy conflicts, duplicates).
- **GUI**: Swing-based interface with:
  - input editor
  - tokens table
  - errors/warnings panel
  - JSON output panel

## Project structure
- `jflex/MedLexer.flex` - JFlex specification (code used to generate the lexer)
- `src/` - Java source code (application + parser + semantics)
- `samples/` - sample MedScript programs for demos/tests

## How to run (CLI)
**Bash/Git Bash:**
```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out medscript.Main samples/sample_ok.med
java -cp out medscript.Main samples/sample_semantic_error.med
java -cp out medscript.Main samples/sample_ok.med --tokens
```

**Windows PowerShell:**
```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Path src -Filter *.java).FullName
java -cp out medscript.Main samples/sample_ok.med
java -cp out medscript.Main samples/sample_semantic_error.med
java -cp out medscript.Main samples/sample_ok.med --tokens
```

## How to run (GUI)
**Bash/Git Bash:**
```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out medscript.gui.MedScriptGUI
```

**Windows PowerShell:**
```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Path src -Filter *.java).FullName
java -cp out medscript.gui.MedScriptGUI
```

## Regenerating the lexer with JFlex (optional)
If you have JFlex installed (or a `jflex-full-*.jar`), run:
```bash
java -jar jflex-full-1.9.1.jar -d src/medscript/compiler jflex/MedLexer.flex
```
This will generate `src/medscript/compiler/MedLexer.java`.