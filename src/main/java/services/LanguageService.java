package services;

import java.util.*;

/**
 * Service for multi-language (i18n) support.
 * Supports French, English, and Arabic.
 */
public class LanguageService {

    private static LanguageService instance;
    private Locale currentLocale = Locale.FRENCH;
    private ResourceBundle bundle;

    private static final String BUNDLE_NAME = "i18n.messages";

    private LanguageService() {
        loadBundle();
    }

    public static LanguageService getInstance() {
        if (instance == null) {
            instance = new LanguageService();
        }
        return instance;
    }

    /**
     * Set language and reload bundle.
     */
    public void setLanguage(String langCode) {
        switch (langCode.toLowerCase()) {
            case "en" -> currentLocale = Locale.ENGLISH;
            case "ar" -> currentLocale = new Locale("ar");
            default -> currentLocale = Locale.FRENCH;
        }
        loadBundle();
    }

    /**
     * Get translated string by key.
     */
    public String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key; // Return key itself if not found
        }
    }

    /**
     * Get translated string with format parameters.
     */
    public String get(String key, Object... params) {
        try {
            return String.format(bundle.getString(key), params);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    public Locale getCurrentLocale() {
        return currentLocale;
    }

    public String getCurrentLanguageCode() {
        return currentLocale.getLanguage();
    }

    public String getCurrentLanguageName() {
        return switch (currentLocale.getLanguage()) {
            case "en" -> "English";
            case "ar" -> "\u0627\u0644\u0639\u0631\u0628\u064A\u0629";
            default -> "Français";
        };
    }

    /**
     * Get available languages.
     */
    public Map<String, String> getAvailableLanguages() {
        Map<String, String> languages = new LinkedHashMap<>();
        languages.put("fr", "Français");
        languages.put("en", "English");
        languages.put("ar", "\u0627\u0644\u0639\u0631\u0628\u064A\u0629");
        return languages;
    }

    private void loadBundle() {
        try {
            bundle = ResourceBundle.getBundle(BUNDLE_NAME, currentLocale);
        } catch (MissingResourceException e) {
            System.err.println("Language bundle not found for locale: " + currentLocale + ". Falling back to French.");
            currentLocale = Locale.FRENCH;
            try {
                bundle = ResourceBundle.getBundle(BUNDLE_NAME, currentLocale);
            } catch (MissingResourceException ex) {
                System.err.println("Default language bundle not found. Using empty bundle.");
                bundle = new ResourceBundle() {
                    @Override
                    protected Object handleGetObject(String key) { return key; }
                    @Override
                    public Enumeration<String> getKeys() { return Collections.emptyEnumeration(); }
                };
            }
        }
    }
}
