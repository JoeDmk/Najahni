package Entites;

public class GroupJoinRequest {

    private int id;
    private int groupId;
    private int userId;
    private String status;
    private String username; // for UI display

    public GroupJoinRequest(int groupId, int userId) {
        this.groupId = groupId;
        this.userId = userId;
        this.status = "PENDING";
    }
    public GroupJoinRequest(int id, int groupId, int userId, String status, String username) {
        this.id = id;
        this.groupId = groupId;
        this.userId = userId;
        this.status = status;
        this.username = username;
    }

    public int getId() { return id; }
    public int getGroupId() { return groupId; }
    public int getUserId() { return userId; }
    public String getStatus() { return status; }
    public String getUsername() { return username; }
}
