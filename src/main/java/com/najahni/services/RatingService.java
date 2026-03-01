package com.najahni.services;

import com.najahni.models.InvestorRating;
import com.najahni.utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service de gestion des évaluations/notations des investisseurs.
 *
 * Permet aux investisseurs ayant payé de noter les opportunités (1-5 étoiles)
 * et de laisser un commentaire.
 */
public class RatingService {

    private static final Logger LOG = Logger.getLogger(RatingService.class.getName());
    private final Connection cnx;

    public RatingService() {
        this.cnx = DBConnection.getInstance().getConnection();
        ensureRatingTable();
    }

    // ─── AUTO-MIGRATION ──────────────────────────────────────

    private void ensureRatingTable() {
        try {
            DatabaseMetaData meta = cnx.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "investor_rating", null)) {
                if (!rs.next()) {
                    try (Statement stmt = cnx.createStatement()) {
                        stmt.executeUpdate("""
                            CREATE TABLE investor_rating (
                                id INT PRIMARY KEY AUTO_INCREMENT,
                                investor_id INT NOT NULL,
                                opportunity_id INT NOT NULL,
                                rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
                                comment TEXT,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                UNIQUE KEY uk_rating (investor_id, opportunity_id),
                                CONSTRAINT fk_rating_investor FOREIGN KEY (investor_id)
                                    REFERENCES user(id) ON DELETE CASCADE,
                                CONSTRAINT fk_rating_opportunity FOREIGN KEY (opportunity_id)
                                    REFERENCES investment_opportunity(id) ON DELETE CASCADE
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                            """);
                        LOG.info("✓ Migration: created investor_rating table");
                    }
                }
            }
        } catch (SQLException e) {
            LOG.warning("⚠ Could not verify rating table: " + e.getMessage());
        }
    }

    // ─── CRUD ────────────────────────────────────────────────

    public InvestorRating createOrUpdate(InvestorRating rating) {
        Optional<InvestorRating> existing = findByInvestorAndOpportunity(
                rating.getInvestorId(), rating.getOpportunityId());

        if (existing.isPresent()) {
            rating.setId(existing.get().getId());
            update(rating);
            return rating;
        }

        String sql = "INSERT INTO investor_rating (investor_id, opportunity_id, rating, comment) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, rating.getInvestorId());
            ps.setInt(2, rating.getOpportunityId());
            ps.setInt(3, rating.getRating());
            ps.setString(4, rating.getComment());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) rating.setId(rs.getInt(1));
            }
            LOG.info("✓ Rating created: " + rating.getRating() + "★ for opportunity " + rating.getOpportunityId());
        } catch (SQLException e) {
            LOG.severe("Error creating rating: " + e.getMessage());
        }
        return rating;
    }

    private boolean update(InvestorRating rating) {
        String sql = "UPDATE investor_rating SET rating = ?, comment = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, rating.getRating());
            ps.setString(2, rating.getComment());
            ps.setInt(3, rating.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.severe("Error updating rating: " + e.getMessage());
            return false;
        }
    }

    public Optional<InvestorRating> findByInvestorAndOpportunity(int investorId, int opportunityId) {
        String sql = """
            SELECT r.*, CONCAT(u.firstname, ' ', u.lastname) AS investor_name
            FROM investor_rating r
            LEFT JOIN user u ON r.investor_id = u.id
            WHERE r.investor_id = ? AND r.opportunity_id = ?
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, investorId);
            ps.setInt(2, opportunityId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRating(rs));
            }
        } catch (SQLException e) {
            LOG.severe("Error finding rating: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<InvestorRating> findByOpportunity(int opportunityId) {
        List<InvestorRating> list = new ArrayList<>();
        String sql = """
            SELECT r.*, CONCAT(u.firstname, ' ', u.lastname) AS investor_name
            FROM investor_rating r
            LEFT JOIN user u ON r.investor_id = u.id
            WHERE r.opportunity_id = ?
            ORDER BY r.created_at DESC
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, opportunityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRating(rs));
            }
        } catch (SQLException e) {
            LOG.severe("Error fetching ratings: " + e.getMessage());
        }
        return list;
    }

    public double getAverageRating(int opportunityId) {
        String sql = "SELECT AVG(rating) FROM investor_rating WHERE opportunity_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, opportunityId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            LOG.severe("Error getting avg rating: " + e.getMessage());
        }
        return 0;
    }

    public int getRatingCount(int opportunityId) {
        String sql = "SELECT COUNT(*) FROM investor_rating WHERE opportunity_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, opportunityId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOG.severe("Error counting ratings: " + e.getMessage());
        }
        return 0;
    }

    // ─── HELPERS ─────────────────────────────────────────────

    private InvestorRating mapRating(ResultSet rs) throws SQLException {
        InvestorRating r = new InvestorRating();
        r.setId(rs.getInt("id"));
        r.setInvestorId(rs.getInt("investor_id"));
        r.setOpportunityId(rs.getInt("opportunity_id"));
        r.setRating(rs.getInt("rating"));
        r.setComment(rs.getString("comment"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) r.setCreatedAt(created.toLocalDateTime());
        try { r.setInvestorName(rs.getString("investor_name")); } catch (SQLException ignored) {}
        return r;
    }
}
