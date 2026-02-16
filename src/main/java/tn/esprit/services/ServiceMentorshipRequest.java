package tn.esprit.services;

import tn.esprit.interfaces.IMentorshipRequest;
import tn.esprit.models.MentorshipRequest;
import tn.esprit.utils.DataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceMentorshipRequest implements IMentorshipRequest {

    private Connection cnx;

    public ServiceMentorshipRequest() {
        cnx = DataBase.getInstance().getCnx();
    }

    @Override
    public void add(MentorshipRequest t) {
        String qry = "INSERT INTO `mentorship_request` (`entrepreneur_id`, `mentor_id`, `project_id`, `date`, `time`, `motivation`, `goals`, `match_score`, `auto_approved`, `status`, `created_at`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try {
            PreparedStatement stm = cnx.prepareStatement(qry, Statement.RETURN_GENERATED_KEYS);
            stm.setInt(1, t.getEntrepreneurId());
            stm.setInt(2, t.getMentorId());
            stm.setInt(3, t.getProjectId());
            stm.setDate(4, t.getDate());
            stm.setString(5, t.getTime());
            stm.setString(6, t.getMotivation());
            stm.setString(7, t.getGoals());
            stm.setFloat(8, t.getMatchScore());
            stm.setBoolean(9, t.isAutoApproved());
            stm.setString(10, t.getStatus().toString());
            stm.executeUpdate();

            ResultSet generatedKeys = stm.getGeneratedKeys();
            if (generatedKeys.next()) {
                int requestId = generatedKeys.getInt(1);
                t.setId(requestId);
                System.out.println("Mentorship Request Added with ID: " + requestId);

                if (t.isAutoApproved() || t.getStatus() == MentorshipRequest.RequestStatus.auto_accepted) {
                    createSessionForRequest(t);
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void createSessionForRequest(MentorshipRequest req) {
        try {
            ServiceMentorshipSession sms = new ServiceMentorshipSession();
            tn.esprit.models.MentorshipSession session = new tn.esprit.models.MentorshipSession();
            session.setRequestId(req.getId());

            // Combine date and time to Timestamp for scheduledAt
            // Assuming time is HH:MM or HH:MM:SS
            String timeStr = req.getTime();
            if (timeStr.length() == 5)
                timeStr += ":00"; // Ensure SS
            String dtStr = req.getDate().toString() + " " + timeStr;
            session.setScheduledAt(Timestamp.valueOf(dtStr));

            session.setDurationMinutes(60); // Default duration?
            session.setStatus(tn.esprit.models.MentorshipSession.SessionStatus.scheduled);

            sms.add(session);
            System.out.println("Auto-created session for request " + req.getId());
        } catch (Exception e) {
            System.out.println("Error creating session: " + e.getMessage());
        }
    }

    @Override
    public void update(MentorshipRequest t) {
        String qry = "UPDATE `mentorship_request` SET `entrepreneur_id`=?, `mentor_id`=?, `project_id`=?, `date`=?, `time`=?, `motivation`=?, `goals`=?, `match_score`=?, `auto_approved`=?, `status`=?, `updated_at`=NOW() WHERE `id`=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(qry);
            stm.setInt(1, t.getEntrepreneurId());
            stm.setInt(2, t.getMentorId());
            stm.setInt(3, t.getProjectId());
            stm.setDate(4, t.getDate());
            stm.setString(5, t.getTime());
            stm.setString(6, t.getMotivation());
            stm.setString(7, t.getGoals());
            stm.setFloat(8, t.getMatchScore());
            stm.setBoolean(9, t.isAutoApproved());
            stm.setString(10, t.getStatus().toString());
            stm.setInt(11, t.getId());
            stm.executeUpdate();
            System.out.println("Mentorship Request Updated!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String qry = "DELETE FROM `mentorship_request` WHERE `id`=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(qry);
            stm.setInt(1, id);
            stm.executeUpdate();
            System.out.println("Mentorship Request Deleted!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public MentorshipRequest getOne(int id) {
        String qry = "SELECT * FROM `mentorship_request` WHERE `id`=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(qry);
            stm.setInt(1, id);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                MentorshipRequest req = new MentorshipRequest();
                req.setId(rs.getInt("id"));
                req.setEntrepreneurId(rs.getInt("entrepreneur_id"));
                req.setMentorId(rs.getInt("mentor_id"));
                req.setProjectId(rs.getInt("project_id"));
                req.setDate(rs.getDate("date"));
                req.setTime(rs.getString("time"));
                req.setMotivation(rs.getString("motivation"));
                req.setGoals(rs.getString("goals"));
                req.setMatchScore(rs.getFloat("match_score"));
                req.setAutoApproved(rs.getBoolean("auto_approved"));
                req.setStatus(MentorshipRequest.RequestStatus.valueOf(rs.getString("status")));
                req.setCreatedAt(rs.getTimestamp("created_at"));
                req.setUpdatedAt(rs.getTimestamp("updated_at"));
                return req;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    // ... (getAll and getApprovedRequests stay the same, they use mapRequest)

    private MentorshipRequest mapRequest(ResultSet rs) throws SQLException {
        MentorshipRequest req = new MentorshipRequest();
        req.setId(rs.getInt("id"));
        req.setEntrepreneurId(rs.getInt("entrepreneur_id"));
        req.setMentorId(rs.getInt("mentor_id"));
        req.setProjectId(rs.getInt("project_id"));
        req.setDate(rs.getDate("date"));
        req.setTime(rs.getString("time"));
        req.setMotivation(rs.getString("motivation"));
        req.setGoals(rs.getString("goals"));
        req.setMatchScore(rs.getFloat("match_score"));
        req.setAutoApproved(rs.getBoolean("auto_approved"));
        try {
            req.setStatus(MentorshipRequest.RequestStatus.valueOf(rs.getString("status")));
        } catch (Exception e) {
        }
        req.setCreatedAt(rs.getTimestamp("created_at"));
        req.setUpdatedAt(rs.getTimestamp("updated_at"));

        String entName = (rs.getString("ent_first") != null ? rs.getString("ent_first") : "") + " "
                + (rs.getString("ent_last") != null ? rs.getString("ent_last") : "");
        req.setEntrepreneurName(entName.trim());
        String menName = (rs.getString("men_first") != null ? rs.getString("men_first") : "") + " "
                + (rs.getString("men_last") != null ? rs.getString("men_last") : "");
        req.setMentorName(menName.trim());
        req.setProjectName(rs.getString("proj_title"));
        return req;
    }

    @Override
    public List<MentorshipRequest> getAll() {
        List<MentorshipRequest> requests = new ArrayList<>();
        // Query to join with users and project to get names
        String qry = "SELECT mr.*, " +
                "u1.firstname as ent_first, u1.lastname as ent_last, " +
                "u2.firstname as men_first, u2.lastname as men_last, " +
                "p.titre as proj_title " +
                "FROM `mentorship_request` mr " +
                "LEFT JOIN `user` u1 ON mr.entrepreneur_id = u1.id " +
                "LEFT JOIN `user` u2 ON mr.mentor_id = u2.id " +
                "LEFT JOIN `projet` p ON mr.project_id = p.id";

        try {
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(qry);
            while (rs.next()) {
                MentorshipRequest req = mapRequest(rs);
                requests.add(req);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return requests;
    }

    public List<MentorshipRequest> getApprovedRequests() {
        List<MentorshipRequest> requests = new ArrayList<>();
        // Fetch only requests with 'accepted' or 'auto_accepted' status.
        // Adjust status strings based on your actual ENUM values if needed.
        // Assuming 'auto_accepted' and 'accepted' are valid.
        // But user said "when approved". Let's check status enum.
        // Enum: pending, accepted, rejected, cancelled, completed, auto_accepted ?
        // I will use 'accepted' and 'auto_accepted'
        String qry = "SELECT mr.*, " +
                "u1.firstname as ent_first, u1.lastname as ent_last, " +
                "u2.firstname as men_first, u2.lastname as men_last, " +
                "p.titre as proj_title " +
                "FROM `mentorship_request` mr " +
                "LEFT JOIN `user` u1 ON mr.entrepreneur_id = u1.id " +
                "LEFT JOIN `user` u2 ON mr.mentor_id = u2.id " +
                "LEFT JOIN `projet` p ON mr.project_id = p.id " +
                "WHERE mr.status = 'accepted' OR mr.status = 'auto_accepted'";

        try {
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(qry);
            while (rs.next()) {
                MentorshipRequest req = mapRequest(rs);
                requests.add(req);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return requests;
    }

}
