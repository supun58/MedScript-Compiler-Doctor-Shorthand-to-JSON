package medscript.compiler;

import java.io.*;
import java.util.*;
import medscript.compiler.AST.*;

public class Parser {
    private final MedLexer lexer;
    private Token current;

    public Parser(Reader r) throws IOException {
        this.lexer = new MedLexer(r);
        this.current = lexer.nextToken();
    }

    private void advance() {
        current = lexer.nextToken();
    }

    private boolean match(TokenType type) {
        if (current.type == type) { advance(); return true; }
        return false;
    }

    private Token expect(TokenType type, List<Diagnostic> diags, String msg) {
        if (current.type == type) {
            Token t = current;
            advance();
            return t;
        }
        diags.add(Diagnostic.error(current.line, current.column, msg + " (found: " + current.type + " '" + current.lexeme + "')"));
        // attempt simple recovery: don't consume EOF
        if (current.type != TokenType.EOF) advance();
        return new Token(type, "", current.line, current.column);
    }

    public static class Diagnostic {
        public enum Level { ERROR, WARNING }
        public final Level level;
        public final int line;
        public final int column;
        public final String message;

        private Diagnostic(Level level, int line, int column, String message) {
            this.level = level;
            this.line = line;
            this.column = column;
            this.message = message;
        }

        public static Diagnostic error(int line, int col, String msg) { return new Diagnostic(Level.ERROR, line, col, msg); }
        public static Diagnostic warn(int line, int col, String msg) { return new Diagnostic(Level.WARNING, line, col, msg); }

        @Override public String toString() {
            return level + " @ " + line + ":" + column + " - " + message;
        }
    }

    public static class ParseResult {
        public final Program program;
        public final List<Diagnostic> diagnostics;
        public ParseResult(Program p, List<Diagnostic> d) { this.program = p; this.diagnostics = d; }
    }

    // Grammar (high level):
    // Program -> (PatientBlock)? (AllergyBlock)? RxBlock (NotesBlock)?
    // PatientBlock -> 'patient' ID ('age' NUMBER)? ('weight' NUMBER 'kg')?
    // AllergyBlock -> 'allergy' ID+
    // RxBlock -> 'rx:' Medication+
    // Medication -> FORM ID Dose (ROUTE)? FREQUENCY Duration (FOOD_MOD)? (ID)*  // extra flags
    // Dose -> Strength (Amount)?
    // Strength -> NUMBER UNIT | NUMBER UNIT '/' NUMBER UNIT | NUMBER '%' | NUMBER '/' NUMBER 'tab'
    // Amount -> NUMBER UNIT (often ml)
    // Duration -> NUMBER DURATION_UNIT

    public ParseResult parse() {
        Program p = new Program();
        List<Diagnostic> diags = new ArrayList<>();

        // allow optional patient/allergy/notes around rx
        while (current.type != TokenType.EOF) {
            if (current.type == TokenType.SECTION_PATIENT) {
                parsePatient(p, diags);
            } else if (current.type == TokenType.SECTION_ALLERGY) {
                parseAllergy(p, diags);
            } else if (current.type == TokenType.SECTION_RX) {
                parseRx(p, diags);
            } else if (current.type == TokenType.SECTION_NOTES) {
                parseNotes(p, diags);
            } else {
                diags.add(Diagnostic.error(current.line, current.column,
                        "Unexpected token at top-level. Expected 'patient', 'allergy', 'rx:' or 'notes:'"));
                advance();
            }
        }

        // minimal syntax requirement: must have at least one medication
        if (p.medications.isEmpty()) {
            diags.add(Diagnostic.error(1, 1, "No medications found. Add an 'rx:' section with at least one medication."));
        }

        return new ParseResult(p, diags);
    }

    private void parsePatient(Program p, List<Diagnostic> diags) {
        expect(TokenType.SECTION_PATIENT, diags, "Expected 'patient'");
        Token nameTok = expect(TokenType.ID, diags, "Expected patient name after 'patient'");
        if (!nameTok.lexeme.isEmpty()) p.patient.name = nameTok.lexeme;

        while (current.type == TokenType.ID) {
            String key = current.lexeme.toLowerCase();
            if ("age".equals(key)) {
                advance();
                Token ageTok = expect(TokenType.NUMBER, diags, "Expected age number");
                try { p.patient.age = (int)Math.round(Double.parseDouble(ageTok.lexeme.replace("/", "."))); }
                catch(Exception e){ diags.add(Diagnostic.error(ageTok.line, ageTok.column, "Invalid age value")); }
            } else if ("weight".equals(key)) {
                advance();
                Token wTok = expect(TokenType.NUMBER, diags, "Expected weight number");
                String val = wTok.lexeme;
                if (match(TokenType.ID)) { /* allow 'kg' as ID */ }
                // if next is ID and equals kg, ok; otherwise ignore
                try { p.patient.weightKg = Double.parseDouble(val.replace("/", ".")); }
                catch(Exception e){ diags.add(Diagnostic.error(wTok.line, wTok.column, "Invalid weight value")); }
            } else {
                // unknown patient attribute: skip one ID
                diags.add(Diagnostic.warn(current.line, current.column, "Unknown patient attribute '" + current.lexeme + "' ignored"));
                advance();
            }
        }
    }

    private void parseAllergy(Program p, List<Diagnostic> diags) {
        expect(TokenType.SECTION_ALLERGY, diags, "Expected 'allergy'");
        // one or more IDs
        int count = 0;
        while (current.type == TokenType.ID) {
            p.allergies.add(current.lexeme.toLowerCase());
            count++;
            advance();
        }
        if (count == 0) {
            diags.add(Diagnostic.error(current.line, current.column, "Expected at least one allergy name after 'allergy'"));
        }
    }

    private void parseRx(Program p, List<Diagnostic> diags) {
        expect(TokenType.SECTION_RX, diags, "Expected 'rx:'");
        // one or more medications until next section or EOF
        while (current.type != TokenType.EOF &&
               current.type != TokenType.SECTION_PATIENT &&
               current.type != TokenType.SECTION_ALLERGY &&
               current.type != TokenType.SECTION_NOTES &&
               current.type != TokenType.SECTION_RX) {
            if (current.type == TokenType.FORM) {
                Medication m = parseMedication(diags);
                if (m != null) p.medications.add(m);
            } else {
                diags.add(Diagnostic.error(current.line, current.column, "Expected medication starting with a FORM (Tab/Cap/Syr/...)"));
                advance();
            }
        }
    }

    private Medication parseMedication(List<Diagnostic> diags) {
        Medication m = new Medication();
        Token formTok = expect(TokenType.FORM, diags, "Expected FORM");
        m.form = formTok.lexeme;

        Token nameTok = expect(TokenType.ID, diags, "Expected medicine name (e.g., PCM, Amox)");
        m.name = nameTok.lexeme;

        m.dose = parseDose(diags);

        // optional route
        if (current.type == TokenType.ROUTE) { m.route = current.lexeme.toLowerCase(); advance(); }

        Token freqTok = expect(TokenType.FREQUENCY, diags, "Expected frequency (od/bd/tds/qid/...)");
        m.freq = freqTok.lexeme.toLowerCase();

        m.duration = parseDuration(diags);

        if (current.type == TokenType.FOOD_MOD) { m.foodMod = current.lexeme.toLowerCase(); advance(); }

        // remaining IDs are treated as extra flags until a section or a new FORM (next med)
        while (current.type == TokenType.ID) {
            String flag = current.lexeme.toLowerCase();
            // allow 'after_food' etc already tokenized but safe
            m.extras.put(flag, "true");
            advance();
        }

        return m;
    }

    private Dose parseDose(List<Diagnostic> diags) {
        Dose d = new Dose();

        // Strength patterns:
        //  NUMBER UNIT
        //  NUMBER UNIT / NUMBER UNIT   (e.g. 5mg/5ml)
        //  NUMBER %                     (e.g. 1%)
        Token num1 = expect(TokenType.NUMBER, diags, "Expected dose number (e.g., 500 or 0.5 or 1/2)");
        String strength = num1.lexeme;

        if (current.type == TokenType.UNIT) {
            strength += current.lexeme;
            advance();
            // optional / NUMBER UNIT
            if (current.type == TokenType.ID && "/".equals(current.lexeme)) {
                // not produced by lexer; treat as fallback
            }
            if (current.type == TokenType.UNKNOWN && "/".equals(current.lexeme)) {
                strength += "/";
                advance();
                Token num2 = expect(TokenType.NUMBER, diags, "Expected number after '/' in strength (e.g., 5 in 5mg/5ml)");
                strength += num2.lexeme;
                Token unit2 = expect(TokenType.UNIT, diags, "Expected unit after second number in strength (e.g., ml)");
                strength += unit2.lexeme;
            }
        } else if (current.type == TokenType.UNIT || current.type == TokenType.ID) {
            // handled above
        } else if (current.type == TokenType.UNKNOWN && "%".equals(current.lexeme)) {
            strength += "%";
            advance();
        } else if (current.type == TokenType.UNIT) {
            strength += current.lexeme;
            advance();
        } else if (current.type == TokenType.ID && "%".equals(current.lexeme)) {
            strength += "%";
            advance();
        } else if (current.type == TokenType.UNKNOWN && "%".equals(current.lexeme)) {
            strength += "%";
            advance();
        } else {
            // unit missing
            diags.add(Diagnostic.error(current.line, current.column, "Expected unit after dose number (mg/ml/g/...)"));
        }

        d.strength = strength;

        // Optional Amount (for Syrups etc): NUMBER UNIT (typically ml)
        if (current.type == TokenType.NUMBER) {
            Token amtNum = current;
            advance();
            if (current.type == TokenType.UNIT) {
                d.amount = amtNum.lexeme + current.lexeme;
                advance();
            } else {
                // rollback idea not implemented; treat as error and continue
                diags.add(Diagnostic.warn(amtNum.line, amtNum.column, "Possible amount provided but missing unit (e.g., '10ml')"));
            }
        }

        return d;
    }

    private Duration parseDuration(List<Diagnostic> diags) {
        Duration dur = new Duration();
        Token vTok = expect(TokenType.NUMBER, diags, "Expected duration number (e.g., 5 in 5d)");
        Token uTok = expect(TokenType.DURATION_UNIT, diags, "Expected duration unit (d/w/m)");

        try { dur.value = Double.parseDouble(vTok.lexeme.replace("/", ".")); }
        catch(Exception e){ diags.add(Diagnostic.error(vTok.line, vTok.column, "Invalid duration value")); dur.value = 0; }

        dur.unit = uTok.lexeme;
        return dur;
    }

    private void parseNotes(Program p, List<Diagnostic> diags) {
        expect(TokenType.SECTION_NOTES, diags, "Expected 'notes:'");
        // collect remaining IDs/NUMBERS/UNITS as note lines until next section
        StringBuilder line = new StringBuilder();
        while (current.type != TokenType.EOF &&
                current.type != TokenType.SECTION_PATIENT &&
                current.type != TokenType.SECTION_ALLERGY &&
                current.type != TokenType.SECTION_RX &&
                current.type != TokenType.SECTION_NOTES) {
            if (current.type == TokenType.UNKNOWN && "\n".equals(current.lexeme)) {
                // should not happen
            }
            // treat whitespace already skipped
            line.append(current.lexeme).append(" ");
            advance();
        }
        String note = line.toString().trim();
        if (!note.isEmpty()) p.notes.add(note);
        else diags.add(Diagnostic.warn(1,1,"Empty notes section"));
    }
}
