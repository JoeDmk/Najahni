package services.community;

import tools.MyConnection;

import java.sql.*;
import java.util.EnumMap;
import java.util.Map;

public class PostReactionCRUD {

    public enum ReactionType { LIKE, LOVE, HAHA, WOW, SAD, ANGRY }

    private final Connection conn;

    public PostReactionCRUD() {
        conn = MyConnection.getInstance().getConnection();
    }

    /** Returns the user's reaction for this post, or null if none */
    public ReactionType getUserReaction(int postId, int userId) throws SQLException {
        String sql = "SELECT reaction_type FROM post_reactions WHERE post_id=? AND user_id=? LIMIT 1";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, postId);
            pst.setInt(2, userId);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return null;
                return ReactionType.valueOf(rs.getString("reaction_type"));
            }
        }
    }

    /** Add or change reaction (UPSERT) */
    public void setReaction(int postId, int userId, ReactionType type) throws SQLException {
        String sql = """
            INSERT INTO post_reactions (post_id, user_id, reaction_type)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE reaction_type = VALUES(reaction_type)
        """;
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, postId);
            pst.setInt(2, userId);
            pst.setString(3, type.name());
            pst.executeUpdate();
        }
    }

    /** Remove reaction */
    public void removeReaction(int postId, int userId) throws SQLException {
        String sql = "DELETE FROM post_reactions WHERE post_id=? AND user_id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, postId);
            pst.setInt(2, userId);
            pst.executeUpdate();
        }
    }
    /** Counts per reaction type */
    public Map<ReactionType, Integer> countReactions(int postId) throws SQLException {
        Map<ReactionType, Integer> map = new EnumMap<>(ReactionType.class);
        for (ReactionType t : ReactionType.values()) map.put(t, 0);

        String sql = """
            SELECT reaction_type, COUNT(*) cnt
            FROM post_reactions
            WHERE post_id=?
            GROUP BY reaction_type
        """;
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, postId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    ReactionType t = ReactionType.valueOf(rs.getString("reaction_type"));
                    map.put(t, rs.getInt("cnt"));
                }
            }
        }
        return map;
    }
}
