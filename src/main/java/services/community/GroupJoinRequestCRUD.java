package services.community;

import models.community.GroupJoinRequest;
import tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GroupJoinRequestCRUD {

    Connection conn = MyConnection.getInstance().getConnection();

    public void ajouter(GroupJoinRequest request) throws SQLException {

        String sql = "INSERT INTO group_join_request (group_id, user_id, status) VALUES (?, ?, 'PENDING')";

        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, request.getGroupId());
        pst.setInt(2, request.getUserId());

        pst.executeUpdate();
    }


    public boolean hasPendingRequest(int groupId, int userId) throws SQLException {

        String sql = "SELECT * FROM group_join_request WHERE group_id=? AND user_id=? AND status='PENDING'";

        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, groupId);
        pst.setInt(2, userId);

        ResultSet rs = pst.executeQuery();

        return rs.next();
    }
    public List<GroupJoinRequest> getPendingRequests(int groupId) throws SQLException {

        List<GroupJoinRequest> list = new ArrayList<>();

        String sql = """
        SELECT gjr.id, gjr.group_id, gjr.user_id, gjr.status, u.firstname
        FROM group_join_request gjr
        JOIN user u ON gjr.user_id = u.id
        WHERE gjr.group_id = ? AND gjr.status = 'PENDING'
    """;

        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, groupId);

        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            list.add(new GroupJoinRequest(
                    rs.getInt("id"),
                    rs.getInt("group_id"),
                    rs.getInt("user_id"),
                    rs.getString("status"),
                    rs.getString("firstname")
            ));
        }

        return list;
    }
    public void approveRequest(int requestId) throws SQLException {

        String sql = "UPDATE group_join_request SET status='APPROVED' WHERE id=?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, requestId);
        pst.executeUpdate();
    }
    public void rejectRequest(int requestId) throws SQLException {

        String sql = "UPDATE group_join_request SET status='REJECTED' WHERE id=?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, requestId);
        pst.executeUpdate();
    }
    public void cancelPendingRequest(int groupId, int userId) throws SQLException {
        String sql = "DELETE FROM group_join_request WHERE group_id=? AND user_id=? AND status='PENDING'";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, groupId);
        pst.setInt(2, userId);
        pst.executeUpdate();
    }


}
