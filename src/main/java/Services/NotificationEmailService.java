package Services;

import Utils.MyBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class NotificationEmailService {

    private final Connection conn = MyBD.getInstance().getConn();
    private final EmailService emailService = new EmailService();

    // ✅ 1) Comment on thread => email thread owner
    public void sendThreadCommentMail(int threadId, int commenterId, String commentText) {
        String sql = """
            SELECT 
              t.user_id AS owner_id,
              u_owner.email AS owner_email,
              u_owner.firstname AS owner_firstname,
              u_actor.firstname AS actor_firstname,
              u_actor.lastname AS actor_lastname,
              t.title AS thread_title
            FROM threads t
            JOIN user u_owner ON u_owner.id = t.user_id
            JOIN user u_actor ON u_actor.id = ?
            WHERE t.id = ?
        """;

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, commenterId);
            pst.setInt(2, threadId);

            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return;

                int ownerId = rs.getInt("owner_id");
                if (ownerId == commenterId) return; // don't email yourself

                String to = rs.getString("owner_email");
                String ownerName = rs.getString("owner_firstname");
                String actorName = rs.getString("actor_firstname") + " " + rs.getString("actor_lastname");
                String threadTitle = rs.getString("thread_title");

                String subject = "New comment on your thread";
                String body = "Hi " + ownerName + ",\n\n"
                        + actorName + " commented on your thread: \"" + threadTitle + "\"\n\n"
                        + "Comment:\n" + commentText + "\n\n"
                        + " Community App";

                emailService.send(to, subject, body);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ 2) Join event => email event creator
    public void sendEventJoinMailToJoiner(int eventId, int joinerId) {

        String sql = """
        SELECT
          u_joiner.email AS joiner_email,
          u_joiner.firstname AS joiner_firstname,

          e.title AS event_title,
          e.event_date AS event_date,

          u_creator.firstname AS creator_firstname,
          u_creator.lastname  AS creator_lastname
        FROM events e
        JOIN user u_joiner ON u_joiner.id = ?
        JOIN user u_creator ON u_creator.id = e.created_by
        WHERE e.id = ?
    """;

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, joinerId);
            pst.setInt(2, eventId);

            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return;

                String to = rs.getString("joiner_email");
                String joinerName = rs.getString("joiner_firstname");
                String eventTitle = rs.getString("event_title");
                Timestamp dt = rs.getTimestamp("event_date");
                String eventDateIso = (dt == null) ? "" : dt.toLocalDateTime().toString();

                // 1) create signature
                String sig = TicketSigner.signature(eventId, joinerId, eventDateIso, eventTitle, joinerName);


                // 2) payload inside QR
                String safeEventTitle = eventTitle.replace("|", " ").replace("\n", " ").trim();
                String safeJoinerName = joinerName.replace("|", " ").replace("\n", " ").trim();

                String payload =
                        "CommunityTicket" +
                                "|event=" + safeEventTitle +
                                "|user=" + safeJoinerName +
                                "|e=" + eventId +
                                "|u=" + joinerId +
                                "|sig=" + sig;

                // 3) generate QR png file (temp)
                String qrPath = System.getProperty("java.io.tmpdir")
                        + "ticket_" + eventId + "_" + joinerId + ".png";
                java.io.File qrFile = QrUtil.generateQrPng(payload, qrPath);

                String creatorName =
                        rs.getString("creator_firstname") + " " + rs.getString("creator_lastname");

                String subject = "You joined an event (Your QR Ticket)";
                String body = "Hi " + joinerName + ",\n\n"
                        + "You successfully joined the event: \"" + eventTitle + "\"\n"
                        + "Created by: " + creatorName + "\n"
                        + "Date: " + eventDateIso + "\n\n"
                        + "✅ Your QR ticket is attached.\n"
                        + "Manual code (if needed): " + sig.substring(0, 10) + "\n\n"
                        + "Community App";


                emailService.sendWithAttachment(to, subject, body, qrFile);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
