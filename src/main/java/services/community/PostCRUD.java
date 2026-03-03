package services.community;

import interfaces.community.IntrefaceCRUD;

import models.community.Post;
import tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostCRUD implements IntrefaceCRUD<Post> {


    private final Connection conn;

    public PostCRUD() {
        conn = MyConnection.getInstance().getConnection();
    }

    @Override
    public void ajouter(Post p) throws SQLException {
        String req = "INSERT INTO posts (user_id, content, image_url) VALUES (?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(req)) {
            pst.setInt(1, p.getUserId());
            pst.setString(2, p.getContent());
            pst.setString(3, p.getImageUrl()); // can be null
            pst.executeUpdate();
        }
    }

    @Override
    public void modifier(Post p) throws SQLException {
        String req = "UPDATE posts SET content=?, image_url=? WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(req)) {
            pst.setString(1, p.getContent());
            pst.setString(2, p.getImageUrl());
            pst.setInt(3, p.getId());
            pst.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM posts WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(req)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }


    // Basic feed without "likedByMe" (needs current user id)
    @Override
    public List<Post> afficher() throws SQLException {
        List<Post> list = new ArrayList<>();

        String req = """
            SELECT p.id, p.user_id, p.content, p.image_url, p.created_at,
                   u.firstname, u.lastname,
                   (SELECT COUNT(*) FROM post_reactions pr WHERE pr.post_id = p.id) AS reactions_count
            FROM posts p
            JOIN user u ON p.user_id = u.id
            ORDER BY p.created_at DESC
        """;

        try (PreparedStatement pst = conn.prepareStatement(req);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                Post post = new Post(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("content")
                );

                post.setImageUrl(rs.getString("image_url"));
                post.setCreatedAt(rs.getTimestamp("created_at"));

                post.setFirstname(rs.getString("firstname"));
                post.setLastname(rs.getString("lastname"));

                post.setReactionsCount(rs.getInt("reactions_count"));
                post.setMyReaction(null); // afficher() is generic feed


                list.add(post);
            }
        }

        return list;
    }

    public List<Post> afficherFeed(int currentUserId) throws SQLException {
        List<Post> list = new ArrayList<>();

        String req = """
        SELECT p.id, p.user_id, p.content, p.image_url, p.created_at,
               u.firstname, u.lastname,
               COUNT(pr.user_id) AS reactions_count,
               MAX(CASE WHEN pr.user_id = ? THEN pr.reaction_type ELSE NULL END) AS my_reaction
        FROM posts p
        JOIN user u ON p.user_id = u.id
        LEFT JOIN post_reactions pr ON pr.post_id = p.id
        GROUP BY p.id, p.user_id, p.content, p.image_url, p.created_at, u.firstname, u.lastname
        ORDER BY p.created_at DESC
    """;

        try (PreparedStatement pst = conn.prepareStatement(req)) {
            pst.setInt(1, currentUserId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Post post = new Post(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("content")
                    );

                    post.setImageUrl(rs.getString("image_url"));
                    post.setCreatedAt(rs.getTimestamp("created_at"));
                    post.setFirstname(rs.getString("firstname"));
                    post.setLastname(rs.getString("lastname"));

                    post.setReactionsCount(rs.getInt("reactions_count"));
                    post.setMyReaction(rs.getString("my_reaction")); // can be null

                    list.add(post);
                }
            }
        }
        return list;
    }

}
