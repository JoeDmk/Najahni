package com.najahni.services;

import com.najahni.models.EtatProgression;
import com.najahni.models.Progression;
import com.najahni.utils.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
/**
 * Service pour la gestion des Progressions.
 * Accède directement à la base de données via JDBC (pas de DAO).
 */
public class ProgressionService {

    private Connection cnx;
    private BadgeService badgeService;

    public ProgressionService() {
        this.cnx = DBConnection.getInstance().getConnection();
        this.badgeService = new BadgeService();
    }

    public ProgressionService(BadgeService badgeService) {
        this.cnx = DBConnection.getInstance().getConnection();
        this.badgeService = badgeService;
    }

    // ─── BUSINESS METHODS ────────────────────────────────────

    public Progression demarrerCours(int userId, int coursId) {
        Optional<Progression> existing = trouverParUtilisateurEtCours(userId, coursId);
        if (existing.isPresent()) {
            System.out.println("⚠ L'utilisateur a déjà commencé ce cours.");
            return existing.get();
        }

        Progression progression = new Progression();
        progression.setUserId(userId);
        progression.setCoursId(coursId);
        progression.setPourcentage(0);
        progression.setPointsXP(0);
        progression.setNiveau(1);
        progression.setEtat(EtatProgression.EN_COURS);
        progression.setDateDebut(LocalDateTime.now());

        return creer(progression);
    }

    public boolean mettreAJourPourcentage(int userId, int coursId, int pourcentage) {
        Optional<Progression> progOpt = trouverParUtilisateurEtCours(userId, coursId);
        if (progOpt.isEmpty()) {
            System.out.println("✗ Aucune progression trouvée.");
            return false;
        }

        Progression prog = progOpt.get();
        if (prog.getEtat() == EtatProgression.COMPLETE) {
            System.out.println("⚠ Le cours est déjà terminé.");
            return false;
        }

        pourcentage = Math.max(0, Math.min(100, pourcentage));
        prog.setPourcentage(pourcentage);

        if (pourcentage == 100) {
            return completerCours(userId, coursId);
        }

        return mettreAJour(prog);
    }

    public boolean mettreAJourProgression(Progression progression) {
        return mettreAJour(progression);
    }

    public boolean completerCours(int userId, int coursId) {
        Optional<Progression> progOpt = trouverParUtilisateurEtCours(userId, coursId);
        if (progOpt.isEmpty()) return false;

        Progression prog = progOpt.get();
        prog.setEtat(EtatProgression.COMPLETE);
        prog.setPourcentage(100);
        prog.setDateObtention(LocalDateTime.now());

        // Récupérer les points XP du cours directement
        int coursXP = getCoursXP(coursId);
        prog.setPointsXP(coursXP);

        boolean updated = mettreAJour(prog);
        if (updated) {
            System.out.println("✓ Cours complété! +" + coursXP + " XP");
            badgeService.verifierEtAttribuerBadges(userId);
        }
        return updated;
    }

    // ─── STATISTICS ──────────────────────────────────────────

    public int getTotalXP(int userId) {
        String sql = "SELECT COALESCE(SUM(points_xp), 0) FROM progression WHERE user_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getNiveauGlobal(int userId) {
        return getTotalXP(userId) / 100;
    }

    public int getNombreCoursCompletes(int userId) {
        String sql = "SELECT COUNT(*) FROM progression WHERE user_id = ? AND etat = 'COMPLETE'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int[] getStatistiquesUtilisateur(int userId) {
        return new int[]{getTotalXP(userId), getNiveauGlobal(userId), getNombreCoursCompletes(userId)};
    }

    public List<Object[]> getLeaderboard(int limit) {
        List<Object[]> leaderboard = new ArrayList<>();
        String sql = """
            SELECT p.user_id,
                   CONCAT(u.firstname, ' ', u.lastname) AS user_name,
                   COALESCE(SUM(p.points_xp), 0) AS total_xp,
                   COUNT(CASE WHEN p.etat = 'COMPLETE' THEN 1 END) AS cours_completes
            FROM progression p
            JOIN user u ON p.user_id = u.id
            GROUP BY p.user_id, u.firstname, u.lastname
            ORDER BY total_xp DESC
            LIMIT ?
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    leaderboard.add(new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("user_name"),
                        rs.getInt("total_xp"),
                        rs.getInt("cours_completes")
                    });
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return leaderboard;
    }

    // ─── CRUD ────────────────────────────────────────────────

    public Progression creer(Progression progression) {
        String sql = """
            INSERT INTO progression (user_id, cours_id, pourcentage, points_xp, niveau,
                                   etat, date_debut, date_obtention)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, progression.getUserId());
            ps.setInt(2, progression.getCoursId());
            ps.setDouble(3, progression.getPourcentage());
            ps.setInt(4, progression.getPointsXP());
            ps.setInt(5, progression.getNiveau());
            ps.setString(6, progression.getEtat() != null ? progression.getEtat().name() : "EN_COURS");
            ps.setTimestamp(7, progression.getDateDebut() != null ?
                    Timestamp.valueOf(progression.getDateDebut()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(8, progression.getDateObtention() != null ?
                    Timestamp.valueOf(progression.getDateObtention()) : null);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) progression.setId(rs.getInt(1));
            }
            return progression;
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la création de la progression: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Optional<Progression> trouverParId(int id) {
        String sql = "SELECT * FROM progression WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToProgression(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public List<Progression> trouverToutes() {
        List<Progression> list = new ArrayList<>();
        String sql = "SELECT * FROM progression ORDER BY updated_at DESC";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSetToProgression(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean mettreAJour(Progression progression) {
        String sql = """
            UPDATE progression SET pourcentage = ?, points_xp = ?, niveau = ?, etat = ?,
                                 date_obtention = ?
            WHERE id = ?
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDouble(1, progression.getPourcentage());
            ps.setInt(2, progression.getPointsXP());
            ps.setInt(3, progression.getNiveau());
            ps.setString(4, progression.getEtat() != null ? progression.getEtat().name() : "EN_COURS");
            ps.setTimestamp(5, progression.getDateObtention() != null ?
                    Timestamp.valueOf(progression.getDateObtention()) : null);
            ps.setInt(6, progression.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la mise à jour de la progression: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM progression WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la suppression: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ─── REQUÊTES SPÉCIFIQUES ────────────────────────────────

    public Optional<Progression> trouverParUtilisateurEtCours(int userId, int coursId) {
        String sql = "SELECT * FROM progression WHERE user_id = ? AND cours_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, coursId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToProgression(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public List<Progression> trouverParUtilisateur(int userId) {
        List<Progression> list = new ArrayList<>();
        String sql = "SELECT * FROM progression WHERE user_id = ? ORDER BY updated_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToProgression(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Progression> trouverParEtat(EtatProgression etat) {
        List<Progression> list = new ArrayList<>();
        String sql = "SELECT * FROM progression WHERE etat = ? ORDER BY updated_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, etat.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToProgression(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ─── HELPERS ─────────────────────────────────────────────

    private int getCoursXP(int coursId) {
        String sql = "SELECT points_xp FROM cours WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, coursId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("points_xp");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    /** Count courses currently in progress for a user */
    public int countCoursEnCours(int userId) {
        String sql = "SELECT COUNT(*) FROM progression WHERE user_id = ? AND etat = 'EN_COURS'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // ─── MAPPING ─────────────────────────────────────────────

    private Progression mapResultSetToProgression(ResultSet rs) throws SQLException {
        Progression prog = new Progression();
        prog.setId(rs.getInt("id"));
        prog.setUserId(rs.getInt("user_id"));
        prog.setCoursId(rs.getInt("cours_id"));
        prog.setPourcentage(rs.getDouble("pourcentage"));
        prog.setPointsXP(rs.getInt("points_xp"));
        prog.setNiveau(rs.getInt("niveau"));

        String etat = rs.getString("etat");
        if (etat != null) {
            try { prog.setEtat(EtatProgression.valueOf(etat.toUpperCase())); }
            catch (IllegalArgumentException e) { prog.setEtat(EtatProgression.EN_COURS); }
        }

        Timestamp dateDebut = rs.getTimestamp("date_debut");
        if (dateDebut != null) prog.setDateDebut(dateDebut.toLocalDateTime());
        Timestamp dateObtention = rs.getTimestamp("date_obtention");
        if (dateObtention != null) prog.setDateObtention(dateObtention.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) prog.setUpdatedAt(updatedAt.toLocalDateTime());

        // Charger les noms via JOIN si disponibles
        try {
            prog.setUserNom(rs.getString("user_name"));
        } catch (SQLException ignored) { }
        try {
            prog.setCoursTitre(rs.getString("cours_title"));
        } catch (SQLException ignored) { }

        return prog;
    }
}
