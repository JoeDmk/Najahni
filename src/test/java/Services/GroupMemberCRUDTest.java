package Services;

import Entites.Group;
import Entites.GroupMember;
import Utils.MyBD;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GroupMemberCRUDTest {

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
        int adminId = createUser();
        int userId = createUser();
        int groupId = createGroup(adminId);

        GroupMemberCRUD crud = new GroupMemberCRUD();
        crud.ajouter(new GroupMember(groupId, userId));

        assertTrue(crud.isUserMember(groupId, userId));
    }

    @Test
    void modifier() throws Exception {
        int adminId = createUser();
        int userId = createUser();
        int group1 = createGroup(adminId);
        int group2 = createGroup(adminId);

        GroupMemberCRUD crud = new GroupMemberCRUD();
        crud.ajouter(new GroupMember(group1, userId));

        GroupMember last = crud.afficher().get(crud.afficher().size() - 1);
        last.setGroupId(group2);

        crud.modifier(last);

        assertTrue(crud.isUserMember(group2, userId));
    }

    @Test
    void supprimer() throws Exception {
        int adminId = createUser();
        int userId = createUser();
        int groupId = createGroup(adminId);

        GroupMemberCRUD crud = new GroupMemberCRUD();
        crud.ajouter(new GroupMember(groupId, userId));

        GroupMember last = crud.afficher().get(crud.afficher().size() - 1);
        crud.supprimer(last.getId());

        assertFalse(crud.isUserMember(groupId, userId));
    }

    @Test
    void afficher() throws Exception {
        GroupMemberCRUD crud = new GroupMemberCRUD();
        assertNotNull(crud.afficher());
    }

    @Test
    void isUserMember() throws Exception {
        int adminId = createUser();
        int userId = createUser();
        int groupId = createGroup(adminId);

        GroupMemberCRUD crud = new GroupMemberCRUD();
        assertFalse(crud.isUserMember(groupId, userId));

        crud.ajouter(new GroupMember(groupId, userId));
        assertTrue(crud.isUserMember(groupId, userId));
    }

    @Test
    void deleteMembership() throws Exception {
        int adminId = createUser();
        int userId = createUser();
        int groupId = createGroup(adminId);

        GroupMemberCRUD crud = new GroupMemberCRUD();
        crud.ajouter(new GroupMember(groupId, userId));
        crud.deleteMembership(groupId, userId);

        assertFalse(crud.isUserMember(groupId, userId));
    }

    @Test
    void getMembersByGroup() throws Exception {
        int adminId = createUser();
        int userId = createUser();
        int groupId = createGroup(adminId);

        GroupMemberCRUD crud = new GroupMemberCRUD();
        crud.ajouter(new GroupMember(groupId, userId));

        List<GroupMember> members = crud.getMembersByGroup(groupId);
        assertTrue(members.stream().anyMatch(m -> m.getUserId() == userId));
        assertNotNull(members.get(0).getFirstname()); // joined with user.firstname
    }
}
