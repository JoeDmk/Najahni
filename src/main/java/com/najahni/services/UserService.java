package com.najahni.services;

import com.najahni.models.Role;
import com.najahni.models.User;
import com.najahni.utils.DBConnection;

import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service pour la gestion des Utilisateurs.
 * Accède directement à la base de données via JDBC (pas de DAO).
 */
public class UserService {

    private Connection cnx;

    public UserService() {
        this.cnx = DBConnection.getInstance().getConnection();
    }

    // ─── PASSWORD HASHING ────────────────────────────────────

    /**
     * Hache un mot de passe avec SHA-256.
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Authentifie un utilisateur par email et mot de passe.
     * @return L'utilisateur si les identifiants sont valides, Optional.empty() sinon.
     */
    public Optional<User> authenticate(String email, String password) {
        Optional<User> userOpt = findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String hashed = hashPassword(password);
            // Support both plain text (legacy) and hashed passwords
            if (hashed.equals(user.getPassword()) || password.equals(user.getPassword())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    // ─── CRUD ────────────────────────────────────────────────

    public User createUser(User user) throws IllegalArgumentException {
        validateUser(user);
        if (emailExists(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }

        String sql = "INSERT INTO user (firstname, lastname, email, password, role) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String[] parts = splitName(user.getName());
            ps.setString(1, parts[0]);
            ps.setString(2, parts[1]);
            ps.setString(3, user.getEmail());
            ps.setString(4, hashPassword(user.getPassword()));
            ps.setString(5, user.getRole().getDbValue());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
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

    public Optional<User> findById(int id) {
        String sql = "SELECT *, CONCAT(firstname, ' ', lastname) AS full_name FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
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

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT *, CONCAT(firstname, ' ', lastname) AS full_name FROM user ORDER BY firstname";
        try (Statement stmt = cnx.createStatement();
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

    public boolean updateUser(User user) throws IllegalArgumentException {
        validateUser(user);
        if (emailExistsForOtherUser(user.getEmail(), user.getId())) {
            throw new IllegalArgumentException("Email already exists for another user: " + user.getEmail());
        }

        String sql = "UPDATE user SET firstname = ?, lastname = ?, email = ?, password = ?, role = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            String[] parts = splitName(user.getName());
            ps.setString(1, parts[0]);
            ps.setString(2, parts[1]);
            ps.setString(3, user.getEmail());
            // Only hash if not already a SHA-256 hash (64 hex chars)
            String pwd = user.getPassword();
            if (pwd.length() != 64 || !pwd.matches("[0-9a-f]{64}")) {
                pwd = hashPassword(pwd);
            }
            ps.setString(4, pwd);
            ps.setString(5, user.getRole().getDbValue());
            ps.setInt(6, user.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("✗ Error updating user: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteUser(int id) {
        String sql = "DELETE FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("✗ Error deleting user: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ─── REQUÊTES SPÉCIFIQUES ────────────────────────────────

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT *, CONCAT(firstname, ' ', lastname) AS full_name FROM user WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
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

    public List<User> findByRole(Role role) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT *, CONCAT(firstname, ' ', lastname) AS full_name FROM user WHERE role = ? ORDER BY firstname";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, role.getDbValue());
            try (ResultSet rs = ps.executeQuery()) {
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

    public List<User> getAllEntrepreneurs() {
        return findByRole(Role.ENTREPRENEUR);
    }

    public List<User> getAllInvestors() {
        return findByRole(Role.INVESTOR);
    }

    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM user WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean emailExistsForOtherUser(String email, int excludeUserId) {
        String sql = "SELECT COUNT(*) FROM user WHERE email = ? AND id != ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, excludeUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ─── MAPPING ─────────────────────────────────────────────

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        String fullName;
        try {
            fullName = rs.getString("full_name");
        } catch (SQLException e) {
            fullName = rs.getString("firstname") + " " + rs.getString("lastname");
        }
        user.setName(fullName != null ? fullName.trim() : "");
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(Role.fromDbValue(rs.getString("role")));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) user.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) user.setUpdatedAt(updatedAt.toLocalDateTime());

        return user;
    }

    // ─── VALIDATION ──────────────────────────────────────────

    private void validateUser(User user) throws IllegalArgumentException {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        if (user.getName() == null || user.getName().trim().isEmpty())
            throw new IllegalArgumentException("Name is required");
        if (user.getName().length() < 2)
            throw new IllegalArgumentException("Name must be at least 2 characters");
        if (user.getName().length() > 100)
            throw new IllegalArgumentException("Name cannot exceed 100 characters");
        if (user.getEmail() == null || user.getEmail().trim().isEmpty())
            throw new IllegalArgumentException("Email is required");
        if (!isValidEmail(user.getEmail()))
            throw new IllegalArgumentException("Invalid email format");
        if (user.getEmail().length() > 150)
            throw new IllegalArgumentException("Email cannot exceed 150 characters");
        if (user.getPassword() == null || user.getPassword().trim().isEmpty())
            throw new IllegalArgumentException("Password is required");
        if (user.getPassword().length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters");
        if (user.getRole() == null)
            throw new IllegalArgumentException("Role is required");
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private String[] splitName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return new String[]{"", ""};
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length == 1 ? new String[]{parts[0], ""} : parts;
    }
}
