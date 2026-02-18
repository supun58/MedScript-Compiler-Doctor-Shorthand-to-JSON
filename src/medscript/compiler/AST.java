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
        public String form;     
        public String name;     
        public Dose dose;       
        public String route;    
        public String freq;     
        public Duration duration; 
        public String foodMod; 
        public Map<String, String> extras = new LinkedHashMap<>(); 
    }

    public static class Dose {
        public String strength; 
        public String amount;   
    }

    public static class Duration {
        public double value;
        public String unit; 
        public int toDaysRounded() {
            if ("d".equals(unit)) return (int)Math.round(value);
            if ("w".equals(unit)) return (int)Math.round(value * 7);
            if ("m".equals(unit)) return (int)Math.round(value * 30);
            return (int)Math.round(value);
        }
    }
}
