package com.najahni.services;

import com.najahni.models.User;

/**
 * Gestionnaire de session utilisateur (Singleton).
 * Stocke l'utilisateur connecté pour toute la durée de l'application.
 */
public final class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /** Connecte l'utilisateur. */
    public void login(User user) {
        this.currentUser = user;
    }

    /** Déconnecte l'utilisateur. */
    public void logout() {
        this.currentUser = null;
    }

    /** Retourne l'utilisateur connecté, ou null. */
    public User getCurrentUser() {
        return currentUser;
    }

    /** Vérifie si un utilisateur est connecté. */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /** Retourne l'ID de l'utilisateur connecté, ou -1. */
    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }
}
