package com.najahni.models;

/**
 * Enum représentant les niveaux de difficulté des cours.
 * Utilisé pour catégoriser les cours selon leur complexité.
 */
public enum NiveauCours {
    DEBUTANT("Débutant", 1),
    INTERMEDIAIRE("Intermédiaire", 2),
    AVANCE("Avancé", 3),
    EXPERT("Expert", 4);

    private final String displayName;
    private final int valeur;

    NiveauCours(String displayName, int valeur) {
        this.displayName = displayName;
        this.valeur = valeur;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getValeur() {
        return valeur;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
