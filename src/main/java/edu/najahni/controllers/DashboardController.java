package edu.najahni.controllers;

import edu.najahni.entities.Projet;
import edu.najahni.services.projetCRUD;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DashboardController implements Initializable {

    @FXML private TableView<Projet> projetsTable;
    @FXML private TableColumn<Projet, String> colTitre;
    @FXML private TableColumn<Projet, String> colDescription;
    @FXML private TableColumn<Projet, String> colSecteur;
    @FXML private TableColumn<Projet, String> colEtape;
    @FXML private TableColumn<Projet, String> colStatut;
    @FXML private TableColumn<Projet, String> colDate;
    @FXML private TableColumn<Projet, Double> colCouts;
    @FXML private TableColumn<Projet, Double> colRevenus;
    @FXML private TableColumn<Projet, Void> actions;
    @FXML private TableColumn<Projet, Integer> colUserId;

    @FXML private ComboBox<String> comboTri;
    @FXML private TextField champRecherche;

    private projetCRUD projetService = new projetCRUD();
    private ObservableList<Projet> projetsList = FXCollections.observableArrayList();
    private ObservableList<Projet> projetsListComplete = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colSecteur.setCellValueFactory(new PropertyValueFactory<>("secteur"));
        colEtape.setCellValueFactory(new PropertyValueFactory<>("etape"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));

        colCouts.setCellValueFactory(cellData -> {
            Projet p = cellData.getValue();
            if (p.getDonneesBusiness() == null) {
                return new SimpleDoubleProperty(0).asObject();
            }
            return new SimpleDoubleProperty(p.getDonneesBusiness().getCoutsEstimes()).asObject();
        });

        colRevenus.setCellValueFactory(cellData -> {
            Projet p = cellData.getValue();
            if (p.getDonneesBusiness() == null) {
                return new SimpleDoubleProperty(0).asObject();
            }
            return new SimpleDoubleProperty(p.getDonneesBusiness().getRevenusAttendus()).asObject();
        });

        actions.setCellFactory(param -> new TableCell<>() {
            private final Button btnModifier = new Button("Modifier");
            private final Button btnSupprimer = new Button("Supprimer");

            {
                String buttonStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8 20; " +
                        "-fx-min-width: 110px; -fx-min-height: 38px; " +
                        "-fx-background-radius: 8; -fx-border-radius: 8;";

                btnModifier.setStyle(buttonStyle + "-fx-background-color: #3498db; -fx-text-fill: white;");
                btnSupprimer.setStyle(buttonStyle + "-fx-background-color: #e74c3c; -fx-text-fill: white;");

                btnModifier.setOnMouseEntered(e ->
                        btnModifier.setStyle(buttonStyle + "-fx-background-color: #2980b9; -fx-cursor: hand;"));
                btnModifier.setOnMouseExited(e ->
                        btnModifier.setStyle(buttonStyle + "-fx-background-color: #3498db; -fx-text-fill: white;"));

                btnSupprimer.setOnMouseEntered(e ->
                        btnSupprimer.setStyle(buttonStyle + "-fx-background-color: #c0392b; -fx-cursor: hand;"));
                btnSupprimer.setOnMouseExited(e ->
                        btnSupprimer.setStyle(buttonStyle + "-fx-background-color: #e74c3c; -fx-text-fill: white;"));

                btnModifier.setOnAction(e -> {
                    Projet projet = getTableView().getItems().get(getIndex());
                    ouvrirModifier(projet);
                });

                btnSupprimer.setOnAction(e -> {
                    Projet projet = getTableView().getItems().get(getIndex());
                    supprimer(projet);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(15, btnModifier, btnSupprimer);
                    hbox.setAlignment(Pos.CENTER);
                    hbox.setStyle("-fx-padding: 4;");
                    setGraphic(hbox);
                }
            }
        });

        // Initialiser le ComboBox de tri
        if (comboTri != null) {
            comboTri.setItems(FXCollections.observableArrayList(
                    "Plus récents d'abord",
                    "Plus anciens d'abord",
                    "Titre (A-Z)",
                    "Titre (Z-A)",
                    "Secteur (A-Z)",
                    "Statut (A-Z)",
                    "User ID (croissant)",
                    "User ID (décroissant)",
                    "Coûts (croissant)",
                    "Coûts (décroissant)",
                    "Revenus (croissant)",
                    "Revenus (décroissant)"
            ));
            comboTri.setValue("Plus récents d'abord");
            comboTri.setOnAction(e -> appliquerTriEtRecherche());
        }

        // Initialiser la recherche
        if (champRecherche != null) {
            champRecherche.textProperty().addListener((obs, oldVal, newVal) -> appliquerTriEtRecherche());
        }

        rafraichir();
    }

    private void appliquerTriEtRecherche() {
        List<Projet> listeFiltree = projetsListComplete;

        // Appliquer la recherche
        if (champRecherche != null && champRecherche.getText() != null && !champRecherche.getText().trim().isEmpty()) {
            String recherche = champRecherche.getText().toLowerCase().trim();
            listeFiltree = listeFiltree.stream()
                    .filter(p ->
                            (p.getTitre() != null && p.getTitre().toLowerCase().contains(recherche)) ||
                                    (p.getDescription() != null && p.getDescription().toLowerCase().contains(recherche)) ||
                                    (p.getSecteur() != null && p.getSecteur().toLowerCase().contains(recherche)) ||
                                    (p.getStatut() != null && p.getStatut().toLowerCase().contains(recherche)) ||
                                    (p.getEtape() != null && p.getEtape().toLowerCase().contains(recherche)) ||
                                    String.valueOf(p.getUserId()).contains(recherche)
                    )
                    .collect(Collectors.toList());
        }

        // Appliquer le tri
        if (comboTri != null && comboTri.getValue() != null) {
            String critere = comboTri.getValue();
            Comparator<Projet> comparator = null;

            switch (critere) {
                case "Plus récents d'abord":
                    comparator = Comparator.comparing(Projet::getDateCreation,
                            Comparator.nullsLast(Comparator.reverseOrder()));
                    break;
                case "Plus anciens d'abord":
                    comparator = Comparator.comparing(Projet::getDateCreation,
                            Comparator.nullsLast(Comparator.naturalOrder()));
                    break;
                case "Titre (A-Z)":
                    comparator = Comparator.comparing(p -> p.getTitre() != null ? p.getTitre() : "",
                            String.CASE_INSENSITIVE_ORDER);
                    break;
                case "Titre (Z-A)":
                    comparator = Comparator.comparing(p -> p.getTitre() != null ? p.getTitre() : "",
                            String.CASE_INSENSITIVE_ORDER.reversed());
                    break;
                case "Secteur (A-Z)":
                    comparator = Comparator.comparing(p -> p.getSecteur() != null ? p.getSecteur() : "",
                            String.CASE_INSENSITIVE_ORDER);
                    break;
                case "Statut (A-Z)":
                    comparator = Comparator.comparing(p -> p.getStatut() != null ? p.getStatut() : "",
                            String.CASE_INSENSITIVE_ORDER);
                    break;
                case "User ID (croissant)":
                    comparator = Comparator.comparing(Projet::getUserId);
                    break;
                case "User ID (décroissant)":
                    comparator = Comparator.comparing(Projet::getUserId, Comparator.reverseOrder());
                    break;
                case "Coûts (croissant)":
                    comparator = Comparator.comparing(p ->
                            p.getDonneesBusiness() != null ? p.getDonneesBusiness().getCoutsEstimes() : 0.0);
                    break;
                case "Coûts (décroissant)":
                    comparator = Comparator.comparing(p ->
                                    p.getDonneesBusiness() != null ? p.getDonneesBusiness().getCoutsEstimes() : 0.0,
                            Comparator.reverseOrder());
                    break;
                case "Revenus (croissant)":
                    comparator = Comparator.comparing(p ->
                            p.getDonneesBusiness() != null ? p.getDonneesBusiness().getRevenusAttendus() : 0.0);
                    break;
                case "Revenus (décroissant)":
                    comparator = Comparator.comparing(p ->
                                    p.getDonneesBusiness() != null ? p.getDonneesBusiness().getRevenusAttendus() : 0.0,
                            Comparator.reverseOrder());
                    break;
            }

            if (comparator != null) {
                listeFiltree = listeFiltree.stream()
                        .sorted(comparator)
                        .collect(Collectors.toList());
            }
        }

        projetsList.setAll(listeFiltree);
        projetsTable.setItems(projetsList);
    }

    @FXML
    private void retourAccueil() {
        try {
            Stage stage = (Stage) projetsTable.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SignIn.fxml"));
            Scene scene = new Scene(loader.load());

            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            stage.setX(screen.getMinX());
            stage.setY(screen.getMinY());
            stage.setWidth(screen.getWidth());
            stage.setHeight(screen.getHeight());

            stage.setTitle("NAJAHNI - Login");
            stage.setScene(scene);
            stage.setMaximized(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void ouvrirAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjoutProjet.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Ajouter un Projet");
            stage.initOwner(projetsTable.getScene().getWindow());

            javafx.application.Platform.runLater(() -> {
                stage.setMaximized(true);
            });

            AjouterProjet controller = loader.getController();
            controller.setStage(stage);

            stage.showAndWait();
            rafraichir();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire d'ajout");
        }
    }

    @FXML
    private void rafraichir() {
        try {
            projetsListComplete.setAll(projetService.afficher());
            appliquerTriEtRecherche();
            System.out.println("✅ " + projetsListComplete.size() + " projets chargés");
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors du chargement des projets :");
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les projets : " + e.getMessage());
        }
    }

    private void supprimer(Projet projet) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer le projet ?");
        confirmation.setContentText("Voulez-vous vraiment supprimer le projet \"" + projet.getTitre() + "\" ?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    projetService.supprimer(projet.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Projet supprimé avec succès");
                    rafraichir();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer le projet : " + e.getMessage());
                }
            }
        });
    }

    private void ouvrirModifier(Projet projet) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjoutProjet.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Modifier le projet");
            stage.initOwner(projetsTable.getScene().getWindow());

            javafx.application.Platform.runLater(() -> {
                stage.setMaximized(true);
            });

            AjouterProjet controller = loader.getController();
            controller.setStage(stage);
            controller.setProjet(projet);

            stage.showAndWait();
            rafraichir();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire de modification");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}