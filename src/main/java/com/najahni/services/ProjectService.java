package com.najahni.services;

import com.najahni.dao.ProjectDAO;
import com.najahni.dao.UserDAO;
import com.najahni.models.Project;
import com.najahni.models.ProjectStatus;
import com.najahni.models.Role;
import com.najahni.models.User;

import java.util.List;
import java.util.Optional;

/**
 * Service class for Project business logic.
 * Handles validation and business rules for project operations.
 */
public class ProjectService {

    private final ProjectDAO projectDAO;
    private final UserDAO userDAO;

    public ProjectService() {
        this.projectDAO = new ProjectDAO();
        this.userDAO = new UserDAO();
    }

    /**
     * Creates a new project after validation.
     * @param project The project to create
     * @return The created project
     * @throws IllegalArgumentException if validation fails
     */
    public Project createProject(Project project) throws IllegalArgumentException {
        validateProject(project);
        validateEntrepreneur(project.getEntrepreneurId());
        
        return projectDAO.create(project);
    }

    /**
     * Updates an existing project after validation.
     * @param project The project to update
     * @return true if update was successful
     * @throws IllegalArgumentException if validation fails
     */
    public boolean updateProject(Project project) throws IllegalArgumentException {
        validateProject(project);
        validateEntrepreneur(project.getEntrepreneurId());
        
        return projectDAO.update(project);
    }

    /**
     * Deletes a project by ID.
     * @param id The project ID
     * @return true if deletion was successful
     */
    public boolean deleteProject(int id) {
        return projectDAO.delete(id);
    }

    /**
     * Finds a project by ID.
     * @param id The project ID
     * @return Optional containing the project if found
     */
    public Optional<Project> findById(int id) {
        return projectDAO.findById(id);
    }

    /**
     * Returns all projects.
     * @return List of all projects
     */
    public List<Project> findAll() {
        return projectDAO.findAll();
    }

    /**
     * Returns all projects by entrepreneur.
     * @param entrepreneurId The entrepreneur's user ID
     * @return List of projects
     */
    public List<Project> findByEntrepreneur(int entrepreneurId) {
        return projectDAO.findByEntrepreneurId(entrepreneurId);
    }

    /**
     * Returns all projects with a specific status.
     * @param status The project status
     * @return List of projects
     */
    public List<Project> findByStatus(ProjectStatus status) {
        return projectDAO.findByStatus(status);
    }

    /**
     * Returns all projects in a specific sector.
     * @param sector The sector name
     * @return List of projects
     */
    public List<Project> findBySector(String sector) {
        return projectDAO.findBySector(sector);
    }

    /**
     * Returns all unique sectors.
     * @return List of sector names
     */
    public List<String> getAllSectors() {
        return projectDAO.getAllSectors();
    }

    /**
     * Returns approved projects available for investment.
     * @return List of approved projects
     */
    public List<Project> getProjectsForInvestment() {
        return projectDAO.findByStatus(ProjectStatus.APPROVED);
    }

    /**
     * Counts projects by status.
     * @param status The project status
     * @return Number of projects
     */
    public int countByStatus(ProjectStatus status) {
        return projectDAO.countByStatus(status);
    }

    /**
     * Updates project status.
     * @param projectId The project ID
     * @param newStatus The new status
     * @return true if update was successful
     */
    public boolean updateStatus(int projectId, ProjectStatus newStatus) {
        Optional<Project> projectOpt = projectDAO.findById(projectId);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            project.setStatus(newStatus);
            return projectDAO.update(project);
        }
        return false;
    }

    /**
     * Validates project data.
     * @param project The project to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateProject(Project project) throws IllegalArgumentException {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }
        
        // Validate title
        if (project.getTitle() == null || project.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (project.getTitle().length() < 3) {
            throw new IllegalArgumentException("Title must be at least 3 characters");
        }
        if (project.getTitle().length() > 200) {
            throw new IllegalArgumentException("Title cannot exceed 200 characters");
        }
        
        // Validate sector
        if (project.getSector() == null || project.getSector().trim().isEmpty()) {
            throw new IllegalArgumentException("Sector is required");
        }
        if (project.getSector().length() > 100) {
            throw new IllegalArgumentException("Sector cannot exceed 100 characters");
        }
        
        // Validate status
        if (project.getStatus() == null) {
            throw new IllegalArgumentException("Status is required");
        }
        
        // Validate entrepreneur ID
        if (project.getEntrepreneurId() <= 0) {
            throw new IllegalArgumentException("Valid entrepreneur is required");
        }
    }

    /**
     * Validates that the entrepreneur exists and has the correct role.
     * @param entrepreneurId The entrepreneur's user ID
     * @throws IllegalArgumentException if validation fails
     */
    private void validateEntrepreneur(int entrepreneurId) throws IllegalArgumentException {
        Optional<User> userOpt = userDAO.findById(entrepreneurId);
        
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Entrepreneur not found");
        }
        
        User user = userOpt.get();
        if (user.getRole() != Role.ENTREPRENEUR) {
            throw new IllegalArgumentException("Selected user is not an entrepreneur");
        }
    }
}
