package com.najahni.models;

/**
 * Statut d'un contrat d'investissement numérique.
 */
public enum ContractStatus {
    DRAFT("Brouillon"),
    INVESTOR_SIGNED("Signé par l'investisseur"),
    FULLY_SIGNED("Signé par les deux parties"),
    CANCELLED("Annulé");

    private final String displayName;

    ContractStatus(String displayName) {
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
