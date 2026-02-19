package com.najahni.services.ml;

/**
 * Résultat de la prédiction ML du niveau de risque.
 *
 * <p>Contient :</p>
 * <ul>
 *   <li><b>label</b> : Le label prédit ("faible", "moyen", "eleve")</li>
 *   <li><b>probability</b> : La probabilité de confiance (0.0 – 1.0)</li>
 *   <li><b>allLabels</b> : Tous les labels possibles</li>
 *   <li><b>allProbabilities</b> : La distribution complète des probabilités</li>
 * </ul>
 *
 * @see RiskPredictor
 */
public class RiskPrediction {

    private final String label;
    private final double probability;
    private final String[] allLabels;
    private final double[] allProbabilities;

    public RiskPrediction(String label, double probability,
                          String[] allLabels, double[] allProbabilities) {
        this.label = label;
        this.probability = probability;
        this.allLabels = allLabels;
        this.allProbabilities = allProbabilities;
    }

    /** Label prédit : "faible", "moyen" ou "eleve". */
    public String getLabel() { return label; }

    /** Probabilité de confiance (0.0 – 1.0). */
    public double getProbability() { return probability; }

    /** Probabilité en pourcentage (0 – 100). */
    public double getProbabilityPercent() { return probability * 100.0; }

    /** Tous les labels possibles. */
    public String[] getAllLabels() { return allLabels; }

    /** Distribution complète des probabilités. */
    public double[] getAllProbabilities() { return allProbabilities; }

    /**
     * Retourne le label avec majuscule et accent.
     * "faible" → "Faible", "moyen" → "Moyen", "eleve" → "Élevé"
     */
    public String getDisplayLabel() {
        return switch (label) {
            case "faible" -> "Faible";
            case "moyen"  -> "Moyen";
            case "eleve"  -> "Élevé";
            default -> label;
        };
    }

    /** Emoji correspondant au niveau. */
    public String getEmoji() {
        return switch (label) {
            case "faible" -> "🟢";
            case "moyen"  -> "🟡";
            case "eleve"  -> "🔴";
            default -> "⚪";
        };
    }

    /**
     * Affichage formaté : "🟢 Faible (85.3%)"
     */
    public String getDisplay() {
        return getEmoji() + " " + getDisplayLabel()
            + " (" + String.format("%.1f%%", getProbabilityPercent()) + ")";
    }

    /**
     * Affichage complet avec toutes les probabilités.
     */
    public String getDetailedDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append(getEmoji()).append(" Prédiction : ").append(getDisplayLabel())
          .append(" (confiance : ").append(String.format("%.1f%%", getProbabilityPercent())).append(")\n\n");
        sb.append("📊 Distribution des probabilités :\n");
        if (allLabels != null && allProbabilities != null) {
            for (int i = 0; i < allLabels.length; i++) {
                String displayName = switch (allLabels[i]) {
                    case "faible" -> "🟢 Faible";
                    case "moyen"  -> "🟡 Moyen";
                    case "eleve"  -> "🔴 Élevé";
                    default -> allLabels[i];
                };
                sb.append("   ").append(displayName).append(" : ")
                  .append(String.format("%.1f%%", allProbabilities[i] * 100))
                  .append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "RiskPrediction{label='" + label + "', probability="
            + String.format("%.3f", probability) + "}";
    }
}
