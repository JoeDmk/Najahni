package models.community;

import java.sql.Timestamp;

public class Group {

    private int id;
    private String name;
    private String description;
    private Timestamp createdAt;
    private int groupAdminId;   // ✅ NEW
    private boolean isPrivate;

    public boolean getIsPrivate(){
        return isPrivate;
    }
    public void setIsPrivate(boolean isPrivate){
        this.isPrivate=isPrivate;
    }
    public Group() {}

    // INSERT constructor
    public Group(String name, String description, int groupAdminId,boolean isPrivate) {
        this.name = name;
        this.description = description;
        this.groupAdminId = groupAdminId;
        this.isPrivate=isPrivate;
    }

    // FULL constructor (SELECT)
    public Group(int id, String name, String description,
                 Timestamp createdAt, int groupAdminId,boolean isPrivate) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.groupAdminId = groupAdminId;
        this.isPrivate=isPrivate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public int getGroupAdminId() { return groupAdminId; }
    public void setGroupAdminId(int groupAdminId) { this.groupAdminId = groupAdminId; }

    @Override
    public String toString() {
        return "Group{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", groupAdminId=" + groupAdminId +
                '}';
    }
}
