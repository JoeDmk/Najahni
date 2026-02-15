package Services;

import Entites.Post;
import Utils.MyBD;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PostCRUDTest {

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

    @Test
    void ajouter() throws Exception {
        int userId = createUser();
        PostCRUD crud = new PostCRUD();

        Post p = new Post(userId, "JUnit post");
        p.setImageUrl(null);
        crud.ajouter(p);

        List<Post> list = crud.afficherFeed(userId);
        assertTrue(list.stream().anyMatch(x -> "JUnit post".equals(x.getContent())));
    }

    @Test
    void modifier() throws Exception {
        int userId = createUser();
        PostCRUD crud = new PostCRUD();

        Post p = new Post(userId, "old");
        p.setImageUrl(null);
        crud.ajouter(p);

        // get the latest inserted id from feed
        Post inserted = crud.afficherFeed(userId).get(0);
        inserted.setContent("new");
        inserted.setImageUrl("img://test");
        crud.modifier(inserted);

        Post updated = crud.afficherFeed(userId).stream().filter(x -> x.getId() == inserted.getId()).findFirst().orElseThrow();
        assertEquals("new", updated.getContent());
        assertEquals("img://test", updated.getImageUrl());
    }

    @Test
    void supprimer() throws Exception {
        int userId = createUser();
        PostCRUD crud = new PostCRUD();

        Post p = new Post(userId, "to delete");
        crud.ajouter(p);

        int postId = crud.afficherFeed(userId).get(0).getId();
        crud.supprimer(postId);

        assertFalse(crud.afficherFeed(userId).stream().anyMatch(x -> x.getId() == postId));
    }

    @Test
    void afficher() throws Exception {
        PostCRUD crud = new PostCRUD();
        List<Post> list = crud.afficher();
        assertNotNull(list);
    }

    @Test
    void afficherFeed() throws Exception {
        int userId = createUser();
        PostCRUD crud = new PostCRUD();

        crud.ajouter(new Post(userId, "feed post"));

        List<Post> feed = crud.afficherFeed(userId);
        assertTrue(feed.stream().anyMatch(x -> "feed post".equals(x.getContent())));
        assertNotNull(feed.get(0).getFirstname()); // join with user.firstname
    }
}
