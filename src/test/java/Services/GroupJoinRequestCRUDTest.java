package Services;

import Entites.Group;
import Entites.GroupJoinRequest;
import Utils.MyBD;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GroupJoinRequestCRUDTest {

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
        Group g = new Group("G_" + UUID.randomUUID(), "desc", adminId, true);
        gcrud.ajouter(g);
        return g.getId();
    }

    @Test
    void ajouter() throws Exception {
        int adminId = createUser();
        int userId = createUser();
        int groupId = createGroup(adminId);

        GroupJoinRequestCRUD crud = new GroupJoinRequestCRUD();
        crud.ajouter(new GroupJoinRequest(groupId, userId));

        assertTrue(crud.hasPendingRequest(groupId, userId));
    }

    @Test
    void hasPendingRequest() throws Exception {
        int adminId = createUser();
        int userId = createUser();
        int groupId = createGroup(adminId);

        GroupJoinRequestCRUD crud = new GroupJoinRequestCRUD();
        assertFalse(crud.hasPendingRequest(groupId, userId));

        crud.ajouter(new GroupJoinRequest(groupId, userId));
        assertTrue(crud.hasPendingRequest(groupId, userId));
    }

    @Test
    void getPendingRequests() throws Exception {
        int adminId = createUser();
        int userId = createUser();
        int groupId = createGroup(adminId);

        GroupJoinRequestCRUD crud = new GroupJoinRequestCRUD();
        crud.ajouter(new GroupJoinRequest(groupId, userId));

        List<GroupJoinRequest> pending = crud.getPendingRequests(groupId);
        assertTrue(pending.stream().anyMatch(r -> r.getUserId() == userId));
    }

    @Test
    void approveRequest() throws Exception {
        int adminId = createUser();
        int userId = createUser();
        int groupId = createGroup(adminId);

        GroupJoinRequestCRUD crud = new GroupJoinRequestCRUD();
        crud.ajouter(new GroupJoinRequest(groupId, userId));

        int reqId = crud.getPendingRequests(groupId).get(0).getId();
        crud.approveRequest(reqId);

        // after approve, it is no longer pending
        assertFalse(crud.hasPendingRequest(groupId, userId));
        assertTrue(crud.getPendingRequests(groupId).isEmpty());
    }

    @Test
    void rejectRequest() throws Exception {
        int adminId = createUser();
        int userId = createUser();
        int groupId = createGroup(adminId);

        GroupJoinRequestCRUD crud = new GroupJoinRequestCRUD();
        crud.ajouter(new GroupJoinRequest(groupId, userId));

        int reqId = crud.getPendingRequests(groupId).get(0).getId();
        crud.rejectRequest(reqId);

        assertFalse(crud.hasPendingRequest(groupId, userId));
        assertTrue(crud.getPendingRequests(groupId).isEmpty());
    }
}
