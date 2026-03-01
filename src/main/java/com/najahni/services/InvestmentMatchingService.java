package com.najahni.services;

import com.najahni.models.*;
import com.najahni.utils.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service de gestion des profils d'investisseur et du moteur de matching IA.
 *
 * Fonctionnalités :
 * - CRUD profil investisseur
 * - Matching par scoring pondéré (secteur, risque, budget, horizon)
 * - Score de compatibilité en pourcentage
 */
public class InvestmentMatchingService {

    private static final Logger LOG = Logger.getLogger(InvestmentMatchingService.class.getName());

    private final Connection cnx;
    private final InvestmentOpportunityService opportunityService;
    private final ProjectService projectService;

    public InvestmentMatchingService() {
        this.cnx = DBConnection.getInstance().getConnection();
        this.opportunityService = new InvestmentOpportunityService();
        this.projectService = new ProjectService();
        ensureProfileTable();
    }

    // ─── AUTO-MIGRATION ──────────────────────────────────────

    private void ensureProfileTable() {
        try {
            DatabaseMetaData meta = cnx.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "investor_profile", null)) {
                if (!rs.next()) {
                    try (Statement stmt = cnx.createStatement()) {
                        stmt.executeUpdate("""
                            CREATE TABLE investor_profile (
                                id INT PRIMARY KEY AUTO_INCREMENT,
                                user_id INT NOT NULL UNIQUE,
                                preferred_sectors VARCHAR(500),
                                risk_tolerance INT DEFAULT 5,
                                budget_min DECIMAL(15,2) DEFAULT 0,
                                budget_max DECIMAL(15,2) DEFAULT 10000000,
                                horizon_months INT DEFAULT 12,
                                description TEXT,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                CONSTRAINT fk_profile_user FOREIGN KEY (user_id)
                                    REFERENCES user(id) ON DELETE CASCADE
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                            """);
                        LOG.info("✓ Migration: created investor_profile table");
                    }
                }
            }
        } catch (SQLException e) {
            LOG.warning("⚠ Could not verify profile table: " + e.getMessage());
        }
    }

    // ─── PROFILE CRUD ────────────────────────────────────────

    public InvestorProfile saveProfile(InvestorProfile profile) {
        Optional<InvestorProfile> existing = findProfileByUser(profile.getUserId());
        if (existing.isPresent()) {
            profile.setId(existing.get().getId());
            updateProfile(profile);
            return profile;
        }

        String sql = """
            INSERT INTO investor_profile (user_id, preferred_sectors, risk_tolerance, budget_min, budget_max, horizon_months, description)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, profile.getUserId());
            ps.setString(2, profile.getPreferredSectors());
            ps.setInt(3, profile.getRiskTolerance());
            ps.setBigDecimal(4, profile.getBudgetMin());
            ps.setBigDecimal(5, profile.getBudgetMax());
            ps.setInt(6, profile.getHorizonMonths());
            ps.setString(7, profile.getDescription());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) profile.setId(rs.getInt(1));
            }
            LOG.info("✓ Investor profile created for user " + profile.getUserId());
        } catch (SQLException e) {
            LOG.severe("Error creating profile: " + e.getMessage());
        }
        return profile;
    }

    public boolean updateProfile(InvestorProfile profile) {
        String sql = """
            UPDATE investor_profile SET preferred_sectors = ?, risk_tolerance = ?,
            budget_min = ?, budget_max = ?, horizon_months = ?, description = ?
            WHERE id = ?
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, profile.getPreferredSectors());
            ps.setInt(2, profile.getRiskTolerance());
            ps.setBigDecimal(3, profile.getBudgetMin());
            ps.setBigDecimal(4, profile.getBudgetMax());
            ps.setInt(5, profile.getHorizonMonths());
            ps.setString(6, profile.getDescription());
            ps.setInt(7, profile.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.severe("Error updating profile: " + e.getMessage());
            return false;
        }
    }

    public Optional<InvestorProfile> findProfileByUser(int userId) {
        String sql = "SELECT * FROM investor_profile WHERE user_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapProfile(rs));
            }
        } catch (SQLException e) {
            LOG.severe("Error finding profile: " + e.getMessage());
        }
        return Optional.empty();
    }

    // ─── MATCHING ENGINE ─────────────────────────────────────

    /**
     * Represents a match result with compatibility score.
     */
    public static class MatchResult {
        public final InvestmentOpportunity opportunity;
        public final Project project;
        public final int compatibilityScore; // 0–100
        public final String explanation;

        public MatchResult(InvestmentOpportunity opp, Project proj, int score, String explanation) {
            this.opportunity = opp;
            this.project = proj;
            this.compatibilityScore = score;
            this.explanation = explanation;
        }
    }

    /**
     * Finds and scores all OPEN opportunities against the investor's profile.
     * Returns sorted by compatibility score (descending).
     */
    public List<MatchResult> findMatches(int userId) {
        Optional<InvestorProfile> optProfile = findProfileByUser(userId);
        if (optProfile.isEmpty()) return List.of();

        InvestorProfile profile = optProfile.get();
        List<InvestmentOpportunity> allOpen = opportunityService.findAll().stream()
                .filter(o -> o.getStatus() == OpportunityStatus.OPEN)
                .toList();

        List<MatchResult> results = new ArrayList<>();
        for (InvestmentOpportunity opp : allOpen) {
            Optional<Project> optProj = projectService.findById(opp.getProjectId());
            Project project = optProj.orElse(null);

            int score = computeCompatibility(profile, opp, project);
            String explanation = buildExplanation(profile, opp, project, score);
            results.add(new MatchResult(opp, project, score, explanation));
        }

        results.sort((a, b) -> Integer.compare(b.compatibilityScore, a.compatibilityScore));
        return results;
    }

    /**
     * Computes compatibility score (0-100) between a profile and an opportunity.
     *
     * Scoring weights:
     * - Sector match: 35%
     * - Budget fit: 25%
     * - Risk alignment: 25%
     * - Deadline/horizon fit: 15%
     */
    private int computeCompatibility(InvestorProfile profile,
                                      InvestmentOpportunity opp,
                                      Project project) {
        double sectorScore = computeSectorScore(profile, project);
        double budgetScore = computeBudgetScore(profile, opp);
        double riskScore = computeRiskScore(profile, opp);
        double horizonScore = computeHorizonScore(profile, opp);

        double total = sectorScore * 0.35
                + budgetScore * 0.25
                + riskScore * 0.25
                + horizonScore * 0.15;

        return (int) Math.round(Math.max(0, Math.min(100, total)));
    }

    /**
     * Sector matching using word overlap (TF-IDF-like).
     */
    private double computeSectorScore(InvestorProfile profile, Project project) {
        if (project == null || project.getSector() == null) return 50; // neutral
        String[] preferred = profile.getSectorArray();
        if (preferred.length == 0) return 60; // no preference = mildly positive

        String projectSector = project.getSector().toLowerCase().trim();
        String projectDesc = project.getDescription() != null ? project.getDescription().toLowerCase() : "";

        for (String sector : preferred) {
            String s = sector.trim().toLowerCase();
            if (s.isEmpty()) continue;
            if (projectSector.contains(s) || s.contains(projectSector)) return 100;
            if (projectDesc.contains(s)) return 80;
        }

        // Partial matching via keyword similarity
        for (String sector : preferred) {
            String s = sector.trim().toLowerCase();
            if (s.isEmpty()) continue;
            // Check if shares at least 3 common chars
            long common = s.chars().distinct().filter(c -> projectSector.indexOf(c) >= 0).count();
            if (common >= 3 && common >= s.length() * 0.5) return 60;
        }

        return 20; // no match
    }

    private double computeBudgetScore(InvestorProfile profile, InvestmentOpportunity opp) {
        if (opp.getTargetAmount() == null) return 50;
        BigDecimal target = opp.getTargetAmount();
        BigDecimal min = profile.getBudgetMin() != null ? profile.getBudgetMin() : BigDecimal.ZERO;
        BigDecimal max = profile.getBudgetMax() != null ? profile.getBudgetMax() : new BigDecimal("10000000");

        if (target.compareTo(min) >= 0 && target.compareTo(max) <= 0) {
            return 100; // within budget
        }

        // How far out of range?
        BigDecimal range = max.subtract(min);
        if (range.compareTo(BigDecimal.ZERO) <= 0) range = BigDecimal.ONE;

        BigDecimal distance;
        if (target.compareTo(min) < 0) {
            distance = min.subtract(target);
        } else {
            distance = target.subtract(max);
        }

        double ratio = distance.doubleValue() / range.doubleValue();
        return Math.max(0, 100 - ratio * 100);
    }

    private double computeRiskScore(InvestorProfile profile, InvestmentOpportunity opp) {
        if (opp.getRiskScore() == null) return 60; // no risk data = neutral

        double oppRisk = opp.getRiskScore(); // 0-100
        double tolerance = profile.getRiskTolerance(); // 1-10

        // Map tolerance to expected risk range
        double expectedRisk = tolerance * 10; // 1→10, 5→50, 10→100
        double diff = Math.abs(oppRisk - expectedRisk);

        // The closer the match, the higher the score
        return Math.max(0, 100 - diff * 1.5);
    }

    private double computeHorizonScore(InvestorProfile profile, InvestmentOpportunity opp) {
        if (opp.getDeadline() == null) return 50; // unknown

        long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.now(), opp.getDeadline());
        double monthsUntil = daysUntil / 30.0;
        int preferredMonths = profile.getHorizonMonths();

        if (monthsUntil <= 0) return 10; // expired
        double diff = Math.abs(monthsUntil - preferredMonths);
        return Math.max(0, 100 - diff * 8);
    }

    private String buildExplanation(InvestorProfile profile,
                                     InvestmentOpportunity opp,
                                     Project project,
                                     int score) {
        List<String> reasons = new ArrayList<>();

        double sectorScore = computeSectorScore(profile, project);
        if (sectorScore >= 80) reasons.add("✅ Secteur correspondant");
        else if (sectorScore >= 50) reasons.add("⚠️ Secteur partiellement compatible");
        else reasons.add("❌ Secteur différent");

        double budgetScore = computeBudgetScore(profile, opp);
        if (budgetScore >= 80) reasons.add("✅ Budget adapté");
        else if (budgetScore >= 40) reasons.add("⚠️ Budget proche de vos critères");
        else reasons.add("❌ Hors budget");

        double riskScore = computeRiskScore(profile, opp);
        if (riskScore >= 70) reasons.add("✅ Risque aligné avec votre tolérance");
        else if (riskScore >= 40) reasons.add("⚠️ Risque modérément compatible");
        else reasons.add("❌ Risque non compatible");

        return String.join(" | ", reasons);
    }

    // ─── HELPERS ─────────────────────────────────────────────

    private InvestorProfile mapProfile(ResultSet rs) throws SQLException {
        InvestorProfile p = new InvestorProfile();
        p.setId(rs.getInt("id"));
        p.setUserId(rs.getInt("user_id"));
        p.setPreferredSectors(rs.getString("preferred_sectors"));
        p.setRiskTolerance(rs.getInt("risk_tolerance"));
        p.setBudgetMin(rs.getBigDecimal("budget_min"));
        p.setBudgetMax(rs.getBigDecimal("budget_max"));
        p.setHorizonMonths(rs.getInt("horizon_months"));
        p.setDescription(rs.getString("description"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) p.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) p.setUpdatedAt(updated.toLocalDateTime());
        return p;
    }
}
