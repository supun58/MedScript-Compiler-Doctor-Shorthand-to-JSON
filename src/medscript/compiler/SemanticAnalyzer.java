package medscript.compiler;

import java.util.*;
import java.util.regex.*;
import medscript.compiler.AST.*;
import medscript.compiler.Parser.Diagnostic;

public class SemanticAnalyzer {

    // Simple brand->generic map for nicer JSON output (extend as needed)
    private static final Map<String, String> GENERIC = new HashMap<>();
    static {
        GENERIC.put("pcm", "Paracetamol");
        GENERIC.put("amox", "Amoxicillin");
        GENERIC.put("cetirizine", "Cetirizine");
        GENERIC.put("hydrocortisone", "Hydrocortisone");
    }

    // Example allergy conflicts (demo-level)
    private static final Map<String, Set<String>> ALLERGY_CONFLICTS = new HashMap<>();
    static {
        ALLERGY_CONFLICTS.put("penicillin", new HashSet<>(Arrays.asList("amox", "amoxicillin")));
    }

    public List<Diagnostic> analyze(Program p) {
        List<Diagnostic> diags = new ArrayList<>();

        // Required patient name (warning; not error)
        if (p.patient.name == null || p.patient.name.isBlank()) {
            diags.add(Diagnostic.warn(1, 1, "Patient name is missing (add: patient <Name> ...)"));
        }

        // Duplicate medication names
        Set<String> seen = new HashSet<>();
        for (Medication m : p.medications) {
            String key = m.name.toLowerCase();
            if (!seen.add(key)) {
                diags.add(Diagnostic.warn(1,1, "Duplicate medication detected: " + m.name));
            }

            // Dose must be positive
            double doseVal = extractFirstNumber(m.dose != null ? m.dose.strength : "");
            if (doseVal <= 0) {
                diags.add(Diagnostic.error(1,1, "Dose must be positive for " + m.name));
            }

            // Duration must be >0
            if (m.duration == null || m.duration.value <= 0) {
                diags.add(Diagnostic.error(1,1, "Duration must be > 0 for " + m.name));
            }

            // Route validation: oint/cream/drops topical vs IV etc
            if (m.route != null) {
                if ((m.form.equalsIgnoreCase("Oint") || m.form.equalsIgnoreCase("Cream")) &&
                        (m.route.equals("iv") || m.route.equals("im"))) {
                    diags.add(Diagnostic.error(1,1, "Invalid route '" + m.route + "' for " + m.form + " " + m.name));
                }
            }

            // Basic dose limit warning (demo): Paracetamol > 1000mg per dose warning
            if (m.name.equalsIgnoreCase("pcm") || m.name.equalsIgnoreCase("paracetamol")) {
                if (m.dose != null && m.dose.strength != null && m.dose.strength.contains("mg")) {
                    double mg = extractFirstNumber(m.dose.strength);
                    if (mg > 1000) {
                        diags.add(Diagnostic.warn(1,1, "High single dose for Paracetamol (" + mg + "mg). Check safety limits."));
                    }
                }
            }

            // Allergy conflicts
            for (String a : p.allergies) {
                Set<String> conflicts = ALLERGY_CONFLICTS.get(a.toLowerCase());
                if (conflicts != null) {
                    String medKey = m.name.toLowerCase();
                    if (conflicts.contains(medKey) || conflicts.contains(GENERIC.getOrDefault(medKey, medKey).toLowerCase())) {
                        diags.add(Diagnostic.error(1,1, "Allergy conflict: patient allergy '" + a + "' conflicts with " + m.name));
                    }
                }
            }
        }

        return diags;
    }

    private double extractFirstNumber(String s) {
        Matcher m = Pattern.compile("([0-9]+(\\.[0-9]+)?)").matcher(s);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1)); } catch(Exception e) { return 0; }
        }
        // handle 1/2
        Matcher f = Pattern.compile("([0-9]+)\\/([0-9]+)").matcher(s);
        if (f.find()) {
            try { return Double.parseDouble(f.group(1)) / Double.parseDouble(f.group(2)); } catch(Exception e) { return 0; }
        }
        return 0;
    }

    public static String genericName(String shortName) {
        return GENERIC.getOrDefault(shortName.toLowerCase(), shortName);
    }
}
