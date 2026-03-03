package models.apprentissage;

/**
 * Enum représentant les types de cours disponibles sur la plateforme NAJAHNI.
 * Chaque type correspond à un format de contenu différent.
 */
public enum TypeCours {
    VIDEO("Vidéo"),
    TEXTE("Texte"),
    QUIZ("Quiz"),
    MIXTE("Mixte");

    private final String displayName;

    TypeCours(String displayName) {
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
