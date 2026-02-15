package Services;

import Entites.Event;
import Utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventCRUD implements IntrefaceCRUD<Event> {

    Connection conn;

    public EventCRUD() {
        conn = MyBD.getInstance().getConn();
    }

    @Override
    public void ajouter(Event e) throws SQLException {

        String req = "INSERT INTO events (title, description, event_date, capacity, created_by) VALUES (?, ?, ?, ?, ?)";

        PreparedStatement pst = conn.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);

        pst.setString(1, e.getTitle());
        pst.setString(2, e.getDescription());
        pst.setTimestamp(3, e.getEventDate());
        pst.setInt(4, e.getCapacity());
        pst.setInt(5, e.getUserId());


        pst.executeUpdate();

        ResultSet rs = pst.getGeneratedKeys();
        if (rs.next()) {
            e.setId(rs.getInt(1));
        }
    }


    @Override
    public void modifier(Event e) throws SQLException {
        String req = "UPDATE events SET title=?, description=?, event_date=? WHERE id=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, e.getTitle());
        pst.setString(2, e.getDescription());
        pst.setTimestamp(3, e.getEventDate());
        pst.setInt(4, e.getId());
        pst.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM events WHERE id=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();
    }

    @Override
    public List<Event> afficher() throws SQLException {
        List<Event> list = new ArrayList<>();
        String req = "SELECT * FROM events";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            list.add(new Event(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getTimestamp("event_date"),
                    rs.getTimestamp("created_at"),
                    rs.getInt("capacity"),
                    rs.getInt("created_by")
            ));

        }
        return list;
    }
}
