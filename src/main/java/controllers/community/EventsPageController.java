package controllers.community;

import models.community.Event;
import services.community.EventCRUD;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class EventsPageController {

    @FXML
    private VBox eventsContainer;

    @FXML
    public void initialize() {
        loadEvents();
    }

    private void loadEvents() {

        eventsContainer.getChildren().clear();

        EventCRUD eventCRUD = new EventCRUD();

        try {

            List<Event> events = eventCRUD.afficher();

            for (Event event : events) {

                VBox card = new VBox();
                card.getStyleClass().add("card");

                Label title = new Label(event.getTitle());
                title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");

                Label date = new Label(
                        "📅 " + event.getEventDate().toLocalDateTime().toLocalDate()
                );

                card.getChildren().addAll(title, date);


                // CLICK → OPEN DETAILS
                card.setOnMouseClicked(e -> openEventDetails(event));

                eventsContainer.getChildren().add(card);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void openEventDetails(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/community/EventDetailsPage.fxml"));
            Parent root = loader.load();

            EventDetailsController controller = loader.getController();
            controller.setEvent(event);

            eventsContainer.getScene().setRoot(root);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @FXML
    private void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/community/CommunityHomePage.fxml"));
            eventsContainer.getScene().setRoot(root);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @FXML
    private void goToAddEvent() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/community/AddEvent.fxml"));
            eventsContainer.getScene().setRoot(root);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @FXML
    private void handleGoBack() {
        goBack();
    }

    @FXML
    private void handleGoHome() {
        tools.NavigationHelper.goHome(eventsContainer);
    }
}
