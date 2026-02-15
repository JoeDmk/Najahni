package Services;

import Entites.Comment;
import Utils.MyBD;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommentCRUDTest {

    CommentCRUD crud = new CommentCRUD();
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
        Comment c = new Comment(1, 1, "JUnit Test Comment");
        crud.ajouter(c);

        List<Comment> comments = crud.afficherByThread(1);
        assertTrue(
                comments.stream()
                        .anyMatch(cm -> cm.getContent().equals("JUnit Test Comment"))
        );
    }

    @Test
    void afficherByThread() throws SQLException {
        List<Comment> list = crud.afficherByThread(1);
        assertNotNull(list);
    }

    @Test
    void modifier() throws SQLException {
        Comment c = new Comment(1, 1, "Old Content");
        crud.ajouter(c);

        List<Comment> list = crud.afficherByThread(1);
        Comment last = list.get(0);

        crud.modifier(last.getId(), "Updated Content");

        List<Comment> updated = crud.afficherByThread(1);
        assertTrue(
                updated.stream()
                        .anyMatch(cm -> cm.getContent().equals("Updated Content"))
        );
    }

    @Test
    void supprimer() throws SQLException {
        Comment c = new Comment(1, 1, "Delete Me");
        crud.ajouter(c);

        List<Comment> list = crud.afficherByThread(1);
        Comment last = list.get(0);

        crud.supprimer(last.getId());

        List<Comment> after = crud.afficherByThread(1);
        assertFalse(
                after.stream()
                        .anyMatch(cm -> cm.getId() == last.getId())
        );
    }

    @Test
    void isOwner() throws SQLException {
        Comment c = new Comment(1, 1, "Owner Test");
        crud.ajouter(c);

        List<Comment> list = crud.afficherByThread(1);
        Comment last = list.get(0);

        assertTrue(crud.isOwner(last.getId(), 1));
        assertFalse(crud.isOwner(last.getId(), 999));
    }
}
