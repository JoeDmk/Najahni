package com.najahni.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modèle représentant une Offre d'Investissement.
 * 
 * Une offre est proposée par un INVESTOR en réponse à une
 * opportunité d'investissement ouverte. Elle contient un montant
 * proposé (proposed_amount) et un statut de traitement.
 * 
 * Relation : investment_offer (N) ←→ (1) investment_opportunity
 * Relation : investment_offer (N) ←→ (1) user (investor)
 */
public class InvestmentOffer {

    private int id;
    private BigDecimal proposedAmount;
    private OfferStatus status;
    private int investorId;
    private int opportunityId;

    // Champs transients pour l'affichage (issus de JOIN SQL)
    private String investorName;
    private String opportunityDescription;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ─── Constructeurs ───────────────────────────────────────

    /** Constructeur par défaut. Statut initial = PENDING. */
    public InvestmentOffer() {
        this.status = OfferStatus.PENDING;
    }

    /** Constructeur sans id (pour création). */
    public InvestmentOffer(BigDecimal proposedAmount, OfferStatus status,
                           int investorId, int opportunityId) {
        this.proposedAmount = proposedAmount;
        this.status = status;
        this.investorId = investorId;
        this.opportunityId = opportunityId;
    }

    /** Constructeur complet (pour lecture depuis la BDD). */
    public InvestmentOffer(int id, BigDecimal proposedAmount, OfferStatus status,
                           int investorId, int opportunityId) {
        this.id = id;
        this.proposedAmount = proposedAmount;
        this.status = status;
        this.investorId = investorId;
        this.opportunityId = opportunityId;
    }

    // ─── Getters & Setters ───────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public BigDecimal getProposedAmount() { return proposedAmount; }
    public void setProposedAmount(BigDecimal proposedAmount) { this.proposedAmount = proposedAmount; }

    public OfferStatus getStatus() { return status; }
    public void setStatus(OfferStatus status) { this.status = status; }

    public int getInvestorId() { return investorId; }
    public void setInvestorId(int investorId) { this.investorId = investorId; }

    public int getOpportunityId() { return opportunityId; }
    public void setOpportunityId(int opportunityId) { this.opportunityId = opportunityId; }

    public String getInvestorName() { return investorName; }
    public void setInvestorName(String investorName) { this.investorName = investorName; }

    public String getOpportunityDescription() { return opportunityDescription; }
    public void setOpportunityDescription(String opportunityDescription) { this.opportunityDescription = opportunityDescription; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ─── Méthodes utilitaires ────────────────────────────────

    /** Retourne le montant formaté avec symbole monétaire. */
    public String getFormattedAmount() {
        if (proposedAmount == null) return "0,00 €";
        return String.format("%,.2f €", proposedAmount);
    }

    @Override
    public String toString() {
        return getFormattedAmount() + " - " + status;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        InvestmentOffer that = (InvestmentOffer) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
