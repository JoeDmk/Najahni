package com.najahni.dao;

import com.najahni.models.Badge;
import com.najahni.utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object pour l'entité Badge.
 * Gère toutes les opérations CRUD liées aux badges de gamification.
 */
public class BadgeDAO implements GenericDAO<Badge> {

    private final Connection connection;

    public BadgeDAO() {
        this.connection = DBConnection.getInstance().getConnection();
    }

    @Override
    public Badge create(Badge badge) {
        String sql = """
            INSERT INTO badge (nom, description, condition_text, condition_type, 
                             condition_valeur, icone, couleur, points_bonus, actif) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, badge.getNom());
            stmt.setString(2, badge.getDescription());
            stmt.setString(3, badge.getCondition());
            stmt.setString(4, badge.getConditionType());
            stmt.setInt(5, badge.getConditionValeur());
            stmt.setString(6, badge.getIcone());
            stmt.setString(7, badge.getCouleur());
            stmt.setInt(8, badge.getPointsBonus());
            stmt.setBoolean(9, badge.isActif());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        badge.setId(generatedKeys.getInt(1));
                    }
                }
            }

            System.out.println("✓ Badge créé avec succès: " + badge.getAffichageComplet());
            return badge;

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la création du badge: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Optional<Badge> findById(int id) {
        String sql = "SELECT * FROM badge WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBadge(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche du badge: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public List<Badge> findAll() {
        List<Badge> badges = new ArrayList<>();
        String sql = "SELECT * FROM badge ORDER BY condition_valeur ASC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                badges.add(mapResultSetToBadge(rs));
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la récupération des badges: " + e.getMessage());
            e.printStackTrace();
        }

        return badges;
    }

    @Override
    public boolean update(Badge badge) {
        String sql = """
            UPDATE badge SET nom = ?, description = ?, condition_text = ?, 
                           condition_type = ?, condition_valeur = ?, icone = ?, 
                           couleur = ?, points_bonus = ?, actif = ? 
            WHERE id = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, badge.getNom());
            stmt.setString(2, badge.getDescription());
            stmt.setString(3, badge.getCondition());
            stmt.setString(4, badge.getConditionType());
            stmt.setInt(5, badge.getConditionValeur());
            stmt.setString(6, badge.getIcone());
            stmt.setString(7, badge.getCouleur());
            stmt.setInt(8, badge.getPointsBonus());
            stmt.setBoolean(9, badge.isActif());
            stmt.setInt(10, badge.getId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✓ Badge mis à jour. ID: " + badge.getId());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la mise à jour du badge: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM badge WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✓ Badge supprimé. ID: " + id);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la suppression du badge: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Recherche les badges actifs.
     */
    public List<Badge> findActifs() {
        List<Badge> badges = new ArrayList<>();
        String sql = "SELECT * FROM badge WHERE actif = true ORDER BY condition_valeur ASC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                badges.add(mapResultSetToBadge(rs));
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche des badges actifs: " + e.getMessage());
            e.printStackTrace();
        }

        return badges;
    }

    /**
     * Recherche les badges par type de condition.
     */
    public List<Badge> findByConditionType(String conditionType) {
        List<Badge> badges = new ArrayList<>();
        String sql = "SELECT * FROM badge WHERE condition_type = ? AND actif = true ORDER BY condition_valeur ASC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, conditionType);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    badges.add(mapResultSetToBadge(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche par type: " + e.getMessage());
            e.printStackTrace();
        }

        return badges;
    }

    /**
     * Compte le nombre total de badges.
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM badge";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors du comptage des badges: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Compte le nombre de badges actifs.
     */
    public int countActifs() {
        String sql = "SELECT COUNT(*) FROM badge WHERE actif = true";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors du comptage des badges actifs: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Recherche les badges éligibles pour un utilisateur basé sur ses statistiques.
     * @param totalXP Total des points XP de l'utilisateur
     * @param coursCompletes Nombre de cours complétés
     * @param niveau Niveau actuel de l'utilisateur
     * @return Liste des badges que l'utilisateur peut obtenir
     */
    public List<Badge> findBadgesEligibles(int totalXP, int coursCompletes, int niveau) {
        List<Badge> badges = new ArrayList<>();
        
        for (Badge badge : findActifs()) {
            if (badge.verifierCondition(totalXP, coursCompletes, niveau)) {
                badges.add(badge);
            }
        }
        
        return badges;
    }

    /**
     * Mappe un ResultSet vers un objet Badge.
     */
    private Badge mapResultSetToBadge(ResultSet rs) throws SQLException {
        Badge badge = new Badge();
        badge.setId(rs.getInt("id"));
        badge.setNom(rs.getString("nom"));
        badge.setDescription(rs.getString("description"));
        badge.setCondition(rs.getString("condition_text"));
        badge.setConditionType(rs.getString("condition_type"));
        badge.setConditionValeur(rs.getInt("condition_valeur"));
        badge.setIcone(rs.getString("icone"));
        badge.setCouleur(rs.getString("couleur"));
        badge.setPointsBonus(rs.getInt("points_bonus"));
        badge.setActif(rs.getBoolean("actif"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            badge.setCreatedAt(createdAt.toLocalDateTime());
        }

        return badge;
    }
}
