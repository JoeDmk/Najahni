package Entites;

import java.sql.Timestamp;

public class GroupMember {

    private int id;
    private int groupId;
    private int userId;
    private Timestamp joinedAt;
    private String firstname;


    public GroupMember() {}

    public GroupMember(int groupId, int userId) {
        this.groupId = groupId;
        this.userId = userId;
    }


    public GroupMember(int id, int groupId, int userId, Timestamp joinedAt) {
        this.id = id;
        this.groupId = groupId;
        this.userId = userId;
        this.joinedAt = joinedAt;
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

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }
    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    @Override
    public String toString() {
        return "GroupMember{" +
                "id=" + id +
                ", groupId=" + groupId +
                ", userId=" + userId +
                ", joinedAt=" + joinedAt +
                '}';
    }
}
