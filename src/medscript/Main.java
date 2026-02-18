package medscript;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import medscript.compiler.*;
import medscript.compiler.Parser.*;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java -cp out medscript.Main <file.med> [--tokens]");
            System.exit(1);
        }

        boolean showTokens = Arrays.asList(args).contains("--tokens");
        String path = args[0];
        String input = Files.readString(Path.of(path));

        // Lex + Parse
        Parser parser = new Parser(new StringReader(input));
        ParseResult pr = parser.parse();

        // Semantic
        SemanticAnalyzer sem = new SemanticAnalyzer();
        List<Diagnostic> semDiags = sem.analyze(pr.program);

        // Tokens option
        if (showTokens) {
            System.out.println("=== TOKENS ===");
            MedLexer lx = new MedLexer(new StringReader(input));
            Token t;
            do {
                t = lx.nextToken();
                System.out.println(t);
            } while (t.type != TokenType.EOF);
            System.out.println();
        }

        // Diagnostics
        List<Diagnostic> all = new ArrayList<>();
        all.addAll(pr.diagnostics);
        all.addAll(semDiags);

        System.out.println("=== DIAGNOSTICS ===");
        if (all.isEmpty()) System.out.println("(none)");
        else for (Diagnostic d: all) System.out.println(d);

        System.out.println();
        System.out.println("=== JSON OUTPUT ===");
        System.out.println(JsonEmitter.toJson(pr.program));
    }
}
