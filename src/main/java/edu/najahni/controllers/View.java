package edu.najahni.controllers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class View extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SignIn.fxml"));

            // CORRECTION : Pas de dimensions fixes !
            Scene scene = new Scene(loader.load());

            primaryStage.setTitle("NAJAHNI - Login");
            primaryStage.setScene(scene);

            // Maximiser la fenêtre
            primaryStage.setMaximized(true);

            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Ces méthodes ne sont plus utilisées (tout se fait via SignInController)
    // Mais on les garde corrigées au cas où

    private void ouvrirAdminDashboard(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DashboardProjets.fxml"));

            // CORRECTION : Pas de dimensions fixes !
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            stage.setTitle("Dashboard Admin - NAJAHNI");
            stage.setScene(scene);

            // Maximiser APRÈS avoir changé la scène
            javafx.application.Platform.runLater(() -> {
                stage.setMaximized(true);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ouvrirClientDashboard(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientDashboard.fxml"));

            // CORRECTION : Pas de dimensions fixes !
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            stage.setTitle("Espace Client - NAJAHNI");
            stage.setScene(scene);

            // Maximiser APRÈS avoir changé la scène
            javafx.application.Platform.runLater(() -> {
                stage.setMaximized(true);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}