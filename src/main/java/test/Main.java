package test;

import models.User;
import services.UserService;

public class Main {

    public static void main(String[] args) throws Exception {
        // Singleton usage
        UserService userService = UserService.getInstance();

        // **CRUD test:**

        // **add new user**
        // User user = new User("Test", "User", "test@najahni.tn", "29923207", "Test1234", Type.ENTREPRENEUR);
        // userService.addUser(user);

        // **get all users**
        System.out.println("=== All Users ===");
        for (User u : userService.getUsers()) {
            System.out.println(u.getId() + " - " + u.getFullName() + " - " + u.getEmail() + " - " + u.getRole());
        }

        // **count stats**
        System.out.println("\nTotal: " + userService.countTotal());
        System.out.println("Active: " + userService.countActive());
        System.out.println("Banned: " + userService.countBanned());
    }
}
