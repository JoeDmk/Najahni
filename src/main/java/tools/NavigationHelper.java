package tools;

import controllers.AdminBackOfficeController;
import controllers.FrontOfficeShellController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import models.User;
import services.SessionService;
import util.Type;

/**
 * Centralized navigation helper that routes users to the correct home view
 * based on their role. Admin users go to AdminBackOffice (sidebar dashboard),
 * while all other users go to the front-office FrontOffice shell.
 */
public class NavigationHelper {

    /**
     * Navigate to the appropriate home page based on user role.
     * ADMIN → AdminBackOffice.fxml (sidebar back-office)
     * Others → FrontOffice.fxml (front-office shell with sidebar)
     *
     * @param sourceNode any node currently in the scene (used to get Stage)
     * @param user       the current user (if null, tries SessionService)
     */
    public static void goHome(Node sourceNode, User user) {
        if (user == null) {
            user = SessionService.getInstance().getCurrentUser();
        }
        if (user == null) {
            System.err.println("NavigationHelper.goHome: No user available");
            return;
        }

        try {
            FXMLLoader loader;
            if (user.getRole() == Type.ADMIN) {
                loader = new FXMLLoader(NavigationHelper.class.getResource("/views/AdminBackOffice.fxml"));
                Parent root = loader.load();
                AdminBackOfficeController ctrl = loader.getController();
                ctrl.setCurrentUser(user);
                SceneHelper.switchScene(SceneHelper.stageOf(sourceNode), root);
            } else {
                loader = new FXMLLoader(NavigationHelper.class.getResource("/views/FrontOffice.fxml"));
                Parent root = loader.load();
                FrontOfficeShellController ctrl = loader.getController();
                ctrl.setCurrentUser(user);
                SceneHelper.switchScene(SceneHelper.stageOf(sourceNode), root);
            }
        } catch (Exception e) {
            System.err.println("NavigationHelper.goHome error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Convenience overload that uses the current session user.
     */
    public static void goHome(Node sourceNode) {
        goHome(sourceNode, null);
    }
}
