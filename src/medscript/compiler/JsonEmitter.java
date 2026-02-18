package medscript.compiler;

import java.util.*;
import medscript.compiler.AST.*;

public class JsonEmitter {

    public static String toJson(Program p) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        // patient
        sb.append("  \"patient\": {\n");
        sb.append("    \"name\": ").append(q(p.patient.name)).append(",\n");
        sb.append("    \"age\": ").append(p.patient.age == null ? "null" : p.patient.age).append(",\n");
        sb.append("    \"weightKg\": ").append(p.patient.weightKg == null ? "null" : trimDouble(p.patient.weightKg)).append("\n");
        sb.append("  },\n");

        // allergies
        sb.append("  \"allergies\": [");
        int ai=0;
        for (String a: p.allergies) {
            if (ai++>0) sb.append(", ");
            sb.append(q(a));
        }
        sb.append("],\n");

        // meds
        sb.append("  \"medications\": [\n");
        for (int i=0;i<p.medications.size();i++) {
            Medication m = p.medications.get(i);
            sb.append("    {\n");
            sb.append("      \"form\": ").append(q(m.form)).append(",\n");
            sb.append("      \"shortName\": ").append(q(m.name)).append(",\n");
            sb.append("      \"name\": ").append(q(SemanticAnalyzer.genericName(m.name))).append(",\n");
            sb.append("      \"dose\": ").append(q(m.dose != null ? m.dose.strength : null)).append(",\n");
            sb.append("      \"amount\": ").append(q(m.dose != null ? m.dose.amount : null)).append(",\n");
            sb.append("      \"route\": ").append(q(m.route)).append(",\n");
            sb.append("      \"frequency\": ").append(q(expandFrequency(m.freq))).append(",\n");
            sb.append("      \"duration\": ").append(q(formatDuration(m.duration))).append(",\n");
            sb.append("      \"food\": ").append(q(expandFood(m.foodMod))).append("\n");
            sb.append("    }");
            if (i < p.medications.size()-1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        // notes
        sb.append("  \"notes\": [");
        for (int i=0;i<p.notes.size();i++) {
            if (i>0) sb.append(", ");
            sb.append(q(p.notes.get(i)));
        }
        sb.append("]\n");

        sb.append("}\n");
        return sb.toString();
    }

    private static String q(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String expandFrequency(String f) {
        if (f == null) return null;
        switch (f.toLowerCase()) {
            case "od": return "once daily";
            case "bd": return "twice daily";
            case "tds": return "three times daily";
            case "qid": return "four times daily";
            case "hs": return "at night";
            case "stat": return "immediately (stat)";
            case "prn": return "as needed (prn)";
            case "sos": return "if needed (sos)";
            default:
                if (f.toLowerCase().startsWith("q") && f.toLowerCase().endsWith("h")) return "every " + f.substring(1, f.length()-1) + " hours";
                return f;
        }
    }

    private static String expandFood(String fm) {
        if (fm == null) return null;
        switch (fm.toLowerCase()) {
            case "ac": return "before food";
            case "pc": return "after food";
            case "with_meals": return "with meals";
            case "after_food": return "after food";
            case "before_food": return "before food";
            default: return fm;
        }
    }

    private static String formatDuration(Duration d) {
        if (d == null) return null;
        String u = d.unit;
        String name = "d".equals(u) ? "days" : "w".equals(u) ? "weeks" : "months";
        return trimDouble(d.value) + " " + name;
    }

    private static String trimDouble(double v) {
        if (Math.abs(v - Math.round(v)) < 1e-9) return String.valueOf((long)Math.round(v));
        return String.valueOf(v);
    }
}
