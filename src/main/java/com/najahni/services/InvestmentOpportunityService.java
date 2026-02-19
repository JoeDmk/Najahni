package com.najahni.services;

import com.najahni.models.InvestmentOpportunity;
import com.najahni.models.OpportunityStatus;
import com.najahni.utils.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service pour la gestion des Opportunités d'Investissement.
 * Accède directement à la base de données via JDBC (pas de DAO).
 */
public class InvestmentOpportunityService {

    private Connection cnx;

    public InvestmentOpportunityService() {
        this.cnx = DBConnection.getInstance().getConnection();
    }

    /** Constructeur pour les tests unitaires. */
    public InvestmentOpportunityService(Connection cnx) {
        this.cnx = cnx;
    }

    // ─── CRUD ────────────────────────────────────────────────

    public InvestmentOpportunity createOpportunity(InvestmentOpportunity opportunity) throws IllegalArgumentException {
        validateOpportunity(opportunity);
        validateProjectExists(opportunity.getProjectId());

        String sql = "INSERT INTO investment_opportunity (target_amount, description, deadline, status, project_id, risk_score, risk_label) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setBigDecimal(1, opportunity.getTargetAmount());
            ps.setString(2, opportunity.getDescription());
            ps.setDate(3, opportunity.getDeadline() != null ? java.sql.Date.valueOf(opportunity.getDeadline()) : null);
            ps.setString(4, opportunity.getStatus() != null ? opportunity.getStatus().name() : "OPEN");
            ps.setInt(5, opportunity.getProjectId());
            if (opportunity.getRiskScore() != null) {
                ps.setDouble(6, opportunity.getRiskScore());
            } else {
                ps.setNull(6, java.sql.Types.DOUBLE);
            }
            ps.setString(7, opportunity.getRiskLabel());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) opportunity.setId(rs.getInt(1));
            }
            System.out.println("✓ Investment opportunity created: " + opportunity.getTargetAmount() + " €");
            return opportunity;
        } catch (SQLException e) {
            System.err.println("✗ Error creating opportunity: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Optional<InvestmentOpportunity> findById(int id) {
        String sql = """
            SELECT io.*, p.titre AS project_title
            FROM investment_opportunity io
            LEFT JOIN projet p ON io.project_id = p.id
            WHERE io.id = ?
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToOpportunity(rs));
            }
        } catch (SQLException e) {
            System.err.println("✗ Error finding opportunity by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<InvestmentOpportunity> findAll() {
        List<InvestmentOpportunity> list = new ArrayList<>();
        String sql = """
            SELECT io.*, p.titre AS project_title
            FROM investment_opportunity io
            LEFT JOIN projet p ON io.project_id = p.id
            ORDER BY io.created_at DESC
            """;
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSetToOpportunity(rs));
        } catch (SQLException e) {
            System.err.println("✗ Error fetching all opportunities: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateOpportunity(InvestmentOpportunity opportunity) throws IllegalArgumentException {
        validateOpportunity(opportunity);
        String sql = "UPDATE investment_opportunity SET target_amount = ?, description = ?, deadline = ?, status = ?, project_id = ?, risk_score = ?, risk_label = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBigDecimal(1, opportunity.getTargetAmount());
            ps.setString(2, opportunity.getDescription());
            ps.setDate(3, opportunity.getDeadline() != null ? java.sql.Date.valueOf(opportunity.getDeadline()) : null);
            ps.setString(4, opportunity.getStatus() != null ? opportunity.getStatus().name() : "OPEN");
            ps.setInt(5, opportunity.getProjectId());
            if (opportunity.getRiskScore() != null) {
                ps.setDouble(6, opportunity.getRiskScore());
            } else {
                ps.setNull(6, java.sql.Types.DOUBLE);
            }
            ps.setString(7, opportunity.getRiskLabel());
            ps.setInt(8, opportunity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("✗ Error updating opportunity: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteOpportunity(int id) {
        String sql = "DELETE FROM investment_opportunity WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("✗ Error deleting opportunity: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ─── REQUÊTES SPÉCIFIQUES ────────────────────────────────

    public List<InvestmentOpportunity> findByProject(int projectId) {
        List<InvestmentOpportunity> list = new ArrayList<>();
        String sql = """
            SELECT io.*, p.titre AS project_title
            FROM investment_opportunity io
            LEFT JOIN projet p ON io.project_id = p.id
            WHERE io.project_id = ?
            ORDER BY io.created_at DESC
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToOpportunity(rs));
            }
        } catch (SQLException e) {
            System.err.println("✗ Error finding opportunities by project: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public List<InvestmentOpportunity> findByStatus(OpportunityStatus status) {
        List<InvestmentOpportunity> list = new ArrayList<>();
        String sql = """
            SELECT io.*, p.titre AS project_title
            FROM investment_opportunity io
            LEFT JOIN projet p ON io.project_id = p.id
            WHERE io.status = ?
            ORDER BY io.created_at DESC
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToOpportunity(rs));
            }
        } catch (SQLException e) {
            System.err.println("✗ Error finding opportunities by status: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public int countByStatus(OpportunityStatus status) {
        String sql = "SELECT COUNT(*) FROM investment_opportunity WHERE status = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public BigDecimal getTotalTargetAmount() {
        String sql = "SELECT COALESCE(SUM(target_amount), 0) FROM investment_opportunity";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    // ─── STATUS MANAGEMENT ───────────────────────────────────

    public boolean closeOpportunity(int opportunityId) {
        return updateOpportunityStatus(opportunityId, OpportunityStatus.CLOSED);
    }

    public boolean markAsFunded(int opportunityId) {
        return updateOpportunityStatus(opportunityId, OpportunityStatus.FUNDED);
    }

    private boolean updateOpportunityStatus(int opportunityId, OpportunityStatus newStatus) {
        Optional<InvestmentOpportunity> oppOpt = findById(opportunityId);
        if (oppOpt.isEmpty()) {
            throw new IllegalArgumentException("Opportunity not found with ID: " + opportunityId);
        }
        InvestmentOpportunity opp = oppOpt.get();
        opp.setStatus(newStatus);
        return updateOpportunity(opp);
    }

    // ─── RISK SCORE IA ──────────────────────────────────────

    /**
     * Met à jour le score de risque IA pour une opportunité.
     *
     * @param opportunityId ID de l'opportunité
     * @param riskScore     Score de risque (0–100)
     * @return true si la mise à jour a réussi
     */
    public boolean updateRiskScore(int opportunityId, double riskScore) {
        String sql = "UPDATE investment_opportunity SET risk_score = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDouble(1, riskScore);
            ps.setInt(2, opportunityId);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                System.out.println("✓ Risk score updated for opportunity #" + opportunityId + " → " + riskScore);
            }
            return updated;
        } catch (SQLException e) {
            System.err.println("✗ Error updating risk score: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Met à jour le label de risque ML pour une opportunité.
     *
     * @param opportunityId ID de l'opportunité
     * @param riskLabel     Label prédit ("faible", "moyen", "eleve")
     * @return true si la mise à jour a réussi
     */
    public boolean updateRiskLabel(int opportunityId, String riskLabel) {
        String sql = "UPDATE investment_opportunity SET risk_label = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, riskLabel);
            ps.setInt(2, opportunityId);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                System.out.println("✓ Risk label updated for opportunity #" + opportunityId + " → " + riskLabel);
            }
            return updated;
        } catch (SQLException e) {
            System.err.println("✗ Error updating risk label: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ─── VALIDATION ──────────────────────────────────────────

    private void validateOpportunity(InvestmentOpportunity opportunity) throws IllegalArgumentException {
        if (opportunity == null)
            throw new IllegalArgumentException("Opportunity cannot be null");
        if (opportunity.getTargetAmount() == null || opportunity.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Target amount must be greater than zero");
        if (opportunity.getDescription() == null || opportunity.getDescription().trim().isEmpty())
            throw new IllegalArgumentException("Description is required");
        if (opportunity.getDeadline() != null && opportunity.getDeadline().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Deadline cannot be in the past");
        if (opportunity.getProjectId() <= 0)
            throw new IllegalArgumentException("Project ID is required");
    }

    private void validateProjectExists(int projectId) throws IllegalArgumentException {
        String sql = "SELECT COUNT(*) FROM projet WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    throw new IllegalArgumentException("Project not found with ID: " + projectId);
                }
            }
        } catch (SQLException e) {
            System.err.println("✗ Error validating project: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─── MAPPING ─────────────────────────────────────────────

    private InvestmentOpportunity mapResultSetToOpportunity(ResultSet rs) throws SQLException {
        InvestmentOpportunity opp = new InvestmentOpportunity();
        opp.setId(rs.getInt("id"));
        opp.setTargetAmount(rs.getBigDecimal("target_amount"));
        opp.setDescription(rs.getString("description"));

        java.sql.Date deadline = rs.getDate("deadline");
        if (deadline != null) opp.setDeadline(deadline.toLocalDate());

        String status = rs.getString("status");
        if (status != null) {
            try { opp.setStatus(OpportunityStatus.valueOf(status.toUpperCase())); }
            catch (IllegalArgumentException e) { opp.setStatus(OpportunityStatus.OPEN); }
        }

        opp.setProjectId(rs.getInt("project_id"));

        // Score de risque IA (peut être NULL)
        double riskScore = rs.getDouble("risk_score");
        if (!rs.wasNull()) {
            opp.setRiskScore(riskScore);
        }

        // Label ML (peut être NULL)
        opp.setRiskLabel(rs.getString("risk_label"));

        try { opp.setProjectTitle(rs.getString("project_title")); }
        catch (SQLException ignored) { }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) opp.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) opp.setUpdatedAt(updatedAt.toLocalDateTime());

        return opp;
    }
}
