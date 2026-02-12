package com.najahni.dao;

import com.najahni.models.InvestmentOffer;
import com.najahni.models.OfferStatus;
import com.najahni.utils.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO pour l'entité InvestmentOffer.
 * Gère toutes les opérations JDBC liées aux offres d'investissement.
 * 
 * Architecture : Controller → Service → DAO → Database
 * Utilise PreparedStatement et try-with-resources systématiquement.
 */
public class InvestmentOfferDAO implements GenericDAO<InvestmentOffer> {

    private final Connection connection;

    public InvestmentOfferDAO() {
        this.connection = DBConnection.getInstance().getConnection();
    }

    // ─── CREATE ──────────────────────────────────────────────

    @Override
    public InvestmentOffer create(InvestmentOffer offer) {
        String sql = "INSERT INTO investment_offer (proposed_amount, status, investor_id, opportunity_id) "
                   + "VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setBigDecimal(1, offer.getProposedAmount());
            stmt.setString(2, offer.getStatus().name());
            stmt.setInt(3, offer.getInvestorId());
            stmt.setInt(4, offer.getOpportunityId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        offer.setId(keys.getInt(1));
                    }
                }
            }

            System.out.println("✓ Offre créée avec succès. Montant proposé : " + offer.getFormattedAmount());
            return offer;

        } catch (SQLException e) {
            System.err.println("✗ Erreur création offre : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ─── READ BY ID ──────────────────────────────────────────

    @Override
    public Optional<InvestmentOffer> findById(int id) {
        String sql = """
            SELECT o.*, u.name AS investor_name, io.description AS opportunity_description
            FROM investment_offer o
            LEFT JOIN user u ON o.investor_id = u.id
            LEFT JOIN investment_opportunity io ON o.opportunity_id = io.id
            WHERE o.id = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur findById offre : " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    // ─── READ ALL ────────────────────────────────────────────

    @Override
    public List<InvestmentOffer> findAll() {
        List<InvestmentOffer> list = new ArrayList<>();
        String sql = """
            SELECT o.*, u.name AS investor_name, io.description AS opportunity_description
            FROM investment_offer o
            LEFT JOIN user u ON o.investor_id = u.id
            LEFT JOIN investment_opportunity io ON o.opportunity_id = io.id
            ORDER BY o.created_at DESC
            """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur findAll offres : " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    // ─── UPDATE ──────────────────────────────────────────────

    @Override
    public boolean update(InvestmentOffer offer) {
        String sql = "UPDATE investment_offer SET proposed_amount = ?, status = ?, "
                   + "investor_id = ?, opportunity_id = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBigDecimal(1, offer.getProposedAmount());
            stmt.setString(2, offer.getStatus().name());
            stmt.setInt(3, offer.getInvestorId());
            stmt.setInt(4, offer.getOpportunityId());
            stmt.setInt(5, offer.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("✓ Offre mise à jour. ID : " + offer.getId());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur update offre : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ─── DELETE ──────────────────────────────────────────────

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM investment_offer WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("✓ Offre supprimée. ID : " + id);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur delete offre : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ─── REQUÊTES SPÉCIFIQUES ────────────────────────────────

    /**
     * Trouve toutes les offres liées à une opportunité.
     */
    public List<InvestmentOffer> findByOpportunityId(int opportunityId) {
        List<InvestmentOffer> list = new ArrayList<>();
        String sql = """
            SELECT o.*, u.name AS investor_name, io.description AS opportunity_description
            FROM investment_offer o
            LEFT JOIN user u ON o.investor_id = u.id
            LEFT JOIN investment_opportunity io ON o.opportunity_id = io.id
            WHERE o.opportunity_id = ?
            ORDER BY o.created_at DESC
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, opportunityId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur findByOpportunityId : " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Trouve toutes les offres d'un investisseur.
     */
    public List<InvestmentOffer> findByInvestorId(int investorId) {
        List<InvestmentOffer> list = new ArrayList<>();
        String sql = """
            SELECT o.*, u.name AS investor_name, io.description AS opportunity_description
            FROM investment_offer o
            LEFT JOIN user u ON o.investor_id = u.id
            LEFT JOIN investment_opportunity io ON o.opportunity_id = io.id
            WHERE o.investor_id = ?
            ORDER BY o.created_at DESC
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, investorId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur findByInvestorId : " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Compte les offres par statut.
     */
    public int countByStatus(OfferStatus status) {
        String sql = "SELECT COUNT(*) FROM investment_offer WHERE status = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur countByStatus offre : " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Calcule le total des montants proposés acceptés pour une opportunité.
     */
    public BigDecimal getTotalAcceptedForOpportunity(int opportunityId) {
        String sql = "SELECT COALESCE(SUM(proposed_amount), 0) AS total "
                   + "FROM investment_offer WHERE opportunity_id = ? AND status = 'ACCEPTED'";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, opportunityId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total");
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur getTotalAcceptedForOpportunity : " + e.getMessage());
            e.printStackTrace();
        }

        return BigDecimal.ZERO;
    }

    // ─── MAPPING ResultSet → Objet ──────────────────────────

    /**
     * Mappe une ligne ResultSet vers un objet InvestmentOffer.
     */
    private InvestmentOffer mapResultSet(ResultSet rs) throws SQLException {
        InvestmentOffer offer = new InvestmentOffer();
        offer.setId(rs.getInt("id"));
        offer.setProposedAmount(rs.getBigDecimal("proposed_amount"));
        offer.setStatus(OfferStatus.valueOf(rs.getString("status")));
        offer.setInvestorId(rs.getInt("investor_id"));
        offer.setOpportunityId(rs.getInt("opportunity_id"));

        // Champs transients issus des JOIN
        try {
            offer.setInvestorName(rs.getString("investor_name"));
        } catch (SQLException ignored) { }

        try {
            offer.setOpportunityDescription(rs.getString("opportunity_description"));
        } catch (SQLException ignored) { }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) offer.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) offer.setUpdatedAt(updatedAt.toLocalDateTime());

        return offer;
    }
}
