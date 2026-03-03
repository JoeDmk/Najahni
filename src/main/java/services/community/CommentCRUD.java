package services.community;

import models.community.Comment;
import tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentCRUD {

    private Connection conn;

    public CommentCRUD() {
        conn = MyConnection.getInstance().getConnection();
    }

    public void ajouter(Comment c) throws SQLException {
        String sql = "INSERT INTO comments (thread_id, user_id, content) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, c.getThreadId());
            ps.setInt(2, c.getUserId());
            ps.setString(3, c.getContent());
            ps.executeUpdate();
        }
    }

    public List<Comment> afficherByThread(int threadId) throws SQLException {

        List<Comment> comments = new ArrayList<>();

        String sql = """
        SELECT c.id, c.thread_id, c.user_id, c.content, c.created_at, u.firstname
        FROM comments c
        JOIN user u ON c.user_id = u.id
        WHERE c.thread_id = ?
        ORDER BY c.created_at DESC
    """;

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, threadId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {

            Comment comment = new Comment(
                    rs.getInt("id"),
                    rs.getInt("thread_id"),
                    rs.getInt("user_id"),
                    rs.getString("content"),
                    rs.getTimestamp("created_at")
            );

            comment.setFirstname(rs.getString("firstname"));


            comments.add(comment);
        }

        return comments;
    }


    // UPDATE
    public void modifier(int commentId, String newContent) throws SQLException {
        String sql = "UPDATE comments SET content=? WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, newContent);
        ps.setInt(2, commentId);
        ps.executeUpdate();
    }

    // DELETE
    public void supprimer(int commentId) throws SQLException {
        String sql = "DELETE FROM comments WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, commentId);
        ps.executeUpdate();
    }

    // Ownership check
    public boolean isOwner(int commentId, int userId) throws SQLException {
        String sql = "SELECT id FROM comments WHERE id=? AND user_id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, commentId);
        ps.setInt(2, userId);
        return ps.executeQuery().next();
    }
}
