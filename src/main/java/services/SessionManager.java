package services;

import exceptions.UserNotFoundException;
import models.User;

import java.io.*;
import java.time.LocalDateTime;

public class SessionManager {

    private static final String SESSION_FILE = "session.txt";

    private static void ensureSessionFileExists() {
        File file = new File(SESSION_FILE);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveSession(String email, String role) {
        ensureSessionFileExists();
        try (FileWriter writer = new FileWriter(SESSION_FILE)) {
            writer.write(email + "," + role + "," + LocalDateTime.now());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String[] loadSession() {
        ensureSessionFileExists();
        try (BufferedReader reader = new BufferedReader(new FileReader(SESSION_FILE))) {
            String line = reader.readLine();
            if (line != null && !line.isEmpty()) {
                return line.split(",");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void clearSession() {
        ensureSessionFileExists();
        try (FileWriter writer = new FileWriter(SESSION_FILE)) {
            writer.write("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static User getCurrentUser() {
        String[] session = loadSession();
        if (session != null && session.length == 3) {
            String email = session[0];
            LocalDateTime lastLogin = LocalDateTime.parse(session[2]);

            if (lastLogin.isAfter(LocalDateTime.now().minusHours(24))) {
                try {
                    return UserService.getInstance().getUserbyEmail(email);
                } catch (UserNotFoundException e) {
                    // User not found, session invalid
                }
            }
        }
        return null;
    }
}
