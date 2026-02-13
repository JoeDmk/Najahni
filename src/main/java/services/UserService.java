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

        String request = "INSERT INTO user (firstname, lastname, email, phone, password, role, bio, profile_picture, " +
                "company_name, linkedin_url, address, date_of_birth, verified, is_active, is_banned, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(request, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFirstname());
            ps.setString(2, user.getLastname());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhone());
            ps.setString(5, cryptPassword(user.getPassword()));
            ps.setString(6, user.getRole().name());
            ps.setString(7, user.getBio());
            ps.setString(8, user.getProfilePicture());
            ps.setString(9, user.getCompanyName());
            ps.setString(10, user.getLinkedinUrl());
            ps.setString(11, user.getAddress());
            ps.setDate(12, user.getDateOfBirth() != null ? Date.valueOf(user.getDateOfBirth()) : null);
            ps.setBoolean(13, false); // not verified yet
            ps.setBoolean(14, true);  // active
            ps.setBoolean(15, false); // not banned
            ps.setTimestamp(16, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(17, Timestamp.valueOf(LocalDateTime.now()));

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

        String request = "UPDATE user SET firstname=?, lastname=?, email=?, phone=?, role=?, bio=?, profile_picture=?, " +
                "company_name=?, linkedin_url=?, address=?, date_of_birth=?, is_banned=?, is_active=?, updated_at=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(request)) {
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
            if (rows > 0) {
                System.out.println("User updated successfully!");
            } else {
                throw new UserNotFoundException("Aucun utilisateur trouvé avec l'ID: " + user.getId());
            }
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

        String request = "UPDATE user SET firstname=?, lastname=?, email=?, phone=?, bio=?, profile_picture=?, " +
                "company_name=?, linkedin_url=?, address=?, date_of_birth=?, updated_at=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(request)) {
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
        String request = "DELETE FROM user WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(request)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("User deleted successfully!");
        } catch (SQLException ex) {
            System.err.println("Error deleting user: " + ex.getMessage());
        }
    }

    @Override
    public List<User> getUsers() {
        List<User> users = new ArrayList<>();
        try {
            String request = "SELECT * FROM user ORDER BY created_at DESC";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(request);
            while (rs.next()) {
                users.add(createUserFromResultSet(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error retrieving users: " + ex.getMessage());
        }
        return users;
    }

    public List<User> getUsersByRole(Type role) {
        List<User> users = new ArrayList<>();
        try {
            String request = "SELECT * FROM user WHERE role = ? ORDER BY created_at DESC";
            PreparedStatement ps = connection.prepareStatement(request);
            ps.setString(1, role.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(createUserFromResultSet(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error retrieving users by role: " + ex.getMessage());
        }
        return users;
    }

    @Override
    public User getUserbyID(int id) throws UserNotFoundException {
        try {
            String query = "SELECT * FROM user WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return createUserFromResultSet(rs);
            }
        } catch (SQLException ex) {
            System.err.println("Error retrieving user by ID: " + ex.getMessage());
        }
        throw new UserNotFoundException("Aucun utilisateur trouvé avec l'ID: " + id);
    }

    @Override
    public User getUserbyEmail(String email) throws UserNotFoundException {
        try {
            String query = "SELECT * FROM user WHERE email = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return createUserFromResultSet(rs);
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

        String request = "UPDATE user SET password = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(request)) {
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

    public void updateEmail(int userId, String newEmail) throws UserNotFoundException, InvalidEmailException {
        if (!validationService.isValidEmail(newEmail)) {
            throw new InvalidEmailException("L'adresse email est invalide.");
        }
        if (isEmailExists(newEmail)) {
            throw new InvalidEmailException("Cet email est déjà utilisé.");
        }

        String request = "UPDATE user SET email = ?, verified = false, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(request)) {
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
        String request = "UPDATE user SET verified = true, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(request)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new UserNotFoundException("Aucun utilisateur trouvé avec l'ID: " + userId);
            }
        } catch (SQLException ex) {
            System.err.println("Error verifying user: " + ex.getMessage());
        }
    }

    // ==================== Ban/Unban ====================

    public void banUser(int userId) throws PermissionException, UserNotFoundException {
        User user = getUserbyID(userId);
        if (user.getRole() == Type.ADMIN) {
            throw new PermissionException("Impossible de bannir un administrateur.");
        }

        String query = "UPDATE user SET is_banned = true, is_active = false, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, userId);
            ps.executeUpdate();
            System.out.println("User " + userId + " has been banned.");
        } catch (SQLException ex) {
            System.err.println("Error banning user: " + ex.getMessage());
        }
    }

    public void unbanUser(int userId) throws PermissionException, UserNotFoundException {
        getUserbyID(userId); // verify exists

        String query = "UPDATE user SET is_banned = false, is_active = true, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, userId);
            ps.executeUpdate();
            System.out.println("User " + userId + " has been unbanned.");
        } catch (SQLException ex) {
            System.err.println("Error unbanning user: " + ex.getMessage());
        }
    }

    // ==================== Statistics ====================

    public int countByRole(Type role) {
        String query = "SELECT COUNT(*) FROM user WHERE role = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, role.name());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("Error counting by role: " + ex.getMessage());
        }
        return 0;
    }

    public int countActive() {
        String query = "SELECT COUNT(*) FROM user WHERE is_active = true";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("Error counting active users: " + ex.getMessage());
        }
        return 0;
    }

    public int countBanned() {
        String query = "SELECT COUNT(*) FROM user WHERE is_banned = true";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("Error counting banned users: " + ex.getMessage());
        }
        return 0;
    }

    public int countTotal() {
        String query = "SELECT COUNT(*) FROM user";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
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
        String query = "SELECT * FROM user WHERE firstname LIKE ? OR lastname LIKE ? OR email LIKE ? OR company_name LIKE ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(createUserFromResultSet(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error searching users: " + ex.getMessage());
        }
        return users;
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

    private boolean isEmailExists(String email) {
        String query = "SELECT COUNT(*) FROM user WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) return true;
        } catch (SQLException ex) {
            System.err.println("Error checking email: " + ex.getMessage());
        }
        return false;
    }

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
        if (checkPassword && isEmailExists(user.getEmail())) {
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

    public User createUserFromResultSet(ResultSet rs) throws SQLException {
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
        user.setIsActive(rs.getBoolean("is_active"));
        user.setIsBanned(rs.getBoolean("is_banned"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        user.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        user.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);

        return user;
    }
}
