package com.najahni.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.najahni.services.SessionManager;
import com.najahni.utils.AlertUtils;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Main controller for the application.
 * Handles navigation between different views with smooth animations.
 */
public class MainController {

    private static final Logger LOG = Logger.getLogger(MainController.class.getName());

    @FXML
    private StackPane contentArea;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnUsers;

    @FXML
    private Button btnProjects;

    @FXML
    private Button btnOpportunities;

    @FXML
    private Button btnOffers;

    @FXML
    private Button btnCours;

    @FXML
    private Button btnApprentissage;

    @FXML
    private Button btnEconomicDashboard;

    private Button activeButton;

    /**
     * Initializes the controller.
     * Called automatically after FXML loading.
     */
    @FXML
    public void initialize() {
        // Store reference so child controllers can navigate via MainController
        if (contentArea.getScene() != null) {
            contentArea.getScene().getRoot().setUserData(this);
        }
        contentArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getRoot().setUserData(this);
            }
        });
        // Show dashboard by default
        showDashboard();
    }

    /**
     * Shows the Dashboard view.
     */
    @FXML
    public void showDashboard() {
        loadView("/fxml/DashboardView.fxml");
        setActiveButton(btnDashboard);
    }

    /**
     * Shows the Users management view.
     */
    @FXML
    public void showUsers() {
        loadView("/fxml/UserView.fxml");
        setActiveButton(btnUsers);
    }

    /**
     * Shows the Projects management view.
     */
    @FXML
    public void showProjects() {
        loadView("/fxml/ProjectView.fxml");
        setActiveButton(btnProjects);
    }

    /**
     * Shows the Investment Opportunities management view.
     */
    @FXML
    public void showOpportunities() {
        loadView("/fxml/InvestmentView.fxml");
        setActiveButton(btnOpportunities);
    }

    /**
     * Shows the Investment Offers management view.
     */
    @FXML
    public void showOffers() {
        loadView("/fxml/InvestmentOfferView.fxml");
        setActiveButton(btnOffers);
    }

    /**
     * Shows the Cours management view.
     */
    @FXML
    public void showCours() {
        loadView("/fxml/CoursView.fxml");
        setActiveButton(btnCours);
    }

    /**
     * Shows the Apprentissage dashboard view.
     */
    @FXML
    public void showApprentissage() {
        loadView("/fxml/ApprentissageView.fxml");
        setActiveButton(btnApprentissage);
    }

    /**
     * Shows the Economic Dashboard IA view.
     */
    @FXML
    public void showEconomicDashboard() {
        loadView("/fxml/EconomicDashboardView.fxml");
        setActiveButton(btnEconomicDashboard);
    }

    /**
     * Opens the Front Office as a completely separate page.
     * Replaces the current scene with the front-office layout.
     */
    @FXML
    public void openFrontOffice() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FrontOfficeView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) contentArea.getScene().getWindow();
            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("NAJAHNI - Espace Investisseur");
        } catch (IOException e) {
            LOG.warning("Erreur ouverture Front Office: " + e.getMessage());
            AlertUtils.showError("Erreur", "Impossible d'ouvrir le Front Office.");
        }
    }

    /**
     * Loads a view into the content area with smooth fade and slide animation.
     * @param fxmlPath Path to the FXML file
     */
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            
            // Setup initial state for animation
            view.setOpacity(0);
            view.setTranslateY(20);
            
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            
            // Create fade in animation
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), view);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            
            // Create slide up animation
            TranslateTransition slideUp = new TranslateTransition(Duration.millis(300), view);
            slideUp.setFromY(20);
            slideUp.setToY(0);
            
            // Play animations together
            ParallelTransition transition = new ParallelTransition(fadeIn, slideUp);
            transition.play();
            
        } catch (IOException e) {
            LOG.warning("Erreur chargement vue: " + fxmlPath + " - " + e.getMessage());
        }
    }

    /**
     * Sets the active navigation button with animation.
     * @param button The button to set as active
     */
    private void setActiveButton(Button button) {
        // Remove active style from previous button
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button-active");
        }
        
        // Add active style to new button
        button.getStyleClass().add("nav-button-active");
        activeButton = button;
    }

    /**
     * Déconnecte l'utilisateur et retourne à la page de connexion.
     */
    @FXML
    public void handleLogout() {
        if (AlertUtils.showConfirmation("Déconnexion", "Êtes-vous sûr de vouloir vous déconnecter ?")) {
            SessionManager.getInstance().logout();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) contentArea.getScene().getWindow();
                Scene scene = new Scene(root, 900, 600);
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                stage.setTitle("NAJAHNI \u2014 Connexion");
                stage.setScene(scene);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
