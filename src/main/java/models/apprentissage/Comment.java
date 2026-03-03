package models.apprentissage;

import java.time.LocalDateTime;

/**
 * Model représentant un Commentaire sur un Cours.
 */
public class Comment {

    private int id;
    private int coursId;
    private int userId;
    private String contenu;
    private double rating; // AI-generated star rating (0.5 to 5.0)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient field for display purposes (user name)
    private String userName;

    // Constructeur par défaut
    public Comment() {
    }

    // Constructeur sans ID (pour création)
    public Comment(int coursId, int userId, String contenu) {
        this.coursId = coursId;
        this.userId = userId;
        this.contenu = contenu;
    }

    // Constructeur complet
    public Comment(int id, int coursId, int userId, String contenu, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.coursId = coursId;
        this.userId = userId;
        this.contenu = contenu;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCoursId() {
        return coursId;
    }

    public void setCoursId(int coursId) {
        this.coursId = coursId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", coursId=" + coursId +
                ", userId=" + userId +
                ", contenu='" + contenu + '\'' +
                ", rating=" + rating +
                ", createdAt=" + createdAt +
                '}';
    }
}
