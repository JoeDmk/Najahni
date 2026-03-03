package services.mentorat;

import interfaces.mentorat.IMentorshipSession;
import models.mentorat.MentorshipSession;
import tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceMentorshipSession implements IMentorshipSession {

    private Connection cnx;

    public ServiceMentorshipSession() {
        cnx = MyConnection.getInstance().getCnx();
    }

    @Override
    public void add(MentorshipSession t) {
        String qry = "INSERT INTO `mentorship_session` (`mentorship_request_id`, `scheduled_at`, `duration_minutes`, `status`, `meeting_link`, `created_at`) VALUES (?, ?, ?, ?, ?, NOW())";
        try {
            PreparedStatement stm = cnx.prepareStatement(qry);
            stm.setInt(1, t.getRequestId());
            stm.setTimestamp(2, t.getScheduledAt());
            stm.setInt(3, t.getDurationMinutes());
            stm.setString(4, t.getStatus().toString());
            stm.setString(5, t.getMeetingLink());

            stm.executeUpdate();
            System.out.println("Mentorship Session Added!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void update(MentorshipSession t) {
        String qry = "UPDATE `mentorship_session` SET `mentorship_request_id`=?, `scheduled_at`=?, `duration_minutes`=?, `status`=?, `meeting_link`=?, `mentor_feedback`=?, `entrepreneur_feedback`=?, `mentor_rating`=?, `entrepreneur_rating`=?, `updated_at`=NOW() WHERE `id`=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(qry);
            stm.setInt(1, t.getRequestId());
            stm.setTimestamp(2, t.getScheduledAt());
            stm.setInt(3, t.getDurationMinutes());
            stm.setString(4, t.getStatus().toString());
            stm.setString(5, t.getMeetingLink());
            stm.setString(6, t.getMentorFeedback());
            stm.setString(7, t.getEntrepreneurFeedback());
            stm.setInt(8, t.getMentorRating());
            stm.setInt(9, t.getEntrepreneurRating());
            stm.setInt(10, t.getId());

            stm.executeUpdate();
            System.out.println("Mentorship Session Updated!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String qry = "DELETE FROM `mentorship_session` WHERE `id`=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(qry);
            stm.setInt(1, id);
            stm.executeUpdate();
            System.out.println("Mentorship Session Deleted!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public MentorshipSession getOne(int id) {
        String qry = "SELECT * FROM `mentorship_session` WHERE `id`=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(qry);
            stm.setInt(1, id);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                MentorshipSession s = new MentorshipSession();
                s.setId(rs.getInt("id"));
                s.setRequestId(rs.getInt("mentorship_request_id"));
                s.setScheduledAt(rs.getTimestamp("scheduled_at"));
                s.setDurationMinutes(rs.getInt("duration_minutes"));
                s.setStatus(MentorshipSession.SessionStatus.valueOf(rs.getString("status")));
                s.setMeetingLink(rs.getString("meeting_link"));
                s.setMentorFeedback(rs.getString("mentor_feedback"));
                s.setEntrepreneurFeedback(rs.getString("entrepreneur_feedback"));
                s.setMentorRating(rs.getInt("mentor_rating"));
                s.setEntrepreneurRating(rs.getInt("entrepreneur_rating"));
                s.setCreatedAt(rs.getTimestamp("created_at"));
                s.setUpdatedAt(rs.getTimestamp("updated_at"));
                return s;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    @Override
    public List<MentorshipSession> getAll() {
        List<MentorshipSession> sessions = new ArrayList<>();
        String qry = "SELECT * FROM `mentorship_session`";
        try {
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(qry);
            while (rs.next()) {
                MentorshipSession s = new MentorshipSession();
                s.setId(rs.getInt("id"));
                s.setRequestId(rs.getInt("mentorship_request_id"));
                s.setScheduledAt(rs.getTimestamp("scheduled_at"));
                s.setDurationMinutes(rs.getInt("duration_minutes"));
                String statusStr = rs.getString("status");
                if (statusStr != null) {
                    try {
                        s.setStatus(MentorshipSession.SessionStatus.valueOf(statusStr));
                    } catch (Exception e) {
                    }
                }
                s.setMeetingLink(rs.getString("meeting_link"));
                s.setMentorFeedback(rs.getString("mentor_feedback"));
                s.setEntrepreneurFeedback(rs.getString("entrepreneur_feedback"));
                s.setMentorRating(rs.getInt("mentor_rating"));
                s.setEntrepreneurRating(rs.getInt("entrepreneur_rating"));
                s.setCreatedAt(rs.getTimestamp("created_at"));
                s.setUpdatedAt(rs.getTimestamp("updated_at"));
                sessions.add(s);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return sessions;
    }

    @Override
    public List<MentorshipSession> getByRequestId(int requestId) {
        List<MentorshipSession> sessions = new ArrayList<>();
        String qry = "SELECT * FROM `mentorship_session` WHERE mentorship_request_id = ?";
        try {
            PreparedStatement stm = cnx.prepareStatement(qry);
            stm.setInt(1, requestId);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                MentorshipSession s = new MentorshipSession();
                s.setId(rs.getInt("id"));
                s.setRequestId(rs.getInt("mentorship_request_id"));
                s.setScheduledAt(rs.getTimestamp("scheduled_at"));
                s.setDurationMinutes(rs.getInt("duration_minutes"));
                try {
                    s.setStatus(MentorshipSession.SessionStatus.valueOf(rs.getString("status")));
                } catch (Exception e) {
                }
                s.setMeetingLink(rs.getString("meeting_link"));
                s.setMentorFeedback(rs.getString("mentor_feedback"));
                s.setEntrepreneurFeedback(rs.getString("entrepreneur_feedback"));
                s.setMentorRating(rs.getInt("mentor_rating"));
                s.setEntrepreneurRating(rs.getInt("entrepreneur_rating"));
                s.setCreatedAt(rs.getTimestamp("created_at"));
                s.setUpdatedAt(rs.getTimestamp("updated_at"));
                sessions.add(s);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return sessions;
    }
}

