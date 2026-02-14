package com.najahni.dao;

import com.najahni.models.EtatProgression;
import com.najahni.models.Progression;
import com.najahni.utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object pour l'entité Progression.
 * Gère toutes les opérations CRUD liées à la progression des utilisateurs.
 */
public class ProgressionDAO implements GenericDAO<Progression> {

    private final Connection connection;

    public ProgressionDAO() {
        this.connection = DBConnection.getInstance().getConnection();
    }

    @Override
    public Progression create(Progression progression) {
        String sql = """
            INSERT INTO progression (user_id, cours_id, pourcentage, points_xp, niveau, 
                                    etat, date_debut, date_obtention) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, progression.getUserId());
            stmt.setInt(2, progression.getCoursId());
            stmt.setDouble(3, progression.getPourcentage());
            stmt.setInt(4, progression.getPointsXP());
            stmt.setInt(5, progression.getNiveau());
            stmt.setString(6, progression.getEtat().name());
            stmt.setTimestamp(7, Timestamp.valueOf(progression.getDateDebut()));
            stmt.setTimestamp(8, progression.getDateObtention() != null ? 
                             Timestamp.valueOf(progression.getDateObtention()) : null);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        progression.setId(generatedKeys.getInt(1));
                    }
                }
            }

            System.out.println("✓ Progression créée avec succès. User ID: " + progression.getUserId() + 
                              ", Cours ID: " + progression.getCoursId());
            return progression;

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la création de la progression: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Optional<Progression> findById(int id) {
        String sql = """
            SELECT p.*, u.name as user_nom, c.titre as cours_titre 
            FROM progression p 
            LEFT JOIN user u ON p.user_id = u.id 
            LEFT JOIN cours c ON p.cours_id = c.id 
            WHERE p.id = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProgression(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche de la progression: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public List<Progression> findAll() {
        List<Progression> progressionList = new ArrayList<>();
        String sql = """
            SELECT p.*, u.name as user_nom, c.titre as cours_titre 
            FROM progression p 
            LEFT JOIN user u ON p.user_id = u.id 
            LEFT JOIN cours c ON p.cours_id = c.id 
            ORDER BY p.updated_at DESC
            """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                progressionList.add(mapResultSetToProgression(rs));
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la récupération des progressions: " + e.getMessage());
            e.printStackTrace();
        }

        return progressionList;
    }

    @Override
    public boolean update(Progression progression) {
        String sql = """
            UPDATE progression SET user_id = ?, cours_id = ?, pourcentage = ?, 
                                  points_xp = ?, niveau = ?, etat = ?, date_obtention = ? 
            WHERE id = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, progression.getUserId());
            stmt.setInt(2, progression.getCoursId());
            stmt.setDouble(3, progression.getPourcentage());
            stmt.setInt(4, progression.getPointsXP());
            stmt.setInt(5, progression.getNiveau());
            stmt.setString(6, progression.getEtat().name());
            stmt.setTimestamp(7, progression.getDateObtention() != null ? 
                             Timestamp.valueOf(progression.getDateObtention()) : null);
            stmt.setInt(8, progression.getId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✓ Progression mise à jour. ID: " + progression.getId());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la mise à jour de la progression: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM progression WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✓ Progression supprimée. ID: " + id);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la suppression de la progression: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Recherche la progression d'un utilisateur pour un cours.
     */
    public Optional<Progression> findByUserAndCours(int userId, int coursId) {
        String sql = """
            SELECT p.*, u.name as user_nom, c.titre as cours_titre 
            FROM progression p 
            LEFT JOIN user u ON p.user_id = u.id 
            LEFT JOIN cours c ON p.cours_id = c.id 
            WHERE p.user_id = ? AND p.cours_id = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, coursId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProgression(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche de la progression: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Recherche toutes les progressions d'un utilisateur.
     */
    public List<Progression> findByUserId(int userId) {
        List<Progression> progressionList = new ArrayList<>();
        String sql = """
            SELECT p.*, u.name as user_nom, c.titre as cours_titre 
            FROM progression p 
            LEFT JOIN user u ON p.user_id = u.id 
            LEFT JOIN cours c ON p.cours_id = c.id 
            WHERE p.user_id = ? 
            ORDER BY p.updated_at DESC
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    progressionList.add(mapResultSetToProgression(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche par utilisateur: " + e.getMessage());
            e.printStackTrace();
        }

        return progressionList;
    }

    /**
     * Recherche les progressions par état.
     */
    public List<Progression> findByEtat(EtatProgression etat) {
        List<Progression> progressionList = new ArrayList<>();
        String sql = """
            SELECT p.*, u.name as user_nom, c.titre as cours_titre 
            FROM progression p 
            LEFT JOIN user u ON p.user_id = u.id 
            LEFT JOIN cours c ON p.cours_id = c.id 
            WHERE p.etat = ? 
            ORDER BY p.updated_at DESC
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, etat.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    progressionList.add(mapResultSetToProgression(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche par état: " + e.getMessage());
            e.printStackTrace();
        }

        return progressionList;
    }

    /**
     * Calcule le total des points XP d'un utilisateur.
     */
    public int getTotalXPByUser(int userId) {
        String sql = "SELECT COALESCE(SUM(points_xp), 0) FROM progression WHERE user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors du calcul du total XP: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Compte le nombre de cours complétés par un utilisateur.
     */
    public int countCoursCompletesByUser(int userId) {
        String sql = "SELECT COUNT(*) FROM progression WHERE user_id = ? AND etat IN ('COMPLETE', 'CERTIFIE')";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors du comptage des cours complétés: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Récupère le classement des utilisateurs par XP total.
     * @param limit Nombre maximum de résultats
     * @return Liste des progressions groupées par utilisateur
     */
    public List<Object[]> getLeaderboard(int limit) {
        List<Object[]> leaderboard = new ArrayList<>();
        String sql = """
            SELECT p.user_id, u.name, SUM(p.points_xp) as total_xp, 
                   COUNT(CASE WHEN p.etat IN ('COMPLETE', 'CERTIFIE') THEN 1 END) as cours_completes,
                   MAX(p.niveau) as niveau_max 
            FROM progression p 
            JOIN user u ON p.user_id = u.id 
            GROUP BY p.user_id, u.name 
            ORDER BY total_xp DESC 
            LIMIT ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Object[] row = new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getInt("total_xp"),
                        rs.getInt("cours_completes"),
                        rs.getInt("niveau_max")
                    };
                    leaderboard.add(row);
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la récupération du leaderboard: " + e.getMessage());
            e.printStackTrace();
        }

        return leaderboard;
    }

    /**
     * Compte le nombre de progressions.
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM progression";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors du comptage des progressions: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Mappe un ResultSet vers un objet Progression.
     */
    private Progression mapResultSetToProgression(ResultSet rs) throws SQLException {
        Progression progression = new Progression();
        progression.setId(rs.getInt("id"));
        progression.setUserId(rs.getInt("user_id"));
        progression.setCoursId(rs.getInt("cours_id"));
        progression.setPourcentage(rs.getDouble("pourcentage"));
        progression.setPointsXP(rs.getInt("points_xp"));
        progression.setNiveau(rs.getInt("niveau"));
        progression.setEtat(EtatProgression.valueOf(rs.getString("etat")));

        try {
            progression.setUserNom(rs.getString("user_nom"));
        } catch (SQLException ignored) {}

        try {
            progression.setCoursTitre(rs.getString("cours_titre"));
        } catch (SQLException ignored) {}

        Timestamp dateDebut = rs.getTimestamp("date_debut");
        if (dateDebut != null) {
            progression.setDateDebut(dateDebut.toLocalDateTime());
        }

        Timestamp dateObtention = rs.getTimestamp("date_obtention");
        if (dateObtention != null) {
            progression.setDateObtention(dateObtention.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            progression.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return progression;
    }
}
