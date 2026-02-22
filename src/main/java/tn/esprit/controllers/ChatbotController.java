package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Scanner;

public class ChatbotController implements Initializable {

    @FXML
    private Button btnRequests;
    @FXML
    private Button btnSessions;
    @FXML
    private Button btnAvailability;

    @FXML
    private TextArea chatArea;
    @FXML
    private TextField inputField;
    @FXML
    private Button btnSend;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (btnRequests != null)
            btnRequests.setOnAction(e -> navigateTo("/FXML/MentorshipRequestList.fxml"));
        if (btnSessions != null)
            btnSessions.setOnAction(e -> navigateTo("/FXML/MentorshipSessionList.fxml"));
        if (btnAvailability != null)
            btnAvailability.setOnAction(e -> navigateTo("/FXML/MentorAvailabilityList.fxml"));

        btnSend.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());
    }

    @FXML
    private void sendMessage() {
        String query = inputField.getText().trim();
        if (query.isEmpty())
            return;

        chatArea.appendText("You: " + query + "\n");
        inputField.clear();

        // Disable input while waiting for api
        inputField.setDisable(true);
        btnSend.setDisable(true);

        new Thread(() -> {
            String answer = callFastAPI(query);
            javafx.application.Platform.runLater(() -> {
                chatArea.appendText("Bot: " + answer + "\n\n");
                inputField.setDisable(false);
                btnSend.setDisable(false);
                inputField.requestFocus();
            });
        }).start();
    }

    private String callFastAPI(String question) {
        try {
            URL url = new URL("http://localhost:8001/chat");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // simple JSON builder
            String safeQuestion = question.replace("\"", "\\\"");
            String jsonInputString = "{\"question\": \"" + safeQuestion + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                try (Scanner scanner = new Scanner(conn.getInputStream(), "utf-8")) {
                    String response = scanner.useDelimiter("\\A").next();
                    // response is {"answer": "..."}
                    // very naive parsing
                    int start = response.indexOf("\"answer\":\"");
                    if (start != -1) {
                        start += 10;
                        int end = response.lastIndexOf("\"");
                        if (end > start) {
                            String answer = response.substring(start, end);
                            // handle escaped newlines
                            return answer.replace("\\n", "\n").replace("\\\"", "\"");
                        }
                    }
                    return response;
                }
            } else {
                return "Error: Could not reach the chatbot service (code " + code
                        + "). Please ensure Python API is running.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: Exception while trying to connect to the chatbot: " + e.getMessage();
        }
    }

    private void navigateTo(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage) btnSend.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
