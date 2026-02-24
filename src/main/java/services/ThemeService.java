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

    private ThemeService() {}

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
        if (scene == null) return;

        String darkCss = getClass().getResource(DARK_THEME_CSS) != null
                ? getClass().getResource(DARK_THEME_CSS).toExternalForm()
                : null;

        if (darkCss == null) {
            System.err.println("Dark theme CSS not found!");
            return;
        }

        if (darkMode) {
            if (!scene.getStylesheets().contains(darkCss)) {
                scene.getStylesheets().add(darkCss);
            }
        } else {
            scene.getStylesheets().remove(darkCss);
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
