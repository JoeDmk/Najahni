package com.najahni.models;

/**
 * Enum representing user roles in the NAJAHNI platform.
 */
public enum Role {
    ENTREPRENEUR("Entrepreneur"),
    INVESTOR("Investisseur");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
