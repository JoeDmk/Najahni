package services;

import javafx.scene.Scene;

/**
 * Service for managing dark/light theme toggle.
 * Persists theme preference in user record and applies CSS dynamically.
 */
public class ThemeService {

    private static ThemeService instance;
    private boolean darkMode = false;

    private static final String DARK_THEME_CSS = "/views/dark-theme.css";
    private final String cachedDarkCssUrl; // Cache the resolved URL once

    private ThemeService() {
        java.net.URL url = getClass().getResource(DARK_THEME_CSS);
        cachedDarkCssUrl = url != null ? url.toExternalForm() : null;
        if (cachedDarkCssUrl == null) {
            System.err.println("Dark theme CSS not found!");
        }
    }

    public static ThemeService getInstance() {
        if (instance == null) {
            instance = new ThemeService();
        }
        return instance;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    /**
     * Toggle theme and apply to scene.
     */
    public void toggleTheme(Scene scene) {
        darkMode = !darkMode;
        applyTheme(scene);
    }

    /**
     * Apply current theme to a scene.
     */
    public void applyTheme(Scene scene) {
        if (scene == null || cachedDarkCssUrl == null) return;

        if (darkMode) {
            if (!scene.getStylesheets().contains(cachedDarkCssUrl)) {
                scene.getStylesheets().add(cachedDarkCssUrl);
            }
        } else {
            scene.getStylesheets().remove(cachedDarkCssUrl);
        }
    }

    /**
     * Load theme preference from user record.
     */
    public void loadPreference(String theme) {
        darkMode = "dark".equalsIgnoreCase(theme);
    }

    /**
     * Get the current theme name for persistence.
     */
    public String getThemeName() {
        return darkMode ? "dark" : "light";
    }
}
