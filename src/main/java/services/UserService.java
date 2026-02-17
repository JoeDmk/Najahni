package services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import exceptions.*;
import interfaces.UserInterface;
import models.User;
import tools.MyConnection;
import util.Type;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service layer for User business logic.
 * Handles all JDBC database access and business rules.
 */
public class UserService implements UserInterface {

    private Connection connection = MyConnection.getInstance().getConnection();
    private ValidationService validationService = new ValidationService();
    private static UserService instance;

    private UserService() {
    }

    public static UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    // ==================== CRUD Operations ====================

    @Override
    public void addUser(User user) throws EmptyFieldException, InvalidPhoneNumberException, InvalidEmailException,
            IncorrectPasswordException, CustomIllegalStateException {

        // Only one admin allowed
        if (user.getRole() == Type.ADMIN && countByRole(Type.ADMIN) > 0) {
            throw new CustomIllegalStateException("Un administrateur existe déjà.");
        }

        validateUserFields(user, true);

        // Hash the password
        user.setPassword(cryptPassword(user.getPassword()));

        String sql = "INSERT INTO user (firstname, lastname, email, phone, password, role, bio, profile_picture, " +
                "company_name, linkedin_url, address, date_of_birth, verified, phone_verified, is_active, is_banned, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFirstname());
            ps.setString(2, user.getLastname());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getPassword());
            ps.setString(6, user.getRole().name());
            ps.setString(7, user.getBio());
            ps.setString(8, user.getProfilePicture());
            ps.setString(9, user.getCompanyName());
            ps.setString(10, user.getLinkedinUrl());
            ps.setString(11, user.getAddress());
            ps.setDate(12, user.getDateOfBirth() != null ? Date.valueOf(user.getDateOfBirth()) : null);
            ps.setBoolean(13, false);
            ps.setBoolean(14, false);
            ps.setBoolean(15, true);
            ps.setBoolean(16, false);
            ps.setTimestamp(17, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(18, Timestamp.valueOf(LocalDateTime.now()));

            ps.executeUpdate();

            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                user.setId(generatedKeys.getInt(1));
            }
            System.out.println("User added successfully!");
        } catch (SQLException ex) {
            System.err.println("Error adding user: " + ex.getMessage());
        }
    }

    @Override
    public void updateUser(User user) throws EmptyFieldException, InvalidPhoneNumberException,
            InvalidEmailException, IncorrectPasswordException, UserNotFoundException {
        if (user == null || user.getId() <= 0) {
            throw new UserNotFoundException("Utilisateur introuvable.");
        }

        validateUserFields(user, false);

        String sql = "UPDATE user SET firstname=?, lastname=?, email=?, phone=?, role=?, bio=?, profile_picture=?, " +
                "company_name=?, linkedin_url=?, address=?, date_of_birth=?, is_banned=?, is_active=?, updated_at=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getFirstname());
            ps.setString(2, user.getLastname());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getRole().name());
            ps.setString(6, user.getBio());
            ps.setString(7, user.getProfilePicture());
            ps.setString(8, user.getCompanyName());
            ps.setString(9, user.getLinkedinUrl());
            ps.setString(10, user.getAddress());
            ps.setDate(11, user.getDateOfBirth() != null ? Date.valueOf(user.getDateOfBirth()) : null);
            ps.setBoolean(12, user.getIsBanned());
            ps.setBoolean(13, user.getIsActive());
            ps.setTimestamp(14, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(15, user.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new UserNotFoundException("Aucun utilisateur trouvé avec l'ID: " + user.getId());
            }
            System.out.println("User updated successfully!");
        } catch (SQLException ex) {
            System.err.println("Error updating user: " + ex.getMessage());
        }
    }

    public void updateProfile(User user) throws EmptyFieldException, InvalidEmailException, InvalidPhoneNumberException {
        if (user.getFirstname().isEmpty() || user.getLastname().isEmpty() || user.getEmail().isEmpty()) {
            throw new EmptyFieldException("Veuillez remplir tous les champs obligatoires.");
        }
        if (!validationService.isValidEmail(user.getEmail())) {
            throw new InvalidEmailException("L'adresse email est invalide.");
        }
        if (user.getPhone() != null && !user.getPhone().isEmpty() && !validationService.isValidPhoneNumber(user.getPhone())) {
            throw new InvalidPhoneNumberException("Le numéro de téléphone est invalide.");
        }

        String sql = "UPDATE user SET firstname=?, lastname=?, email=?, phone=?, bio=?, profile_picture=?, " +
                "company_name=?, linkedin_url=?, address=?, date_of_birth=?, updated_at=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getFirstname());
            ps.setString(2, user.getLastname());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getBio());
            ps.setString(6, user.getProfilePicture());
            ps.setString(7, user.getCompanyName());
            ps.setString(8, user.getLinkedinUrl());
            ps.setString(9, user.getAddress());
            ps.setDate(10, user.getDateOfBirth() != null ? Date.valueOf(user.getDateOfBirth()) : null);
            ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(12, user.getId());

            ps.executeUpdate();
            System.out.println("Profile updated successfully!");
        } catch (SQLException ex) {
            System.err.println("Error updating profile: " + ex.getMessage());
        }
    }

    @Override
    public void deleteUser(int id) throws UserNotFoundException {
        getUserbyID(id); // verify exists

        try {
            connection.setAutoCommit(false);

            // Find all tables that have a foreign key referencing the user table
            String fkQuery = "SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                    "WHERE REFERENCED_TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME = 'user' AND REFERENCED_COLUMN_NAME = 'id'";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(fkQuery)) {
                while (rs.next()) {
                    String childTable = rs.getString("TABLE_NAME");
                    String childColumn = rs.getString("COLUMN_NAME");
                    String deleteSql = "DELETE FROM `" + childTable + "` WHERE `" + childColumn + "` = ?";
                    try (PreparedStatement ps = connection.prepareStatement(deleteSql)) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                }
            }

            // Now delete the user
            String sql = "DELETE FROM user WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            connection.commit();
            System.out.println("User deleted successfully!");
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Error during rollback: " + rollbackEx.getMessage());
            }
            System.err.println("Error deleting user: " + ex.getMessage());
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                System.err.println("Error resetting auto-commit: " + ex.getMessage());
            }
        }
    }

    @Override
    public List<User> getUsers() {
        List<User> users = new ArrayList<>();
        try {
            String sql = "SELECT * FROM user ORDER BY created_at DESC";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                users.add(mapResultSet(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error retrieving users: " + ex.getMessage());
        }
        return users;
    }

    public List<User> getUsersByRole(Type role) {
        List<User> users = new ArrayList<>();
        try {
            String sql = "SELECT * FROM user WHERE role = ? ORDER BY created_at DESC";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, role.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(mapResultSet(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error retrieving users by role: " + ex.getMessage());
        }
        return users;
    }

    @Override
    public User getUserbyID(int id) throws UserNotFoundException {
        try {
            String sql = "SELECT * FROM user WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException ex) {
            System.err.println("Error retrieving user by ID: " + ex.getMessage());
        }
        throw new UserNotFoundException("Aucun utilisateur trouvé avec l'ID: " + id);
    }

    @Override
    public User getUserbyEmail(String email) throws UserNotFoundException {
        try {
            String sql = "SELECT * FROM user WHERE email = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException ex) {
            System.err.println("Error retrieving user by email: " + ex.getMessage());
        }
        throw new UserNotFoundException("Aucun utilisateur trouvé avec l'email: " + email);
    }

    // ==================== Password Management ====================

    public void updatePassword(int userId, String newPassword) throws UserNotFoundException, IncorrectPasswordException, EmptyFieldException {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new EmptyFieldException("Le nouveau mot de passe ne peut pas être vide.");
        }
        if (!validationService.isValidPassword(newPassword)) {
            throw new IncorrectPasswordException("Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et faire au moins 6 caractères.");
        }

        String sql = "UPDATE user SET password = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, cryptPassword(newPassword));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, userId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new UserNotFoundException("Aucun utilisateur trouvé avec l'ID: " + userId);
            }
        } catch (SQLException ex) {
            System.err.println("Error updating password: " + ex.getMessage());
        }
    }

    public void updatePasswordByEmail(String email, String hashedPassword) {
        String sql = "UPDATE user SET password = ? WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setString(2, email);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error resetting password by email: " + ex.getMessage());
        }
    }

    public void updateEmail(int userId, String newEmail) throws UserNotFoundException, InvalidEmailException {
        if (!validationService.isValidEmail(newEmail)) {
            throw new InvalidEmailException("L'adresse email est invalide.");
        }
        if (emailExists(newEmail)) {
            throw new InvalidEmailException("Cet email est déjà utilisé.");
        }

        String sql = "UPDATE user SET email = ?, verified = false, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newEmail);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, userId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new UserNotFoundException("Aucun utilisateur trouvé avec l'ID: " + userId);
            }
        } catch (SQLException ex) {
            System.err.println("Error updating email: " + ex.getMessage());
        }
    }

    // ==================== Email Verification ====================

    public void verifyUser(int userId) throws UserNotFoundException {
        getUserbyID(userId); // verify exists
        setVerified(userId, true);
    }

    public void unverifyEmail(int userId) {
        setVerified(userId, false);
    }

    public void verifyPhone(int userId) {
        setPhoneVerified(userId, true);
    }

    public void unverifyPhone(int userId) {
        setPhoneVerified(userId, false);
    }

    private void setVerified(int userId, boolean verified) {
        String sql = "UPDATE user SET verified = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, verified);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error updating verified status: " + ex.getMessage());
        }
    }

    private void setPhoneVerified(int userId, boolean verified) {
        String sql = "UPDATE user SET phone_verified = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, verified);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error updating phone verified status: " + ex.getMessage());
        }
    }

    // ==================== Ban/Unban ====================

    public void banUser(int userId) throws PermissionException, UserNotFoundException {
        User user = getUserbyID(userId);
        if (user.getRole() == Type.ADMIN) {
            throw new PermissionException("Impossible de bannir un administrateur.");
        }
        setBanStatus(userId, true, false);
        System.out.println("User " + userId + " has been banned.");
    }

    public void unbanUser(int userId) throws PermissionException, UserNotFoundException {
        getUserbyID(userId); // verify exists
        setBanStatus(userId, false, true);
        System.out.println("User " + userId + " has been unbanned.");
    }

    private void setBanStatus(int userId, boolean banned, boolean active) {
        String sql = "UPDATE user SET is_banned = ?, is_active = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, banned);
            ps.setBoolean(2, active);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(4, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error updating ban status: " + ex.getMessage());
        }
    }

    // ==================== Statistics ====================

    public int countByRole(Type role) {
        String sql = "SELECT COUNT(*) FROM user WHERE role = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, role.name());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("Error counting by role: " + ex.getMessage());
        }
        return 0;
    }

    public int countActive() {
        String sql = "SELECT COUNT(*) FROM user WHERE is_active = true";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("Error counting active users: " + ex.getMessage());
        }
        return 0;
    }

    public int countBanned() {
        String sql = "SELECT COUNT(*) FROM user WHERE is_banned = true";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("Error counting banned users: " + ex.getMessage());
        }
        return 0;
    }

    public int countTotal() {
        String sql = "SELECT COUNT(*) FROM user";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("Error counting total users: " + ex.getMessage());
        }
        return 0;
    }

    // ==================== Search ====================

    public List<User> searchUsers(String keyword) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user WHERE firstname LIKE ? OR lastname LIKE ? OR email LIKE ? OR company_name LIKE ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(mapResultSet(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error searching users: " + ex.getMessage());
        }
        return users;
    }

    // ==================== Google OAuth ====================

    public User getUserByGoogleId(String googleId) {
        try {
            String sql = "SELECT * FROM user WHERE google_provider_id = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, googleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException ex) {
            System.err.println("Error retrieving user by Google ID: " + ex.getMessage());
        }
        return null;
    }

    /**
     * Finds an existing user linked to the given Google account, or creates a new one.
     */
    public User findOrCreateGoogleUser(String googleId, String email, String firstName,
                                        String lastName, String pictureUrl) {
        // 1. Check by Google provider ID
        User user = getUserByGoogleId(googleId);
        if (user != null) {
            System.out.println("Found existing user by Google ID: " + email);
            return user;
        }

        // 2. Check by email
        try {
            User existingByEmail = getUserbyEmail(email);
            linkGoogleId(existingByEmail.getId(), googleId);
            existingByEmail.setGoogleProviderId(googleId);
            System.out.println("Linked Google ID to existing user: " + email);
            return existingByEmail;
        } catch (UserNotFoundException ignored) {
            // No existing user with that email, continue to create
        }

        // 3. Create a new user from Google profile
        int newId = insertGoogleUser(firstName, lastName, email, pictureUrl, googleId, Type.ENTREPRENEUR);
        if (newId > 0) {
            System.out.println("Created new Google user with ID: " + newId + ", email: " + email);
            try {
                return getUserbyID(newId);
            } catch (UserNotFoundException e) {
                System.err.println("Error retrieving newly created Google user: " + e.getMessage());
            }
        }

        return null;
    }

    private void linkGoogleId(int userId, String googleId) {
        String sql = "UPDATE user SET google_provider_id = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, googleId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error linking Google ID: " + ex.getMessage());
        }
    }

    private int insertGoogleUser(String firstName, String lastName, String email,
                                 String pictureUrl, String googleId, Type role) {
        String sql = "INSERT INTO user (firstname, lastname, email, phone, password, role, " +
                "profile_picture, google_provider_id, verified, is_active, is_banned, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setString(4, "");
            ps.setString(5, "GOOGLE_OAUTH_NO_PASSWORD");
            ps.setString(6, role.name());
            ps.setString(7, pictureUrl);
            ps.setString(8, googleId);
            ps.setBoolean(9, true);
            ps.setBoolean(10, true);
            ps.setBoolean(11, false);
            ps.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));

            ps.executeUpdate();

            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            }
        } catch (SQLException ex) {
            System.err.println("Error creating Google user: " + ex.getMessage());
        }
        return -1;
    }

    // ==================== Account Lock ====================

    public void lockAccountByEmail(String email) {
        String sqlCheck = "SELECT role FROM user WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sqlCheck)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                if (!Type.ADMIN.name().equals(role)) {
                    String sqlUpdate = "UPDATE user SET is_active = false, is_banned = true WHERE email = ?";
                    try (PreparedStatement ups = connection.prepareStatement(sqlUpdate)) {
                        ups.setString(1, email);
                        ups.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error locking account: " + e.getMessage());
        }
    }

    public void unlockAccountByEmail(String email) {
        String sql = "UPDATE user SET is_active = true, is_banned = false WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
            System.out.println("Account unlocked: " + email);
        } catch (SQLException e) {
            System.err.println("Error unlocking account: " + e.getMessage());
        }
    }

    public boolean isAccountLockedByEmail(String email) {
        String sql = "SELECT is_banned, role FROM user WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (Type.ADMIN.name().equals(rs.getString("role"))) {
                    return false;
                }
                return rs.getBoolean("is_banned");
            }
        } catch (SQLException e) {
            System.err.println("Error checking lock status: " + e.getMessage());
        }
        return false;
    }

    // ==================== Helper: Email Exists ====================

    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM user WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) return true;
        } catch (SQLException ex) {
            System.err.println("Error checking email: " + ex.getMessage());
        }
        return false;
    }

    // ==================== Crypto Helpers ====================

    public String cryptPassword(String passwordToCrypt) {
        char[] bcryptChars = BCrypt.with(BCrypt.Version.VERSION_2Y).hashToChar(13, passwordToCrypt.toCharArray());
        return Stream.of(bcryptChars).map(String::valueOf).collect(Collectors.joining(""));
    }

    public boolean verifyPassword(String plainPassword, String encryptedPassword) {
        BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), encryptedPassword);
        return result.verified;
    }

    // ==================== Private Helpers ====================

    private void validateUserFields(User user, boolean checkPassword) throws EmptyFieldException,
            InvalidEmailException, InvalidPhoneNumberException, IncorrectPasswordException {
        if (user.getFirstname() == null || user.getFirstname().isEmpty() ||
                user.getLastname() == null || user.getLastname().isEmpty() ||
                user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new EmptyFieldException("Veuillez remplir tous les champs obligatoires.");
        }
        if (!validationService.isValidEmail(user.getEmail())) {
            throw new InvalidEmailException("Adresse email invalide.");
        }
        if (checkPassword && emailExists(user.getEmail())) {
            throw new InvalidEmailException("Cet email est déjà utilisé.");
        }
        if (user.getPhone() != null && !user.getPhone().isEmpty() && !validationService.isValidPhoneNumber(user.getPhone())) {
            throw new InvalidPhoneNumberException("Numéro de téléphone invalide.");
        }
        if (checkPassword) {
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                throw new EmptyFieldException("Le mot de passe est requis.");
            }
            if (!validationService.isValidPassword(user.getPassword())) {
                throw new IncorrectPasswordException("Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et faire au moins 6 caractères.");
            }
        }
    }

    // ==================== ResultSet Mapping ====================

    /**
     * Maps a ResultSet row to a User object.
     * Public so other services (e.g. ConnectionService) can reuse it.
     */
    public User mapResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setFirstname(rs.getString("firstname"));
        user.setLastname(rs.getString("lastname"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setPassword(rs.getString("password"));
        user.setRole(Type.valueOf(rs.getString("role")));
        user.setBio(rs.getString("bio"));
        user.setProfilePicture(rs.getString("profile_picture"));
        user.setCompanyName(rs.getString("company_name"));
        user.setLinkedinUrl(rs.getString("linkedin_url"));
        user.setAddress(rs.getString("address"));

        Date dob = rs.getDate("date_of_birth");
        user.setDateOfBirth(dob != null ? dob.toLocalDate() : null);

        user.setVerified(rs.getBoolean("verified"));
        user.setPhoneVerified(rs.getBoolean("phone_verified"));
        user.setIsActive(rs.getBoolean("is_active"));
        user.setIsBanned(rs.getBoolean("is_banned"));

        user.setGoogleProviderId(rs.getString("google_provider_id"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        user.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        user.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);

        return user;
    }
}
