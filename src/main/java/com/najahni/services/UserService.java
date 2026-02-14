package com.najahni.services;

import com.najahni.dao.UserDAO;
import com.najahni.models.Role;
import com.najahni.models.User;

import java.util.List;
import java.util.Optional;

/**
 * Service class for User business logic.
 * Handles validation and business rules for user operations.
 */
public class UserService {

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Creates a new user after validation.
     * @param user The user to create
     * @return The created user
     * @throws IllegalArgumentException if validation fails
     */
    public User createUser(User user) throws IllegalArgumentException {
        validateUser(user);
        
        // Check if email already exists
        if (userDAO.emailExists(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }
        
        return userDAO.create(user);
    }

    /**
     * Updates an existing user after validation.
     * @param user The user to update
     * @return true if update was successful
     * @throws IllegalArgumentException if validation fails
     */
    public boolean updateUser(User user) throws IllegalArgumentException {
        validateUser(user);
        
        // Check if email exists for another user
        if (userDAO.emailExistsForOtherUser(user.getEmail(), user.getId())) {
            throw new IllegalArgumentException("Email already exists for another user: " + user.getEmail());
        }
        
        return userDAO.update(user);
    }

    /**
     * Deletes a user by ID.
     * @param id The user ID
     * @return true if deletion was successful
     */
    public boolean deleteUser(int id) {
        return userDAO.delete(id);
    }

    /**
     * Finds a user by ID.
     * @param id The user ID
     * @return Optional containing the user if found
     */
    public Optional<User> findById(int id) {
        return userDAO.findById(id);
    }

    /**
     * Finds a user by email.
     * @param email The email address
     * @return Optional containing the user if found
     */
    public Optional<User> findByEmail(String email) {
        return userDAO.findByEmail(email);
    }

    /**
     * Returns all users.
     * @return List of all users
     */
    public List<User> findAll() {
        return userDAO.findAll();
    }

    /**
     * Returns all users with a specific role.
     * @param role The role to filter by
     * @return List of users
     */
    public List<User> findByRole(Role role) {
        return userDAO.findByRole(role);
    }

    /**
     * Returns all entrepreneurs.
     * @return List of entrepreneur users
     */
    public List<User> getAllEntrepreneurs() {
        return userDAO.findByRole(Role.ENTREPRENEUR);
    }

    /**
     * Returns all investors.
     * @return List of investor users
     */
    public List<User> getAllInvestors() {
        return userDAO.findByRole(Role.INVESTOR);
    }

    /**
     * Validates user data.
     * @param user The user to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateUser(User user) throws IllegalArgumentException {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        // Validate name
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (user.getName().length() < 2) {
            throw new IllegalArgumentException("Name must be at least 2 characters");
        }
        if (user.getName().length() > 100) {
            throw new IllegalArgumentException("Name cannot exceed 100 characters");
        }
        
        // Validate email
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!isValidEmail(user.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (user.getEmail().length() > 150) {
            throw new IllegalArgumentException("Email cannot exceed 150 characters");
        }
        
        // Validate password
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (user.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        
        // Validate role
        if (user.getRole() == null) {
            throw new IllegalArgumentException("Role is required");
        }
    }

    /**
     * Validates email format using regex.
     * @param email The email to validate
     * @return true if email format is valid
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}
