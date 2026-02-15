package Services;

import Entites.Group;
import Utils.MyBD;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GroupCRUDTest {

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
        int adminId = createUser();
        GroupCRUD crud = new GroupCRUD();

        Group g = new Group("G_" + UUID.randomUUID(), "desc", adminId, false);
        crud.ajouter(g);

        assertTrue(crud.afficher().stream().anyMatch(x -> x.getId() == g.getId()));
    }

    @Test
    void modifier() throws Exception {
        int adminId = createUser();
        GroupCRUD crud = new GroupCRUD();

        Group g = new Group("OldName", "desc", adminId, false);
        crud.ajouter(g);

        g.setName("NewName");
        g.setDescription("new desc");
        crud.modifier(g);

        Group updated = crud.afficher().stream().filter(x -> x.getId() == g.getId()).findFirst().orElseThrow();
        assertEquals("NewName", updated.getName());
    }

    @Test
    void supprimer() throws Exception {
        int adminId = createUser();
        GroupCRUD crud = new GroupCRUD();

        Group g = new Group("ToDelete", "desc", adminId, false);
        crud.ajouter(g);

        crud.supprimer(g.getId());

        assertFalse(crud.afficher().stream().anyMatch(x -> x.getId() == g.getId()));
    }

    @Test
    void afficher() throws Exception {
        GroupCRUD crud = new GroupCRUD();
        List<Group> list = crud.afficher();
        assertNotNull(list);
    }
}
