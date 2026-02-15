package Controller;

import java.io.IOException;
import java.sql.Timestamp;

import Entites.Event;
import Entites.EventParticipant;
import Services.EventCRUD;
import Services.EventParticipantCRUD;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EventAddController {

    // TODO: Replace with SessionService.getInstance().getCurrentUser().getId()
    // Temporary logged user for testing (integration with User module not done yet)
    private final int currentUserId = 1;
    @FXML
    private TextField eventcapacity;

    @FXML
    private Button createevent;

    @FXML
    private TextField eventtitle;

    @FXML
    private DatePicker eventdate;   // ✅ CORRECT TYPE

    @FXML
    private TextArea eventdescription; // ✅ CORRECT TYPE

    @FXML
    private Label statusLabel;


    @FXML
    void addEvent(ActionEvent event) {

        if (eventtitle.getText().isEmpty() ||
                eventdescription.getText().isEmpty() ||
                eventdate.getValue() == null ||
                eventcapacity.getText().isEmpty()) {

            statusLabel.setText("Please fill all fields");
            return;
        }

        int capacity;

        try {
            capacity = Integer.parseInt(eventcapacity.getText());
            if (capacity <= 0) {
                statusLabel.setText("Capacity must be > 0");
                return;
            }
        } catch (NumberFormatException ex) {
            statusLabel.setText("Capacity must be a number");
            return;
        }

        LocalDate date = eventdate.getValue();
        LocalDateTime dateTime = date.atStartOfDay();
        Timestamp ts = Timestamp.valueOf(dateTime);

        Event e = new Event(
                0, // id (will be generated)
                eventtitle.getText(),
                eventdescription.getText(),
                ts,
                null, // createdAt (DB default)
                capacity,currentUserId
        );

        try {
            new EventCRUD().ajouter(e);
            EventParticipantCRUD epCrud = new EventParticipantCRUD();
            epCrud.ajouter(new EventParticipant(e.getId(), currentUserId));

            // redirect to Events page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/EventsPage.fxml"));
            Parent root = loader.load();
            createevent.getScene().setRoot(root);


            // Clear fields
            eventtitle.clear();
            eventdescription.clear();
            eventdate.setValue(null);
            eventcapacity.clear();

        } catch (SQLException ex) {
            statusLabel.setText("Database error");
            ex.printStackTrace();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/EventsPage.fxml"));
            Parent root = loader.load();
            createevent.getScene().setRoot(root);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


}
