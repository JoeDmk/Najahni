package com.najahni.dao;

import com.najahni.models.Cours;
import com.najahni.models.NiveauCours;
import com.najahni.models.TypeCours;
import com.najahni.utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object pour l'entité Cours.
 * Gère toutes les opérations CRUD liées aux cours.
 */
public class CoursDAO implements GenericDAO<Cours> {

    private final Connection connection;

    public CoursDAO() {
        this.connection = DBConnection.getInstance().getConnection();
    }

    @Override
    public Cours create(Cours cours) {
        String sql = """
            INSERT INTO cours (titre, description, type, niveau, certification, 
                             points_xp, duree_minutes, image_url, createur_id) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, cours.getTitre());
            stmt.setString(2, cours.getDescription());
            stmt.setString(3, cours.getType().name());
            stmt.setString(4, cours.getNiveau().name());
            stmt.setBoolean(5, cours.isCertification());
            stmt.setInt(6, cours.getPointsXP());
            stmt.setInt(7, cours.getDureeMinutes());
            stmt.setString(8, cours.getImageUrl());
            stmt.setInt(9, cours.getCreateurId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        cours.setId(generatedKeys.getInt(1));
                    }
                }
            }

            System.out.println("✓ Cours créé avec succès: " + cours.getTitre());
            return cours;

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la création du cours: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Optional<Cours> findById(int id) {
        String sql = """
            SELECT c.*, u.name as createur_nom 
            FROM cours c 
            LEFT JOIN user u ON c.createur_id = u.id 
            WHERE c.id = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCours(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche du cours par ID: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public List<Cours> findAll() {
        List<Cours> coursList = new ArrayList<>();
        String sql = """
            SELECT c.*, u.name as createur_nom 
            FROM cours c 
            LEFT JOIN user u ON c.createur_id = u.id 
            ORDER BY c.created_at DESC
            """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                coursList.add(mapResultSetToCours(rs));
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la récupération des cours: " + e.getMessage());
            e.printStackTrace();
        }

        return coursList;
    }

    @Override
    public boolean update(Cours cours) {
        String sql = """
            UPDATE cours SET titre = ?, description = ?, type = ?, niveau = ?, 
                           certification = ?, points_xp = ?, duree_minutes = ?, 
                           image_url = ?, createur_id = ? 
            WHERE id = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, cours.getTitre());
            stmt.setString(2, cours.getDescription());
            stmt.setString(3, cours.getType().name());
            stmt.setString(4, cours.getNiveau().name());
            stmt.setBoolean(5, cours.isCertification());
            stmt.setInt(6, cours.getPointsXP());
            stmt.setInt(7, cours.getDureeMinutes());
            stmt.setString(8, cours.getImageUrl());
            stmt.setInt(9, cours.getCreateurId());
            stmt.setInt(10, cours.getId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✓ Cours mis à jour avec succès. ID: " + cours.getId());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la mise à jour du cours: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM cours WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✓ Cours supprimé avec succès. ID: " + id);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la suppression du cours: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Recherche les cours par niveau.
     */
    public List<Cours> findByNiveau(NiveauCours niveau) {
        List<Cours> coursList = new ArrayList<>();
        String sql = """
            SELECT c.*, u.name as createur_nom 
            FROM cours c 
            LEFT JOIN user u ON c.createur_id = u.id 
            WHERE c.niveau = ? 
            ORDER BY c.created_at DESC
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, niveau.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    coursList.add(mapResultSetToCours(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche par niveau: " + e.getMessage());
            e.printStackTrace();
        }

        return coursList;
    }

    /**
     * Recherche les cours par type.
     */
    public List<Cours> findByType(TypeCours type) {
        List<Cours> coursList = new ArrayList<>();
        String sql = """
            SELECT c.*, u.name as createur_nom 
            FROM cours c 
            LEFT JOIN user u ON c.createur_id = u.id 
            WHERE c.type = ? 
            ORDER BY c.created_at DESC
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, type.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    coursList.add(mapResultSetToCours(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche par type: " + e.getMessage());
            e.printStackTrace();
        }

        return coursList;
    }

    /**
     * Recherche les cours avec certification.
     */
    public List<Cours> findWithCertification() {
        List<Cours> coursList = new ArrayList<>();
        String sql = """
            SELECT c.*, u.name as createur_nom 
            FROM cours c 
            LEFT JOIN user u ON c.createur_id = u.id 
            WHERE c.certification = true 
            ORDER BY c.created_at DESC
            """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                coursList.add(mapResultSetToCours(rs));
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche des cours certifiants: " + e.getMessage());
            e.printStackTrace();
        }

        return coursList;
    }

    /**
     * Recherche les cours par créateur.
     */
    public List<Cours> findByCreateur(int createurId) {
        List<Cours> coursList = new ArrayList<>();
        String sql = """
            SELECT c.*, u.name as createur_nom 
            FROM cours c 
            LEFT JOIN user u ON c.createur_id = u.id 
            WHERE c.createur_id = ? 
            ORDER BY c.created_at DESC
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, createurId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    coursList.add(mapResultSetToCours(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche par créateur: " + e.getMessage());
            e.printStackTrace();
        }

        return coursList;
    }

    /**
     * Recherche les cours par mot-clé dans le titre ou la description.
     */
    public List<Cours> search(String keyword) {
        List<Cours> coursList = new ArrayList<>();
        String sql = """
            SELECT c.*, u.name as createur_nom 
            FROM cours c 
            LEFT JOIN user u ON c.createur_id = u.id 
            WHERE c.titre LIKE ? OR c.description LIKE ? 
            ORDER BY c.created_at DESC
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            String searchPattern = "%" + keyword + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    coursList.add(mapResultSetToCours(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de la recherche: " + e.getMessage());
            e.printStackTrace();
        }

        return coursList;
    }

    /**
     * Compte le nombre total de cours.
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM cours";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors du comptage des cours: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Compte les cours par niveau.
     */
    public int countByNiveau(NiveauCours niveau) {
        String sql = "SELECT COUNT(*) FROM cours WHERE niveau = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, niveau.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur lors du comptage par niveau: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Mappe un ResultSet vers un objet Cours.
     */
    private Cours mapResultSetToCours(ResultSet rs) throws SQLException {
        Cours cours = new Cours();
        cours.setId(rs.getInt("id"));
        cours.setTitre(rs.getString("titre"));
        cours.setDescription(rs.getString("description"));
        cours.setType(TypeCours.valueOf(rs.getString("type")));
        cours.setNiveau(NiveauCours.valueOf(rs.getString("niveau")));
        cours.setCertification(rs.getBoolean("certification"));
        cours.setPointsXP(rs.getInt("points_xp"));
        cours.setDureeMinutes(rs.getInt("duree_minutes"));
        cours.setImageUrl(rs.getString("image_url"));
        cours.setCreateurId(rs.getInt("createur_id"));
        
        // Champ optionnel de jointure
        try {
            cours.setCreateurNom(rs.getString("createur_nom"));
        } catch (SQLException ignored) {}

        // Timestamps
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            cours.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            cours.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return cours;
    }
}
