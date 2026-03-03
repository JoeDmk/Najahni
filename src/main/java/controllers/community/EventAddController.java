    package controllers.community;

    import java.io.IOException;
    import java.sql.Timestamp;

    import models.community.Event;
    import models.community.EventParticipant;
    import services.SessionService;
    import services.community.EventCRUD;
    import services.community.EventParticipantCRUD;
    import javafx.event.ActionEvent;
    import javafx.fxml.FXML;
    import javafx.fxml.FXMLLoader;
    import javafx.scene.Parent;
    import javafx.scene.control.*;

    import java.sql.SQLException;
    import java.time.LocalDate;
    import java.time.LocalDateTime;

    public class EventAddController {

        private final int currentUserId = SessionService.getInstance().getCurrentUser().getId();
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
        @FXML private Label weatherPreviewLabel;

        private final services.community.WeatherService weatherService = new services.community.WeatherService();
        private static final double DEFAULT_LAT = 36.8065;
        private static final double DEFAULT_LON = 10.1815;

        @FXML
        public void initialize() {
            eventdate.valueProperty().addListener((obs, oldV, newV) -> {
                if (newV == null) return;
                previewWeather(newV);
            });
        }

        private void previewWeather(java.time.LocalDate d) {
            weatherPreviewLabel.setText("🌤 Loading weather...");

            if (d.isAfter(java.time.LocalDate.now().plusDays(16))) {
                weatherPreviewLabel.setText("🌤 Weather: not available yet (max 16 days ahead)");
                return;
            }

            services.community.EmailAsync.run(() -> {
                try {
                    var info = weatherService.getDaily(d, DEFAULT_LAT, DEFAULT_LON);
                    String label = services.community.WeatherService.labelFromCode(info.weatherCode);

                    String txt = String.format(
                            "Weather: %s | %.0f°-%.0f° | Rain %d%%",
                            label, info.tMin, info.tMax, info.rainProbMax
                    );

                    javafx.application.Platform.runLater(() -> weatherPreviewLabel.setText(txt));
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() ->
                            weatherPreviewLabel.setText("Weather: unavailable"));
                }
            });
        }

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
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/community/EventsPage.fxml"));
                Parent root = loader.load();
                createevent.getScene().setRoot(root);


                // Clear fields
                eventtitle.clear();
                eventdescription.clear();
                eventdate.setValue(null);
                eventcapacity.clear();

            } catch (SQLException ex) {

                if ("EVENT_TITLE_EXISTS".equals(ex.getMessage())) {
                    statusLabel.setText("Event title already exists.");
                    return;
                }

                statusLabel.setText("Database error");
                ex.printStackTrace();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        @FXML
        private void goBack() {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/community/EventsPage.fxml"));
                Parent root = loader.load();
                createevent.getScene().setRoot(root);
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
            tools.NavigationHelper.goHome(createevent);
        }


    }
