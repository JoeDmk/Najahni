package com.najahni.models;

/**
 * Enum representing project status in the NAJAHNI platform.
 */
public enum ProjectStatus {
    DRAFT("Brouillon"),
    PENDING("En attente"),
    APPROVED("Approuvé"),
    REJECTED("Rejeté"),
    FUNDED("Financé");

    private final String displayName;

    ProjectStatus(String displayName) {
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
