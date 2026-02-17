package services;

import exceptions.*;
import models.User;

import java.util.HashMap;
import java.util.Map;


public class SessionService {

    private UserService userService = UserService.getInstance();
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
        userService.lockAccountByEmail(email);
    }

    public void unlockAccount(String email) {
        userService.unlockAccountByEmail(email);
        loginAttempts.remove(email);
    }

    public boolean isAccountLocked(String email) {
        return userService.isAccountLockedByEmail(email);
    }
}
