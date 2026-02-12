package com.najahni.models;

/**
 * Enum représentant le statut d'une offre d'investissement.
 * PENDING  : L'offre est en attente de traitement par l'entrepreneur.
 * ACCEPTED : L'offre a été acceptée.
 * REJECTED : L'offre a été rejetée.
 */
public enum OfferStatus {
    PENDING("En attente"),
    ACCEPTED("Acceptée"),
    REJECTED("Rejetée");

    private final String displayName;

    OfferStatus(String displayName) {
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
