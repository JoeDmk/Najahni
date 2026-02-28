package edu.najahni.controllers;

import edu.najahni.entities.Projet;
import edu.najahni.entities.StatutProjet;
import edu.najahni.services.projetCRUD;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ClientDashboardController {

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
    @FXML private TableColumn<Projet, String> colStatutProjet;

    @FXML private ComboBox<String> comboTri;
    @FXML private TextField champRecherche;

    private projetCRUD projetService = new projetCRUD();
    private ObservableList<Projet> projetsList = FXCollections.observableArrayList();
    private ObservableList<Projet> projetsListComplete = FXCollections.observableArrayList();
    private int connectedUserId;

    public void setConnectedUser(int userId) {
        this.connectedUserId = userId;
        chargerProjets();
    }

    @FXML
    public void initialize() {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colSecteur.setCellValueFactory(new PropertyValueFactory<>("secteur"));
        colEtape.setCellValueFactory(new PropertyValueFactory<>("etape"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));

        if (colStatutProjet != null) {
            colStatutProjet.setCellValueFactory(new PropertyValueFactory<>("statutProjet"));
            colStatutProjet.setCellFactory(column -> new TableCell<Projet, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setStyle(""); return; }
                    setText(item);
                    switch (item) {
                        case "BROUILLON": setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;"); break;
                        case "SOUMIS":    setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;"); break;
                        case "EVALUE":    setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;"); break;
                    }
                }
            });
        }

        colCouts.setCellValueFactory(cellData -> {
            Projet p = cellData.getValue();
            return p.getDonneesBusiness() == null
                    ? new SimpleDoubleProperty(0).asObject()
                    : new SimpleDoubleProperty(p.getDonneesBusiness().getCoutsEstimes()).asObject();
        });

        colRevenus.setCellValueFactory(cellData -> {
            Projet p = cellData.getValue();
            return p.getDonneesBusiness() == null
                    ? new SimpleDoubleProperty(0).asObject()
                    : new SimpleDoubleProperty(p.getDonneesBusiness().getRevenusAttendus()).asObject();
        });

        // =====================================================================
        // COLONNE ACTIONS
        // =====================================================================
        actions.setCellFactory(param -> new TableCell<>() {
            private final Button btnModifier  = new Button("✏️ Modifier");
            private final Button btnSupprimer = new Button("🗑️ Supprimer");
            private final Button btnSoumettre = new Button("🚀 Soumettre & Évaluer");
            private final Button btnVoir      = new Button("👁️ Voir évaluation");

            {
                String base = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5 10; " +
                        "-fx-min-height: 30px; -fx-background-radius: 5; -fx-border-radius: 5;";

                btnModifier .setStyle(base + "-fx-background-color: #3498db; -fx-text-fill: white;");
                btnSupprimer.setStyle(base + "-fx-background-color: #e74c3c; -fx-text-fill: white;");
                btnSoumettre.setStyle(base + "-fx-background-color: #f39c12; -fx-text-fill: white;");
                btnVoir     .setStyle(base + "-fx-background-color: #27ae60; -fx-text-fill: white;");

                btnModifier .setOnAction(e -> ouvrirModifier(getTableView().getItems().get(getIndex())));
                btnSupprimer.setOnAction(e -> supprimer(getTableView().getItems().get(getIndex())));
                btnSoumettre.setOnAction(e -> soumettreEtEvaluer(getTableView().getItems().get(getIndex())));
                btnVoir     .setOnAction(e -> ouvrirDetailsProjet(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }

                Projet projet = getTableView().getItems().get(getIndex());
                HBox hbox = new HBox(5);
                hbox.setAlignment(Pos.CENTER);

                if (projet.getStatutProjet() == StatutProjet.BROUILLON) {
                    hbox.getChildren().addAll(btnModifier, btnSoumettre, btnSupprimer);
                } else {
                    // EVALUE ou SOUMIS : modifier disponible (relancera l'évaluation)
                    hbox.getChildren().addAll(btnModifier, btnVoir, btnSupprimer);
                }
                setGraphic(hbox);
            }
        });

        if (comboTri != null) {
            comboTri.setItems(FXCollections.observableArrayList(
                    "Plus récents d'abord", "Plus anciens d'abord",
                    "Titre (A-Z)", "Titre (Z-A)", "Secteur (A-Z)",
                    "Statut (A-Z)", "Statut workflow",
                    "Coûts (croissant)", "Coûts (décroissant)",
                    "Revenus (croissant)", "Revenus (décroissant)"
            ));
            comboTri.setValue("Plus récents d'abord");
            comboTri.setOnAction(e -> appliquerTriEtRecherche());
        }

        if (champRecherche != null) {
            champRecherche.textProperty().addListener((obs, o, n) -> appliquerTriEtRecherche());
        }
    }

    // =====================================================================
    // SOUMETTRE + LANCER L'ÉVALUATION IA
    // =====================================================================
    private void soumettreEtEvaluer(Projet projet) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Soumettre pour évaluation IA");
        confirmation.setHeaderText("Soumettre « " + projet.getTitre() + " » ?");
        confirmation.setContentText(
                "Le projet sera soumis et l'évaluation IA se lancera immédiatement.\n" +
                        "Vous pourrez toujours modifier le projet après l'évaluation.\n\n" +
                        "Voulez-vous continuer ?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    projetService.soumettreProjet(projet.getId(), connectedUserId);
                    projet.setStatutProjet(StatutProjet.SOUMIS);
                    ouvrirDetailsProjet(projet);
                    chargerProjets();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur BDD", "Erreur technique : " + e.getMessage());
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur inattendue", e.getMessage());
                }
            }
        });
    }

    // =====================================================================
    // OUVRIR DETAILS PROJET (évaluation IA)
    // =====================================================================
    private void ouvrirDetailsProjet(Projet projet) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DetailsProjet.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Évaluation IA — " + projet.getTitre());
            stage.initOwner(projetsTable.getScene().getWindow());
            stage.setMaximized(true);

            DetailsProjetController controller = loader.getController();
            controller.setConnectedUserId(connectedUserId);
            controller.setProjet(projet);

            stage.showAndWait();
            chargerProjets();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la page : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void chargerProjets() {
        try {
            List<Projet> list = projetService.afficherParUser(connectedUserId);
            projetsListComplete.setAll(list);
            appliquerTriEtRecherche();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les projets : " + e.getMessage());
        }
    }

    private void appliquerTriEtRecherche() {
        List<Projet> listeFiltree = projetsListComplete.stream().collect(Collectors.toList());

        if (champRecherche != null && champRecherche.getText() != null && !champRecherche.getText().trim().isEmpty()) {
            String recherche = champRecherche.getText().toLowerCase().trim();
            listeFiltree = listeFiltree.stream()
                    .filter(p ->
                            (p.getTitre()       != null && p.getTitre().toLowerCase().contains(recherche)) ||
                                    (p.getDescription() != null && p.getDescription().toLowerCase().contains(recherche)) ||
                                    (p.getSecteur()     != null && p.getSecteur().toLowerCase().contains(recherche)) ||
                                    (p.getStatut()      != null && p.getStatut().toLowerCase().contains(recherche)) ||
                                    (p.getEtape()       != null && p.getEtape().toLowerCase().contains(recherche)) ||
                                    (p.getStatutProjet()!= null && p.getStatutProjet().name().toLowerCase().contains(recherche)))
                    .collect(Collectors.toList());
        }

        if (comboTri != null && comboTri.getValue() != null) {
            Comparator<Projet> comparator = null;
            switch (comboTri.getValue()) {
                case "Plus récents d'abord":  comparator = Comparator.comparing(Projet::getDateCreation, Comparator.nullsLast(Comparator.reverseOrder())); break;
                case "Plus anciens d'abord":  comparator = Comparator.comparing(Projet::getDateCreation, Comparator.nullsLast(Comparator.naturalOrder())); break;
                case "Titre (A-Z)":           comparator = Comparator.comparing(p -> p.getTitre() != null ? p.getTitre() : "", String.CASE_INSENSITIVE_ORDER); break;
                case "Titre (Z-A)":           comparator = Comparator.comparing((Projet p) -> p.getTitre() != null ? p.getTitre() : "", String.CASE_INSENSITIVE_ORDER).reversed(); break;
                case "Secteur (A-Z)":         comparator = Comparator.comparing(p -> p.getSecteur() != null ? p.getSecteur() : "", String.CASE_INSENSITIVE_ORDER); break;
                case "Statut (A-Z)":          comparator = Comparator.comparing(p -> p.getStatut() != null ? p.getStatut() : "", String.CASE_INSENSITIVE_ORDER); break;
                case "Statut workflow":       comparator = Comparator.comparing(p -> p.getStatutProjet() != null ? p.getStatutProjet().name() : "", String.CASE_INSENSITIVE_ORDER); break;
                case "Coûts (croissant)":     comparator = Comparator.comparing(p -> p.getDonneesBusiness() != null ? p.getDonneesBusiness().getCoutsEstimes() : 0.0); break;
                case "Coûts (décroissant)":   comparator = Comparator.comparing((Projet p) -> p.getDonneesBusiness() != null ? p.getDonneesBusiness().getCoutsEstimes() : 0.0).reversed(); break;
                case "Revenus (croissant)":   comparator = Comparator.comparing(p -> p.getDonneesBusiness() != null ? p.getDonneesBusiness().getRevenusAttendus() : 0.0); break;
                case "Revenus (décroissant)": comparator = Comparator.comparing((Projet p) -> p.getDonneesBusiness() != null ? p.getDonneesBusiness().getRevenusAttendus() : 0.0).reversed(); break;
            }
            if (comparator != null) {
                listeFiltree = listeFiltree.stream().sorted(comparator).collect(Collectors.toList());
            }
        }

        projetsList.setAll(listeFiltree);
        projetsTable.setItems(projetsList);
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
            javafx.application.Platform.runLater(() -> stage.setMaximized(true));

            AjouterProjet controller = loader.getController();
            controller.setStage(stage);
            controller.setClientMode(connectedUserId);

            stage.showAndWait();
            chargerProjets();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire d'ajout");
        }
    }

    @FXML
    private void rafraichir() { chargerProjets(); }

    // =====================================================================
    // MODIFIER — après modification, ouvre automatiquement DetailsProjet
    // pour lancer la nouvelle évaluation IA
    // =====================================================================
    private void ouvrirModifier(Projet projet) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjoutProjet.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            Stage stageModif = new Stage();
            stageModif.setScene(scene);
            stageModif.setTitle("Modifier le projet — " + projet.getTitre());
            stageModif.initOwner(projetsTable.getScene().getWindow());
            javafx.application.Platform.runLater(() -> stageModif.setMaximized(true));

            AjouterProjet controller = loader.getController();
            controller.setStage(stageModif);
            controller.setClientMode(connectedUserId);
            controller.setProjet(projet);

            // ✅ CALLBACK : quand la modification est validée → ouvrir DetailsProjet
            controller.setOnModificationComplete(() -> {
                // Recharger le projet depuis la BDD pour avoir le statut SOUMIS à jour
                javafx.application.Platform.runLater(() -> {
                    try {
                        chargerProjets();
                        // Récupérer le projet mis à jour
                        Projet projetMisAJour = projetService.getProjetById(projet.getId());
                        if (projetMisAJour != null) {
                            // Ouvrir DetailsProjet pour lancer la réévaluation IA
                            ouvrirDetailsProjet(projetMisAJour);
                        }
                    } catch (SQLException e) {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de recharger le projet : " + e.getMessage());
                    }
                });
            });

            stageModif.showAndWait();
            chargerProjets();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire de modification");
        }
    }

    private void supprimer(Projet projet) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer le projet ?");
        confirmation.setContentText("Voulez-vous vraiment supprimer « " + projet.getTitre() + " » ?\nCette action est irréversible.");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    projetService.supprimer(projet.getId(), connectedUserId);
                    chargerProjets();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void retourAccueil() {
        try {
            Stage stage = (Stage) projetsTable.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SignIn.fxml"));
            Scene scene = new Scene(loader.load());
            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            stage.setX(screen.getMinX()); stage.setY(screen.getMinY());
            stage.setWidth(screen.getWidth()); stage.setHeight(screen.getHeight());
            stage.setTitle("NAJAHNI - Login");
            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}