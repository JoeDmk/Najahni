package services;

import models.LoginHistory;
import tools.MyConnection;

import java.net.InetAddress;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for tracking login history and device management.
 */
public class LoginHistoryService {

    private Connection connection = MyConnection.getInstance().getConnection();
    private static LoginHistoryService instance;

    private LoginHistoryService() {}

    public static LoginHistoryService getInstance() {
        if (instance == null) {
            instance = new LoginHistoryService();
        }
        return instance;
    }

    /**
     * Record a login attempt.
     */
    public void recordLogin(int userId, String loginMethod, boolean success) {
        String ipAddress = getLocalIpAddress();
        String deviceInfo = getDeviceInfo();

        String sql = "INSERT INTO login_history (user_id, ip_address, device_info, login_method, success, location, login_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, ipAddress);
            ps.setString(3, deviceInfo);
            ps.setString(4, loginMethod);
            ps.setBoolean(5, success);
            ps.setString(6, "Local");
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error recording login: " + ex.getMessage());
        }
    }

    /**
     * Get login history for a specific user.
     */
    public List<LoginHistory> getLoginHistory(int userId) {
        return getLoginHistory(userId, 50);
    }

    public List<LoginHistory> getLoginHistory(int userId, int limit) {
        List<LoginHistory> history = new ArrayList<>();
        String sql = "SELECT * FROM login_history WHERE user_id = ? ORDER BY login_time DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                history.add(mapResultSet(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error getting login history: " + ex.getMessage());
        }
        return history;
    }

    /**
     * Get all login history (admin view).
     */
    public List<LoginHistory> getAllLoginHistory(int limit) {
        List<LoginHistory> history = new ArrayList<>();
        String sql = "SELECT lh.*, u.email as user_email, CONCAT(u.firstname, ' ', u.lastname) as user_full_name " +
                "FROM login_history lh JOIN user u ON lh.user_id = u.id ORDER BY lh.login_time DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LoginHistory lh = mapResultSet(rs);
                lh.setUserEmail(rs.getString("user_email"));
                lh.setUserFullName(rs.getString("user_full_name"));
                history.add(lh);
            }
        } catch (SQLException ex) {
            System.err.println("Error getting all login history: " + ex.getMessage());
        }
        return history;
    }

    /**
     * Get failed login count for a user in the last N minutes.
     */
    public int getRecentFailedAttempts(int userId, int minutes) {
        String sql = "SELECT COUNT(*) FROM login_history WHERE user_id = ? AND success = FALSE AND login_time > ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusMinutes(minutes)));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("Error counting failed attempts: " + ex.getMessage());
        }
        return 0;
    }

    /**
     * Detect suspicious login: different IP from last successful login.
     */
    public boolean isSuspiciousLogin(int userId) {
        String sql = "SELECT ip_address FROM login_history WHERE user_id = ? AND success = TRUE ORDER BY login_time DESC LIMIT 2";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            List<String> ips = new ArrayList<>();
            while (rs.next()) {
                ips.add(rs.getString("ip_address"));
            }
            if (ips.size() >= 2) {
                return !ips.get(0).equals(ips.get(1));
            }
        } catch (SQLException ex) {
            System.err.println("Error checking suspicious login: " + ex.getMessage());
        }
        return false;
    }

    /**
     * Check if login is at unusual hour (between midnight and 5 AM).
     */
    public boolean isUnusualHourLogin() {
        int hour = LocalDateTime.now().getHour();
        return hour >= 0 && hour < 5;
    }

    /**
     * Get count of logins today for a user.
     */
    public int getLoginCountToday(int userId) {
        String sql = "SELECT COUNT(*) FROM login_history WHERE user_id = ? AND DATE(login_time) = CURDATE()";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("Error counting today's logins: " + ex.getMessage());
        }
        return 0;
    }

    /**
     * Check for rapid successive logins (more than 5 in last 10 minutes) - possible brute force.
     */
    public boolean isRapidLoginAttempt(int userId) {
        int recentAttempts = getRecentFailedAttempts(userId, 10);
        return recentAttempts >= 5;
    }

    /**
     * Delete login history for a user.
     */
    public void clearHistory(int userId) {
        String sql = "DELETE FROM login_history WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error clearing login history: " + ex.getMessage());
        }
    }

    /**
     * Get unique devices used by a user.
     */
    public List<String> getUniqueDevices(int userId) {
        List<String> devices = new ArrayList<>();
        String sql = "SELECT DISTINCT device_info FROM login_history WHERE user_id = ? AND success = TRUE ORDER BY login_time DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                devices.add(rs.getString("device_info"));
            }
        } catch (SQLException ex) {
            System.err.println("Error getting unique devices: " + ex.getMessage());
        }
        return devices;
    }

    // ==================== Helper Methods ====================

    private LoginHistory mapResultSet(ResultSet rs) throws SQLException {
        LoginHistory lh = new LoginHistory();
        lh.setId(rs.getInt("id"));
        lh.setUserId(rs.getInt("user_id"));
        lh.setIpAddress(rs.getString("ip_address"));
        lh.setDeviceInfo(rs.getString("device_info"));
        lh.setLoginMethod(rs.getString("login_method"));
        lh.setSuccess(rs.getBoolean("success"));
        lh.setLocation(rs.getString("location"));
        Timestamp ts = rs.getTimestamp("login_time");
        if (ts != null) lh.setLoginTime(ts.toLocalDateTime());
        return lh;
    }

    private String getLocalIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private String getDeviceInfo() {
        String os = System.getProperty("os.name", "Unknown");
        String osVersion = System.getProperty("os.version", "");
        String arch = System.getProperty("os.arch", "");
        String javaVersion = System.getProperty("java.version", "");
        return os + " " + osVersion + " (" + arch + ") - Java " + javaVersion;
    }
}
