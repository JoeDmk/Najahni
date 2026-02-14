package com.najahni.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model class representing an Investment Opportunity entity.
 * Investment opportunities are linked to projects.
 */
public class InvestmentOpportunity {

    private int id;
    private BigDecimal amount;
    private InvestmentStatus status;
    private int projectId;
    private String projectTitle; // For display purposes
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public InvestmentOpportunity() {
        this.status = InvestmentStatus.PENDING;
    }

    // Constructor without id (for creating new investments)
    public InvestmentOpportunity(BigDecimal amount, InvestmentStatus status, int projectId) {
        this.amount = amount;
        this.status = status;
        this.projectId = projectId;
    }

    // Full constructor
    public InvestmentOpportunity(int id, BigDecimal amount, InvestmentStatus status, int projectId) {
        this.id = id;
        this.amount = amount;
        this.status = status;
        this.projectId = projectId;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public InvestmentStatus getStatus() {
        return status;
    }

    public void setStatus(InvestmentStatus status) {
        this.status = status;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Returns formatted amount with currency symbol.
     */
    public String getFormattedAmount() {
        return String.format("%,.2f €", amount);
    }

    @Override
    public String toString() {
        return getFormattedAmount() + " - " + status;
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
