package services;

import models.Notification;
import tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for in-app notifications.
 */
public class NotificationService {

    private Connection connection = MyConnection.getInstance().getConnection();
    private static NotificationService instance;

    private NotificationService() {}

    public static NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    /**
     * Create a notification.
     */
    public void createNotification(int userId, String title, String message, String type) {
        String sql = "INSERT INTO notification (user_id, title, message, type, is_read, created_at) VALUES (?, ?, ?, ?, FALSE, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, message);
            ps.setString(4, type);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error creating notification: " + ex.getMessage());
        }
    }

    /**
     * Get all notifications for a user (latest first).
     */
    public List<Notification> getNotifications(int userId) {
        return getNotifications(userId, 50);
    }

    public List<Notification> getNotifications(int userId, int limit) {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notification WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                notifications.add(mapResultSet(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error getting notifications: " + ex.getMessage());
        }
        return notifications;
    }

    /**
     * Get unread notification count for a user.
     */
    public int getUnreadCount(int userId) {
        String sql = "SELECT COUNT(*) FROM notification WHERE user_id = ? AND is_read = FALSE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("Error counting unread notifications: " + ex.getMessage());
        }
        return 0;
    }

    /**
     * Mark a notification as read.
     */
    public void markAsRead(int notificationId) {
        String sql = "UPDATE notification SET is_read = TRUE WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error marking notification as read: " + ex.getMessage());
        }
    }

    /**
     * Mark all notifications as read for a user.
     */
    public void markAllAsRead(int userId) {
        String sql = "UPDATE notification SET is_read = TRUE WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error marking all as read: " + ex.getMessage());
        }
    }

    /**
     * Delete a notification.
     */
    public void deleteNotification(int notificationId) {
        String sql = "DELETE FROM notification WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error deleting notification: " + ex.getMessage());
        }
    }

    /**
     * Delete all notifications for a user.
     */
    public void clearAll(int userId) {
        String sql = "DELETE FROM notification WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error clearing notifications: " + ex.getMessage());
        }
    }

    /**
     * Send notification to all users (broadcast).
     */
    public void broadcastNotification(String title, String message, String type) {
        String sql = "INSERT INTO notification (user_id, title, message, type, is_read, created_at) " +
                "SELECT id, ?, ?, ?, FALSE, ? FROM user";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, message);
            ps.setString(3, type);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error broadcasting notification: " + ex.getMessage());
        }
    }

    // ==================== Helper Methods ====================

    private Notification mapResultSet(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setUserId(rs.getInt("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(rs.getString("type"));
        n.setRead(rs.getBoolean("is_read"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) n.setCreatedAt(ts.toLocalDateTime());
        return n;
    }
}
