package models.investissement;

import java.time.LocalDateTime;

/**
 * Modèle représentant un Contrat d'investissement.
 * Un contrat est créé automatiquement après le paiement d'une offre.
 * Les deux parties (investisseur et entrepreneur) doivent signer avec SHA-256.
 */
public class Contract {

    public enum ContractStatus {
        PENDING,         // Created, awaiting signatures
        INVESTOR_SIGNED, // Investor has signed
        ENTREPRENEUR_SIGNED, // Entrepreneur has signed
        FULLY_SIGNED,    // Both parties signed
        CANCELLED
    }

    private int id;
    private int offerId;
    private int investorId;
    private int entrepreneurId;

    // SHA-256 digital signatures
    private String investorSignature;
    private String entrepreneurSignature;

    private LocalDateTime investorSignedAt;
    private LocalDateTime entrepreneurSignedAt;

    private ContractStatus status;
    private LocalDateTime createdAt;

    // Transient fields for display (from JOINs)
    private String investorName;
    private String entrepreneurName;
    private String projectTitle;
    private String projectSector;
    private String proposedAmount;
    private String paymentIntentId;

    public Contract() {
        this.status = ContractStatus.PENDING;
    }

    public Contract(int offerId, int investorId, int entrepreneurId) {
        this.offerId = offerId;
        this.investorId = investorId;
        this.entrepreneurId = entrepreneurId;
        this.status = ContractStatus.PENDING;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOfferId() { return offerId; }
    public void setOfferId(int offerId) { this.offerId = offerId; }

    public int getInvestorId() { return investorId; }
    public void setInvestorId(int investorId) { this.investorId = investorId; }

    public int getEntrepreneurId() { return entrepreneurId; }
    public void setEntrepreneurId(int entrepreneurId) { this.entrepreneurId = entrepreneurId; }

    public String getInvestorSignature() { return investorSignature; }
    public void setInvestorSignature(String investorSignature) { this.investorSignature = investorSignature; }

    public String getEntrepreneurSignature() { return entrepreneurSignature; }
    public void setEntrepreneurSignature(String entrepreneurSignature) { this.entrepreneurSignature = entrepreneurSignature; }

    public LocalDateTime getInvestorSignedAt() { return investorSignedAt; }
    public void setInvestorSignedAt(LocalDateTime investorSignedAt) { this.investorSignedAt = investorSignedAt; }

    public LocalDateTime getEntrepreneurSignedAt() { return entrepreneurSignedAt; }
    public void setEntrepreneurSignedAt(LocalDateTime entrepreneurSignedAt) { this.entrepreneurSignedAt = entrepreneurSignedAt; }

    public ContractStatus getStatus() { return status; }
    public void setStatus(ContractStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getInvestorName() { return investorName; }
    public void setInvestorName(String investorName) { this.investorName = investorName; }

    public String getEntrepreneurName() { return entrepreneurName; }
    public void setEntrepreneurName(String entrepreneurName) { this.entrepreneurName = entrepreneurName; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public String getProjectSector() { return projectSector; }
    public void setProjectSector(String projectSector) { this.projectSector = projectSector; }

    public String getProposedAmount() { return proposedAmount; }
    public void setProposedAmount(String proposedAmount) { this.proposedAmount = proposedAmount; }

    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }

    public boolean isInvestorSigned() {
        return investorSignature != null && !investorSignature.isEmpty();
    }

    public boolean isEntrepreneurSigned() {
        return entrepreneurSignature != null && !entrepreneurSignature.isEmpty();
    }

    public boolean isFullySigned() {
        return status == ContractStatus.FULLY_SIGNED;
    }

    public String getStatusDisplayName() {
        return switch (status) {
            case PENDING -> "⏳ En attente";
            case INVESTOR_SIGNED -> "✍️ Signé par l'investisseur";
            case ENTREPRENEUR_SIGNED -> "✍️ Signé par l'entrepreneur";
            case FULLY_SIGNED -> "✅ Signé par les deux parties";
            case CANCELLED -> "❌ Annulé";
        };
    }

    @Override
    public String toString() {
        return "Contract #" + id + " [" + status + "] Offre #" + offerId;
    }
}
