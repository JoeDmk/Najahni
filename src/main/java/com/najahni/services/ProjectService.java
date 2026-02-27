package com.najahni.services;

import com.najahni.models.Project;
import com.najahni.models.ProjectStatus;
import com.najahni.models.Role;
import com.najahni.utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service pour la gestion des Projets.
 * Accède directement à la base de données via JDBC (pas de DAO).
 */
public class ProjectService {

    private Connection cnx;

    public ProjectService() {
        this.cnx = DBConnection.getInstance().getConnection();
    }

    // ─── CRUD ────────────────────────────────────────────────

    public Project createProject(Project project) throws IllegalArgumentException {
        validateProject(project);
        if (project.getEntrepreneurId() > 0) {
            validateEntrepreneur(project.getEntrepreneurId());
        }

        String sql = project.getEntrepreneurId() > 0
                ? "INSERT INTO projet (titre, description, secteur, statut, entrepreneur_id) VALUES (?, ?, ?, ?, ?)"
                : "INSERT INTO projet (titre, description, secteur, statut) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, project.getTitle());
            ps.setString(2, project.getDescription());
            ps.setString(3, project.getSector());
            ps.setString(4, project.getStatus() != null ? project.getStatus().name() : "DRAFT");
            if (project.getEntrepreneurId() > 0) {
                ps.setInt(5, project.getEntrepreneurId());
            }

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) project.setId(rs.getInt(1));
            }
            System.out.println("✓ Project created successfully: " + project.getTitle());
            return project;
        } catch (SQLException e) {
            System.err.println("✗ Error creating project: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Optional<Project> findById(int id) {
        String sql = "SELECT * FROM projet WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToProject(rs));
            }
        } catch (SQLException e) {
            System.err.println("✗ Error finding project by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Project> findAll() {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT * FROM projet ORDER BY date_creation DESC";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) projects.add(mapResultSetToProject(rs));
        } catch (SQLException e) {
            System.err.println("✗ Error fetching all projects: " + e.getMessage());
            e.printStackTrace();
        }
        return projects;
    }

    public boolean updateProject(Project project) throws IllegalArgumentException {
        validateProject(project);
        if (project.getEntrepreneurId() > 0) {
            validateEntrepreneur(project.getEntrepreneurId());
        }

        String sql = "UPDATE projet SET titre = ?, description = ?, secteur = ?, statut = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, project.getTitle());
            ps.setString(2, project.getDescription());
            ps.setString(3, project.getSector());
            ps.setString(4, project.getStatus() != null ? project.getStatus().name() : "DRAFT");
            ps.setInt(5, project.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("✗ Error updating project: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteProject(int id) {
        String sql = "DELETE FROM projet WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("✗ Error deleting project: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ─── REQUÊTES SPÉCIFIQUES ────────────────────────────────

    public List<Project> findByEntrepreneur(int entrepreneurId) {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT * FROM projet WHERE entrepreneur_id = ? ORDER BY date_creation DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, entrepreneurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) projects.add(mapResultSetToProject(rs));
            }
        } catch (SQLException e) {
            // Fallback: if entrepreneur_id column doesn't exist, return all
            System.err.println("⚠ findByEntrepreneur fallback to findAll: " + e.getMessage());
            return findAll();
        }
        return projects;
    }

    public List<Project> findByStatus(ProjectStatus status) {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT * FROM projet WHERE statut = ? ORDER BY date_creation DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) projects.add(mapResultSetToProject(rs));
            }
        } catch (SQLException e) {
            System.err.println("✗ Error finding projects by status: " + e.getMessage());
            e.printStackTrace();
        }
        return projects;
    }

    public List<Project> findBySector(String sector) {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT * FROM projet WHERE secteur = ? ORDER BY date_creation DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, sector);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) projects.add(mapResultSetToProject(rs));
            }
        } catch (SQLException e) {
            System.err.println("✗ Error finding projects by sector: " + e.getMessage());
            e.printStackTrace();
        }
        return projects;
    }

    public List<String> getAllSectors() {
        List<String> sectors = new ArrayList<>();
        String sql = "SELECT DISTINCT secteur FROM projet ORDER BY secteur";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) sectors.add(rs.getString("secteur"));
        } catch (SQLException e) {
            System.err.println("✗ Error fetching sectors: " + e.getMessage());
            e.printStackTrace();
        }
        return sectors;
    }

    public List<Project> getProjectsForInvestment() {
        return findByStatus(ProjectStatus.APPROVED);
    }

    public int countByStatus(ProjectStatus status) {
        String sql = "SELECT COUNT(*) FROM projet WHERE statut = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean updateStatus(int projectId, ProjectStatus newStatus) {
        Optional<Project> projectOpt = findById(projectId);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            project.setStatus(newStatus);
            return updateProject(project);
        }
        return false;
    }

    // ─── MAPPING ─────────────────────────────────────────────

    private Project mapResultSetToProject(ResultSet rs) throws SQLException {
        Project project = new Project();
        project.setId(rs.getInt("id"));
        project.setTitle(rs.getString("titre"));
        project.setDescription(rs.getString("description"));
        project.setSector(rs.getString("secteur"));

        String statut = rs.getString("statut");
        if (statut != null) {
            try { project.setStatus(ProjectStatus.valueOf(statut.toUpperCase())); }
            catch (IllegalArgumentException e) { project.setStatus(ProjectStatus.DRAFT); }
        } else {
            project.setStatus(ProjectStatus.DRAFT);
        }

        try { project.setEntrepreneurId(rs.getInt("entrepreneur_id")); }
        catch (SQLException ignored) { project.setEntrepreneurId(0); }

        try {
            java.sql.Date dateCreation = rs.getDate("date_creation");
            if (dateCreation != null) project.setCreatedAt(dateCreation.toLocalDate().atStartOfDay());
        } catch (SQLException ignored) { }

        return project;
    }

    // ─── VALIDATION ──────────────────────────────────────────

    private void validateProject(Project project) throws IllegalArgumentException {
        if (project == null) throw new IllegalArgumentException("Project cannot be null");
        if (project.getTitle() == null || project.getTitle().trim().isEmpty())
            throw new IllegalArgumentException("Title is required");
        if (project.getTitle().length() < 3)
            throw new IllegalArgumentException("Title must be at least 3 characters");
        if (project.getTitle().length() > 200)
            throw new IllegalArgumentException("Title cannot exceed 200 characters");
        if (project.getSector() == null || project.getSector().trim().isEmpty())
            throw new IllegalArgumentException("Sector is required");
        if (project.getSector().length() > 100)
            throw new IllegalArgumentException("Sector cannot exceed 100 characters");
        if (project.getStatus() == null)
            throw new IllegalArgumentException("Status is required");
    }

    private void validateEntrepreneur(int entrepreneurId) throws IllegalArgumentException {
        String sql = "SELECT role FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, entrepreneurId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Entrepreneur not found");
                String role = rs.getString("role");
                if (!Role.ENTREPRENEUR.getDbValue().equals(role))
                    throw new IllegalArgumentException("Selected user is not an entrepreneur");
            }
        } catch (SQLException e) {
            System.err.println("✗ Error validating entrepreneur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
