package Controller;

import Services.EmailService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

public class CommunityHomeController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private void goToGroups() {
        loadPage("/GroupsPage.fxml");
    }

    @FXML
    private void goToPosts() {
        loadPage("/PostsPage.fxml");
    }

    @FXML
    private void goToEvents() {
        loadPage("/EventsPage.fxml");
    }

    private void loadPage(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            rootPane.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    public void initialize() {

        rootPane.setOpacity(0);

        javafx.animation.FadeTransition ft =
                new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(800),
                        rootPane
                );
        ft.setToValue(1);
        ft.play();
    }

}
