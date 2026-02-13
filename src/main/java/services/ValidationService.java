package services;

public class ValidationService {

    public boolean isValidPhoneNumber(String phoneNumber) {
        // Accepts 8-digit phone numbers (Tunisia) or international format
        String phoneRegex = "^(\\+\\d{1,3})?\\d{8,15}$";
        return phoneNumber.matches(phoneRegex);
    }

    public boolean isValidPassword(String password) {
        // At least 6 chars, 1 uppercase, 1 lowercase, 1 digit
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,}$";
        return password.matches(passwordRegex);
    }

    public boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    public boolean isValidLinkedinUrl(String url) {
        if (url == null || url.isEmpty()) return true; // optional field
        return url.matches("^https?://(www\\.)?linkedin\\.com/.*$");
    }
}
