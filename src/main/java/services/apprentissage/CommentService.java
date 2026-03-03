package services.apprentissage;

import models.apprentissage.Comment;
import tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service pour la gestion des Commentaires.
 * Contient la logique métier ET l'accès direct à la base de données (pas de DAO).
 */
public class CommentService {

    private final Connection connection;

    public CommentService() {
        this.connection = MyConnection.getInstance().getCnx();
    }

    // ═══════════════════════════════════════════════════════════
    //  CRUD
    // ═══════════════════════════════════════════════════════════

    public Comment ajouterCommentaire(Comment comment) throws IllegalArgumentException {
        validerCommentaire(comment);
        String sql = "INSERT INTO comment (cours_id, user_id, contenu, rating, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, comment.getCoursId());
            stmt.setInt(2, comment.getUserId());
            stmt.setString(3, comment.getContenu());
            if (comment.getRating() > 0) stmt.setDouble(4, comment.getRating());
            else stmt.setNull(4, Types.DECIMAL);

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) comment.setId(keys.getInt(1));
                }
            }
            return comment;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création du commentaire", e);
        }
    }

    public boolean modifierCommentaire(Comment comment) throws IllegalArgumentException {
        validerCommentaire(comment);
        String sql = "UPDATE comment SET contenu = ?, rating = ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, comment.getContenu());
            if (comment.getRating() > 0) stmt.setDouble(2, comment.getRating());
            else stmt.setNull(2, Types.DECIMAL);
            stmt.setInt(3, comment.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour du commentaire", e);
        }
    }

    public boolean supprimerCommentaire(int id) {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM comment WHERE id = ?")) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du commentaire", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  FINDERS
    // ═══════════════════════════════════════════════════════════

    public Optional<Comment> trouverParId(int id) {
        String sql = "SELECT c.*, CONCAT(u.firstname, ' ', u.lastname) AS user_name FROM comment c LEFT JOIN user u ON c.user_id = u.id WHERE c.id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche du commentaire", e);
        }
        return Optional.empty();
    }

    public List<Comment> trouverTous() {
        String sql = "SELECT c.*, CONCAT(u.firstname, ' ', u.lastname) AS user_name FROM comment c LEFT JOIN user u ON c.user_id = u.id ORDER BY c.created_at DESC";
        List<Comment> list = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des commentaires", e);
        }
        return list;
    }

    public List<Comment> trouverParCours(int coursId) {
        String sql = "SELECT c.*, CONCAT(u.firstname, ' ', u.lastname) AS user_name FROM comment c LEFT JOIN user u ON c.user_id = u.id WHERE c.cours_id = ? ORDER BY c.created_at DESC";
        List<Comment> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, coursId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des commentaires du cours", e);
        }
        return list;
    }

    public List<Comment> trouverParUtilisateur(int userId) {
        String sql = "SELECT c.*, CONCAT(u.firstname, ' ', u.lastname) AS user_name FROM comment c LEFT JOIN user u ON c.user_id = u.id WHERE c.user_id = ? ORDER BY c.created_at DESC";
        List<Comment> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des commentaires de l'utilisateur", e);
        }
        return list;
    }

    // ═══════════════════════════════════════════════════════════
    //  VALIDATION
    // ═══════════════════════════════════════════════════════════

    private void validerCommentaire(Comment comment) throws IllegalArgumentException {
        if (comment.getContenu() == null || comment.getContenu().trim().isEmpty())
            throw new IllegalArgumentException("Le contenu du commentaire ne peut pas être vide.");
        if (comment.getContenu().trim().length() < 2)
            throw new IllegalArgumentException("Le commentaire doit contenir au moins 2 caractères.");
        if (comment.getContenu().length() > 2000)
            throw new IllegalArgumentException("Le commentaire ne peut pas dépasser 2000 caractères.");
        if (comment.getCoursId() <= 0)
            throw new IllegalArgumentException("L'ID du cours est invalide.");
        if (comment.getUserId() <= 0)
            throw new IllegalArgumentException("L'ID de l'utilisateur est invalide.");
    }

    // ═══════════════════════════════════════════════════════════
    //  MAPPING
    // ═══════════════════════════════════════════════════════════

    private Comment mapResultSet(ResultSet rs) throws SQLException {
        Comment comment = new Comment();
        comment.setId(rs.getInt("id"));
        comment.setCoursId(rs.getInt("cours_id"));
        comment.setUserId(rs.getInt("user_id"));
        comment.setContenu(rs.getString("contenu"));

        double rating = rs.getDouble("rating");
        if (!rs.wasNull()) comment.setRating(rating);

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) comment.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) comment.setUpdatedAt(updatedAt.toLocalDateTime());

        try {
            String userName = rs.getString("user_name");
            if (userName != null) comment.setUserName(userName);
        } catch (SQLException ignored) {}

        return comment;
    }
}
