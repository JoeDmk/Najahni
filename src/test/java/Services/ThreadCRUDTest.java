package Services;

import Entites.Group;
import Entites.Thread;
import Utils.MyBD;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ThreadCRUDTest {

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

    private int createGroup(int adminId) throws SQLException {
        GroupCRUD gcrud = new GroupCRUD();
        Group g = new Group("G_" + UUID.randomUUID(), "desc", adminId, false);
        gcrud.ajouter(g);
        return g.getId();
    }

    @Test
    void ajouter() throws Exception {
        int userId = createUser();
        int groupId = createGroup(userId);

        ThreadCRUD crud = new ThreadCRUD();
        crud.ajouter(new Thread(groupId, userId, "t1", "c1"));

        assertTrue(crud.getThreadsByGroup(groupId).stream().anyMatch(t -> "t1".equals(t.getTitle())));
    }

    @Test
    void modifier() throws Exception {
        int userId = createUser();
        int groupId = createGroup(userId);

        ThreadCRUD crud = new ThreadCRUD();
        crud.ajouter(new Thread(groupId, userId, "old", "oldc"));

        Thread inserted = crud.getThreadsByGroup(groupId).get(0);
        inserted.setTitle("new");
        inserted.setContent("newc");
        crud.modifier(inserted);

        Thread updated = crud.getThreadsByGroup(groupId).stream().filter(t -> t.getId() == inserted.getId()).findFirst().orElseThrow();
        assertEquals("new", updated.getTitle());
    }

    @Test
    void supprimer() throws Exception {
        int userId = createUser();
        int groupId = createGroup(userId);

        ThreadCRUD crud = new ThreadCRUD();
        crud.ajouter(new Thread(groupId, userId, "toDel", "x"));

        int threadId = crud.getThreadsByGroup(groupId).get(0).getId();
        crud.supprimer(threadId);

        assertFalse(crud.getThreadsByGroup(groupId).stream().anyMatch(t -> t.getId() == threadId));
    }

    @Test
    void afficher() throws Exception {
        ThreadCRUD crud = new ThreadCRUD();
        List<Thread> list = crud.afficher();
        assertNotNull(list);
    }

    @Test
    void getThreadsByGroup() throws Exception {
        int userId = createUser();
        int groupId = createGroup(userId);

        ThreadCRUD crud = new ThreadCRUD();
        crud.ajouter(new Thread(groupId, userId, "A", "a"));
        crud.ajouter(new Thread(groupId, userId, "B", "b"));

        List<Thread> list = crud.getThreadsByGroup(groupId);
        assertEquals(2, list.size());
        assertNotNull(list.get(0).getFirstname()); // join with user.firstname
    }
}
