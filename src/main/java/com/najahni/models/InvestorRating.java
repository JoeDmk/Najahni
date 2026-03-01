package com.najahni.models;

import java.time.LocalDateTime;

/**
 * Avis / notation d'un investisseur sur une opportunité ou un projet.
 * Permet aux investisseurs de donner un feedback après investissement.
 */
public class InvestorRating {

    private int id;
    private int investorId;
    private int opportunityId;
    private int rating;           // 1-5 étoiles
    private String comment;       // Avis textuel
    private LocalDateTime createdAt;

    // Transient
    private String investorName;
    private String opportunityDescription;
    private String projectTitle;

    public InvestorRating() {}

    public InvestorRating(int investorId, int opportunityId, int rating, String comment) {
        this.investorId = investorId;
        this.opportunityId = opportunityId;
        this.rating = rating;
        this.comment = comment;
    }

    // ─── Getters & Setters ───────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getInvestorId() { return investorId; }
    public void setInvestorId(int investorId) { this.investorId = investorId; }

    public int getOpportunityId() { return opportunityId; }
    public void setOpportunityId(int opportunityId) { this.opportunityId = opportunityId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = Math.max(1, Math.min(5, rating)); }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getInvestorName() { return investorName; }
    public void setInvestorName(String investorName) { this.investorName = investorName; }

    public String getOpportunityDescription() { return opportunityDescription; }
    public void setOpportunityDescription(String opportunityDescription) { this.opportunityDescription = opportunityDescription; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    /** Returns star display: ★★★★☆ */
    public String getStarDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            sb.append(i <= rating ? "★" : "☆");
        }
        return sb.toString();
    }

    /** Returns emoji color by rating. */
    public String getRatingColor() {
        if (rating >= 4) return "#27ae60";
        if (rating >= 3) return "#f39c12";
        return "#e74c3c";
    }
}
