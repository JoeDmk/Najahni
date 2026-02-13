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
}
