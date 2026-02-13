package services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PasswordResetService {

    private static PasswordResetService instance;
    private Map<String, CodeWithTimestamp> verificationCodes = new HashMap<>();

    private PasswordResetService() {
    }

    public static PasswordResetService getInstance() {
        if (instance == null) {
            instance = new PasswordResetService();
        }
        return instance;
    }

    /**
     * Generate and store a verification code for email-based password reset.
     */
    public String generateAndStoreCode(String email) {
        String code = generateVerificationCode();
        verificationCodes.put(email, new CodeWithTimestamp(code, System.currentTimeMillis()));
        return code;
    }

    /**
     * Verify code entered by user.
     */
    public boolean verifyCode(String email, String code) {
        cleanExpiredCodes();
        CodeWithTimestamp stored = verificationCodes.get(email);
        if (stored == null) return false;
        return stored.getCode().equals(code);
    }

    /**
     * Change password for a user by email.
     */
    public void changePassword(String newPassword, String email) {
        try {
            Connection connection = MyConnection.getInstance().getConnection();
            String query = "UPDATE user SET password = ? WHERE email = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, hashPassword(newPassword));
            ps.setString(2, email);
            ps.executeUpdate();
            // Remove used code
            verificationCodes.remove(email);
        } catch (Exception e) {
            System.err.println("Error resetting password: " + e.getMessage());
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 1000 + random.nextInt(9000); // 4-digit code
        return String.valueOf(code);
    }

    private String hashPassword(String password) {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray());
    }

    private void cleanExpiredCodes() {
        long currentTime = System.currentTimeMillis();
        verificationCodes.entrySet().removeIf(entry ->
                currentTime - entry.getValue().getTimestamp() > 5 * 60 * 1000 // 5 min expiry
        );
    }

    private static class CodeWithTimestamp {
        private String code;
        private long timestamp;

        public CodeWithTimestamp(String code, long timestamp) {
            this.code = code;
            this.timestamp = timestamp;
        }

        public String getCode() { return code; }
        public long getTimestamp() { return timestamp; }
    }
}
