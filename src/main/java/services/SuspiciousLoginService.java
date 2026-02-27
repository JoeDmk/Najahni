package services;

import models.LoginHistory;
import models.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI-Based Suspicious Login Detection Service.
 * Analyzes login patterns to detect anomalies.
 */
public class SuspiciousLoginService {

    private static SuspiciousLoginService instance;
    private final LoginHistoryService loginHistoryService = LoginHistoryService.getInstance();
    private final NotificationService notificationService = NotificationService.getInstance();

    private SuspiciousLoginService() {}

    public static SuspiciousLoginService getInstance() {
        if (instance == null) {
            instance = new SuspiciousLoginService();
        }
        return instance;
    }

    /**
     * Analyze a login and generate risk score (0-100).
     * Higher score = more suspicious.
     */
    public int analyzeLogin(User user) {
        int riskScore = 0;

        // 1. Check unusual hour (midnight - 5 AM)
        if (loginHistoryService.isUnusualHourLogin()) {
            riskScore += 20;
        }

        // 2. Check for rapid login attempts (brute force indicator)
        if (loginHistoryService.isRapidLoginAttempt(user.getId())) {
            riskScore += 35;
        }

        // 3. Check IP change from last successful login
        if (loginHistoryService.isSuspiciousLogin(user.getId())) {
            riskScore += 25;
        }

        // 4. Check frequent failed attempts recently
        int failedAttempts = loginHistoryService.getRecentFailedAttempts(user.getId(), 30);
        if (failedAttempts >= 3) {
            riskScore += 15;
        } else if (failedAttempts >= 1) {
            riskScore += 5;
        }

        // 5. New device detected
        List<String> devices = loginHistoryService.getUniqueDevices(user.getId());
        String currentDevice = System.getProperty("os.name", "Unknown") + " " +
                System.getProperty("os.version", "") + " (" +
                System.getProperty("os.arch", "") + ") - Java " +
                System.getProperty("java.version", "");
        if (!devices.isEmpty() && !devices.contains(currentDevice)) {
            riskScore += 15;
        }

        return Math.min(riskScore, 100);
    }

    /**
     * Get risk level string from score.
     */
    public String getRiskLevel(int score) {
        if (score >= 70) return "CRITICAL";
        if (score >= 50) return "HIGH";
        if (score >= 30) return "MEDIUM";
        if (score >= 10) return "LOW";
        return "SAFE";
    }

    /**
     * Get risk color for UI display.
     */
    public String getRiskColor(int score) {
        if (score >= 70) return "#E74C3C";
        if (score >= 50) return "#E17055";
        if (score >= 30) return "#FDCB6E";
        if (score >= 10) return "#74B9FF";
        return "#00B894";
    }

    /**
     * Handle suspicious login: create notifications and optionally alert admin.
     */
    public void handleSuspiciousLogin(User user, int riskScore) {
        String riskLevel = getRiskLevel(riskScore);

        if (riskScore >= 30) {
            // Notify the user
            String message = String.format(
                    "Connexion inhabituelle détectée (Risque: %s, Score: %d/100). " +
                    "Si ce n'est pas vous, changez votre mot de passe immédiatement.",
                    riskLevel, riskScore
            );
            notificationService.createNotification(user.getId(),
                    "⚠ Connexion suspecte détectée", message, "WARNING");
        }

        if (riskScore >= 50) {
            // Also notify admins
            notifyAdmins(user, riskScore, riskLevel);
        }

        System.out.println("Login risk analysis for " + user.getEmail() +
                ": Score=" + riskScore + " Level=" + riskLevel);
    }

    /**
     * Get a summary of login patterns for a user.
     */
    public String getLoginAnalysisSummary(User user) {
        List<LoginHistory> history = loginHistoryService.getLoginHistory(user.getId(), 20);
        int totalLogins = history.size();
        long failedLogins = history.stream().filter(h -> !h.isSuccess()).count();
        long uniqueIPs = history.stream().map(LoginHistory::getIpAddress).distinct().count();
        List<String> devices = loginHistoryService.getUniqueDevices(user.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("📊 Analyse de connexion pour ").append(user.getFullName()).append("\n\n");
        sb.append("• Dernières connexions: ").append(totalLogins).append("\n");
        sb.append("• Tentatives échouées: ").append(failedLogins).append("\n");
        sb.append("• Adresses IP uniques: ").append(uniqueIPs).append("\n");
        sb.append("• Appareils connus: ").append(devices.size()).append("\n");
        int currentRiskScore = analyzeLogin(user);
        sb.append("• Score de risque actuel: ").append(currentRiskScore).append("/100\n");
        sb.append("• Niveau: ").append(getRiskLevel(currentRiskScore));

        return sb.toString();
    }

    private void notifyAdmins(User user, int riskScore, String riskLevel) {
        try {
            List<User> admins = UserService.getInstance().getUsersByRole(util.Type.ADMIN);
            for (User admin : admins) {
                String message = String.format(
                        "Connexion suspecte pour %s (%s). Score de risque: %d/100 (%s).",
                        user.getFullName(), user.getEmail(), riskScore, riskLevel
                );
                notificationService.createNotification(admin.getId(),
                        "🚨 Alerte sécurité", message, "DANGER");
            }
        } catch (Exception e) {
            System.err.println("Error notifying admins: " + e.getMessage());
        }
    }
}
