package models.community;

import java.sql.Timestamp;

public class Comment {
    private String firstname;


    private int id;
    private int threadId;
    private int userId;
    private String content;
    private Timestamp createdAt;

    // Empty constructor
    public Comment() {}

    // Constructor for INSERT
    public Comment(int threadId, int userId, String content) {
        this.threadId = threadId;
        this.userId = userId;
        this.content = content;
    }

    // Full constructor
    public Comment(int id, int threadId, int userId, String content, Timestamp createdAt) {
        this.id = id;
        this.threadId = threadId;
        this.userId = userId;
        this.content = content;
        this.createdAt = createdAt;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public int getThreadId() { return threadId; }
    public void setThreadId(int threadId) { this.threadId = threadId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", threadId=" + threadId +
                ", userId=" + userId +
                ", content='" + content + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
