package services.projets;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.InputStream;
import java.util.Properties;

/**
 * Service d'envoi d'emails — Notifications NAJAHNI
 * Utilise Gmail SMTP (gratuit)
 */
public class EmailNotificationService {

    private static final String EMAIL_EXPEDITEUR;
    private static final String MOT_DE_PASSE_APP;

    static {
        String email = "", pwd = "";
        try (InputStream is = EmailNotificationService.class.getResourceAsStream("/secrets.properties")) {
            Properties p = new Properties();
            p.load(is);
            email = p.getProperty("email.sender", "");
            pwd   = p.getProperty("email.app_password", "");
        } catch (Exception e) { e.printStackTrace(); }
        EMAIL_EXPEDITEUR = email;
        MOT_DE_PASSE_APP = pwd;
    }

    // ===== ENVOI EMAIL PROJET AJOUTÉ =====
    public static void envoyerConfirmationAjout(String emailDestinataire,
                                                String nomUtilisateur,
                                                String titreDuProjet,
                                                String secteur,
                                                String etape) {
        String sujet = "✅ Votre projet a été ajouté — NAJAHNI";
        String corps = construireEmailAjout(nomUtilisateur, titreDuProjet, secteur, etape);
        envoyerEmail(emailDestinataire, sujet, corps);
    }

    // ===== ENVOI EMAIL ÉVALUATION TERMINÉE =====
    public static void envoyerResultatEvaluation(String emailDestinataire,
                                                 String nomUtilisateur,
                                                 String titreDuProjet,
                                                 double score,
                                                 String verdict) {
        String sujet = "🤖 Résultat de l'évaluation IA — " + titreDuProjet;
        String corps = construireEmailEvaluation(nomUtilisateur, titreDuProjet, score, verdict);
        envoyerEmail(emailDestinataire, sujet, corps);
    }

    // ===== MÉTHODE PRINCIPALE D'ENVOI =====
    private static void envoyerEmail(String destinataire, String sujet, String corps) {
        // Lancer dans un thread séparé pour ne pas bloquer l'UI
        Thread emailThread = new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth",            "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host",            "smtp.gmail.com");
                props.put("mail.smtp.port",            "587");
                props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(EMAIL_EXPEDITEUR, MOT_DE_PASSE_APP);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EMAIL_EXPEDITEUR, "NAJAHNI Platform"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
                message.setSubject(sujet);
                message.setContent(corps, "text/html; charset=UTF-8");

                Transport.send(message);
                System.out.println("✅ Email envoyé à : " + destinataire);

            } catch (Exception e) {
                // Ne pas crasher l'app si l'email échoue
                System.err.println("⚠️ Email non envoyé à " + destinataire + " : " + e.getMessage());
            }
        });
        emailThread.setDaemon(true);
        emailThread.start();
    }

    // ===== TEMPLATE EMAIL — AJOUT PROJET =====
    private static String construireEmailAjout(String nom, String titre, String secteur, String etape) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8"/>
        </head>
        <body>
            <h2>Bonjour %s 👋</h2>

            <p>Votre projet a été <strong>ajouté avec succès</strong>.</p>

            <p><strong>Titre :</strong> %s</p>
            <p><strong>Secteur :</strong> %s</p>
            <p><strong>Étape :</strong> %s</p>
        </body>
        </html>
        """.formatted(nom, titre, secteur, etape);
    }


    // ===== TEMPLATE EMAIL — RÉSULTAT ÉVALUATION =====
    private static String construireEmailEvaluation(String nom, String titre, double score, String verdict) {

        String couleurScore = score >= 70 ? "#27ae60"
                : score >= 50 ? "#f39c12"
                : "#e74c3c";

        String emoji = score >= 70 ? "🟢"
                : score >= 50 ? "🟡"
                : "🔴";

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8"/>
        </head>
        <body>
            <h2>Résultat pour %s</h2>

            <p>Projet : <strong>%s</strong></p>
            <p>Score : <span style="color:%s">%s %.1f/100</span></p>
            <p>Verdict : <strong>%s</strong></p>
        </body>
        </html>
        """.formatted(nom, titre, couleurScore, emoji, score, verdict);
    }

}