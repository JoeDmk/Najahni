package com.najahni.services;

import com.najahni.dao.InvestmentOpportunityDAO;
import com.najahni.dao.ProjectDAO;
import com.najahni.models.InvestmentOpportunity;
import com.najahni.models.InvestmentStatus;
import com.najahni.models.Project;
import com.najahni.models.ProjectStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service class for InvestmentOpportunity business logic.
 * Handles validation and business rules for investment operations.
 */
public class InvestmentOpportunityService {

    private final InvestmentOpportunityDAO investmentDAO;
    private final ProjectDAO projectDAO;

    public InvestmentOpportunityService() {
        this.investmentDAO = new InvestmentOpportunityDAO();
        this.projectDAO = new ProjectDAO();
    }

    /**
     * Creates a new investment opportunity after validation.
     * @param investment The investment to create
     * @return The created investment
     * @throws IllegalArgumentException if validation fails
     */
    public InvestmentOpportunity createInvestment(InvestmentOpportunity investment) throws IllegalArgumentException {
        validateInvestment(investment);
        validateProject(investment.getProjectId());
        
        return investmentDAO.create(investment);
    }

    /**
     * Updates an existing investment after validation.
     * @param investment The investment to update
     * @return true if update was successful
     * @throws IllegalArgumentException if validation fails
     */
    public boolean updateInvestment(InvestmentOpportunity investment) throws IllegalArgumentException {
        validateInvestment(investment);
        validateProject(investment.getProjectId());
        
        return investmentDAO.update(investment);
    }

    /**
     * Deletes an investment by ID.
     * @param id The investment ID
     * @return true if deletion was successful
     */
    public boolean deleteInvestment(int id) {
        return investmentDAO.delete(id);
    }

    /**
     * Finds an investment by ID.
     * @param id The investment ID
     * @return Optional containing the investment if found
     */
    public Optional<InvestmentOpportunity> findById(int id) {
        return investmentDAO.findById(id);
    }

    /**
     * Returns all investments.
     * @return List of all investments
     */
    public List<InvestmentOpportunity> findAll() {
        return investmentDAO.findAll();
    }

    /**
     * Returns all investments for a project.
     * @param projectId The project ID
     * @return List of investments
     */
    public List<InvestmentOpportunity> findByProject(int projectId) {
        return investmentDAO.findByProjectId(projectId);
    }

    /**
     * Returns all investments with a specific status.
     * @param status The investment status
     * @return List of investments
     */
    public List<InvestmentOpportunity> findByStatus(InvestmentStatus status) {
        return investmentDAO.findByStatus(status);
    }

    /**
     * Gets total investment for a project.
     * @param projectId The project ID
     * @return Total investment amount
     */
    public BigDecimal getTotalInvestmentForProject(int projectId) {
        return investmentDAO.getTotalInvestmentForProject(projectId);
    }

    /**
     * Gets total investment amount across all projects.
     * @return Total investment amount
     */
    public BigDecimal getTotalInvestmentAmount() {
        return investmentDAO.getTotalInvestmentAmount();
    }

    /**
     * Counts investments by status.
     * @param status The investment status
     * @return Number of investments
     */
    public int countByStatus(InvestmentStatus status) {
        return investmentDAO.countByStatus(status);
    }

    /**
     * Accepts an investment.
     * @param investmentId The investment ID
     * @return true if successful
     */
    public boolean acceptInvestment(int investmentId) {
        return updateStatus(investmentId, InvestmentStatus.ACCEPTED);
    }

    /**
     * Rejects an investment.
     * @param investmentId The investment ID
     * @return true if successful
     */
    public boolean rejectInvestment(int investmentId) {
        return updateStatus(investmentId, InvestmentStatus.REJECTED);
    }

    /**
     * Marks an investment as completed.
     * @param investmentId The investment ID
     * @return true if successful
     */
    public boolean completeInvestment(int investmentId) {
        return updateStatus(investmentId, InvestmentStatus.COMPLETED);
    }

    /**
     * Updates investment status.
     * @param investmentId The investment ID
     * @param newStatus The new status
     * @return true if update was successful
     */
    public boolean updateStatus(int investmentId, InvestmentStatus newStatus) {
        Optional<InvestmentOpportunity> investmentOpt = investmentDAO.findById(investmentId);
        if (investmentOpt.isPresent()) {
            InvestmentOpportunity investment = investmentOpt.get();
            investment.setStatus(newStatus);
            return investmentDAO.update(investment);
        }
        return false;
    }

    /**
     * Validates investment data.
     * @param investment The investment to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateInvestment(InvestmentOpportunity investment) throws IllegalArgumentException {
        if (investment == null) {
            throw new IllegalArgumentException("Investment cannot be null");
        }
        
        // Validate amount
        if (investment.getAmount() == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (investment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (investment.getAmount().compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw new IllegalArgumentException("Amount exceeds maximum allowed value");
        }
        
        // Validate status
        if (investment.getStatus() == null) {
            throw new IllegalArgumentException("Status is required");
        }
        
        // Validate project ID
        if (investment.getProjectId() <= 0) {
            throw new IllegalArgumentException("Valid project is required");
        }
    }

    /**
     * Validates that the project exists and is eligible for investment.
     * @param projectId The project ID
     * @throws IllegalArgumentException if validation fails
     */
    private void validateProject(int projectId) throws IllegalArgumentException {
        Optional<Project> projectOpt = projectDAO.findById(projectId);
        
        if (projectOpt.isEmpty()) {
            throw new IllegalArgumentException("Project not found");
        }
        
        Project project = projectOpt.get();
        if (project.getStatus() != ProjectStatus.APPROVED && project.getStatus() != ProjectStatus.FUNDED) {
            // Allow investments only for approved or funded projects
            // You can adjust this business rule as needed
        }
    }
}
