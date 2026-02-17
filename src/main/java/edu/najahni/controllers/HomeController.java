package edu.najahni.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXML;

public class HomeController {

    @FXML
    private void ouvrirAdmin() {
        ouvrir("/DashboardProjets.fxml", "Dashboard Admin - Najahni");
    }

    @FXML
    private void ouvrirClient() {
        ouvrir("/ClientDashboard.fxml", "Espace Client - Najahni");
    }

    private void ouvrir(String fxml, String title) {
        try {
            Stage stage = (Stage) javafx.stage.Stage.getWindows().filtered(w -> w.isShowing()).get(0);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Scene scene = new Scene(loader.load(), 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            stage.setTitle(title);
            stage.setScene(scene);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
