package Services;

import Entites.EventParticipant;
import Utils.MyBD;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventParticipantCRUDTest {

    EventParticipantCRUD crud = new EventParticipantCRUD();
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
    @Test
    void ajouter() throws SQLException {
        EventParticipant ep = new EventParticipant(1, 1);
        crud.ajouter(ep);

        assertTrue(crud.isUserParticipant(1,1));
    }

    @Test
    void isUserParticipant() throws SQLException {
        crud.ajouter(new EventParticipant(1,1));
        assertTrue(crud.isUserParticipant(1,1));
    }

    @Test
    void countParticipants() throws SQLException {
        int count = crud.countParticipants(1);
        assertTrue(count >= 0);
    }

    @Test
    void leaveEvent() throws SQLException {
        crud.ajouter(new EventParticipant(1,1));
        crud.leaveEvent(1,1);
        assertFalse(crud.isUserParticipant(1,1));
    }

    @Test
    void getParticipantsByEvent() throws SQLException {
        List<EventParticipant> list = crud.getParticipantsByEvent(1);
        assertNotNull(list);
    }
}
