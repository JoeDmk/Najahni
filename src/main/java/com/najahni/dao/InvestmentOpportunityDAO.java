package com.najahni.dao;

import com.najahni.models.InvestmentOpportunity;
import com.najahni.models.InvestmentStatus;
import com.najahni.utils.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for InvestmentOpportunity entity.
 * Handles all database operations related to investment opportunities.
 */
public class InvestmentOpportunityDAO implements GenericDAO<InvestmentOpportunity> {

    private final Connection connection;

    public InvestmentOpportunityDAO() {
        this.connection = DBConnection.getInstance().getConnection();
    }

    @Override
    public InvestmentOpportunity create(InvestmentOpportunity investment) {
        String sql = "INSERT INTO investment_opportunity (amount, status, project_id) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setBigDecimal(1, investment.getAmount());
            stmt.setString(2, investment.getStatus().name());
            stmt.setInt(3, investment.getProjectId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        investment.setId(generatedKeys.getInt(1));
                    }
                }
            }
            
            System.out.println("✓ Investment opportunity created successfully. Amount: " + investment.getFormattedAmount());
            return investment;
            
        } catch (SQLException e) {
            System.err.println("✗ Error creating investment opportunity: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Optional<InvestmentOpportunity> findById(int id) {
        String sql = """
            SELECT io.*, p.title as project_title 
            FROM investment_opportunity io 
            LEFT JOIN project p ON io.project_id = p.id 
            WHERE io.id = ?
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToInvestment(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error finding investment by ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return Optional.empty();
    }

    @Override
    public List<InvestmentOpportunity> findAll() {
        List<InvestmentOpportunity> investments = new ArrayList<>();
        String sql = """
            SELECT io.*, p.title as project_title 
            FROM investment_opportunity io 
            LEFT JOIN project p ON io.project_id = p.id 
            ORDER BY io.created_at DESC
            """;
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                investments.add(mapResultSetToInvestment(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error fetching all investments: " + e.getMessage());
            e.printStackTrace();
        }
        
        return investments;
    }

    @Override
    public boolean update(InvestmentOpportunity investment) {
        String sql = "UPDATE investment_opportunity SET amount = ?, status = ?, project_id = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBigDecimal(1, investment.getAmount());
            stmt.setString(2, investment.getStatus().name());
            stmt.setInt(3, investment.getProjectId());
            stmt.setInt(4, investment.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("✓ Investment opportunity updated successfully. ID: " + investment.getId());
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error updating investment: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM investment_opportunity WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("✓ Investment opportunity deleted successfully. ID: " + id);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error deleting investment: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Finds all investments for a specific project.
     * @param projectId The project ID
     * @return List of investments
     */
    public List<InvestmentOpportunity> findByProjectId(int projectId) {
        List<InvestmentOpportunity> investments = new ArrayList<>();
        String sql = """
            SELECT io.*, p.title as project_title 
            FROM investment_opportunity io 
            LEFT JOIN project p ON io.project_id = p.id 
            WHERE io.project_id = ? 
            ORDER BY io.created_at DESC
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, projectId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    investments.add(mapResultSetToInvestment(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error finding investments by project: " + e.getMessage());
            e.printStackTrace();
        }
        
        return investments;
    }

    /**
     * Finds all investments with a specific status.
     * @param status The investment status
     * @return List of investments
     */
    public List<InvestmentOpportunity> findByStatus(InvestmentStatus status) {
        List<InvestmentOpportunity> investments = new ArrayList<>();
        String sql = """
            SELECT io.*, p.title as project_title 
            FROM investment_opportunity io 
            LEFT JOIN project p ON io.project_id = p.id 
            WHERE io.status = ? 
            ORDER BY io.created_at DESC
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    investments.add(mapResultSetToInvestment(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error finding investments by status: " + e.getMessage());
            e.printStackTrace();
        }
        
        return investments;
    }

    /**
     * Calculates total investment amount for a project.
     * @param projectId The project ID
     * @return Total investment amount
     */
    public BigDecimal getTotalInvestmentForProject(int projectId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) as total FROM investment_opportunity WHERE project_id = ? AND status IN ('ACCEPTED', 'COMPLETED')";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, projectId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error calculating total investment: " + e.getMessage());
            e.printStackTrace();
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * Counts investments by status.
     * @param status The investment status
     * @return Number of investments
     */
    public int countByStatus(InvestmentStatus status) {
        String sql = "SELECT COUNT(*) FROM investment_opportunity WHERE status = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error counting investments by status: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }

    /**
     * Gets total investment amount across all accepted/completed investments.
     * @return Total investment amount
     */
    public BigDecimal getTotalInvestmentAmount() {
        String sql = "SELECT COALESCE(SUM(amount), 0) as total FROM investment_opportunity WHERE status IN ('ACCEPTED', 'COMPLETED')";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error calculating total investment amount: " + e.getMessage());
            e.printStackTrace();
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * Maps a ResultSet row to an InvestmentOpportunity object.
     */
    private InvestmentOpportunity mapResultSetToInvestment(ResultSet rs) throws SQLException {
        InvestmentOpportunity investment = new InvestmentOpportunity();
        investment.setId(rs.getInt("id"));
        investment.setAmount(rs.getBigDecimal("amount"));
        investment.setStatus(InvestmentStatus.valueOf(rs.getString("status")));
        investment.setProjectId(rs.getInt("project_id"));
        
        // Get project title from joined query
        try {
            investment.setProjectTitle(rs.getString("project_title"));
        } catch (SQLException ignored) {
            // Column might not exist in all queries
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            investment.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            investment.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return investment;
    }
}
