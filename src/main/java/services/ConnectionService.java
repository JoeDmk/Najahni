package services;

import models.User;
import tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages follow-style connections between users.
 */
public class ConnectionService {
    private Connection connection = MyConnection.getInstance().getConnection();
    private static ConnectionService instance;

    private ConnectionService() {
    }

    public static ConnectionService getInstance() {
        if (instance == null) {
            instance = new ConnectionService();
        }
        return instance;
    }

    /**
     * Follow a user.
     */
    public void follow(int followerId, int followedId) {
        if (followerId == followedId) {
            System.err.println("Cannot follow yourself.");
            return;
        }
        if (isFollowing(followerId, followedId)) {
            System.out.println("Already following this user.");
            return;
        }

        String query = "INSERT INTO user_connection (follower_id, followed_id, created_at) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, followerId);
            ps.setInt(2, followedId);
            ps.setTimestamp(3, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.executeUpdate();
            System.out.println("User " + followerId + " now follows user " + followedId);
        } catch (SQLException ex) {
            System.err.println("Error following user: " + ex.getMessage());
        }
    }

    /**
     * Unfollow a user.
     */
    public void unfollow(int followerId, int followedId) {
        String query = "DELETE FROM user_connection WHERE follower_id = ? AND followed_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, followerId);
            ps.setInt(2, followedId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("User " + followerId + " unfollowed user " + followedId);
            }
        } catch (SQLException ex) {
            System.err.println("Error unfollowing user: " + ex.getMessage());
        }
    }

    /**
     * Check if follower is following followed.
     */
    public boolean isFollowing(int followerId, int followedId) {
        String query = "SELECT COUNT(*) FROM user_connection WHERE follower_id = ? AND followed_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, followerId);
            ps.setInt(2, followedId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException ex) {
            System.err.println("Error checking follow status: " + ex.getMessage());
        }
        return false;
    }

    /**
     * Get all users that a user is following.
     */
    public List<User> getFollowing(int userId) {
        List<User> users = new ArrayList<>();
        String query = "SELECT u.* FROM user u INNER JOIN user_connection uc ON u.id = uc.followed_id WHERE uc.follower_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(UserService.getInstance().createUserFromResultSet(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error getting following list: " + ex.getMessage());
        }
        return users;
    }

    /**
     * Get all followers of a user.
     */
    public List<User> getFollowers(int userId) {
        List<User> users = new ArrayList<>();
        String query = "SELECT u.* FROM user u INNER JOIN user_connection uc ON u.id = uc.follower_id WHERE uc.followed_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(UserService.getInstance().createUserFromResultSet(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error getting followers list: " + ex.getMessage());
        }
        return users;
    }

    /**
     * Count following.
     */
    public int countFollowing(int userId) {
        String query = "SELECT COUNT(*) FROM user_connection WHERE follower_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("Error counting following: " + ex.getMessage());
        }
        return 0;
    }

    /**
     * Count followers.
     */
    public int countFollowers(int userId) {
        String query = "SELECT COUNT(*) FROM user_connection WHERE followed_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("Error counting followers: " + ex.getMessage());
        }
        return 0;
    }

    /**
     * Get suggested users to follow (not already following, not self).
     */
    public List<User> getSuggestions(int userId, int limit) {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM user WHERE id != ? AND id NOT IN " +
                "(SELECT followed_id FROM user_connection WHERE follower_id = ?) " +
                "ORDER BY RAND() LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(UserService.getInstance().createUserFromResultSet(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error getting suggestions: " + ex.getMessage());
        }
        return users;
    }
}
