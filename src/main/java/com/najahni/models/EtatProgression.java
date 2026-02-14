package com.najahni.models;

/**
 * Enum représentant l'état de progression d'un utilisateur dans un cours.
 * Permet de suivre l'avancement de l'apprentissage.
 */
public enum EtatProgression {
    NON_COMMENCE("Non commencé"),
    EN_COURS("En cours"),
    COMPLETE("Complété"),
    CERTIFIE("Certifié");

    private final String displayName;

    EtatProgression(String displayName) {
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
