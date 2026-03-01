package com.najahni.models;

import java.time.LocalDateTime;

/**
 * Modèle représentant un Contrat d'Investissement Numérique.
 * 
 * Généré automatiquement après paiement, le contrat doit être signé
 * par l'investisseur puis par l'entrepreneur pour être validé.
 * L'intégrité est vérifiable via un hash SHA-256 et un QR code.
 */
public class InvestmentContract {

    private int id;
    private int offerId;
    private int investorId;
    private int entrepreneurId;
    private String contractNumber;        // ex: NAJAHNI-2026-000042
    private ContractStatus status;
    private String termsText;             // Texte juridique auto-généré
    private String investorSignature;     // Base64 de l'image signature
    private String entrepreneurSignature; // Base64 de l'image signature
    private String sha256Hash;            // Hash d'intégrité du contrat
    private LocalDateTime investorSignedAt;
    private LocalDateTime entrepreneurSignedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient display fields
    private String investorName;
    private String entrepreneurName;
    private String projectTitle;
    private String offerAmount;

    public InvestmentContract() {
        this.status = ContractStatus.DRAFT;
    }

    // ─── Getters & Setters ───────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOfferId() { return offerId; }
    public void setOfferId(int offerId) { this.offerId = offerId; }

    public int getInvestorId() { return investorId; }
    public void setInvestorId(int investorId) { this.investorId = investorId; }

    public int getEntrepreneurId() { return entrepreneurId; }
    public void setEntrepreneurId(int entrepreneurId) { this.entrepreneurId = entrepreneurId; }

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public ContractStatus getStatus() { return status; }
    public void setStatus(ContractStatus status) { this.status = status; }

    public String getTermsText() { return termsText; }
    public void setTermsText(String termsText) { this.termsText = termsText; }

    public String getInvestorSignature() { return investorSignature; }
    public void setInvestorSignature(String investorSignature) { this.investorSignature = investorSignature; }

    public String getEntrepreneurSignature() { return entrepreneurSignature; }
    public void setEntrepreneurSignature(String entrepreneurSignature) { this.entrepreneurSignature = entrepreneurSignature; }

    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }

    public LocalDateTime getInvestorSignedAt() { return investorSignedAt; }
    public void setInvestorSignedAt(LocalDateTime investorSignedAt) { this.investorSignedAt = investorSignedAt; }

    public LocalDateTime getEntrepreneurSignedAt() { return entrepreneurSignedAt; }
    public void setEntrepreneurSignedAt(LocalDateTime entrepreneurSignedAt) { this.entrepreneurSignedAt = entrepreneurSignedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getInvestorName() { return investorName; }
    public void setInvestorName(String investorName) { this.investorName = investorName; }

    public String getEntrepreneurName() { return entrepreneurName; }
    public void setEntrepreneurName(String entrepreneurName) { this.entrepreneurName = entrepreneurName; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public String getOfferAmount() { return offerAmount; }
    public void setOfferAmount(String offerAmount) { this.offerAmount = offerAmount; }

    public String getStatusEmoji() {
        return switch (status) {
            case DRAFT -> "📝";
            case INVESTOR_SIGNED -> "✍️";
            case FULLY_SIGNED -> "✅";
            case CANCELLED -> "❌";
        };
    }

    @Override
    public String toString() {
        return contractNumber + " — " + status.getDisplayName();
    }
}
