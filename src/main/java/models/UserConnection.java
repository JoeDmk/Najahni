package models;

import java.time.LocalDateTime;

/**
 * Represents a follow-style connection between users.
 * follower follows the followed user (one-way).
 */
public class UserConnection {
    private int id;
    private int followerId;
    private int followedId;
    private LocalDateTime createdAt;

    public UserConnection() {
    }

    public UserConnection(int followerId, int followedId) {
        this.followerId = followerId;
        this.followedId = followedId;
        this.createdAt = LocalDateTime.now();
    }

    public UserConnection(int id, int followerId, int followedId, LocalDateTime createdAt) {
        this.id = id;
        this.followerId = followerId;
        this.followedId = followedId;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getFollowerId() { return followerId; }
    public void setFollowerId(int followerId) { this.followerId = followerId; }

    public int getFollowedId() { return followedId; }
    public void setFollowedId(int followedId) { this.followedId = followedId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "UserConnection{" +
                "id=" + id +
                ", followerId=" + followerId +
                ", followedId=" + followedId +
                ", createdAt=" + createdAt +
                '}';
    }
}
