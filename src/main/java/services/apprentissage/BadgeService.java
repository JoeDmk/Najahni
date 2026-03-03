package services.apprentissage;

import models.apprentissage.Badge;
import models.apprentissage.Progression;
import tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des Badges de gamification.
 * Contient la logique métier ET l'accès direct à la base de données (pas de DAO).
 */
public class BadgeService {

    private final Connection connection;

    public BadgeService() {
        this.connection = MyConnection.getInstance().getCnx();
    }

    // ═══════════════════════════════════════════════════════════
    //  BADGE CRUD
    // ═══════════════════════════════════════════════════════════

    public Badge creerBadge(Badge badge) throws IllegalArgumentException {
        validerBadge(badge);
        String sql = """
            INSERT INTO badge (nom, description, icone, condition_obtention,
                             points_requis, cours_requis, niveau_requis, categorie, rarete, actif, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, badge.getNom());
            stmt.setString(2, badge.getDescription());
            stmt.setString(3, badge.getIcone());
            stmt.setString(4, badge.getConditionObtention());
            stmt.setInt(5, badge.getPointsRequis());
            stmt.setInt(6, badge.getCoursRequis());
            stmt.setInt(7, badge.getNiveauRequis());
            stmt.setString(8, badge.getCategorie());
            stmt.setString(9, badge.getRarete().name());
            stmt.setBoolean(10, badge.isActif());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) badge.setId(keys.getInt(1));
                }
            }
            return badge;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création du badge", e);
        }
    }

    public boolean modifierBadge(Badge badge) throws IllegalArgumentException {
        validerBadge(badge);
        String sql = """
            UPDATE badge SET
                nom = ?, description = ?, icone = ?, condition_obtention = ?,
                points_requis = ?, cours_requis = ?, niveau_requis = ?,
                categorie = ?, rarete = ?, actif = ?
            WHERE id = ?
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, badge.getNom());
            stmt.setString(2, badge.getDescription());
            stmt.setString(3, badge.getIcone());
            stmt.setString(4, badge.getConditionObtention());
            stmt.setInt(5, badge.getPointsRequis());
            stmt.setInt(6, badge.getCoursRequis());
            stmt.setInt(7, badge.getNiveauRequis());
            stmt.setString(8, badge.getCategorie());
            stmt.setString(9, badge.getRarete().name());
            stmt.setBoolean(10, badge.isActif());
            stmt.setInt(11, badge.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour du badge", e);
        }
    }

    public boolean supprimerBadge(int id) {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM badge WHERE id = ?")) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du badge", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  BADGE FINDERS
    // ═══════════════════════════════════════════════════════════

    public Optional<Badge> trouverParId(int id) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM badge WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapBadge(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du badge", e);
        }
        return Optional.empty();
    }

    public List<Badge> trouverTous() {
        String sql = "SELECT * FROM badge ORDER BY nom ASC";
        List<Badge> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapBadge(rs));
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des badges: " + e.getMessage());
        }
        return list;
    }

    public List<Badge> trouverActifs() {
        return trouverTous();
    }

    public List<Badge> trouverParTypeCondition(String conditionType) {
        String sql = "SELECT * FROM badge WHERE categorie = ? ORDER BY nom ASC";
        List<Badge> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, conditionType);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapBadge(rs));
            }
        } catch (SQLException e) {
            return trouverTous().stream()
                    .filter(b -> conditionType.equals(b.getCategorie()))
                    .collect(Collectors.toList());
        }
        return list;
    }

    public List<Badge> findByRarete(Badge.Rarete rarete) {
        String sql = "SELECT * FROM badge WHERE rarete = ? ORDER BY nom ASC";
        List<Badge> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, rarete.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapBadge(rs));
            }
        } catch (SQLException e) {
            return trouverTous().stream()
                    .filter(b -> rarete.equals(b.getRarete()))
                    .collect(Collectors.toList());
        }
        return list;
    }

    public List<Badge> searchByName(String nom) {
        String sql = "SELECT * FROM badge WHERE nom LIKE ? ORDER BY nom ASC";
        List<Badge> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + nom + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapBadge(rs));
            }
        } catch (SQLException e) { /* empty */ }
        return list;
    }

    // ═══════════════════════════════════════════════════════════
    //  STATISTICS
    // ═══════════════════════════════════════════════════════════

    public int compterBadges() {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM badge");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { /* fallback */ }
        return 0;
    }

    public int compterBadgesActifs() {
        return compterBadges();
    }

    public int compterBadgesUtilisateur(int userId) {
        return findBadgesEligibles(userId).size();
    }

    // ═══════════════════════════════════════════════════════════
    //  BADGE ELIGIBILITY & XP
    // ═══════════════════════════════════════════════════════════

    public List<Badge> findBadgesEligibles(int userId) {
        int totalXP = getUserTotalXP(userId);
        int coursCompletes = countCoursCompletesByUser(userId);

        int niveau = 1;
        for (int i = Progression.SEUILS_NIVEAU.length - 1; i >= 0; i--) {
            if (totalXP >= Progression.SEUILS_NIVEAU[i]) { niveau = i + 1; break; }
        }

        List<Badge> allBadges = trouverActifs();
        List<Badge> eligibles = new ArrayList<>();
        for (Badge badge : allBadges) {
            if (badge.verifierCondition(totalXP, coursCompletes, niveau)) {
                eligibles.add(badge);
            }
        }
        return eligibles;
    }

    public List<Badge> verifierEtAttribuerBadges(int userId) {
        return findBadgesEligibles(userId);
    }

    public Set<Integer> getUnlockedBadgeIds(int userId) {
        return findBadgesEligibles(userId).stream()
                .map(Badge::getId)
                .collect(Collectors.toSet());
    }

    public void attribuerBonusXPBadges(int userId) {
        int baseXP = getProgressionTotalXPByUser(userId);
        List<Badge> eligible = findBadgesEligibles(userId);
        int bonusTotal = eligible.stream().mapToInt(Badge::getPointsBonus).sum();
        int newTotal = baseXP + bonusTotal;
        updateUserTotalXP(userId, newTotal);
        System.out.println("DEBUG XP sync — base=" + baseXP + " badgeBonus=" + bonusTotal + " total=" + newTotal);
    }

    // ═══════════════════════════════════════════════════════════
    //  USER XP HELPERS (formerly in UserDAO)
    // ═══════════════════════════════════════════════════════════

    private int getUserTotalXP(int userId) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT total_xp FROM user WHERE id = ?")) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du XP utilisateur", e);
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
    //  PROGRESSION HELPERS (subset formerly in ProgressionDAO)
    // ═══════════════════════════════════════════════════════════

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

    private int getProgressionTotalXPByUser(int userId) {
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
            throw new RuntimeException("Erreur lors du calcul des XP", e);
        }
        return 0;
    }

    // ═══════════════════════════════════════════════════════════
    //  VALIDATION
    // ═══════════════════════════════════════════════════════════

    private void validerBadge(Badge badge) throws IllegalArgumentException {
        if (badge == null)
            throw new IllegalArgumentException("Le badge ne peut pas être null.");
        if (badge.getNom() == null || badge.getNom().trim().isEmpty())
            throw new IllegalArgumentException("Le nom du badge est obligatoire.");
        if (badge.getNom().length() > 100)
            throw new IllegalArgumentException("Le nom du badge ne doit pas dépasser 100 caractères.");
        if (badge.getDescription() != null && badge.getDescription().length() > 500)
            throw new IllegalArgumentException("La description ne doit pas dépasser 500 caractères.");
        if (badge.getCondition() == null || badge.getCondition().trim().isEmpty())
            throw new IllegalArgumentException("La condition d'obtention est obligatoire.");
    }

    // ═══════════════════════════════════════════════════════════
    //  BADGE MAPPING
    // ═══════════════════════════════════════════════════════════

    private String str(ResultSet rs, String col, String def) {
        try { String v = rs.getString(col); return v != null ? v : def; } catch (SQLException e) { return def; }
    }
    private int intVal(ResultSet rs, String col, int def) {
        try { return rs.getInt(col); } catch (SQLException e) { return def; }
    }
    private boolean bool(ResultSet rs, String col, boolean def) {
        try { return rs.getBoolean(col); } catch (SQLException e) { return def; }
    }

    private Badge mapBadge(ResultSet rs) throws SQLException {
        Badge badge = new Badge();
        badge.setId(rs.getInt("id"));
        badge.setNom(str(rs, "nom", ""));
        badge.setDescription(str(rs, "description", ""));
        badge.setIcone(str(rs, "icone", "\uD83C\uDFC6"));
        badge.setConditionObtention(str(rs, "condition_obtention", ""));
        badge.setPointsRequis(intVal(rs, "points_requis", 0));
        badge.setCoursRequis(intVal(rs, "cours_requis", 0));
        badge.setNiveauRequis(intVal(rs, "niveau_requis", 0));
        badge.setCategorie(str(rs, "categorie", "Général"));
        try {
            badge.setRarete(Badge.Rarete.valueOf(str(rs, "rarete", "COMMUN")));
        } catch (Exception ignored) {
            badge.setRarete(Badge.Rarete.COMMUN);
        }
        badge.setActif(bool(rs, "actif", true));
        try {
            Timestamp ts = rs.getTimestamp("created_at");
            if (ts != null) badge.setCreatedAt(ts.toLocalDateTime());
        } catch (SQLException ignored) {}
        return badge;
    }
}
