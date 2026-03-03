package services.community;

import interfaces.community.IntrefaceCRUD;

import models.community.EventParticipant;
import tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventParticipantCRUD implements IntrefaceCRUD<EventParticipant> {

    private final Connection conn;

    public EventParticipantCRUD() {
        conn = MyConnection.getInstance().getConnection();
    }

    // =============================
    // BASIC CRUD
    // =============================

    @Override
    public void ajouter(EventParticipant ep) throws SQLException {

        String sql = "INSERT INTO event_participants (event_id, user_id) VALUES (?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ep.getEventId());
            ps.setInt(2, ep.getUserId());
            ps.executeUpdate();


        }
    }

    @Override
    public void modifier(EventParticipant ep) throws SQLException {

        String sql = "UPDATE event_participants SET event_id=?, user_id=? WHERE id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ep.getEventId());
            ps.setInt(2, ep.getUserId());
            ps.setInt(3, ep.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {

        String sql = "DELETE FROM event_participants WHERE id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<EventParticipant> afficher() throws SQLException {

        List<EventParticipant> list = new ArrayList<>();
        String sql = "SELECT * FROM event_participants";

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new EventParticipant(
                        rs.getInt("id"),
                        rs.getInt("event_id"),
                        rs.getInt("user_id")
                ));
            }
        }

        return list;
    }

    // =============================
    // CUSTOM METHODS
    // =============================

    public boolean isUserParticipant(int eventId, int userId) throws SQLException {

        String sql = "SELECT 1 FROM event_participants WHERE event_id=? AND user_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int countParticipants(int eventId) throws SQLException {

        String sql = "SELECT COUNT(*) FROM event_participants WHERE event_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    public void leaveEvent(int eventId, int userId) throws SQLException {

        String sql = "DELETE FROM event_participants WHERE event_id=? AND user_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void kickUser(int eventId, int userId) throws SQLException {

        String sql = "DELETE FROM event_participants WHERE event_id=? AND user_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    // IMPORTANT: This replaces your firstname method
    public List<EventParticipant> getParticipantsByEvent(int eventId) throws SQLException {

        List<EventParticipant> list = new ArrayList<>();

        String sql = """
        SELECT ep.id, ep.event_id, ep.user_id, u.firstname
        FROM event_participants ep
        JOIN user u ON ep.user_id = u.id
        WHERE ep.event_id = ?
        """;


        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    EventParticipant ep = new EventParticipant(
                            rs.getInt("id"),
                            rs.getInt("event_id"),
                            rs.getInt("user_id")
                    );

                    ep.setFirstname(rs.getString("firstname"));   // 🔥 THIS WAS MISSING

                    list.add(ep);
                }

            }
        }

        return list;
    }
}
