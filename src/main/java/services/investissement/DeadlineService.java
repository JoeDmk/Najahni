package services.investissement;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Service de suivi intelligent des deadlines.
 *
 * <h3>Badges colorés :</h3>
 * <ul>
 *   <li>🔴 Rouge : moins de 7 jours (URGENT)</li>
 *   <li>🟠 Orange : moins de 15 jours (ATTENTION)</li>
 *   <li>🟡 Jaune : moins de 30 jours (PROCHE)</li>
 *   <li>🟢 Vert : plus de 30 jours (OK)</li>
 *   <li>⚫ Noir : deadline dépassée (EXPIRÉ)</li>
 * </ul>
 */
public class DeadlineService {

    /** Seuils en jours. */
    public static final int SEUIL_URGENT = 7;
    public static final int SEUIL_ATTENTION = 15;
    public static final int SEUIL_PROCHE = 30;

    /**
     * Calcule le nombre de jours restants avant la deadline.
     *
     * @param deadline Date limite (peut être null)
     * @return Nombre de jours restants, ou Long.MAX_VALUE si null
     */
    public static long calculateRemainingDays(LocalDate deadline) {
        if (deadline == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), deadline);
    }

    /**
     * Retourne le badge textuel coloré pour l'affichage.
     *
     * @param deadline Date limite
     * @return Badge formaté (ex: "🔴 3j — URGENT")
     */
    public static String getDeadlineBadge(LocalDate deadline) {
        if (deadline == null) return "—";

        long days = calculateRemainingDays(deadline);

        if (days < 0) {
            return "⚫ Expiré (" + Math.abs(days) + "j)";
        } else if (days == 0) {
            return "🔴 Aujourd'hui !";
        } else if (days <= SEUIL_URGENT) {
            return "🔴 " + days + "j — URGENT";
        } else if (days <= SEUIL_ATTENTION) {
            return "🟠 " + days + "j — Attention";
        } else if (days <= SEUIL_PROCHE) {
            return "🟡 " + days + "j — Proche";
        } else {
            return "🟢 " + days + "j";
        }
    }

    /**
     * Retourne le niveau d'urgence.
     */
    public static DeadlineLevel getLevel(LocalDate deadline) {
        if (deadline == null) return DeadlineLevel.NONE;
        long days = calculateRemainingDays(deadline);

        if (days < 0) return DeadlineLevel.EXPIRED;
        if (days <= SEUIL_URGENT) return DeadlineLevel.URGENT;
        if (days <= SEUIL_ATTENTION) return DeadlineLevel.ATTENTION;
        if (days <= SEUIL_PROCHE) return DeadlineLevel.PROCHE;
        return DeadlineLevel.OK;
    }

    /**
     * Retourne le style CSS JavaFX pour le badge de deadline.
     */
    public static String getDeadlineStyle(LocalDate deadline) {
        DeadlineLevel level = getLevel(deadline);
        return switch (level) {
            case EXPIRED -> "-fx-text-fill: #1a1a2e; -fx-font-weight: bold; -fx-background-color: rgba(26,26,46,0.1); -fx-background-radius: 4; -fx-padding: 2 6;";
            case URGENT -> "-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-background-color: rgba(231,76,60,0.1); -fx-background-radius: 4; -fx-padding: 2 6;";
            case ATTENTION -> "-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-background-color: rgba(230,126,34,0.1); -fx-background-radius: 4; -fx-padding: 2 6;";
            case PROCHE -> "-fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-background-color: rgba(243,156,18,0.1); -fx-background-radius: 4; -fx-padding: 2 6;";
            case OK -> "-fx-text-fill: #27ae60; -fx-background-color: rgba(39,174,96,0.08); -fx-background-radius: 4; -fx-padding: 2 6;";
            case NONE -> "-fx-text-fill: #95a5a6; -fx-font-style: italic;";
        };
    }

    /**
     * Niveaux d'urgence de deadline.
     */
    public enum DeadlineLevel {
        EXPIRED, URGENT, ATTENTION, PROCHE, OK, NONE
    }
}
