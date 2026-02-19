package com.najahni.services;

import com.najahni.models.Cours;
import com.najahni.models.NiveauCours;
import com.najahni.models.TypeCours;
import com.najahni.utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service pour la gestion des Cours.
 * Accède directement à la base de données via JDBC (pas de DAO).
 */
public class CoursService {

    private Connection cnx;

    public CoursService() {
        this.cnx = DBConnection.getInstance().getConnection();
    }

    // ─── CRUD ────────────────────────────────────────────────

    public Cours creerCours(Cours cours) throws IllegalArgumentException {
        validerCours(cours);
        String sql = """
            INSERT INTO cours (titre, description, categorie, niveau_difficulte, certification,
                             points_xp, duree_estimee, image_url)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cours.getTitre());
            ps.setString(2, cours.getDescription());
            ps.setString(3, cours.getType() != null ? cours.getType().name() : "TEXTE");
            ps.setString(4, cours.getNiveau() != null ? cours.getNiveau().name() : "DEBUTANT");
            ps.setBoolean(5, cours.isCertification());
            ps.setInt(6, cours.getPointsXP());
            ps.setInt(7, cours.getDureeMinutes());
            ps.setString(8, cours.getImageUrl());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) cours.setId(rs.getInt(1));
            }
            System.out.println("✓ Cours créé avec succès: " + cours.getTitre());
            return cours;
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la création du cours: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Optional<Cours> trouverParId(int id) {
        String sql = "SELECT * FROM cours WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToCours(rs));
            }
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche du cours par ID: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Cours> trouverTous() {
        List<Cours> coursList = new ArrayList<>();
        String sql = "SELECT * FROM cours ORDER BY created_at DESC";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) coursList.add(mapResultSetToCours(rs));
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la récupération des cours: " + e.getMessage());
            e.printStackTrace();
        }
        return coursList;
    }

    public boolean modifierCours(Cours cours) throws IllegalArgumentException {
        validerCours(cours);
        String sql = """
            UPDATE cours SET titre = ?, description = ?, categorie = ?, niveau_difficulte = ?,
                           certification = ?, points_xp = ?, duree_estimee = ?, image_url = ?
            WHERE id = ?
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, cours.getTitre());
            ps.setString(2, cours.getDescription());
            ps.setString(3, cours.getType() != null ? cours.getType().name() : "TEXTE");
            ps.setString(4, cours.getNiveau() != null ? cours.getNiveau().name() : "DEBUTANT");
            ps.setBoolean(5, cours.isCertification());
            ps.setInt(6, cours.getPointsXP());
            ps.setInt(7, cours.getDureeMinutes());
            ps.setString(8, cours.getImageUrl());
            ps.setInt(9, cours.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la mise à jour du cours: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean supprimerCours(int id) {
        String sql = "DELETE FROM cours WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la suppression du cours: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ─── REQUÊTES SPÉCIFIQUES ────────────────────────────────

    public List<Cours> trouverParNiveau(NiveauCours niveau) {
        List<Cours> coursList = new ArrayList<>();
        String sql = "SELECT * FROM cours WHERE niveau_difficulte = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, niveau.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) coursList.add(mapResultSetToCours(rs));
            }
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche par niveau: " + e.getMessage());
            e.printStackTrace();
        }
        return coursList;
    }

    public List<Cours> trouverParType(TypeCours type) {
        List<Cours> coursList = new ArrayList<>();
        String sql = "SELECT * FROM cours WHERE categorie = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) coursList.add(mapResultSetToCours(rs));
            }
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche par type: " + e.getMessage());
            e.printStackTrace();
        }
        return coursList;
    }

    public List<Cours> trouverCoursCertifiants() {
        List<Cours> coursList = new ArrayList<>();
        String sql = "SELECT * FROM cours WHERE certification = true ORDER BY created_at DESC";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) coursList.add(mapResultSetToCours(rs));
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche des cours certifiants: " + e.getMessage());
            e.printStackTrace();
        }
        return coursList;
    }

    public List<Cours> trouverParCreateur(int createurId) {
        return trouverTous();
    }

    public List<Cours> rechercher(String motCle) {
        if (motCle == null || motCle.trim().isEmpty()) return trouverTous();
        List<Cours> coursList = new ArrayList<>();
        String sql = "SELECT * FROM cours WHERE titre LIKE ? OR description LIKE ? ORDER BY created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            String pattern = "%" + motCle.trim() + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) coursList.add(mapResultSetToCours(rs));
            }
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche: " + e.getMessage());
            e.printStackTrace();
        }
        return coursList;
    }

    public int compterTous() {
        String sql = "SELECT COUNT(*) FROM cours";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int compterParNiveau(NiveauCours niveau) {
        String sql = "SELECT COUNT(*) FROM cours WHERE niveau_difficulte = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, niveau.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int compterCertifiants() {
        return trouverCoursCertifiants().size();
    }

    public int calculerTotalXP() {
        return trouverTous().stream().mapToInt(Cours::getPointsXP).sum();
    }

    // ─── MAPPING ─────────────────────────────────────────────

    private Cours mapResultSetToCours(ResultSet rs) throws SQLException {
        Cours cours = new Cours();
        cours.setId(rs.getInt("id"));
        cours.setTitre(rs.getString("titre"));
        cours.setDescription(rs.getString("description"));

        String categorie = rs.getString("categorie");
        if (categorie != null) {
            try { cours.setType(TypeCours.valueOf(categorie.toUpperCase())); }
            catch (IllegalArgumentException e) { cours.setType(TypeCours.TEXTE); }
        }

        String niveau = rs.getString("niveau_difficulte");
        if (niveau != null) {
            try { cours.setNiveau(NiveauCours.valueOf(niveau.toUpperCase())); }
            catch (IllegalArgumentException e) { cours.setNiveau(NiveauCours.DEBUTANT); }
        }

        cours.setCertification(rs.getBoolean("certification"));
        cours.setPointsXP(rs.getInt("points_xp"));
        cours.setDureeMinutes(rs.getInt("duree_estimee"));
        cours.setImageUrl(rs.getString("image_url"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) cours.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) cours.setUpdatedAt(updatedAt.toLocalDateTime());

        return cours;
    }

    // ─── VALIDATION ──────────────────────────────────────────

    private void validerCours(Cours cours) throws IllegalArgumentException {
        if (cours == null) throw new IllegalArgumentException("Le cours ne peut pas être null.");
        if (cours.getTitre() == null || cours.getTitre().trim().isEmpty())
            throw new IllegalArgumentException("Le titre du cours est obligatoire.");
        if (cours.getTitre().length() > 255)
            throw new IllegalArgumentException("Le titre ne doit pas dépasser 255 caractères.");
        if (cours.getDescription() != null && cours.getDescription().length() > 2000)
            throw new IllegalArgumentException("La description ne doit pas dépasser 2000 caractères.");
        if (cours.getPointsXP() < 0)
            throw new IllegalArgumentException("Les points XP doivent être positifs.");
        if (cours.getDureeMinutes() < 0)
            throw new IllegalArgumentException("La durée estimée doit être positive.");
        if (cours.getNiveau() == null)
            throw new IllegalArgumentException("Le niveau de difficulté est obligatoire.");
        if (cours.getType() == null)
            throw new IllegalArgumentException("Le type de cours est obligatoire.");
    }
}
