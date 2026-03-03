package services.mentorat;

import models.User;
import tools.MyConnection;
import util.Type;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Mentorat-specific user queries (mentor availability joins).
 * Basic role queries delegate to the unified user table.
 */
public class ServiceUser {
    private Connection cnx;

    public ServiceUser() {
        cnx = MyConnection.getInstance().getCnx();
    }

    public List<User> getAll() {
        List<User> list = new ArrayList<>();
        String qry = "SELECT * FROM `user`";
        try {
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(qry);
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public List<User> getByRole(String role) {
        List<User> list = new ArrayList<>();
        String qry = "SELECT * FROM `user` WHERE role = ?";
        try {
            PreparedStatement stm = cnx.prepareStatement(qry);
            stm.setString(1, role);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public List<User> getAvailableMentors() {
        List<User> list = new ArrayList<>();
        String qry = "SELECT DISTINCT u.* FROM `user` u JOIN `mentor_availability` ma ON u.id = ma.mentor_id";
        try {
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(qry);
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public List<User> getAvailableMentors(java.sql.Date date, String time) {
        List<User> list = new ArrayList<>();
        String qry = "SELECT DISTINCT u.* FROM `user` u JOIN `mentor_availability` ma ON u.id = ma.mentor_id WHERE ma.date = ? AND ? BETWEEN ma.start_time AND ma.end_time";
        try {
            PreparedStatement stm = cnx.prepareStatement(qry);
            stm.setDate(1, date);
            stm.setString(2, time);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setFirstname(rs.getString("firstname"));
        u.setLastname(rs.getString("lastname"));
        u.setEmail(rs.getString("email"));
        u.setRole(Type.valueOf(rs.getString("role")));
        return u;
    }
}
