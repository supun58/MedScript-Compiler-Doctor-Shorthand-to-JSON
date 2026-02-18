package medscript.compiler;

public enum TokenType {
    // Sections
    SECTION_PATIENT,
    SECTION_ALLERGY,
    SECTION_RX,
    SECTION_NOTES,

    // Core tokens
    FORM,
    ID,
    NUMBER,
    UNIT,
    ROUTE,
    FREQUENCY,
    DURATION_UNIT,
    FOOD_MOD,

    COLON,

    UNKNOWN,
    EOF
}
