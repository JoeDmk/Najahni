package com.najahni.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Contrôleur pour la page Front Office séparée.
 * Gère la navigation entre les vues front-office (Opportunités / Offres)
 * et le retour vers le Back Office.
 */
public class FrontOfficeController {

    @FXML
    private StackPane foContentArea;

    @FXML
    private Button btnFoOpportunities;

    @FXML
    private Button btnFoOffers;

    private Button activeNavLink;

    @FXML
    public void initialize() {
        // Store reference so child controllers can navigate via FrontOfficeController
        foContentArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getRoot().setUserData(this);
            }
        });
        // Show opportunities by default
        showOpportunities();
    }

    /**
     * Shows the front-office Opportunities view.
     */
    @FXML
    public void showOpportunities() {
        loadView("/fxml/FrontOpportunitiesView.fxml");
        setActiveNavLink(btnFoOpportunities);
    }

    /**
     * Shows the front-office Offers view.
     */
    @FXML
    public void showOffers() {
        loadView("/fxml/FrontOffersView.fxml");
        setActiveNavLink(btnFoOffers);
    }

    /**
     * Shows the front-office Offers view with a pre-selected opportunity.
     * Called when user clicks "Investir" on an opportunity card.
     * @param opportunityId The ID of the opportunity to pre-select
     */
    public void showOffers(int opportunityId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FrontOffersView.fxml"));
            Parent view = loader.load();

            // Pre-select the opportunity in the form
            FrontOffersController controller = loader.getController();
            controller.preselectOpportunity(opportunityId);

            animateIn(view);
            setActiveNavLink(btnFoOffers);
        } catch (IOException e) {
            System.err.println("✗ Erreur lors du chargement de FrontOffersView: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Navigates back to the Back Office (MainView).
     */
    @FXML
    public void goBackOffice() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) foContentArea.getScene().getWindow();
            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("NAJAHNI - Plateforme d'Entrepreneuriat et d'Investissement");
        } catch (IOException e) {
            System.err.println("✗ Erreur lors du retour au Back Office: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads a view into the front-office content area with smooth animation.
     * @param fxmlPath Path to the FXML file
     */
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            animateIn(view);
        } catch (IOException e) {
            System.err.println("✗ Erreur lors du chargement de la vue: " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Animates a view into the content area with fade + slide.
     */
    private void animateIn(Parent view) {
        view.setOpacity(0);
        view.setTranslateY(15);

        foContentArea.getChildren().clear();
        foContentArea.getChildren().add(view);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), view);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideUp = new TranslateTransition(Duration.millis(250), view);
        slideUp.setFromY(15);
        slideUp.setToY(0);

        new ParallelTransition(fadeIn, slideUp).play();
    }

    /**
     * Sets the active navigation link style.
     */
    private void setActiveNavLink(Button button) {
        if (activeNavLink != null) {
            activeNavLink.getStyleClass().remove("fo-nav-link-active");
        }
        if (!button.getStyleClass().contains("fo-nav-link-active")) {
            button.getStyleClass().add("fo-nav-link-active");
        }
        activeNavLink = button;
    }
}
