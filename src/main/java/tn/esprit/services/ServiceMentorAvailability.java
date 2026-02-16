package tn.esprit.services;

import tn.esprit.interfaces.IMentorAvailability;
import tn.esprit.models.MentorAvailability;
import tn.esprit.utils.DataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceMentorAvailability implements IMentorAvailability {

    private Connection cnx;

    public ServiceMentorAvailability() {
        cnx = DataBase.getInstance().getCnx();
    }

    @Override
    public void add(MentorAvailability t) {
        String qry = "INSERT INTO `mentor_availability` (`mentor_id`, `date`, `start_time`, `end_time`, `created_at`) VALUES (?, ?, ?, ?, NOW())";
        try {
            PreparedStatement stm = cnx.prepareStatement(qry);
            stm.setInt(1, t.getMentorId());
            stm.setDate(2, t.getDate());
            stm.setTime(3, t.getStartTime());
            stm.setTime(4, t.getEndTime());
            stm.executeUpdate();
            System.out.println("Availability Added!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void update(MentorAvailability t) {
        String qry = "UPDATE `mentor_availability` SET `mentor_id`=?, `date`=?, `start_time`=?, `end_time`=? WHERE `id`=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(qry);
            stm.setInt(1, t.getMentorId());
            stm.setDate(2, t.getDate());
            stm.setTime(3, t.getStartTime());
            stm.setTime(4, t.getEndTime());
            stm.setInt(5, t.getId());
            stm.executeUpdate();
            System.out.println("Availability Updated!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String qry = "DELETE FROM `mentor_availability` WHERE `id`=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(qry);
            stm.setInt(1, id);
            stm.executeUpdate();
            System.out.println("Availability Deleted!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public MentorAvailability getOne(int id) {
        String qry = "SELECT * FROM `mentor_availability` WHERE `id`=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(qry);
            stm.setInt(1, id);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                MentorAvailability av = new MentorAvailability();
                av.setId(rs.getInt("id"));
                av.setMentorId(rs.getInt("mentor_id"));
                av.setDate(rs.getDate("date"));
                av.setStartTime(rs.getTime("start_time"));
                av.setEndTime(rs.getTime("end_time"));
                av.setCreatedAt(rs.getTimestamp("created_at"));
                return av;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    @Override
    public List<MentorAvailability> getAll() {
        List<MentorAvailability> list = new ArrayList<>();
        String qry = "SELECT ma.*, u.firstname, u.lastname FROM `mentor_availability` ma JOIN `user` u ON ma.mentor_id = u.id WHERE ma.date > '0000-00-00'";
        try {
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(qry);
            while (rs.next()) {
                MentorAvailability av = new MentorAvailability();
                av.setId(rs.getInt("id"));
                av.setMentorId(rs.getInt("mentor_id"));
                av.setDate(rs.getDate("date"));
                av.setStartTime(rs.getTime("start_time"));
                av.setEndTime(rs.getTime("end_time"));
                av.setCreatedAt(rs.getTimestamp("created_at"));

                String mentorName = rs.getString("firstname") + " " + rs.getString("lastname");
                av.setMentorName(mentorName);

                list.add(av);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    @Override
    public List<MentorAvailability> getByMentorId(int mentorId) {
        List<MentorAvailability> list = new ArrayList<>();
        String qry = "SELECT * FROM `mentor_availability` WHERE `mentor_id`=? AND `date` > '0000-00-00'";
        try {
            PreparedStatement stm = cnx.prepareStatement(qry);
            stm.setInt(1, mentorId);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                MentorAvailability av = new MentorAvailability();
                av.setId(rs.getInt("id"));
                av.setMentorId(rs.getInt("mentor_id"));
                av.setDate(rs.getDate("date"));
                av.setStartTime(rs.getTime("start_time"));
                av.setEndTime(rs.getTime("end_time"));
                av.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(av);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public List<MentorAvailability> getMentorsByDateAndTime(java.sql.Date date, String time) {
        List<MentorAvailability> list = new ArrayList<>();
        // Assuming time is in "HH:MM" format or similar, we might need to cast or
        // parse.
        // Query checks if the date matches and the requested time is BETWEEN start_time
        // and end_time.
        // Note: `time` argument is string, need to ensure DB compatibility or parsing.
        // Let's assume standard SQL time comparison works if the string is HH:MM:SS.
        String qry = "SELECT ma.*, u.firstname, u.lastname FROM `mentor_availability` ma JOIN `user` u ON ma.mentor_id = u.id WHERE ma.date = ? AND ? BETWEEN ma.start_time AND ma.end_time";
        try {
            PreparedStatement stm = cnx.prepareStatement(qry);
            stm.setDate(1, date);
            stm.setString(2, time);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                MentorAvailability av = new MentorAvailability();
                av.setId(rs.getInt("id"));
                av.setMentorId(rs.getInt("mentor_id"));
                av.setDate(rs.getDate("date"));
                av.setStartTime(rs.getTime("start_time"));
                av.setEndTime(rs.getTime("end_time"));
                av.setCreatedAt(rs.getTimestamp("created_at"));

                String mentorName = rs.getString("firstname") + " " + rs.getString("lastname");
                av.setMentorName(mentorName);

                list.add(av);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }
}
