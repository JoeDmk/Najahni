package com.najahni.models;

/**
 * Enum representing investment opportunity status in the NAJAHNI platform.
 */
public enum InvestmentStatus {
    PENDING("En attente"),
    ACCEPTED("Accepté"),
    REJECTED("Rejeté"),
    COMPLETED("Terminé");

    private final String displayName;

    InvestmentStatus(String displayName) {
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
