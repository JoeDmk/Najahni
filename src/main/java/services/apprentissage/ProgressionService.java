package services.apprentissage;

import models.apprentissage.Cours;
import models.apprentissage.EtatProgression;
import models.apprentissage.Progression;
import tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service pour la gestion des Progressions.
 * Contient la logique métier ET l'accès direct à la base de données (pas de DAO).
 */
public class ProgressionService {

    private final Connection connection;
    private final CoursService coursService;
    private final BadgeService badgeService;

    public ProgressionService() {
        this.connection = MyConnection.getInstance().getCnx();
        this.coursService = new CoursService();
        this.badgeService = new BadgeService();
    }

    // ═══════════════════════════════════════════════════════════
    //  PROGRESSION CRUD
    // ═══════════════════════════════════════════════════════════

    public Progression creer(Progression progression) {
        String sql = "INSERT INTO progression (user_id, cours_id, pourcentage, points_xp, niveau, etat, date_debut, date_obtention) VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, progression.getUserId());
            stmt.setInt(2, progression.getCoursId());
            stmt.setDouble(3, progression.getPourcentage());
            stmt.setInt(4, progression.getPointsXP());
            stmt.setInt(5, progression.getNiveau());
            stmt.setString(6, progression.getEtat().name());
            if (progression.getDateObtention() != null)
                stmt.setTimestamp(7, Timestamp.valueOf(progression.getDateObtention()));
            else stmt.setNull(7, Types.TIMESTAMP);

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) progression.setId(keys.getInt(1));
                }
            }
            return progression;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création de la progression", e);
        }
    }

    public boolean mettreAJour(Progression progression) {
        // Only award XP if both conditions are met: 100% AND COMPLETE/CERTIFIE
        boolean conditionsMet = progression.getPourcentage() >= 100.0 &&
                (progression.getEtat() == EtatProgression.COMPLETE ||
                        progression.getEtat() == EtatProgression.CERTIFIE);
        int xpToSave = conditionsMet ? progression.getPointsXP() : 0;

        String sql = """
            UPDATE progression SET
                pourcentage = ?, points_xp = ?, niveau = ?,
                etat = ?, date_obtention = ?, updated_at = NOW()
            WHERE id = ?
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, progression.getPourcentage());
            stmt.setInt(2, xpToSave);
            stmt.setInt(3, progression.getNiveau());
            stmt.setString(4, progression.getEtat().name());
            if (progression.getDateObtention() != null)
                stmt.setTimestamp(5, Timestamp.valueOf(progression.getDateObtention()));
            else stmt.setNull(5, Types.TIMESTAMP);
            stmt.setInt(6, progression.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour de la progression", e);
        }
    }

    public boolean supprimer(int id) {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM progression WHERE id = ?")) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de la progression", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  FINDERS
    // ═══════════════════════════════════════════════════════════

    public Optional<Progression> trouverParId(int id) {
        String sql = "SELECT p.*, CONCAT(u.firstname, ' ', u.lastname) AS user_nom, c.titre AS cours_titre FROM progression p LEFT JOIN user u ON p.user_id = u.id LEFT JOIN cours c ON p.cours_id = c.id WHERE p.id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération de la progression", e);
        }
        return Optional.empty();
    }

    public List<Progression> trouverToutes() {
        String sql = "SELECT p.*, CONCAT(u.firstname, ' ', u.lastname) AS user_nom, c.titre AS cours_titre FROM progression p LEFT JOIN user u ON p.user_id = u.id LEFT JOIN cours c ON p.cours_id = c.id ORDER BY p.updated_at DESC";
        List<Progression> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des progressions: " + e.getMessage());
        }
        return list;
    }

    public Optional<Progression> trouverParUtilisateurEtCours(int userId, int coursId) {
        String sql = "SELECT p.*, CONCAT(u.firstname, ' ', u.lastname) AS user_nom, c.titre AS cours_titre FROM progression p LEFT JOIN user u ON p.user_id = u.id LEFT JOIN cours c ON p.cours_id = c.id WHERE p.user_id = ? AND p.cours_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, coursId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche de progression", e);
        }
        return Optional.empty();
    }

    public List<Progression> trouverParUtilisateur(int userId) {
        String sql = "SELECT p.*, CONCAT(u.firstname, ' ', u.lastname) AS user_nom, c.titre AS cours_titre FROM progression p LEFT JOIN user u ON p.user_id = u.id LEFT JOIN cours c ON p.cours_id = c.id WHERE p.user_id = ? ORDER BY p.updated_at DESC";
        List<Progression> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des progressions", e);
        }
        return list;
    }

    public List<Progression> trouverParEtat(EtatProgression etat) {
        String sql = "SELECT p.*, CONCAT(u.firstname, ' ', u.lastname) AS user_nom, c.titre AS cours_titre FROM progression p LEFT JOIN user u ON p.user_id = u.id LEFT JOIN cours c ON p.cours_id = c.id WHERE p.etat = ? ORDER BY p.updated_at DESC";
        List<Progression> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, etat.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des progressions par état", e);
        }
        return list;
    }

    // ═══════════════════════════════════════════════════════════
    //  BUSINESS LOGIC
    // ═══════════════════════════════════════════════════════════

    public Progression demarrerCours(int userId, int coursId) {
        Optional<Progression> existante = trouverParUtilisateurEtCours(userId, coursId);
        if (existante.isPresent()) return existante.get();

        Progression progression = new Progression(userId, coursId);
        progression.setEtat(EtatProgression.EN_COURS);
        return creer(progression);
    }

    public boolean mettreAJourPourcentage(int progressionId, double nouveauPourcentage) {
        Optional<Progression> opt = trouverParId(progressionId);
        if (opt.isEmpty()) return false;

        Progression p = opt.get();
        p.setPourcentage(nouveauPourcentage);
        if (nouveauPourcentage >= 100.0 && p.getDateObtention() == null) {
            p.setDateObtention(LocalDateTime.now());
            p.setEtat(EtatProgression.COMPLETE);
        }
        return mettreAJour(p);
    }

    public boolean mettreAJourProgression(int progressionId, double nouveauPourcentage, int pointsXP) {
        Optional<Progression> opt = trouverParId(progressionId);
        if (opt.isEmpty()) return false;

        Progression p = opt.get();
        p.setPourcentage(nouveauPourcentage);
        if (nouveauPourcentage >= 100.0) {
            p.setEtat(EtatProgression.COMPLETE);
            p.setDateObtention(LocalDateTime.now());
            int newTotalXP = getTotalXPByUser(p.getUserId());
            updateUserTotalXP(p.getUserId(), newTotalXP);
            badgeService.verifierEtAttribuerBadges(p.getUserId());
        }
        return mettreAJour(p);
    }

    public Progression completerCours(int userId, int coursId) {
        Progression progression = demarrerCours(userId, coursId);
        progression.setPourcentage(100.0);
        progression.setEtat(EtatProgression.COMPLETE);
        progression.setDateObtention(LocalDateTime.now());

        Optional<Cours> coursOpt = coursService.trouverParId(coursId);
        if (coursOpt.isPresent()) {
            Cours cours = coursOpt.get();
            progression.ajouterPointsXP(cours.getPointsXP());
            if (cours.isCertification()) progression.setEtat(EtatProgression.CERTIFIE);
        }

        mettreAJour(progression);
        int newTotalXP = getTotalXPByUser(userId);
        updateUserTotalXP(userId, newTotalXP);
        badgeService.verifierEtAttribuerBadges(userId);
        return progression;
    }

    // ═══════════════════════════════════════════════════════════
    //  STATISTICS & XP
    // ═══════════════════════════════════════════════════════════

    public int getTotalXP(int userId) {
        return getTotalXPByUser(userId);
    }

    public int getNiveauGlobal(int userId) {
        int totalXP = getTotalXP(userId);
        for (int i = Progression.SEUILS_NIVEAU.length - 1; i >= 0; i--) {
            if (totalXP >= Progression.SEUILS_NIVEAU[i]) return i + 1;
        }
        return 1;
    }

    public int getNombreCoursCompletes(int userId) {
        return countCoursCompletesByUser(userId);
    }

    public int[] getStatistiquesUtilisateur(int userId) {
        int[] stats = new int[4];
        stats[0] = getTotalXP(userId);
        stats[1] = getNiveauGlobal(userId);
        stats[2] = countCoursCompletesByUser(userId);
        stats[3] = (int) trouverParUtilisateur(userId).stream()
                .filter(p -> p.getEtat() == EtatProgression.EN_COURS).count();
        return stats;
    }

    public List<Object[]> getLeaderboard(int limit) {
        String sql = "SELECT p.user_id, CONCAT(u.firstname, ' ', u.lastname) AS user_nom, SUM(p.points_xp) AS total_xp, COUNT(CASE WHEN p.etat IN ('COMPLETE', 'CERTIFIE') THEN 1 END) AS cours_completes FROM progression p LEFT JOIN user u ON p.user_id = u.id GROUP BY p.user_id, u.firstname, u.lastname ORDER BY total_xp DESC LIMIT ?";
        List<Object[]> leaderboard = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    leaderboard.add(new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("user_nom"),
                        rs.getInt("total_xp"),
                        rs.getInt("cours_completes")
                    });
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du leaderboard", e);
        }
        return leaderboard;
    }

    public List<Object[]> getLeaderboard() {
        return getLeaderboard(10);
    }

    // ═══════════════════════════════════════════════════════════
    //  PRIVATE DB HELPERS
    // ═══════════════════════════════════════════════════════════

    private int getTotalXPByUser(int userId) {
        String sql = """
            SELECT COALESCE(SUM(points_xp), 0) FROM progression
            WHERE user_id = ? AND pourcentage >= 100.0 AND etat IN ('COMPLETE', 'CERTIFIE')
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du calcul des XP totaux", e);
        }
        return 0;
    }

    private int countCoursCompletesByUser(int userId) {
        String sql = "SELECT COUNT(*) FROM progression WHERE user_id = ? AND etat IN ('COMPLETE', 'CERTIFIE')";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du comptage des cours complétés", e);
        }
        return 0;
    }

    private void updateUserTotalXP(int userId, int totalXP) {
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE user SET total_xp = ? WHERE id = ?")) {
            stmt.setInt(1, totalXP);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour du XP utilisateur", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  RESULT SET MAPPING
    // ═══════════════════════════════════════════════════════════

    private Progression mapResultSet(ResultSet rs) throws SQLException {
        Progression p = new Progression();
        p.setId(rs.getInt("id"));
        p.setUserId(rs.getInt("user_id"));
        p.setCoursId(rs.getInt("cours_id"));
        p.setPointsXP(rs.getInt("points_xp"));
        p.setNiveau(rs.getInt("niveau"));
        p.setPourcentage(rs.getDouble("pourcentage"));
        // Override etat with actual DB value (setPourcentage auto-updates etat)
        p.setEtat(EtatProgression.valueOf(rs.getString("etat")));

        Timestamp dateDebut = rs.getTimestamp("date_debut");
        if (dateDebut != null) p.setDateDebut(dateDebut.toLocalDateTime());
        Timestamp dateObtention = rs.getTimestamp("date_obtention");
        if (dateObtention != null) p.setDateObtention(dateObtention.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) p.setUpdatedAt(updatedAt.toLocalDateTime());

        try { p.setUserNom(rs.getString("user_nom")); } catch (SQLException ignored) {}
        try { p.setCoursTitre(rs.getString("cours_titre")); } catch (SQLException ignored) {}
        return p;
    }
}
