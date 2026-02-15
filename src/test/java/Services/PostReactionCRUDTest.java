package Services;

import Utils.MyBD;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PostReactionCRUDTest {

    private Connection conn;
    private boolean oldAutoCommit;

    @BeforeEach
    void beginTx() throws Exception {
        conn = MyBD.getInstance().getConn();
        oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
    }

    @AfterEach
    void rollbackTx() throws Exception {
        conn.rollback();
        conn.setAutoCommit(oldAutoCommit);
    }

    private int createUser() throws SQLException {
        String email = "test_" + UUID.randomUUID() + "@mail.com";
        String sql = "INSERT INTO user(firstname, lastname, email, password) VALUES(?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "Test");
            ps.setString(2, "User");
            ps.setString(3, email);
            ps.setString(4, "1234");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getInt(1); }
        }
    }

    private int createPost(int userId) throws SQLException {
        String sql = "INSERT INTO posts(user_id, content, image_url) VALUES(?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, "post_" + UUID.randomUUID());

            // If your DB does NOT allow NULL for image_url, change to ""
            ps.setString(3, null);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getInt(1); }
        }
    }

    @Test
    void getUserReaction() throws Exception {
        int userId = createUser();
        int postId = createPost(userId);

        PostReactionCRUD crud = new PostReactionCRUD();
        assertNull(crud.getUserReaction(postId, userId));
    }

    @Test
    void setReaction_insertsOrUpdates() throws Exception {
        int userId = createUser();
        int postId = createPost(userId);

        PostReactionCRUD crud = new PostReactionCRUD();

        // Insert
        crud.setReaction(postId, userId, PostReactionCRUD.ReactionType.LIKE);
        assertEquals(PostReactionCRUD.ReactionType.LIKE, crud.getUserReaction(postId, userId));

        // Update (same row, different type)
        crud.setReaction(postId, userId, PostReactionCRUD.ReactionType.LOVE);
        assertEquals(PostReactionCRUD.ReactionType.LOVE, crud.getUserReaction(postId, userId));
    }

    @Test
    void removeReaction() throws Exception {
        int userId = createUser();
        int postId = createPost(userId);

        PostReactionCRUD crud = new PostReactionCRUD();
        crud.setReaction(postId, userId, PostReactionCRUD.ReactionType.WOW);

        assertNotNull(crud.getUserReaction(postId, userId));

        crud.removeReaction(postId, userId);
        assertNull(crud.getUserReaction(postId, userId));
    }

    @Test
    void countReactions() throws Exception {
        int u1 = createUser();
        int u2 = createUser();
        int postId = createPost(u1);

        PostReactionCRUD crud = new PostReactionCRUD();
        crud.setReaction(postId, u1, PostReactionCRUD.ReactionType.LIKE);
        crud.setReaction(postId, u2, PostReactionCRUD.ReactionType.LIKE);

        Map<PostReactionCRUD.ReactionType, Integer> map = crud.countReactions(postId);

        assertEquals(2, map.get(PostReactionCRUD.ReactionType.LIKE));
        assertEquals(0, map.get(PostReactionCRUD.ReactionType.LOVE));
        assertEquals(0, map.get(PostReactionCRUD.ReactionType.HAHA));
        assertEquals(0, map.get(PostReactionCRUD.ReactionType.WOW));
        assertEquals(0, map.get(PostReactionCRUD.ReactionType.SAD));
        assertEquals(0, map.get(PostReactionCRUD.ReactionType.ANGRY));
    }
}
