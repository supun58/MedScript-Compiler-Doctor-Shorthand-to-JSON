package medscript.compiler;

import java.io.*;
import java.util.*;
import java.util.regex.*;


public class MedLexer {
    private final String input;
    private int index = 0;
    private int line = 1;
    private int col = 1;

    private static final Pattern WS = Pattern.compile("^[ \t\f\r\n]+");
    private static final Pattern COMMENT = Pattern.compile("^#.*(?:\\R|$)");

    private static final Pattern SECTION_RX = Pattern.compile("^rx:");
    private static final Pattern SECTION_NOTES = Pattern.compile("^notes:");
    private static final Pattern SECTION_PATIENT = Pattern.compile("^patient\\b");
    private static final Pattern SECTION_ALLERGY = Pattern.compile("^allergy\\b");

    private static final Pattern FORM = Pattern.compile("^(Tab|Cap|Syr|Inj|Oint|Drops|Cream|Neb)\\b");

    private static final Pattern ROUTE = Pattern.compile("^(po|iv|im|sc|sl|pr|topical|inhale)\\b");
    private static final Pattern FREQ = Pattern.compile("^(od|bd|tds|qid|hs|stat|prn|sos|q[0-9]+h)\\b");
    private static final Pattern FOOD = Pattern.compile("^(ac|pc|with_meals|after_food|before_food)\\b");

    private static final Pattern UNIT = Pattern.compile("^(mg|g|ml|mcg|IU|%|drops)\\b");
    private static final Pattern DURUNIT = Pattern.compile("^(d|w|m)\\b");

    private static final Pattern NUMBER = Pattern.compile("^(([0-9]+(\\.[0-9]+)?)|([0-9]+/[0-9]+))");
    private static final Pattern ID = Pattern.compile("^([A-Za-z][A-Za-z0-9_-]*)\\b");

    public MedLexer(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = reader.read(buf)) != -1) sb.append(buf, 0, n);
        this.input = sb.toString();
    }

    private Token token(TokenType type, String lexeme) {
        return new Token(type, lexeme, line, col);
    }

    private void advance(String lexeme) {
        for (int i = 0; i < lexeme.length(); i++) {
            char c = lexeme.charAt(i);
            if (c == '\n') { line++; col = 1; }
            else { col++; }
        }
        index += lexeme.length();
    }

    private String remaining() {
        return input.substring(index);
    }

    private Token tryMatch(Pattern p, TokenType type) {
        Matcher m = p.matcher(remaining());
        if (m.find()) {
            String lex = m.group();
            Token t = token(type, lex);
            advance(lex);
            return t;
        }
        return null;
    }

    public Token nextToken() {
        while (index < input.length()) {
            Token t;
            // whitespace
            t = tryMatch(WS, null);
            if (t != null) continue;
            // comments
            Matcher cm = COMMENT.matcher(remaining());
            if (cm.find()) { advance(cm.group()); continue; }

            // sections
            t = tryMatch(SECTION_RX, TokenType.SECTION_RX); if (t != null) return t;
            t = tryMatch(SECTION_NOTES, TokenType.SECTION_NOTES); if (t != null) return t;
            t = tryMatch(SECTION_PATIENT, TokenType.SECTION_PATIENT); if (t != null) return t;
            t = tryMatch(SECTION_ALLERGY, TokenType.SECTION_ALLERGY); if (t != null) return t;

            // punctuation
            if (remaining().startsWith(":")) {
                Token tok = token(TokenType.COLON, ":");
                advance(":");
                return tok;
            }

            // other tokens
            t = tryMatch(FORM, TokenType.FORM); if (t != null) return t;
            t = tryMatch(ROUTE, TokenType.ROUTE); if (t != null) return t;
            t = tryMatch(FREQ, TokenType.FREQUENCY); if (t != null) return t;
            t = tryMatch(FOOD, TokenType.FOOD_MOD); if (t != null) return t;
            t = tryMatch(UNIT, TokenType.UNIT); if (t != null) return t;
            t = tryMatch(DURUNIT, TokenType.DURATION_UNIT); if (t != null) return t;
            t = tryMatch(NUMBER, TokenType.NUMBER); if (t != null) return t;
            t = tryMatch(ID, TokenType.ID); if (t != null) return t;

            String lex = remaining().substring(0, 1);
            Token unk = token(TokenType.UNKNOWN, lex);
            advance(lex);
            return unk;
        }
        return new Token(TokenType.EOF, "<EOF>", line, col);
    }
}