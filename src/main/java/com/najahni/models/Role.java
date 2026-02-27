package com.najahni.models;

/**
 * Enum representing user roles in the NAJAHNI platform.
 */
public enum Role {
    ENTREPRENEUR("Entrepreneur", "ENTREPRENEUR"),
    INVESTOR("Investisseur", "INVESTISSEUR"),
    ADMIN("Administrateur", "ADMIN"),
    MENTOR("Mentor", "MENTOR");

    private final String displayName;
    private final String dbValue;

    Role(String displayName, String dbValue) {
        this.displayName = displayName;
        this.dbValue = dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Retourne la valeur stockée en base de données. */
    public String getDbValue() {
        return dbValue;
    }

    /**
     * Convertit une valeur de la base de données en enum Role.
     * Supporte les deux formats : INVESTOR et INVESTISSEUR.
     */
    public static Role fromDbValue(String dbValue) {
        if (dbValue == null) return ENTREPRENEUR;
        for (Role role : values()) {
            if (role.dbValue.equalsIgnoreCase(dbValue) || role.name().equalsIgnoreCase(dbValue)) {
                return role;
            }
        }
        System.err.println("⚠ Unknown role value: " + dbValue + ", defaulting to ENTREPRENEUR");
        return ENTREPRENEUR; // valeur par défaut
    }

    @Override
    public String toString() {
        return displayName;
    }
}
