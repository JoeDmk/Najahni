package edu.najahni.gui;

import edu.najahni.controllers.AjouterProjet;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Najahni extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjoutProjet.fxml"));
        Scene scene = new Scene(loader.load(), 700, 900);

        // Récupère le controller pour lui passer la stage (pour fermer la fenêtre)
        AjouterProjet controller = loader.getController();


        stage.setTitle("Ajouter un Projet - Najahni");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}