package Services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class TicketSigner {
    private static final String SECRET = "PUT_A_LONG_RANDOM_SECRET_HERE_123!@#";

    public static String signature(int eventId, int userId, String eventDateIso, String eventTitle, String userFirstname) {
        try {
            String data = eventId + ":" + userId + ":" + eventDateIso + ":" + eventTitle + ":" + userFirstname + ":" + SECRET;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
