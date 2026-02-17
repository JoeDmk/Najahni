package edu.najahni.controllers;

import edu.najahni.entities.Projet;
import edu.najahni.entities.donneesBusiness;
import edu.najahni.services.projetCRUD;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;

public class AjouterProjet {

    @FXML private TextField titre;
    @FXML private TextArea description;
    @FXML private TextField secteur;
    @FXML private TextField etape;
    @FXML private TextField statut;
    @FXML private TextField userIdField;

    @FXML private TextField tailleMarche;
    @FXML private TextField modeleRevenu;
    @FXML private TextField couts;
    @FXML private TextField revenus;
    @FXML private TextField risque;
    @FXML private TextField forceEquipe;

    private projetCRUD projetService = new projetCRUD();
    private Stage stage;
    private boolean isClientMode = false;
    private int connectedUserId;

    private boolean isModification = false;
    private Projet projetAModifier;

    public void setProjet(Projet projet) {
        this.projetAModifier = projet;
        this.isModification = (projet != null);

        if (isModification) {
            titre.setText(projet.getTitre());
            description.setText(projet.getDescription());
            secteur.setText(projet.getSecteur());
            etape.setText(projet.getEtape());
            statut.setText(projet.getStatut());

            if (projet.getDonneesBusiness() != null) {
                donneesBusiness db = projet.getDonneesBusiness();
                tailleMarche.setText(db.getTailleMarche() != null ? db.getTailleMarche() : "");
                modeleRevenu.setText(db.getModeleRevenu() != null ? db.getModeleRevenu() : "");
                couts.setText(String.valueOf(db.getCoutsEstimes()));
                revenus.setText(String.valueOf(db.getRevenusAttendus()));
                risque.setText(db.getNiveauRisque() != null ? db.getNiveauRisque() : "");
                forceEquipe.setText(String.valueOf(db.getForceEquipe()));
            }
        }
    }

    @FXML
    private void AjouterProjet() {

        // =============================
        // 1️⃣ Validate required fields
        // =============================
        if (titre.getText().trim().isEmpty() ||
                secteur.getText().trim().isEmpty() ||
                etape.getText().trim().isEmpty() ||
                statut.getText().trim().isEmpty() ||
                tailleMarche.getText().trim().isEmpty() ||
                modeleRevenu.getText().trim().isEmpty() ||
                couts.getText().trim().isEmpty() ||
                revenus.getText().trim().isEmpty() ||
                risque.getText().trim().isEmpty() ||
                forceEquipe.getText().trim().isEmpty()) {

            showAlert(Alert.AlertType.WARNING,
                    "Champs obligatoires",
                    "Tous les champs marqués * doivent être remplis !");
            return;
        }

        double coutsValue;
        double revenusValue;
        int forceValue;
        int userIdValue = 0;

        // =============================
        // 2️⃣ If admin mode → validate userId
        // =============================
        if (!isClientMode) {

            if (userIdField.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR,
                        "Erreur",
                        "Veuillez saisir le User ID.");
                return;
            }

            try {
                userIdValue = Integer.parseInt(userIdField.getText().trim());
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR,
                        "Erreur",
                        "User ID invalide.");
                return;
            }

            try {
                if (!projetService.userExiste(userIdValue)) {
                    showAlert(Alert.AlertType.ERROR,
                            "Erreur",
                            "Cet utilisateur n'existe pas.");
                    return;
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR,
                        "Erreur BDD",
                        e.getMessage());
                return;
            }
        }

        // =============================
        // 3️⃣ Validate numeric fields
        // =============================
        try {
            coutsValue = Double.parseDouble(couts.getText().trim());
            revenusValue = Double.parseDouble(revenus.getText().trim());
            forceValue = Integer.parseInt(forceEquipe.getText().trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR,
                    "Erreur de saisie",
                    "Coûts, Revenus et Force équipe doivent être des nombres valides.");
            return;
        }

        if (coutsValue <= 0 || revenusValue <= 0 || forceValue < 1 || forceValue > 10) {
            showAlert(Alert.AlertType.WARNING,
                    "Valeurs invalides",
                    "Vérifiez les valeurs numériques (positives, force entre 1 et 10).");
            return;
        }

        // =============================
        // 4️⃣ Create / Update project
        // =============================
        Projet projet = isModification ? projetAModifier : new Projet();

        if (isClientMode) {
            projet.setUserId(connectedUserId);
        } else {
            projet.setUserId(userIdValue);
        }

        projet.setTitre(titre.getText().trim());
        projet.setDescription(description.getText().trim());
        projet.setSecteur(secteur.getText().trim());
        projet.setEtape(etape.getText().trim());
        projet.setStatut(statut.getText().trim());

        if (!isModification) {
            projet.setDateCreation(LocalDate.now());
        }

        donneesBusiness db =
                isModification && projet.getDonneesBusiness() != null
                        ? projet.getDonneesBusiness()
                        : new donneesBusiness();

        db.setTailleMarche(tailleMarche.getText().trim());
        db.setModeleRevenu(modeleRevenu.getText().trim());
        db.setCoutsEstimes(coutsValue);
        db.setRevenusAttendus(revenusValue);
        db.setNiveauRisque(risque.getText().trim());
        db.setForceEquipe(forceValue);

        projet.setDonneesBusiness(db);

        // =============================
        // 5️⃣ Save to database
        // =============================
        try {
            if (isModification) {
                projetService.modifier(projet);
                showAlert(Alert.AlertType.INFORMATION,
                        "Succès",
                        "Projet modifié avec succès !");
            } else {
                projetService.ajouter(projet);
                showAlert(Alert.AlertType.INFORMATION,
                        "Succès",
                        "Projet ajouté avec succès !");
                clearAllFields();
            }

            if (stage != null) {
                stage.close();
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR,
                    "Erreur base de données",
                    e.getMessage());
        }
    }

    private void clearAllFields() {
        titre.clear();
        description.clear();
        secteur.clear();
        etape.clear();
        statut.clear();
        tailleMarche.clear();
        modeleRevenu.clear();
        couts.clear();
        revenus.clear();
        risque.clear();
        forceEquipe.clear();
        if (userIdField != null) {
            userIdField.clear();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void annuler(javafx.event.ActionEvent actionEvent) {
        if (stage != null) {
            stage.close();
        }
    }

    public void setClientMode(int userId) {
        this.isClientMode = true;
        this.connectedUserId = userId;
        userIdField.setVisible(false);
        userIdField.setManaged(false);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
}
