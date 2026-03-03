package services.investissement;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Service d'envoi d'e-mails de confirmation de paiement pour le module investissement.
 * Utilise Jakarta Mail via Gmail SMTP (SSL, port 465).
 * Envoie des e-mails HTML formatés avec le branding NAJAHNI.
 */
public class PaymentEmailService {

    private static final Logger LOG = Logger.getLogger(PaymentEmailService.class.getName());

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 465;

    // Loaded from secrets.properties
    private static final String FROM_EMAIL;
    private static final String FROM_PASSWORD;
    static {
        String email = "";
        String password = "";
        try (InputStream is = PaymentEmailService.class.getClassLoader().getResourceAsStream("secrets.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                email = props.getProperty("email.sender", "");
                password = props.getProperty("email.app_password", "");
            }
        } catch (Exception e) {
            LOG.warning("Could not load email credentials from secrets.properties: " + e.getMessage());
        }
        FROM_EMAIL = email;
        FROM_PASSWORD = password;
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    /**
     * Sends a payment confirmation email asynchronously.
     *
     * @param recipientEmail   The investor's email address
     * @param investorName     The investor's full name
     * @param amount           The payment amount (formatted string, e.g. "5 000,00 €")
     * @param projectTitle     The project title
     * @param transactionId    The Stripe PaymentIntent ID
     * @param offerId          The offer ID
     */
    public CompletableFuture<Boolean> sendPaymentConfirmation(String recipientEmail, String investorName,
                                                               String amount, String projectTitle,
                                                               String transactionId, int offerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String subject = "✅ NAJAHNI — Confirmation de paiement #" + offerId;
                String htmlBody = buildPaymentConfirmationHtml(investorName, amount, projectTitle,
                        transactionId, offerId, LocalDateTime.now());

                sendHtmlEmail(recipientEmail, subject, htmlBody);
                LOG.info("✅ Payment confirmation email sent to " + recipientEmail);
                return true;
            } catch (Exception e) {
                LOG.warning("❌ Failed to send payment confirmation email: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Sends an HTML-formatted email.
     */
    private void sendHtmlEmail(String to, String subject, String htmlBody) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        props.put("mail.smtp.ssl.checkserveridentity", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
            }
        });

        MimeMessage message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(FROM_EMAIL, "NAJAHNI Platform"));
        } catch (java.io.UnsupportedEncodingException e) {
            message.setFrom(new InternetAddress(FROM_EMAIL));
        }
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);

        // HTML content with fallback text
        MimeMultipart multipart = new MimeMultipart("alternative");

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Votre paiement a été confirmé avec succès sur NAJAHNI.", "utf-8");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=utf-8");

        multipart.addBodyPart(textPart);
        multipart.addBodyPart(htmlPart);

        message.setContent(multipart);
        Transport.send(message);
    }

    /**
     * Builds the HTML body for a payment confirmation email.
     */
    private String buildPaymentConfirmationHtml(String investorName, String amount, String projectTitle,
                                                 String transactionId, int offerId, LocalDateTime paidAt) {
        String dateStr = paidAt.format(DATE_FMT);

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;font-family:'Segoe UI',Arial,sans-serif;background-color:#f4f6f9;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:20px auto;">
                    <tr>
                        <td>
                            <!-- Header -->
                            <table width="100%%" cellpadding="0" cellspacing="0" style="background:linear-gradient(135deg,#667eea 0%%,#764ba2 100%%);border-radius:16px 16px 0 0;padding:30px 40px;">
                                <tr>
                                    <td style="text-align:center;">
                                        <h1 style="color:#fff;margin:0;font-size:28px;letter-spacing:1px;">NAJAHNI</h1>
                                        <p style="color:#e0d4ff;margin:5px 0 0;font-size:13px;">Plateforme d'Investissement Intelligente</p>
                                    </td>
                                </tr>
                            </table>
                            
                            <!-- Success Badge -->
                            <table width="100%%" cellpadding="0" cellspacing="0" style="background:#fff;padding:30px 40px 20px;">
                                <tr>
                                    <td style="text-align:center;">
                                        <div style="display:inline-block;background:#eafaf1;border-radius:50%%;width:72px;height:72px;line-height:72px;font-size:36px;text-align:center;">✅</div>
                                        <h2 style="color:#27ae60;margin:16px 0 5px;font-size:22px;">Paiement Confirmé</h2>
                                        <p style="color:#7f8c8d;margin:0;font-size:14px;">Votre investissement a été traité avec succès</p>
                                    </td>
                                </tr>
                            </table>
                            
                            <!-- Greeting -->
                            <table width="100%%" cellpadding="0" cellspacing="0" style="background:#fff;padding:10px 40px;">
                                <tr>
                                    <td>
                                        <p style="color:#2c3e50;font-size:15px;">Bonjour <strong>%s</strong>,</p>
                                        <p style="color:#555;font-size:14px;line-height:1.6;">
                                            Nous avons le plaisir de vous confirmer que votre paiement a été traité avec succès. 
                                            Voici le récapitulatif de votre transaction :
                                        </p>
                                    </td>
                                </tr>
                            </table>
                            
                            <!-- Transaction Details -->
                            <table width="100%%" cellpadding="0" cellspacing="0" style="background:#fff;padding:10px 40px 20px;">
                                <tr>
                                    <td>
                                        <table width="100%%" cellpadding="12" cellspacing="0" style="background:#f8f9fb;border-radius:12px;border:1px solid #e9ecef;">
                                            <tr>
                                                <td style="color:#7f8c8d;font-size:13px;border-bottom:1px solid #e9ecef;width:40%%;">📋 Offre N°</td>
                                                <td style="color:#2c3e50;font-weight:bold;font-size:14px;border-bottom:1px solid #e9ecef;">#%d</td>
                                            </tr>
                                            <tr>
                                                <td style="color:#7f8c8d;font-size:13px;border-bottom:1px solid #e9ecef;">📌 Projet</td>
                                                <td style="color:#2c3e50;font-weight:bold;font-size:14px;border-bottom:1px solid #e9ecef;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="color:#7f8c8d;font-size:13px;border-bottom:1px solid #e9ecef;">💰 Montant</td>
                                                <td style="color:#27ae60;font-weight:bold;font-size:16px;border-bottom:1px solid #e9ecef;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="color:#7f8c8d;font-size:13px;border-bottom:1px solid #e9ecef;">🔐 Transaction</td>
                                                <td style="color:#667eea;font-size:12px;font-family:monospace;border-bottom:1px solid #e9ecef;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="color:#7f8c8d;font-size:13px;">📅 Date</td>
                                                <td style="color:#2c3e50;font-size:14px;">%s</td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                            
                            <!-- Info Box -->
                            <table width="100%%" cellpadding="0" cellspacing="0" style="background:#fff;padding:10px 40px 20px;">
                                <tr>
                                    <td>
                                        <table width="100%%" cellpadding="16" cellspacing="0" style="background:#eef0ff;border-radius:12px;border-left:4px solid #667eea;">
                                            <tr>
                                                <td style="color:#2c3e50;font-size:13px;line-height:1.6;">
                                                    💡 <strong>Prochaines étapes :</strong><br>
                                                    • Votre investissement apparaît dans votre <strong>Portefeuille</strong><br>
                                                    • Un contrat sera disponible pour signature dans l'onglet <strong>Contrats</strong><br>
                                                    • Vous recevrez des mises à jour sur l'avancement du projet
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                            
                            <!-- Footer -->
                            <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f8f9fb;border-radius:0 0 16px 16px;padding:20px 40px;border-top:1px solid #e9ecef;">
                                <tr>
                                    <td style="text-align:center;">
                                        <p style="color:#95a5a6;font-size:11px;margin:0;">
                                            Cet email a été envoyé automatiquement par <strong>NAJAHNI</strong>.<br>
                                            Ne répondez pas à cet email. Pour toute question, contactez-nous via l'application.
                                        </p>
                                        <p style="color:#b0b0b0;font-size:10px;margin:10px 0 0;">
                                            © 2025 NAJAHNI — Plateforme d'Investissement Intelligente 🇹🇳
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(investorName, offerId, projectTitle, amount, transactionId, dateStr);
    }
}
