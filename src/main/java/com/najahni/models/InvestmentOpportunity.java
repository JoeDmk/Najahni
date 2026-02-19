package com.najahni.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Modèle représentant une Opportunité d'Investissement.
 * 
 * Une opportunité est créée par un ENTREPRENEUR pour demander
 * un financement sur un projet donné. Elle définit un montant cible
 * (target_amount), une description, une deadline et un statut.
 * 
 * Relation : investment_opportunity (N) ←→ (1) project
 * Relation : investment_opportunity (1) ←→ (N) investment_offer
 */
public class InvestmentOpportunity {

    private int id;
    private BigDecimal targetAmount;
    private String description;
    private LocalDate deadline;
    private OpportunityStatus status;
    private int projectId;

    /** Score de risque IA calculé (0–100). Null si pas encore calculé. */
    private Double riskScore;

    /** Label de risque ML prédit ("faible", "moyen", "eleve"). Null si pas encore prédit. */
    private String riskLabel;

    // Champs transients pour l'affichage (issus de JOIN SQL)
    private String projectTitle;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ─── Constructeurs ───────────────────────────────────────

    /** Constructeur par défaut. Statut initial = OPEN. */
    public InvestmentOpportunity() {
        this.status = OpportunityStatus.OPEN;
    }

    /** Constructeur sans id (pour création). */
    public InvestmentOpportunity(BigDecimal targetAmount, String description,
                                  LocalDate deadline, OpportunityStatus status, int projectId) {
        this.targetAmount = targetAmount;
        this.description = description;
        this.deadline = deadline;
        this.status = status;
        this.projectId = projectId;
    }

    /** Constructeur complet (pour lecture depuis la BDD). */
    public InvestmentOpportunity(int id, BigDecimal targetAmount, String description,
                                  LocalDate deadline, OpportunityStatus status, int projectId) {
        this.id = id;
        this.targetAmount = targetAmount;
        this.description = description;
        this.deadline = deadline;
        this.status = status;
        this.projectId = projectId;
    }

    // ─── Getters & Setters ───────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public OpportunityStatus getStatus() { return status; }
    public void setStatus(OpportunityStatus status) { this.status = status; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }

    public String getRiskLabel() { return riskLabel; }
    public void setRiskLabel(String riskLabel) { this.riskLabel = riskLabel; }

    /**
     * Retourne le label ML formaté pour affichage.
     * @return ex: "🟢 Faible" ou "Non prédit"
     */
    public String getFormattedRiskLabel() {
        if (riskLabel == null || riskLabel.isBlank()) return "Non prédit";
        String emoji = switch (riskLabel.toLowerCase()) {
            case "faible" -> "🟢";
            case "moyen" -> "🟡";
            case "eleve", "élevé" -> "🔴";
            default -> "⚪";
        };
        String display = switch (riskLabel.toLowerCase()) {
            case "faible" -> "Faible";
            case "moyen" -> "Moyen";
            case "eleve", "élevé" -> "Élevé";
            default -> riskLabel;
        };
        return emoji + " " + display;
    }

    /**
     * Retourne le niveau de risque textuel basé sur le score IA.
     * @return "Faible", "Moyen", "Élevé" ou "—" si non calculé
     */
    public String getRiskLevel() {
        if (riskScore == null) return "—";
        int score = (int) Math.round(riskScore);
        if (score <= 33) return "Faible";
        if (score <= 66) return "Moyen";
        return "Élevé";
    }

    /**
     * Retourne l'affichage formaté du score de risque.
     * @return ex: "🟢 25/100 (Faible)" ou "Non calculé"
     */
    public String getFormattedRiskScore() {
        if (riskScore == null) return "Non calculé";
        int score = (int) Math.round(riskScore);
        String emoji;
        if (score <= 33) emoji = "🟢";
        else if (score <= 66) emoji = "🟡";
        else emoji = "🔴";
        return emoji + " " + score + "/100 (" + getRiskLevel() + ")";
    }

    // ─── Méthodes utilitaires ────────────────────────────────

    /** Retourne le montant formaté avec symbole monétaire. */
    public String getFormattedAmount() {
        if (targetAmount == null) return "0,00 €";
        return String.format("%,.2f €", targetAmount);
    }

    @Override
    public String toString() {
        return getFormattedAmount() + " - " + status + " [" + (projectTitle != null ? projectTitle : "Projet #" + projectId) + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        InvestmentOpportunity that = (InvestmentOpportunity) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
