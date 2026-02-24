package models;

import java.time.LocalDateTime;

public class Notification {
    private int id;
    private int userId;
    private String title;
    private String message;
    private String type; // INFO, WARNING, SUCCESS, DANGER, LOGIN
    private boolean read;
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(int userId, String title, String message, String type) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getTypeIcon() {
        return switch (type) {
            case "WARNING" -> "\u26A0";
            case "SUCCESS" -> "\u2714";
            case "DANGER" -> "\u26D4";
            case "LOGIN" -> "\uD83D\uDD11";
            default -> "\u2139";
        };
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                ", read=" + read +
                '}';
    }
}
