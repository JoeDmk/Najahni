package com.najahni.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Profil d'investissement d'un utilizateur.
 * Utilisé par l'IA Matching Engine pour recommander des opportunités compatibles.
 */
public class InvestorProfile {

    private int id;
    private int userId;
    private String preferredSectors;      // CSV: "tech,santé,énergie"
    private int riskTolerance;            // 1-10
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private int horizonMonths;            // Investment horizon in months
    private String description;           // Free-text about investment goals
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient
    private String userName;

    public InvestorProfile() {
        this.riskTolerance = 5;
    }

    // ─── Getters & Setters ───────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getPreferredSectors() { return preferredSectors; }
    public void setPreferredSectors(String preferredSectors) { this.preferredSectors = preferredSectors; }

    public int getRiskTolerance() { return riskTolerance; }
    public void setRiskTolerance(int riskTolerance) { this.riskTolerance = riskTolerance; }

    public BigDecimal getBudgetMin() { return budgetMin; }
    public void setBudgetMin(BigDecimal budgetMin) { this.budgetMin = budgetMin; }

    public BigDecimal getBudgetMax() { return budgetMax; }
    public void setBudgetMax(BigDecimal budgetMax) { this.budgetMax = budgetMax; }

    public int getHorizonMonths() { return horizonMonths; }
    public void setHorizonMonths(int horizonMonths) { this.horizonMonths = horizonMonths; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    /** Returns sectors as array. */
    public String[] getSectorArray() {
        if (preferredSectors == null || preferredSectors.isBlank()) return new String[0];
        return preferredSectors.split(",");
    }
}
