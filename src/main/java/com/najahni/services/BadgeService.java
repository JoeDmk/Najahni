package com.najahni.services;

import com.najahni.models.Badge;
import com.najahni.utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service pour la gestion des Badges.
 * Accède directement à la base de données via JDBC (pas de DAO).
 */
public class BadgeService {

    private Connection cnx;

    public BadgeService() {
        this.cnx = DBConnection.getInstance().getConnection();
    }

    // ─── CRUD ────────────────────────────────────────────────

    public Badge creerBadge(Badge badge) throws IllegalArgumentException {
        validerBadge(badge);
        String sql = """
            INSERT INTO badge (nom, description, icone, condition_obtention, points_requis,
                             cours_requis, niveau_requis, categorie, rarete, actif)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, badge.getNom());
            ps.setString(2, badge.getDescription());
            ps.setString(3, badge.getIcone());
            ps.setString(4, badge.getCondition());
            ps.setInt(5, badge.getPointsRequis());
            ps.setInt(6, badge.getCoursRequis());
            ps.setInt(7, badge.getNiveauRequis());
            ps.setString(8, badge.getCategorie());
            ps.setString(9, badge.getRarete());
            ps.setBoolean(10, badge.isActif());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) badge.setId(rs.getInt(1));
            }
            System.out.println("✓ Badge créé avec succès: " + badge.getNom());
            return badge;
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la création du badge: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Optional<Badge> trouverParId(int id) {
        String sql = "SELECT * FROM badge WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToBadge(rs));
            }
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche du badge par ID: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Badge> trouverTous() {
        List<Badge> badges = new ArrayList<>();
        String sql = "SELECT * FROM badge ORDER BY nom";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) badges.add(mapResultSetToBadge(rs));
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la récupération des badges: " + e.getMessage());
            e.printStackTrace();
        }
        return badges;
    }

    public boolean modifierBadge(Badge badge) throws IllegalArgumentException {
        validerBadge(badge);
        String sql = """
            UPDATE badge SET nom = ?, description = ?, icone = ?, condition_obtention = ?,
                           points_requis = ?, cours_requis = ?, niveau_requis = ?,
                           categorie = ?, rarete = ?, actif = ?
            WHERE id = ?
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, badge.getNom());
            ps.setString(2, badge.getDescription());
            ps.setString(3, badge.getIcone());
            ps.setString(4, badge.getCondition());
            ps.setInt(5, badge.getPointsRequis());
            ps.setInt(6, badge.getCoursRequis());
            ps.setInt(7, badge.getNiveauRequis());
            ps.setString(8, badge.getCategorie());
            ps.setString(9, badge.getRarete());
            ps.setBoolean(10, badge.isActif());
            ps.setInt(11, badge.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la mise à jour du badge: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean supprimerBadge(int id) {
        String sql = "DELETE FROM badge WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la suppression du badge: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ─── REQUÊTES SPÉCIFIQUES ────────────────────────────────

    public List<Badge> trouverActifs() {
        List<Badge> badges = new ArrayList<>();
        String sql = "SELECT * FROM badge WHERE actif = true ORDER BY nom";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) badges.add(mapResultSetToBadge(rs));
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la récupération des badges actifs: " + e.getMessage());
            e.printStackTrace();
        }
        return badges;
    }

    public List<Badge> trouverParTypeCondition(String conditionType) {
        List<Badge> badges = new ArrayList<>();
        String sql = "SELECT * FROM badge WHERE condition_obtention = ? ORDER BY nom";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, conditionType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) badges.add(mapResultSetToBadge(rs));
            }
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche par condition: " + e.getMessage());
            e.printStackTrace();
        }
        return badges;
    }

    public int compterBadges() {
        String sql = "SELECT COUNT(*) FROM badge";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int compterBadgesActifs() {
        String sql = "SELECT COUNT(*) FROM badge WHERE actif = true";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int compterBadgesUtilisateur(int userId) {
        // Eligible badges count based on current user stats
        return findBadgesEligibles(userId).size();
    }

    public List<Badge> findBadgesEligibles(int userId) {
        List<Badge> eligibleBadges = new ArrayList<>();
        // Récupérer les stats de l'utilisateur via JDBC directement
        int totalXP = getTotalXPByUser(userId);
        int coursCompletes = countCoursCompletesByUser(userId);

        for (Badge badge : trouverActifs()) {
            boolean eligible = false;
            switch (badge.getCondition() != null ? badge.getCondition() : "") {
                case "POINTS":
                    eligible = totalXP >= badge.getPointsRequis();
                    break;
                case "COURS":
                    eligible = coursCompletes >= badge.getCoursRequis();
                    break;
                case "NIVEAU":
                    int niveauGlobal = totalXP / 100;
                    eligible = niveauGlobal >= badge.getNiveauRequis();
                    break;
                default:
                    eligible = false;
            }
            if (eligible) eligibleBadges.add(badge);
        }
        return eligibleBadges;
    }

    public List<Badge> verifierEtAttribuerBadges(int userId) {
        List<Badge> eligibles = findBadgesEligibles(userId);
        if (!eligibles.isEmpty()) {
            System.out.println("  🏆 Badges éligibles pour l'utilisateur " + userId + ": " + eligibles.size());
            for (Badge badge : eligibles) {
                System.out.println("    → " + badge.getNom() + " (" + badge.getRarete() + ")");
            }
        }
        return eligibles;
    }

    // ─── HELPER QUERIES (for badge eligibility) ──────────────

    private int getTotalXPByUser(int userId) {
        String sql = "SELECT COALESCE(SUM(points_xp), 0) FROM progression WHERE user_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private int countCoursCompletesByUser(int userId) {
        String sql = "SELECT COUNT(*) FROM progression WHERE user_id = ? AND etat = 'COMPLETE'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // ─── MAPPING ─────────────────────────────────────────────

    private Badge mapResultSetToBadge(ResultSet rs) throws SQLException {
        Badge badge = new Badge();
        badge.setId(rs.getInt("id"));
        badge.setNom(rs.getString("nom"));
        badge.setDescription(rs.getString("description"));
        badge.setIcone(rs.getString("icone"));
        badge.setCondition(rs.getString("condition_obtention"));
        badge.setPointsRequis(rs.getInt("points_requis"));
        badge.setCoursRequis(rs.getInt("cours_requis"));
        badge.setNiveauRequis(rs.getInt("niveau_requis"));
        badge.setCategorie(rs.getString("categorie"));
        badge.setRarete(rs.getString("rarete"));
        badge.setActif(rs.getBoolean("actif"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) badge.setCreatedAt(createdAt.toLocalDateTime());

        return badge;
    }

    // ─── VALIDATION ──────────────────────────────────────────

    private void validerBadge(Badge badge) throws IllegalArgumentException {
        if (badge == null) throw new IllegalArgumentException("Le badge ne peut pas être null.");
        if (badge.getNom() == null || badge.getNom().trim().isEmpty())
            throw new IllegalArgumentException("Le nom du badge est obligatoire.");
        if (badge.getNom().length() > 100)
            throw new IllegalArgumentException("Le nom ne doit pas dépasser 100 caractères.");
        if (badge.getPointsRequis() < 0)
            throw new IllegalArgumentException("Les points requis doivent être positifs.");
        if (badge.getCoursRequis() < 0)
            throw new IllegalArgumentException("Le nombre de cours requis doit être positif.");
        if (badge.getNiveauRequis() < 0)
            throw new IllegalArgumentException("Le niveau requis doit être positif.");
    }
}
