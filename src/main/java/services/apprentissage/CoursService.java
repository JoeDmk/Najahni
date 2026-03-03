package services.apprentissage;

import models.apprentissage.Cours;
import models.apprentissage.NiveauCours;
import models.apprentissage.TypeCours;
import tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des Cours.
 * Contient la logique métier ET l'accès direct à la base de données (pas de DAO).
 */
public class CoursService {

    private final Connection connection;

    public CoursService() {
        this.connection = MyConnection.getInstance().getCnx();
    }

    // ═══════════════════════════════════════════════════════════
    //  CRUD
    // ═══════════════════════════════════════════════════════════

    public Cours creerCours(Cours cours) throws IllegalArgumentException {
        validerCours(cours);
        String sql = """
            INSERT INTO cours (titre, description, type, niveau,
                             points_xp, duree_minutes, image_url, certification, createur_id, document_path, video_url)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, cours.getTitre());
            stmt.setString(2, cours.getDescription());
            stmt.setString(3, cours.getType() != null ? cours.getType().name() : "TEXTE");
            stmt.setString(4, cours.getNiveauDifficulte().name());
            stmt.setInt(5, cours.getPointsXP());
            stmt.setInt(6, cours.getDureeEstimee());
            stmt.setString(7, cours.getImageUrl());
            stmt.setBoolean(8, cours.isCertification());
            stmt.setInt(9, cours.getCreateurId());
            setOptionalString(stmt, 10, cours.getDocumentPath().filter(s -> !s.isBlank()).orElse(null));
            setOptionalString(stmt, 11, cours.getVideoUrl().filter(s -> !s.isBlank()).orElse(null));

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) cours.setId(keys.getInt(1));
                }
            }
            return cours;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création du cours", e);
        }
    }

    public boolean modifierCours(Cours cours) throws IllegalArgumentException {
        validerCours(cours);
        String sql = """
            UPDATE cours SET
                titre = ?, description = ?, type = ?, niveau = ?,
                points_xp = ?, duree_minutes = ?, image_url = ?,
                certification = ?, createur_id = ?, document_path = ?, video_url = ?
            WHERE id = ?
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, cours.getTitre());
            stmt.setString(2, cours.getDescription());
            stmt.setString(3, cours.getType() != null ? cours.getType().name() : "TEXTE");
            stmt.setString(4, cours.getNiveauDifficulte().name());
            stmt.setInt(5, cours.getPointsXP());
            stmt.setInt(6, cours.getDureeEstimee());
            stmt.setString(7, cours.getImageUrl());
            stmt.setBoolean(8, cours.isCertification());
            stmt.setInt(9, cours.getCreateurId());
            setOptionalString(stmt, 10, cours.getDocumentPath().filter(s -> !s.isBlank()).orElse(null));
            setOptionalString(stmt, 11, cours.getVideoUrl().filter(s -> !s.isBlank()).orElse(null));
            stmt.setInt(12, cours.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour du cours", e);
        }
    }

    public boolean supprimerCours(int id) {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM cours WHERE id = ?")) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du cours", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  FINDERS
    // ═══════════════════════════════════════════════════════════

    public Optional<Cours> trouverParId(int id) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM cours WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du cours", e);
        }
        return Optional.empty();
    }

    public List<Cours> trouverTous() {
        return executeQuery("SELECT * FROM cours ORDER BY titre ASC");
    }

    public List<Cours> trouverParNiveau(NiveauCours niveau) {
        String sql = "SELECT * FROM cours WHERE niveau = ? ORDER BY titre ASC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, niveau.name());
            return executeQueryFromStmt(stmt);
        } catch (SQLException e) {
            return trouverTous().stream()
                    .filter(c -> c.getNiveauDifficulte() == niveau)
                    .collect(Collectors.toList());
        }
    }

    public List<Cours> trouverParType(TypeCours type) {
        return trouverTous().stream()
                .filter(c -> c.getType() == type)
                .collect(Collectors.toList());
    }

    public List<Cours> trouverCoursCertifiants() {
        String sql = "SELECT * FROM cours WHERE certification = 1 ORDER BY titre ASC";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            List<Cours> list = new ArrayList<>();
            while (rs.next()) list.add(mapResultSet(rs));
            return list;
        } catch (SQLException e) {
            return trouverTous().stream().filter(Cours::isCertification).collect(Collectors.toList());
        }
    }

    public List<Cours> trouverParCreateur(int createurId) {
        return trouverTous().stream()
                .filter(c -> c.getCreateurId() == createurId)
                .collect(Collectors.toList());
    }

    public List<Cours> rechercher(String motCle) {
        if (motCle == null || motCle.trim().isEmpty()) return trouverTous();
        String sql = "SELECT * FROM cours WHERE titre LIKE ? ORDER BY titre ASC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + motCle.trim() + "%");
            return executeQueryFromStmt(stmt);
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  STATISTICS
    // ═══════════════════════════════════════════════════════════

    public int compterTous() {
        return countQuery("SELECT COUNT(*) FROM cours");
    }

    public int compterParNiveau(NiveauCours niveau) {
        return trouverParNiveau(niveau).size();
    }

    public int compterCertifiants() {
        return (int) trouverTous().stream().filter(Cours::isCertification).count();
    }

    public int calculerTotalXP() {
        return trouverTous().stream().mapToInt(Cours::getPointsXP).sum();
    }

    public int sumXPPoints() {
        String sql = "SELECT COALESCE(SUM(points_xp), 0) FROM cours";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { /* fallback */ }
        return 0;
    }

    // ═══════════════════════════════════════════════════════════
    //  VALIDATION
    // ═══════════════════════════════════════════════════════════

    private void validerCours(Cours cours) throws IllegalArgumentException {
        if (cours == null)
            throw new IllegalArgumentException("Le cours ne peut pas être null.");
        if (cours.getTitre() == null || cours.getTitre().trim().isEmpty())
            throw new IllegalArgumentException("Le titre du cours est obligatoire.");
        if (cours.getTitre().length() > 255)
            throw new IllegalArgumentException("Le titre ne doit pas dépasser 255 caractères.");
        if (cours.getDescription() != null && cours.getDescription().length() > 2000)
            throw new IllegalArgumentException("La description ne doit pas dépasser 2000 caractères.");
        if (cours.getPointsXP() < 0)
            throw new IllegalArgumentException("Les points XP doivent être positifs.");
        if (cours.getDureeEstimee() < 0)
            throw new IllegalArgumentException("La durée estimée doit être positive.");
        if (cours.getNiveauDifficulte() == null)
            throw new IllegalArgumentException("Le niveau de difficulté est obligatoire.");
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════

    private List<Cours> executeQuery(String sql) {
        List<Cours> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des cours: " + e.getMessage());
        }
        return list;
    }

    private List<Cours> executeQueryFromStmt(PreparedStatement stmt) throws SQLException {
        List<Cours> list = new ArrayList<>();
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    private int countQuery(String sql) {
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { /* fallback */ }
        return 0;
    }

    private void setOptionalString(PreparedStatement stmt, int index, String value) throws SQLException {
        if (value != null) stmt.setString(index, value);
        else stmt.setNull(index, Types.VARCHAR);
    }

    private String str(ResultSet rs, String col, String def) {
        try { String v = rs.getString(col); return v != null ? v : def; } catch (SQLException e) { return def; }
    }
    private int intVal(ResultSet rs, String col, int def) {
        try { return rs.getInt(col); } catch (SQLException e) { return def; }
    }
    private boolean bool(ResultSet rs, String col, boolean def) {
        try { return rs.getBoolean(col); } catch (SQLException e) { return def; }
    }
    private Timestamp ts(ResultSet rs, String col) {
        try { return rs.getTimestamp(col); } catch (SQLException e) { return null; }
    }

    private Cours mapResultSet(ResultSet rs) throws SQLException {
        Cours cours = new Cours();
        cours.setId(rs.getInt("id"));
        cours.setTitre(str(rs, "titre", ""));
        cours.setDescription(str(rs, "description", ""));
        // DB column is 'type' (ENUM: VIDEO, TEXTE, QUIZ, MIXTE)
        String typeStr = str(rs, "type", "TEXTE");
        try {
            cours.setType(TypeCours.valueOf(typeStr));
        } catch (Exception e) {
            cours.setType(TypeCours.TEXTE);
        }
        cours.setCategorie(typeStr);
        // DB column is 'niveau' (ENUM: DEBUTANT, INTERMEDIAIRE, AVANCE, EXPERT)
        try {
            cours.setNiveauDifficulte(NiveauCours.valueOf(str(rs, "niveau", "DEBUTANT")));
        } catch (Exception e) {
            cours.setNiveauDifficulte(NiveauCours.DEBUTANT);
        }
        cours.setPointsXP(intVal(rs, "points_xp", 100));
        cours.setDureeEstimee(intVal(rs, "duree_minutes", 30));
        cours.setImageUrl(str(rs, "image_url", null));
        cours.setCertification(bool(rs, "certification", false));
        cours.setCreateurId(intVal(rs, "createur_id", 0));
        cours.setDocumentPath(str(rs, "document_path", null));
        cours.setVideoUrl(str(rs, "video_url", null));
        Timestamp created = ts(rs, "created_at");
        if (created != null) cours.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = ts(rs, "updated_at");
        if (updated != null) cours.setUpdatedAt(updated.toLocalDateTime());
        return cours;
    }
}
