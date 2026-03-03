package services.community;

import interfaces.community.IntrefaceCRUD;

import models.community.Thread;
import tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ThreadCRUD implements IntrefaceCRUD<Thread> {

    Connection conn;

    public ThreadCRUD() {
        conn = MyConnection.getInstance().getConnection();
    }

    @Override
    public void ajouter(Thread t) throws SQLException {
        String req = "INSERT INTO threads (group_id, user_id, title, content) VALUES (?, ?, ?, ?)";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, t.getGroupId());
        pst.setInt(2, t.getUserId());
        pst.setString(3, t.getTitle());
        pst.setString(4, t.getContent());
        pst.executeUpdate();

    }

    @Override
    public void modifier(Thread t) throws SQLException {
        String req = "UPDATE threads SET title=?, content=? WHERE id=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, t.getTitle());
        pst.setString(2, t.getContent());
        pst.setInt(3, t.getId());
        pst.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM threads WHERE id=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();
    }

    @Override
    public List<Thread> afficher() throws SQLException {
        List<Thread> list = new ArrayList<>();
        String req = """
    SELECT t.id, t.group_id, t.user_id, t.title, t.content, t.created_at, u.firstname
    FROM threads t
    JOIN user u ON t.user_id = u.id
""";

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {

            Thread thread = new Thread(
                    rs.getInt("id"),
                    rs.getInt("group_id"),
                    rs.getInt("user_id"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getTimestamp("created_at")
            );

            thread.setFirstname(rs.getString("firstname"));  // ✅ set it here

            list.add(thread);  // ✅ then add it
        }

        return list;
    }
    public List<Thread> getThreadsByGroup(int groupId) throws SQLException {

        List<Thread> list = new ArrayList<>();

        String req = "SELECT t.id, t.group_id, t.user_id, t.title, t.content, t.created_at, u.firstname\n" +
                "FROM threads t\n" +
                "JOIN user u ON t.user_id = u.id\n" +
                "WHERE t.group_id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, groupId);

        ResultSet rs = pst.executeQuery();

        while (rs.next()) {

            Thread thread = new Thread(
                    rs.getInt("id"),
                    rs.getInt("group_id"),
                    rs.getInt("user_id"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getTimestamp("created_at")
            );

            thread.setFirstname(rs.getString("firstname"));  // ✅ set it here

            list.add(thread);  // ✅ then add it
        }

        return list;
    }

}
