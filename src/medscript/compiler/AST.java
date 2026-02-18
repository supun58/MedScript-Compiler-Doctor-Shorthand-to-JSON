package medscript.compiler;

import java.util.*;

public class AST {
    public static class Program {
        public Patient patient = new Patient();
        public Set<String> allergies = new LinkedHashSet<>();
        public List<Medication> medications = new ArrayList<>();
        public List<String> notes = new ArrayList<>();
    }

    public static class Patient {
        public String name = null;
        public Integer age = null;
        public Double weightKg = null;
    }

    public static class Medication {
        public String form;     // Tab, Cap, Syr...
        public String name;     // PCM, Amox...
        public Dose dose;       // 500mg or 5mg/5ml + 10ml
        public String route;    // po, iv...
        public String freq;     // tds...
        public Duration duration; // 5d, 2w, 1m
        public String foodMod;  // after_food...
        public Map<String, String> extras = new LinkedHashMap<>(); // freeform flags
    }

    public static class Dose {
        public String strength; // e.g., 500mg or 5mg/5ml or 1%
        public String amount;   // e.g., 10ml (for syrups) optional
    }

    public static class Duration {
        public double value;
        public String unit; // d, w, m
        public int toDaysRounded() {
            if ("d".equals(unit)) return (int)Math.round(value);
            if ("w".equals(unit)) return (int)Math.round(value * 7);
            if ("m".equals(unit)) return (int)Math.round(value * 30);
            return (int)Math.round(value);
        }
    }
}
