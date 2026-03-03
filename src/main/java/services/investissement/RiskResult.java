package services.investissement;

/**
 * Résultat du calcul de risque pour une opportunité d'investissement.
 *
 * <p>Contient :</p>
 * <ul>
 *   <li><b>score</b> : Valeur numérique entre 0 et 100</li>
 *   <li><b>level</b> : Niveau textuel ("Faible", "Moyen", "Élevé")</li>
 *   <li><b>emoji</b> : Indicateur visuel (🟢, 🟡, 🔴)</li>
 * </ul>
 *
 * @see RiskCalculator
 * @see RiskService
 */
public class RiskResult {

    private final int score;
    private final String level;
    private final String emoji;

    /**
     * Constructeur.
     *
     * @param score Score de risque (0–100)
     * @param level Niveau textuel ("Faible", "Moyen", "Élevé")
     * @param emoji Emoji (🟢, 🟡, 🔴)
     */
    public RiskResult(int score, String level, String emoji) {
        this.score = score;
        this.level = level;
        this.emoji = emoji;
    }

    /** Score de risque entre 0 et 100. */
    public int getScore() { return score; }

    /** Niveau de risque : "Faible", "Moyen" ou "Élevé". */
    public String getLevel() { return level; }

    /** Emoji du niveau : 🟢, 🟡 ou 🔴. */
    public String getEmoji() { return emoji; }

    /**
     * Affichage formaté : "🟢 25/100 (Faible)".
     */
    public String getDisplay() {
        return emoji + " " + score + "/100 (" + level + ")";
    }

    @Override
    public String toString() {
        return "RiskResult{score=" + score + ", level='" + level + "', emoji='" + emoji + "'}";
    }
}
