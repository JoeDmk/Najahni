package models.investissement;

/**
 * Enum représentant le statut d'une opportunité d'investissement.
 * OPEN   : L'opportunité est ouverte aux offres.
 * CLOSED : L'opportunité est fermée (deadline dépassée ou manuellement).
 * FUNDED : L'opportunité a atteint son objectif de financement.
 */
public enum OpportunityStatus {
    OPEN("Ouverte"),
    CLOSED("Fermée"),
    FUNDED("Financée");

    private final String displayName;

    OpportunityStatus(String displayName) {
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
