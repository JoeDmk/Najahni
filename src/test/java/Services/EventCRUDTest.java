package Services;

import Entites.Event;
import Utils.MyBD;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventCRUDTest {

    EventCRUD crud = new EventCRUD();
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
        Event e = new Event();
        e.setTitle("JUnit Event");
        e.setDescription("Test Desc");
        e.setEventDate(new Timestamp(System.currentTimeMillis()));
        e.setCapacity(10);
        e.setUserId(1);

        crud.ajouter(e);

        assertTrue(e.getId() > 0);
    }

    @Test
    void modifier() throws SQLException {
        Event e = new Event();
        e.setTitle("Modify Event");
        e.setDescription("Before");
        e.setEventDate(new Timestamp(System.currentTimeMillis()));
        e.setCapacity(5);
        e.setUserId(1);

        crud.ajouter(e);

        e.setDescription("After");
        crud.modifier(e);

        List<Event> list = crud.afficher();
        assertTrue(
                list.stream()
                        .anyMatch(ev -> ev.getDescription().equals("After"))
        );
    }

    @Test
    void supprimer() throws SQLException {
        Event e = new Event();
        e.setTitle("Delete Event");
        e.setDescription("Test");
        e.setEventDate(new Timestamp(System.currentTimeMillis()));
        e.setCapacity(5);
        e.setUserId(1);

        crud.ajouter(e);
        int id = e.getId();

        crud.supprimer(id);

        List<Event> list = crud.afficher();
        assertFalse(
                list.stream()
                        .anyMatch(ev -> ev.getId() == id)
        );
    }

    @Test
    void afficher() throws SQLException {
        List<Event> list = crud.afficher();
        assertNotNull(list);
    }
}
