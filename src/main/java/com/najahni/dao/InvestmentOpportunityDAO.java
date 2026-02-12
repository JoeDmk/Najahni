package com.najahni.dao;

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
 * DAO pour l'entité InvestmentOpportunity.
 * Gère toutes les opérations JDBC liées aux opportunités d'investissement.
 * 
 * Architecture : Controller → Service → DAO → Database
 * Utilise PreparedStatement et try-with-resources systématiquement.
 */
public class InvestmentOpportunityDAO implements GenericDAO<InvestmentOpportunity> {

    private final Connection connection;

    public InvestmentOpportunityDAO() {
        this.connection = DBConnection.getInstance().getConnection();
    }

    // ─── CREATE ──────────────────────────────────────────────

    @Override
    public InvestmentOpportunity create(InvestmentOpportunity opp) {
        String sql = "INSERT INTO investment_opportunity (target_amount, description, deadline, status, project_id) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setBigDecimal(1, opp.getTargetAmount());
            stmt.setString(2, opp.getDescription());
            stmt.setDate(3, opp.getDeadline() != null ? Date.valueOf(opp.getDeadline()) : null);
            stmt.setString(4, opp.getStatus().name());
            stmt.setInt(5, opp.getProjectId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        opp.setId(keys.getInt(1));
                    }
                }
            }

            System.out.println("✓ Opportunité créée avec succès. Montant cible : " + opp.getFormattedAmount());
            return opp;

        } catch (SQLException e) {
            System.err.println("✗ Erreur création opportunité : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ─── READ BY ID ──────────────────────────────────────────

    @Override
    public Optional<InvestmentOpportunity> findById(int id) {
        String sql = """
            SELECT io.*, p.title AS project_title
            FROM investment_opportunity io
            LEFT JOIN project p ON io.project_id = p.id
            WHERE io.id = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur findById opportunité : " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    // ─── READ ALL ────────────────────────────────────────────

    @Override
    public List<InvestmentOpportunity> findAll() {
        List<InvestmentOpportunity> list = new ArrayList<>();
        String sql = """
            SELECT io.*, p.title AS project_title
            FROM investment_opportunity io
            LEFT JOIN project p ON io.project_id = p.id
            ORDER BY io.created_at DESC
            """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur findAll opportunités : " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    // ─── UPDATE ──────────────────────────────────────────────

    @Override
    public boolean update(InvestmentOpportunity opp) {
        String sql = "UPDATE investment_opportunity SET target_amount = ?, description = ?, "
                   + "deadline = ?, status = ?, project_id = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBigDecimal(1, opp.getTargetAmount());
            stmt.setString(2, opp.getDescription());
            stmt.setDate(3, opp.getDeadline() != null ? Date.valueOf(opp.getDeadline()) : null);
            stmt.setString(4, opp.getStatus().name());
            stmt.setInt(5, opp.getProjectId());
            stmt.setInt(6, opp.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("✓ Opportunité mise à jour. ID : " + opp.getId());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur update opportunité : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ─── DELETE ──────────────────────────────────────────────

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM investment_opportunity WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("✓ Opportunité supprimée. ID : " + id);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur delete opportunité : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ─── REQUÊTES SPÉCIFIQUES ────────────────────────────────

    /**
     * Trouve toutes les opportunités liées à un projet.
     */
    public List<InvestmentOpportunity> findByProjectId(int projectId) {
        List<InvestmentOpportunity> list = new ArrayList<>();
        String sql = """
            SELECT io.*, p.title AS project_title
            FROM investment_opportunity io
            LEFT JOIN project p ON io.project_id = p.id
            WHERE io.project_id = ?
            ORDER BY io.created_at DESC
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, projectId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur findByProjectId : " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Trouve toutes les opportunités avec un statut donné.
     */
    public List<InvestmentOpportunity> findByStatus(OpportunityStatus status) {
        List<InvestmentOpportunity> list = new ArrayList<>();
        String sql = """
            SELECT io.*, p.title AS project_title
            FROM investment_opportunity io
            LEFT JOIN project p ON io.project_id = p.id
            WHERE io.status = ?
            ORDER BY io.created_at DESC
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur findByStatus : " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Compte les opportunités par statut.
     */
    public int countByStatus(OpportunityStatus status) {
        String sql = "SELECT COUNT(*) FROM investment_opportunity WHERE status = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur countByStatus : " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Calcule le montant total cible de toutes les opportunités ouvertes.
     */
    public BigDecimal getTotalTargetAmount() {
        String sql = "SELECT COALESCE(SUM(target_amount), 0) AS total FROM investment_opportunity WHERE status = 'OPEN'";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getBigDecimal("total");
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur getTotalTargetAmount : " + e.getMessage());
            e.printStackTrace();
        }

        return BigDecimal.ZERO;
    }

    // ─── MAPPING ResultSet → Objet ──────────────────────────

    /**
     * Mappe une ligne ResultSet vers un objet InvestmentOpportunity.
     */
    private InvestmentOpportunity mapResultSet(ResultSet rs) throws SQLException {
        InvestmentOpportunity opp = new InvestmentOpportunity();
        opp.setId(rs.getInt("id"));
        opp.setTargetAmount(rs.getBigDecimal("target_amount"));
        opp.setDescription(rs.getString("description"));

        Date deadline = rs.getDate("deadline");
        if (deadline != null) {
            opp.setDeadline(deadline.toLocalDate());
        }

        opp.setStatus(OpportunityStatus.valueOf(rs.getString("status")));
        opp.setProjectId(rs.getInt("project_id"));

        // Champ transient issu du JOIN
        try {
            opp.setProjectTitle(rs.getString("project_title"));
        } catch (SQLException ignored) {
            // La colonne peut ne pas exister dans toutes les requêtes
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) opp.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) opp.setUpdatedAt(updatedAt.toLocalDateTime());

        return opp;
    }
}
