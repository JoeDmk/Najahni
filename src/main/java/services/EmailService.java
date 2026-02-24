package services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {
    // TODO: Replace with your actual email credentials
    private final String senderEmail = "ilyessguesmi7@gmail.com";
    private final String senderPassword = "mmnl bmvu kslp iqbf";

    private Properties getMailProperties() {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        return properties;
    }

    private Session getSession() {
        return Session.getInstance(getMailProperties(), new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });
    }

    public void sendPasswordChangeEmail(String recipientEmail) {
        try {
            Message message = new MimeMessage(getSession());
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Confirmation de changement de mot de passe - Najahni");
            message.setText("Bonjour,\n\nVotre mot de passe a été changé avec succès sur Najahni.\n\nCordialement,\nL'équipe Najahni");
            Transport.send(message);
            System.out.println("Password change email sent!");
        } catch (MessagingException e) {
            System.err.println("Error sending password change email: " + e.getMessage());
        }
    }

    public void sendVerificationEmail(String recipientEmail, String verificationCode) {
        try {
            Message message = new MimeMessage(getSession());
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Code de vérification - Najahni");
            message.setText("Bonjour,\n\nVotre code de vérification est : " + verificationCode +
                    "\n\nCe code expire dans 5 minutes.\n\nCordialement,\nL'équipe Najahni");
            Transport.send(message);
            System.out.println("Verification email sent!");
        } catch (MessagingException e) {
            System.err.println("Error sending verification email: " + e.getMessage());
        }
    }

    public void sendWelcomeEmail(String recipientEmail, String firstname) {
        try {
            Message message = new MimeMessage(getSession());
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Bienvenue sur Najahni !");
            message.setText("Bonjour " + firstname + ",\n\n" +
                    "Bienvenue sur Najahni ! Votre compte a été créé avec succès.\n\n" +
                    "Connectez-vous avec d'autres entrepreneurs, mentors et investisseurs.\n\n" +
                    "Cordialement,\nL'équipe Najahni");
            Transport.send(message);
            System.out.println("Welcome email sent!");
        } catch (MessagingException e) {
            System.err.println("Error sending welcome email: " + e.getMessage());
        }
    }

    /**
     * Send broadcast email to a single recipient.
     */
    public void sendBroadcastEmail(String recipientEmail, String subject, String body) {
        try {
            Message message = new MimeMessage(getSession());
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject + " - Najahni");
            message.setContent(
                    "<html><body style='font-family: Segoe UI, Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<div style='background: linear-gradient(135deg, #6C63FF, #8B85FF); padding: 20px; border-radius: 12px 12px 0 0;'>" +
                    "<h1 style='color: white; margin: 0; font-size: 24px;'>Najahni</h1>" +
                    "</div>" +
                    "<div style='background: #f8f9fa; padding: 30px; border-radius: 0 0 12px 12px;'>" +
                    "<h2 style='color: #2D3436;'>" + subject + "</h2>" +
                    "<p style='color: #636E72; line-height: 1.6;'>" + body.replace("\n", "<br>") + "</p>" +
                    "<hr style='border: none; border-top: 1px solid #E1E8ED; margin: 20px 0;'>" +
                    "<p style='color: #B0B0B0; font-size: 12px;'>Cet email a été envoyé par l'équipe Najahni.</p>" +
                    "</div></div></body></html>",
                    "text/html; charset=UTF-8"
            );
            Transport.send(message);
            System.out.println("Broadcast email sent to: " + recipientEmail);
        } catch (MessagingException e) {
            System.err.println("Error sending broadcast email to " + recipientEmail + ": " + e.getMessage());
        }
    }

    /**
     * Send broadcast to multiple recipients (runs in background thread).
     */
    public void sendBroadcastToAll(java.util.List<String> emails, String subject, String body) {
        new Thread(() -> {
            int sent = 0;
            for (String email : emails) {
                sendBroadcastEmail(email, subject, body);
                sent++;
                try { Thread.sleep(200); } catch (InterruptedException ignored) {} // Rate limiting
            }
            System.out.println("Broadcast complete: " + sent + "/" + emails.size() + " emails sent.");
        }, "email-broadcast").start();
    }
}
