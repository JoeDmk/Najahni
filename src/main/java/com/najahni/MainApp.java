package com.najahni;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application class for NAJAHNI - Business & Entrepreneurship Platform.
 * Entry point for the JavaFX Desktop Application.
 */
public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        
        // Charger la vue principale
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        Parent root = loader.load();
        
        // Configurer la scène
        Scene scene = new Scene(root, 1200, 700);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        
        // Configurer la scène
        stage.setTitle("NAJAHNI - Plateforme d'Entrepreneuriat et d'Investissement");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        stage.show();
        
        System.out.println("✓ Application NAJAHNI démarrée avec succès !");
    }

    /**
     * Returns the primary stage.
     * @return Primary stage instance
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Main method - application entry point.
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("  NAJAHNI - Entrepreneuriat & Investissement");
        System.out.println("  Application Bureau JavaFX - PIDEV");
        System.out.println("===========================================");
        launch(args);
    }
}
