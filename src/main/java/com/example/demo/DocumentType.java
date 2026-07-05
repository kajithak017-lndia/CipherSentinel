package com.example.demo;

public enum DocumentType {

    PAN_CARD("PAN Card", "🪪"),
    AADHAAR_CARD("Aadhaar Card", "🆔"),
    SALARY_SLIP("Salary Slip", "💰"),
    BANK_STATEMENT("Bank Statement", "🏦"),
    LAND_RECORD("Land Record", "📜"),
    VEHICLE_RC("Vehicle RC", "🚗"),
    INCOME_CERTIFICATE("Income Certificate", "📄"),
    ADDRESS_PROOF("Address Proof", "🏠"),
    PHOTOGRAPH("Photograph", "📷"),
    ITR_RETURNS("ITR Returns", "🧾"),
    ADMISSION_LETTER("Admission Letter", "🎓"),
    OTHER("Other Document", "📁");

    private final String label;
    private final String icon;

    DocumentType(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public String getIcon() { return icon; }

    public static DocumentType fromString(String value) {
        if (value == null) return OTHER;
        try {
            return DocumentType.valueOf(value.trim().toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}