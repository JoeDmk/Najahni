package services.mentorat;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import models.mentorat.MentorshipRequest;
import models.mentorat.MentorshipSession;
import tools.MyConnection;

import java.io.InputStream;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Service de notifications pour le module Mentorat.
 * Envoie des emails (Jakarta Mail / Gmail SMTP) et des SMS (Twilio)
 * lors des evenements importants du mentorat.
 *
 * APIs utilisees:
 *  - Jakarta Mail (SMTP Gmail, port 587, STARTTLS)
 *  - Twilio SMS API (REST)
 */
public class MentoratNotificationService {

    private static final Logger LOG = Logger.getLogger(MentoratNotificationService.class.getName());

    // Email config
    private final String senderEmail;
    private final String senderPassword;

    // Twilio config
    private final String twilioAccountSid;
    private final String twilioAuthToken;
    private final String twilioPhoneNumber;

    private boolean twilioInitialized = false;

    public MentoratNotificationService() {
        Properties secrets = loadSecrets();
        // Email credentials (reuse project-level secrets or fallback to EmailService defaults)
        this.senderEmail = secrets.getProperty("email.sender",
                "ilyessguesmi7@gmail.com");
        this.senderPassword = secrets.getProperty("email.app_password",
                "mmnl bmvu kslp iqbf");

        // Twilio credentials
        this.twilioAccountSid = secrets.getProperty("twilio.account_sid", "");
        this.twilioAuthToken = secrets.getProperty("twilio.auth_token", "");
        this.twilioPhoneNumber = secrets.getProperty("twilio.phone_number", "");

        try {
            if (!twilioAccountSid.isEmpty() && !twilioAuthToken.isEmpty()) {
                Twilio.init(twilioAccountSid, twilioAuthToken);
                twilioInitialized = true;
            }
        } catch (Exception e) {
            LOG.warning("Twilio init failed: " + e.getMessage());
        }
    }

    private Properties loadSecrets() {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/secrets.properties")) {
            if (is != null) props.load(is);
        } catch (Exception e) {
            LOG.warning("Could not load secrets.properties: " + e.getMessage());
        }
        return props;
    }

    // ====== EMAIL NOTIFICATIONS ======

    private Session getMailSession() {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        return Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });
    }

    /**
     * Send a styled HTML email notification.
     */
    private void sendEmail(String recipientEmail, String subject, String htmlBody) {
        new Thread(() -> {
            try {
                MimeMessage message = new MimeMessage(getMailSession());
                message.setFrom(new InternetAddress(senderEmail));
                message.setRecipients(jakarta.mail.Message.RecipientType.TO,
                        InternetAddress.parse(recipientEmail));
                message.setSubject(subject + " - Najahni Mentorat");
                message.setContent(
                        "<html><body style='font-family: Segoe UI, Arial, sans-serif; margin: 0; padding: 0;'>"
                        + "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>"
                        + "<div style='background: linear-gradient(135deg, #e67e22, #f39c12); padding: 24px; border-radius: 12px 12px 0 0;'>"
                        + "<h1 style='color: white; margin: 0; font-size: 22px;'>Najahni - Mentorat</h1>"
                        + "<p style='color: rgba(255,255,255,0.85); margin: 4px 0 0 0; font-size: 13px;'>Plateforme de mise en relation Entrepreneur-Mentor</p>"
                        + "</div>"
                        + "<div style='background: #ffffff; padding: 28px; border: 1px solid #e8e8e8; border-top: none;'>"
                        + htmlBody
                        + "</div>"
                        + "<div style='background: #f8f9fa; padding: 16px 28px; border-radius: 0 0 12px 12px; border: 1px solid #e8e8e8; border-top: none;'>"
                        + "<p style='color: #999; font-size: 11px; margin: 0;'>Cet email a ete envoye automatiquement par Najahni. Ne pas repondre.</p>"
                        + "</div></div></body></html>",
                        "text/html; charset=UTF-8"
                );
                Transport.send(message);
                LOG.info("Mentorat email sent to: " + recipientEmail + " | Subject: " + subject);
            } catch (MessagingException e) {
                LOG.warning("Failed to send mentorat email to " + recipientEmail + ": " + e.getMessage());
            }
        }, "mentorat-email-" + System.currentTimeMillis()).start();
    }

    // ====== SMS NOTIFICATIONS ======

    /**
     * Send an SMS notification via Twilio.
     */
    private void sendSms(String phoneNumber, String messageText) {
        if (!twilioInitialized || phoneNumber == null || phoneNumber.isBlank()) {
            LOG.info("SMS skipped (no Twilio config or phone) for: " + messageText);
            return;
        }
        new Thread(() -> {
            try {
                String formattedPhone = phoneNumber.startsWith("+") ? phoneNumber : "+216" + phoneNumber;
                Message.creator(
                        new PhoneNumber(formattedPhone),
                        new PhoneNumber(twilioPhoneNumber),
                        messageText
                ).create();
                LOG.info("Mentorat SMS sent to: " + formattedPhone);
            } catch (Exception e) {
                LOG.warning("Failed to send mentorat SMS: " + e.getMessage());
            }
        }, "mentorat-sms-" + System.currentTimeMillis()).start();
    }

    // ====== USER LOOKUP ======

    private String[] getUserInfo(int userId) {
        // Returns [email, phone, firstname, lastname]
        try {
            Connection cnx = MyConnection.getInstance().getCnx();
            PreparedStatement stm = cnx.prepareStatement(
                    "SELECT email, phone, firstname, lastname FROM `user` WHERE id = ?");
            stm.setInt(1, userId);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return new String[]{
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("firstname"),
                        rs.getString("lastname")
                };
            }
        } catch (SQLException e) {
            LOG.warning("Failed to get user info for ID " + userId + ": " + e.getMessage());
        }
        return null;
    }

    // ====== MENTORAT EVENT NOTIFICATIONS ======

    /**
     * Notify when a mentorship request status changes (accepted, rejected, etc.)
     */
    public void notifyRequestStatusChange(MentorshipRequest request,
                                          MentorshipRequest.RequestStatus oldStatus,
                                          MentorshipRequest.RequestStatus newStatus) {
        // Notify the entrepreneur
        String[] entInfo = getUserInfo(request.getEntrepreneurId());
        String[] mentorInfo = getUserInfo(request.getMentorId());

        String entName = entInfo != null ? entInfo[2] : "Entrepreneur";
        String mentorName = mentorInfo != null ? (mentorInfo[2] + " " + mentorInfo[3]) : "Mentor";
        String dateStr = request.getDate() != null ? request.getDate().toString() : "N/A";
        String statusLabel = getStatusLabel(newStatus);

        // Email to entrepreneur
        if (entInfo != null && entInfo[0] != null && !entInfo[0].isBlank()) {
            String htmlBody = "<h2 style='color: #2c3e50; margin-top: 0;'>Mise a jour de votre demande</h2>"
                    + "<p style='color: #555; line-height: 1.6;'>Bonjour <strong>" + entName + "</strong>,</p>"
                    + "<p style='color: #555; line-height: 1.6;'>Votre demande de mentorat a change de statut :</p>"
                    + "<div style='background: #f8f9fa; border-left: 4px solid " + getStatusColorHex(newStatus) + "; padding: 14px 18px; margin: 16px 0; border-radius: 4px;'>"
                    + "<p style='margin: 0; color: #333;'><strong>Nouveau statut :</strong> " + statusLabel + "</p>"
                    + "<p style='margin: 6px 0 0 0; color: #666;'><strong>Mentor :</strong> " + mentorName + "</p>"
                    + "<p style='margin: 6px 0 0 0; color: #666;'><strong>Date :</strong> " + dateStr + "</p>"
                    + (request.getTime() != null ? "<p style='margin: 6px 0 0 0; color: #666;'><strong>Heure :</strong> " + request.getTime() + "</p>" : "")
                    + "</div>"
                    + (newStatus == MentorshipRequest.RequestStatus.auto_accepted
                    ? "<p style='color: #27ae60; font-weight: bold;'>Votre session a ete automatiquement planifiee. Consultez vos sessions pour les details.</p>"
                    : "")
                    + "<p style='color: #555;'>Connectez-vous a Najahni pour plus de details.</p>";
            sendEmail(entInfo[0], "Demande de mentorat - " + statusLabel, htmlBody);
        }

        // Email to mentor if accepted/auto-accepted
        if (mentorInfo != null && mentorInfo[0] != null && !mentorInfo[0].isBlank()
                && (newStatus == MentorshipRequest.RequestStatus.auto_accepted)) {
            String htmlBody = "<h2 style='color: #2c3e50; margin-top: 0;'>Nouvelle session de mentorat</h2>"
                    + "<p style='color: #555; line-height: 1.6;'>Bonjour <strong>" + mentorInfo[2] + "</strong>,</p>"
                    + "<p style='color: #555; line-height: 1.6;'>Une demande de mentorat de <strong>" + entName + "</strong> a ete acceptee.</p>"
                    + "<div style='background: #f0fff4; border-left: 4px solid #27ae60; padding: 14px 18px; margin: 16px 0; border-radius: 4px;'>"
                    + "<p style='margin: 0; color: #333;'><strong>Date :</strong> " + dateStr + "</p>"
                    + (request.getTime() != null ? "<p style='margin: 6px 0 0 0; color: #666;'><strong>Heure :</strong> " + request.getTime() + "</p>" : "")
                    + "<p style='margin: 6px 0 0 0; color: #666;'><strong>Motivation :</strong> " + (request.getMotivation() != null ? request.getMotivation() : "-") + "</p>"
                    + "</div>"
                    + "<p style='color: #555;'>Connectez-vous a Najahni pour gerer vos sessions.</p>";
            sendEmail(mentorInfo[0], "Nouvelle session planifiee", htmlBody);
        }

        // SMS to entrepreneur
        if (entInfo != null && entInfo[1] != null && !entInfo[1].isBlank()) {
            String smsText = "Najahni Mentorat: Votre demande a ete " + statusLabel.toLowerCase()
                    + ". Mentor: " + mentorName + ", Date: " + dateStr
                    + ". Connectez-vous pour plus de details.";
            sendSms(entInfo[1], smsText);
        }
    }

    /**
     * Notify when a new mentorship request is created.
     */
    public void notifyNewRequest(MentorshipRequest request) {
        String[] mentorInfo = getUserInfo(request.getMentorId());
        String[] entInfo = getUserInfo(request.getEntrepreneurId());
        String entName = entInfo != null ? (entInfo[2] + " " + entInfo[3]) : "Un entrepreneur";
        String dateStr = request.getDate() != null ? request.getDate().toString() : "N/A";

        // Email to mentor about new request
        if (mentorInfo != null && mentorInfo[0] != null && !mentorInfo[0].isBlank()) {
            String htmlBody = "<h2 style='color: #2c3e50; margin-top: 0;'>Nouvelle demande de mentorat</h2>"
                    + "<p style='color: #555; line-height: 1.6;'>Bonjour <strong>" + mentorInfo[2] + "</strong>,</p>"
                    + "<p style='color: #555; line-height: 1.6;'><strong>" + entName + "</strong> souhaite beneficier de votre mentorat.</p>"
                    + "<div style='background: #fff8f0; border-left: 4px solid #e67e22; padding: 14px 18px; margin: 16px 0; border-radius: 4px;'>"
                    + "<p style='margin: 0; color: #333;'><strong>Date souhaitee :</strong> " + dateStr + "</p>"
                    + (request.getTime() != null ? "<p style='margin: 6px 0 0 0; color: #666;'><strong>Heure :</strong> " + request.getTime() + "</p>" : "")
                    + (request.getMotivation() != null ? "<p style='margin: 6px 0 0 0; color: #666;'><strong>Motivation :</strong> " + request.getMotivation() + "</p>" : "")
                    + (request.getGoals() != null ? "<p style='margin: 6px 0 0 0; color: #666;'><strong>Objectifs :</strong> " + request.getGoals() + "</p>" : "")
                    + "</div>"
                    + "<p style='color: #555;'>Connectez-vous a Najahni pour consulter et repondre a cette demande.</p>";
            sendEmail(mentorInfo[0], "Nouvelle demande de mentorat", htmlBody);
        }

        // SMS to mentor
        if (mentorInfo != null && mentorInfo[1] != null && !mentorInfo[1].isBlank()) {
            String smsText = "Najahni: Nouvelle demande de mentorat de " + entName
                    + " pour le " + dateStr + ". Connectez-vous pour repondre.";
            sendSms(mentorInfo[1], smsText);
        }
    }

    /**
     * Notify when a session is completed (for feedback reminder).
     */
    public void notifySessionCompleted(MentorshipSession session, int entrepreneurId, int mentorId) {
        String[] entInfo = getUserInfo(entrepreneurId);
        String[] mentorInfo = getUserInfo(mentorId);
        String dateStr = session.getScheduledAt() != null
                ? session.getScheduledAt().toLocalDateTime().toLocalDate().toString() : "N/A";

        // Email reminder to both to leave feedback
        if (entInfo != null && entInfo[0] != null && !entInfo[0].isBlank()) {
            String htmlBody = "<h2 style='color: #2c3e50; margin-top: 0;'>Session terminee - Donnez votre avis</h2>"
                    + "<p style='color: #555; line-height: 1.6;'>Bonjour <strong>" + entInfo[2] + "</strong>,</p>"
                    + "<p style='color: #555; line-height: 1.6;'>Votre session de mentorat du <strong>" + dateStr + "</strong> est maintenant terminee.</p>"
                    + "<div style='background: #f0f7ff; border-left: 4px solid #3498db; padding: 14px 18px; margin: 16px 0; border-radius: 4px;'>"
                    + "<p style='margin: 0; color: #333;'>N'oubliez pas de laisser votre evaluation et vos commentaires sur la session.</p>"
                    + "<p style='margin: 6px 0 0 0; color: #666;'>Votre retour aide a ameliorer l'experience de mentorat pour tous.</p>"
                    + "</div>";
            sendEmail(entInfo[0], "Session terminee - Evaluez votre experience", htmlBody);
        }

        if (mentorInfo != null && mentorInfo[0] != null && !mentorInfo[0].isBlank()) {
            String htmlBody = "<h2 style='color: #2c3e50; margin-top: 0;'>Session terminee - Evaluez l'entrepreneur</h2>"
                    + "<p style='color: #555; line-height: 1.6;'>Bonjour <strong>" + mentorInfo[2] + "</strong>,</p>"
                    + "<p style='color: #555; line-height: 1.6;'>Votre session de mentorat du <strong>" + dateStr + "</strong> est maintenant terminee.</p>"
                    + "<div style='background: #f0f7ff; border-left: 4px solid #3498db; padding: 14px 18px; margin: 16px 0; border-radius: 4px;'>"
                    + "<p style='margin: 0; color: #333;'>Merci de fournir votre retour sur l'entrepreneur via la plateforme.</p>"
                    + "</div>";
            sendEmail(mentorInfo[0], "Session terminee - Evaluez l'entrepreneur", htmlBody);
        }
    }

    // ====== HELPERS ======

    private String getStatusLabel(MentorshipRequest.RequestStatus status) {
        if (status == null) return "Inconnu";
        return switch (status) {
            case auto_accepted -> "Acceptee automatiquement";
            case pending_review -> "En attente de revision";
            case rejected -> "Rejetee";
            case cancelled -> "Annulee";
            case completed -> "Completee";
        };
    }

    private String getStatusColorHex(MentorshipRequest.RequestStatus status) {
        if (status == null) return "#7f8c8d";
        return switch (status) {
            case auto_accepted -> "#27ae60";
            case pending_review -> "#e67e22";
            case rejected -> "#e74c3c";
            case cancelled -> "#95a5a6";
            case completed -> "#8e44ad";
        };
    }
}
