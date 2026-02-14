package com.najahni.dao;

import com.najahni.models.Role;
import com.najahni.models.User;
import com.najahni.utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for User entity.
 * Handles all database operations related to users.
 */
public class UserDAO implements GenericDAO<User> {

    private final Connection connection;

    public UserDAO() {
        this.connection = DBConnection.getInstance().getConnection();
    }

    @Override
    public User create(User user) {
        String sql = "INSERT INTO user (name, email, password, role) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getRole().name());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
            }
            
            System.out.println("✓ User created successfully: " + user.getName());
            return user;
            
        } catch (SQLException e) {
            System.err.println("✗ Error creating user: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Optional<User> findById(int id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error finding user by ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user ORDER BY name";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error fetching all users: " + e.getMessage());
            e.printStackTrace();
        }
        
        return users;
    }

    @Override
    public boolean update(User user) {
        String sql = "UPDATE user SET name = ?, email = ?, password = ?, role = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getRole().name());
            stmt.setInt(5, user.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("✓ User updated successfully: " + user.getName());
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error updating user: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM user WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("✓ User deleted successfully. ID: " + id);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error deleting user: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Finds a user by email address.
     * @param email The email to search for
     * @return Optional containing the user if found
     */
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error finding user by email: " + e.getMessage());
            e.printStackTrace();
        }
        
        return Optional.empty();
    }

    /**
     * Finds all users with a specific role.
     * @param role The role to filter by
     * @return List of users with the specified role
     */
    public List<User> findByRole(Role role) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user WHERE role = ? ORDER BY name";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, role.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error finding users by role: " + e.getMessage());
            e.printStackTrace();
        }
        
        return users;
    }

    /**
     * Checks if an email already exists in the database.
     * @param email The email to check
     * @return true if email exists
     */
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM user WHERE email = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error checking email existence: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Checks if an email exists for a different user (for update validation).
     * @param email The email to check
     * @param excludeUserId The user ID to exclude from check
     * @return true if email exists for another user
     */
    public boolean emailExistsForOtherUser(String email, int excludeUserId) {
        String sql = "SELECT COUNT(*) FROM user WHERE email = ? AND id != ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setInt(2, excludeUserId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error checking email existence: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Maps a ResultSet row to a User object.
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(Role.valueOf(rs.getString("role")));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return user;
    }
}
