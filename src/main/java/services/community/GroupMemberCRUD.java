package services.community;

import interfaces.community.IntrefaceCRUD;

import models.community.GroupMember;
import tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupMemberCRUD implements IntrefaceCRUD<GroupMember> {

    Connection conn;

    public GroupMemberCRUD() {
        conn = MyConnection.getInstance().getConnection();
    }

    @Override
    public void ajouter(GroupMember gm) throws SQLException {
        String sql = "INSERT INTO group_members (group_id, user_id) VALUES (?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, gm.getGroupId());
        ps.setInt(2, gm.getUserId());
        ps.executeUpdate();

    }

    @Override
    public void modifier(GroupMember gm) throws SQLException {
        String sql = "UPDATE group_members SET group_id=?, user_id=? WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, gm.getGroupId());
        ps.setInt(2, gm.getUserId());
        ps.setInt(3, gm.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM group_members WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<GroupMember> afficher() throws SQLException {
        List<GroupMember> list = new ArrayList<>();
        String sql = "SELECT * FROM group_members";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            list.add(new GroupMember(
                    rs.getInt("id"),
                    rs.getInt("group_id"),
                    rs.getInt("user_id"),
                    rs.getTimestamp("joined_at")
            ));
        }
        return list;
    }
    public boolean isUserMember(int groupId, int userId) throws SQLException {
        String sql = "SELECT 1 FROM group_members WHERE group_id=? AND user_id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, groupId);
        ps.setInt(2, userId);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }
    public void deleteMembership(int groupId, int userId) throws SQLException {

        String sql = "DELETE FROM group_members WHERE group_id = ? AND user_id = ?";

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, groupId);
        ps.setInt(2, userId);

        ps.executeUpdate();
    }
    public List<GroupMember> getMembersByGroup(int groupId) throws SQLException {

        List<GroupMember> list = new ArrayList<>();

        String sql = """
        SELECT gm.id, gm.group_id, gm.user_id, gm.joined_at, u.firstname
                                                               
        FROM group_members gm
        JOIN user u ON gm.user_id = u.id
        WHERE gm.group_id = ?
    """;

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, groupId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {

            GroupMember gm = new GroupMember(
                    rs.getInt("id"),
                    rs.getInt("group_id"),
                    rs.getInt("user_id"),
                    rs.getTimestamp("joined_at")
            );

            gm.setFirstname(rs.getString("firstname"));

            list.add(gm);
        }

        return list;
    }


}
