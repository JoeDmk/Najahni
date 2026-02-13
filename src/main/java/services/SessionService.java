package services;

import exceptions.*;
import models.User;
import tools.MyConnection;
import util.Type;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class SessionService {
    private Connection connection = MyConnection.getInstance().getConnection();
    private Map<String, Integer> loginAttempts = new HashMap<>();
    public final int MAX_LOGIN_ATTEMPTS = 3;

    private static SessionService instance;
    private static User currentUser;

    private SessionService() {
    }

    public static SessionService getInstance() {
        if (instance == null) {
            instance = new SessionService();
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        currentUser = user;
    }

    public boolean login(String email, String password) throws EmptyFieldException, InvalidEmailException,
            IncorrectPasswordException, UserNotFoundException, AccountLockedException, AccountBannedException {

        UserService userService = UserService.getInstance();
        ValidationService validationService = new ValidationService();

        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            throw new EmptyFieldException("Veuillez saisir votre email et votre mot de passe.");
        }
        if (!validationService.isValidEmail(email)) {
            throw new InvalidEmailException("Email invalide.");
        }
        if (isAccountLocked(email)) {
            throw new AccountLockedException("Compte verrouillé. Veuillez réinitialiser votre mot de passe.");
        }

        User user = userService.getUserbyEmail(email);
        if (user.getIsBanned()) {
            throw new AccountBannedException("Votre compte a été banni. Contactez l'administrateur.");
        }

        if (userService.verifyPassword(password, user.getPassword())) {
            currentUser = user;
            loginAttempts.remove(email);
            return true;
        } else {
            // Track failed attempts
            int attempts = loginAttempts.getOrDefault(email, 0) + 1;
            loginAttempts.put(email, attempts);

            if (attempts >= MAX_LOGIN_ATTEMPTS) {
                lockAccount(email);
                throw new AccountLockedException("Trop de tentatives échouées. Compte verrouillé.");
            }

            throw new IncorrectPasswordException("Mot de passe incorrect. Tentatives restantes: " + (MAX_LOGIN_ATTEMPTS - attempts));
        }
    }

    public void logout() {
        currentUser = null;
        SessionManager.clearSession();
    }

    public void lockAccount(String email) {
        // Don't lock admin accounts
        String sql = "SELECT role FROM user WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                if (!Type.ADMIN.name().equals(role)) {
                    String updateSql = "UPDATE user SET is_active = false, is_banned = true WHERE email = ?";
                    try (PreparedStatement ups = connection.prepareStatement(updateSql)) {
                        ups.setString(1, email);
                        ups.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error locking account: " + e.getMessage());
        }
    }

    public void unlockAccount(String email) {
        String sql = "UPDATE user SET is_active = true, is_banned = false WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
            loginAttempts.remove(email);
            System.out.println("Account unlocked: " + email);
        } catch (SQLException e) {
            System.err.println("Error unlocking account: " + e.getMessage());
        }
    }

    public boolean isAccountLocked(String email) {
        String sql = "SELECT is_banned, role FROM user WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (Type.ADMIN.name().equals(rs.getString("role"))) {
                    return false; // Admin can never be locked
                }
                return rs.getBoolean("is_banned");
            }
        } catch (SQLException e) {
            System.err.println("Error checking lock status: " + e.getMessage());
        }
        return false;
    }
}
