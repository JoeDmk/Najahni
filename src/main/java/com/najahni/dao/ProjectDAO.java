package com.najahni.dao;

import com.najahni.models.Project;
import com.najahni.models.ProjectStatus;
import com.najahni.utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Project entity.
 * Handles all database operations related to projects.
 */
public class ProjectDAO implements GenericDAO<Project> {

    private final Connection connection;

    public ProjectDAO() {
        this.connection = DBConnection.getInstance().getConnection();
    }

    @Override
    public Project create(Project project) {
        String sql = "INSERT INTO project (title, description, sector, status, entrepreneur_id) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, project.getTitle());
            stmt.setString(2, project.getDescription());
            stmt.setString(3, project.getSector());
            stmt.setString(4, project.getStatus().name());
            stmt.setInt(5, project.getEntrepreneurId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        project.setId(generatedKeys.getInt(1));
                    }
                }
            }
            
            System.out.println("✓ Project created successfully: " + project.getTitle());
            return project;
            
        } catch (SQLException e) {
            System.err.println("✗ Error creating project: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Optional<Project> findById(int id) {
        String sql = """
            SELECT p.*, u.name as entrepreneur_name 
            FROM project p 
            LEFT JOIN user u ON p.entrepreneur_id = u.id 
            WHERE p.id = ?
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProject(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error finding project by ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return Optional.empty();
    }

    @Override
    public List<Project> findAll() {
        List<Project> projects = new ArrayList<>();
        String sql = """
            SELECT p.*, u.name as entrepreneur_name 
            FROM project p 
            LEFT JOIN user u ON p.entrepreneur_id = u.id 
            ORDER BY p.created_at DESC
            """;
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                projects.add(mapResultSetToProject(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error fetching all projects: " + e.getMessage());
            e.printStackTrace();
        }
        
        return projects;
    }

    @Override
    public boolean update(Project project) {
        String sql = "UPDATE project SET title = ?, description = ?, sector = ?, status = ?, entrepreneur_id = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, project.getTitle());
            stmt.setString(2, project.getDescription());
            stmt.setString(3, project.getSector());
            stmt.setString(4, project.getStatus().name());
            stmt.setInt(5, project.getEntrepreneurId());
            stmt.setInt(6, project.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("✓ Project updated successfully: " + project.getTitle());
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error updating project: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM project WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("✓ Project deleted successfully. ID: " + id);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error deleting project: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Finds all projects by entrepreneur ID.
     * @param entrepreneurId The entrepreneur's user ID
     * @return List of projects
     */
    public List<Project> findByEntrepreneurId(int entrepreneurId) {
        List<Project> projects = new ArrayList<>();
        String sql = """
            SELECT p.*, u.name as entrepreneur_name 
            FROM project p 
            LEFT JOIN user u ON p.entrepreneur_id = u.id 
            WHERE p.entrepreneur_id = ? 
            ORDER BY p.created_at DESC
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, entrepreneurId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    projects.add(mapResultSetToProject(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error finding projects by entrepreneur: " + e.getMessage());
            e.printStackTrace();
        }
        
        return projects;
    }

    /**
     * Finds all projects with a specific status.
     * @param status The project status
     * @return List of projects
     */
    public List<Project> findByStatus(ProjectStatus status) {
        List<Project> projects = new ArrayList<>();
        String sql = """
            SELECT p.*, u.name as entrepreneur_name 
            FROM project p 
            LEFT JOIN user u ON p.entrepreneur_id = u.id 
            WHERE p.status = ? 
            ORDER BY p.created_at DESC
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    projects.add(mapResultSetToProject(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error finding projects by status: " + e.getMessage());
            e.printStackTrace();
        }
        
        return projects;
    }

    /**
     * Finds all projects in a specific sector.
     * @param sector The sector name
     * @return List of projects
     */
    public List<Project> findBySector(String sector) {
        List<Project> projects = new ArrayList<>();
        String sql = """
            SELECT p.*, u.name as entrepreneur_name 
            FROM project p 
            LEFT JOIN user u ON p.entrepreneur_id = u.id 
            WHERE p.sector = ? 
            ORDER BY p.created_at DESC
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, sector);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    projects.add(mapResultSetToProject(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error finding projects by sector: " + e.getMessage());
            e.printStackTrace();
        }
        
        return projects;
    }

    /**
     * Gets all unique sectors from existing projects.
     * @return List of sector names
     */
    public List<String> getAllSectors() {
        List<String> sectors = new ArrayList<>();
        String sql = "SELECT DISTINCT sector FROM project ORDER BY sector";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                sectors.add(rs.getString("sector"));
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error fetching sectors: " + e.getMessage());
            e.printStackTrace();
        }
        
        return sectors;
    }

    /**
     * Counts projects by status.
     * @param status The project status
     * @return Number of projects
     */
    public int countByStatus(ProjectStatus status) {
        String sql = "SELECT COUNT(*) FROM project WHERE status = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error counting projects by status: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }

    /**
     * Maps a ResultSet row to a Project object.
     */
    private Project mapResultSetToProject(ResultSet rs) throws SQLException {
        Project project = new Project();
        project.setId(rs.getInt("id"));
        project.setTitle(rs.getString("title"));
        project.setDescription(rs.getString("description"));
        project.setSector(rs.getString("sector"));
        project.setStatus(ProjectStatus.valueOf(rs.getString("status")));
        project.setEntrepreneurId(rs.getInt("entrepreneur_id"));
        
        // Get entrepreneur name from joined query
        try {
            project.setEntrepreneurName(rs.getString("entrepreneur_name"));
        } catch (SQLException ignored) {
            // Column might not exist in all queries
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            project.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            project.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return project;
    }
}
