package Entites;

import java.sql.Timestamp;

public class Thread {

    private int id;
    private int groupId;
    private int userId;
    private String title;
    private String content;
    private Timestamp createdAt;
    private String firstname;  // 🔥 add this
    public String getFirstname() {
        return firstname;
    }
    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }
    public Thread() {}

    public Thread(int groupId, int userId, String title, String content) {
        this.groupId = groupId;
        this.userId = userId;
        this.title = title;
        this.content = content;
    }

    public Thread(int id, int groupId, int userId, String title, String content, Timestamp createdAt) {
        this.id = id;
        this.groupId = groupId;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Thread{" +
                "id=" + id +
                ", groupId=" + groupId +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
