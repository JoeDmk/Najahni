package com.najahni.services;

import com.najahni.models.InvestmentOffer;
import com.najahni.models.OfferStatus;
import com.najahni.utils.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service pour la gestion des Offres d'Investissement.
 * Accède directement à la base de données via JDBC (pas de DAO).
 */
public class InvestmentOfferService {

    private Connection cnx;

    public InvestmentOfferService() {
        this.cnx = DBConnection.getInstance().getConnection();
    }

    /** Constructeur pour les tests unitaires. */
    public InvestmentOfferService(Connection cnx) {
        this.cnx = cnx;
    }

    // ─── CRUD ────────────────────────────────────────────────

    public InvestmentOffer createOffer(InvestmentOffer offer) throws IllegalArgumentException {
        validateOffer(offer);
        validateInvestorRole(offer.getInvestorId());
        validateOpportunityOpen(offer.getOpportunityId());

        String sql = "INSERT INTO investment_offer (proposed_amount, status, investor_id, opportunity_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setBigDecimal(1, offer.getProposedAmount());
            ps.setString(2, offer.getStatus() != null ? offer.getStatus().name() : "PENDING");
            ps.setInt(3, offer.getInvestorId());
            ps.setInt(4, offer.getOpportunityId());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) offer.setId(rs.getInt(1));
            }
            System.out.println("✓ Investment offer created: " + offer.getProposedAmount() + " €");
            return offer;
        } catch (SQLException e) {
            System.err.println("✗ Error creating investment offer: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Optional<InvestmentOffer> findById(int id) {
        String sql = """
            SELECT io.*,
                   CONCAT(u.firstname, ' ', u.lastname) AS investor_name,
                   iop.description AS opportunity_description
            FROM investment_offer io
            LEFT JOIN user u ON io.investor_id = u.id
            LEFT JOIN investment_opportunity iop ON io.opportunity_id = iop.id
            WHERE io.id = ?
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToOffer(rs));
            }
        } catch (SQLException e) {
            System.err.println("✗ Error finding offer by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<InvestmentOffer> findAll() {
        List<InvestmentOffer> offers = new ArrayList<>();
        String sql = """
            SELECT io.*,
                   CONCAT(u.firstname, ' ', u.lastname) AS investor_name,
                   iop.description AS opportunity_description
            FROM investment_offer io
            LEFT JOIN user u ON io.investor_id = u.id
            LEFT JOIN investment_opportunity iop ON io.opportunity_id = iop.id
            ORDER BY io.created_at DESC
            """;
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) offers.add(mapResultSetToOffer(rs));
        } catch (SQLException e) {
            System.err.println("✗ Error fetching all offers: " + e.getMessage());
            e.printStackTrace();
        }
        return offers;
    }

    public boolean updateOffer(InvestmentOffer offer) throws IllegalArgumentException {
        validateOffer(offer);
        String sql = "UPDATE investment_offer SET proposed_amount = ?, status = ?, investor_id = ?, opportunity_id = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBigDecimal(1, offer.getProposedAmount());
            ps.setString(2, offer.getStatus() != null ? offer.getStatus().name() : "PENDING");
            ps.setInt(3, offer.getInvestorId());
            ps.setInt(4, offer.getOpportunityId());
            ps.setInt(5, offer.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("✗ Error updating offer: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteOffer(int id) {
        String sql = "DELETE FROM investment_offer WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("✗ Error deleting offer: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ─── REQUÊTES SPÉCIFIQUES ────────────────────────────────

    public List<InvestmentOffer> findByOpportunity(int opportunityId) {
        List<InvestmentOffer> offers = new ArrayList<>();
        String sql = """
            SELECT io.*,
                   CONCAT(u.firstname, ' ', u.lastname) AS investor_name,
                   iop.description AS opportunity_description
            FROM investment_offer io
            LEFT JOIN user u ON io.investor_id = u.id
            LEFT JOIN investment_opportunity iop ON io.opportunity_id = iop.id
            WHERE io.opportunity_id = ?
            ORDER BY io.created_at DESC
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, opportunityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) offers.add(mapResultSetToOffer(rs));
            }
        } catch (SQLException e) {
            System.err.println("✗ Error finding offers by opportunity: " + e.getMessage());
            e.printStackTrace();
        }
        return offers;
    }

    public List<InvestmentOffer> findByInvestor(int investorId) {
        List<InvestmentOffer> offers = new ArrayList<>();
        String sql = """
            SELECT io.*,
                   CONCAT(u.firstname, ' ', u.lastname) AS investor_name,
                   iop.description AS opportunity_description
            FROM investment_offer io
            LEFT JOIN user u ON io.investor_id = u.id
            LEFT JOIN investment_opportunity iop ON io.opportunity_id = iop.id
            WHERE io.investor_id = ?
            ORDER BY io.created_at DESC
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, investorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) offers.add(mapResultSetToOffer(rs));
            }
        } catch (SQLException e) {
            System.err.println("✗ Error finding offers by investor: " + e.getMessage());
            e.printStackTrace();
        }
        return offers;
    }

    public int countByStatus(OfferStatus status) {
        String sql = "SELECT COUNT(*) FROM investment_offer WHERE status = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public BigDecimal getTotalAcceptedForOpportunity(int opportunityId) {
        String sql = "SELECT COALESCE(SUM(proposed_amount), 0) FROM investment_offer WHERE opportunity_id = ? AND status = 'ACCEPTED'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, opportunityId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    // ─── STATUS MANAGEMENT ───────────────────────────────────

    public boolean acceptOffer(int offerId) {
        return updateOfferStatus(offerId, OfferStatus.ACCEPTED);
    }

    public boolean rejectOffer(int offerId) {
        return updateOfferStatus(offerId, OfferStatus.REJECTED);
    }

    private boolean updateOfferStatus(int offerId, OfferStatus newStatus) {
        Optional<InvestmentOffer> offerOpt = findById(offerId);
        if (offerOpt.isEmpty()) {
            throw new IllegalArgumentException("Offer not found with ID: " + offerId);
        }
        InvestmentOffer offer = offerOpt.get();
        offer.setStatus(newStatus);
        return updateOffer(offer);
    }

    // ─── VALIDATION ──────────────────────────────────────────

    private void validateOffer(InvestmentOffer offer) throws IllegalArgumentException {
        if (offer == null) throw new IllegalArgumentException("Offer cannot be null");
        if (offer.getProposedAmount() == null || offer.getProposedAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Proposed amount must be greater than zero");
        if (offer.getInvestorId() <= 0)
            throw new IllegalArgumentException("Investor ID is required");
        if (offer.getOpportunityId() <= 0)
            throw new IllegalArgumentException("Opportunity ID is required");
    }

    private void validateInvestorRole(int investorId) throws IllegalArgumentException {
        String sql = "SELECT role FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, investorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Investor not found with ID: " + investorId);
                }
                String role = rs.getString("role");
                if (!"INVESTISSEUR".equals(role)) {
                    throw new IllegalArgumentException("User is not an investor");
                }
            }
        } catch (SQLException e) {
            System.err.println("✗ Error validating investor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void validateOpportunityOpen(int opportunityId) throws IllegalArgumentException {
        String sql = "SELECT status FROM investment_opportunity WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, opportunityId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Investment opportunity not found with ID: " + opportunityId);
                }
                String status = rs.getString("status");
                if (!"OPEN".equals(status)) {
                    throw new IllegalArgumentException("Investment opportunity is not open for offers");
                }
            }
        } catch (SQLException e) {
            System.err.println("✗ Error validating opportunity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─── MAPPING ─────────────────────────────────────────────

    private InvestmentOffer mapResultSetToOffer(ResultSet rs) throws SQLException {
        InvestmentOffer offer = new InvestmentOffer();
        offer.setId(rs.getInt("id"));
        offer.setProposedAmount(rs.getBigDecimal("proposed_amount"));

        String status = rs.getString("status");
        if (status != null) {
            try { offer.setStatus(OfferStatus.valueOf(status.toUpperCase())); }
            catch (IllegalArgumentException e) { offer.setStatus(OfferStatus.PENDING); }
        }

        offer.setInvestorId(rs.getInt("investor_id"));
        offer.setOpportunityId(rs.getInt("opportunity_id"));

        try { offer.setInvestorName(rs.getString("investor_name")); }
        catch (SQLException ignored) { }
        try { offer.setOpportunityDescription(rs.getString("opportunity_description")); }
        catch (SQLException ignored) { }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) offer.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) offer.setUpdatedAt(updatedAt.toLocalDateTime());

        return offer;
    }
}
