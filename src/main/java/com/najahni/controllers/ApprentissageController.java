package com.najahni.controllers;

import com.najahni.models.Badge;
import com.najahni.models.EtatProgression;
import com.najahni.models.Progression;
import com.najahni.services.BadgeService;
import com.najahni.services.ProgressionService;
import com.najahni.utils.AlertUtils;
import com.najahni.utils.AnimationUtils;
import com.najahni.utils.WrappedTextCellFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller pour le Dashboard d'Apprentissage.
 * Affiche les statistiques, la progression et la gamification.
 */
public class ApprentissageController {

    // Cartes de statistiques utilisateur
    @FXML
    private Label lblTotalXP;

    @FXML
    private Label lblNiveau;

    @FXML
    private Label lblCoursCompletes;

    @FXML
    private Label lblBadges;

    @FXML
    private ProgressBar progressNiveau;

    @FXML
    private Label lblProgressNiveau;

    // Tableau de progression
    @FXML
    private TableView<Progression> progressionTable;

    @FXML
    private TableColumn<Progression, String> colCours;

    @FXML
    private TableColumn<Progression, String> colPourcentage;

    @FXML
    private TableColumn<Progression, String> colXP;

    @FXML
    private TableColumn<Progression, String> colNiveauCours;

    @FXML
    private TableColumn<Progression, String> colEtat;

    @FXML
    private TableColumn<Progression, Void> colActions;

    // Filtre
    @FXML
    private ComboBox<String> cboEtatFilter;

    // Section badges
    @FXML
    private FlowPane badgesContainer;

    // Leaderboard
    @FXML
    private VBox leaderboardContainer;

    @FXML
    private TableView<Object[]> leaderboardTable;

    @FXML
    private TableColumn<Object[], Integer> colRang;

    @FXML
    private TableColumn<Object[], String> colNom;

    @FXML
    private TableColumn<Object[], Integer> colLeaderXP;

    @FXML
    private TableColumn<Object[], Integer> colLeaderNiveau;

    private final ProgressionService progressionService;
    private final BadgeService badgeService;
    private ObservableList<Progression> progressionList;
    
    // ID de l'utilisateur connecté (à remplacer par session)
    private int currentUserId = 1;

    public ApprentissageController() {
        this.progressionService = new ProgressionService();
        this.badgeService = new BadgeService();
    }

    /**
     * Initialise le contrôleur.
     */
    @FXML
    public void initialize() {
        setupTableColumns();
        setupLeaderboardColumns();
        setupComboBoxes();
        loadUserStats();
        loadProgressions();
        loadBadges();
        loadLeaderboard();

        // Animation des composants au chargement
        AnimationUtils.playFadeScaleIn(progressionTable, 300, 150);
    }

    /**
     * Configure les colonnes du tableau de progression.
     */
    private void setupTableColumns() {
        colCours.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getCoursTitre()));

        colPourcentage.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getPourcentageFormate()));

        // Ajouter une barre de progression visuelle
        colPourcentage.setCellFactory(column -> new TableCell<Progression, String>() {
            private final ProgressBar progressBar = new ProgressBar();
            private final Label label = new Label();
            private final HBox container = new HBox(5, progressBar, label);

            {
                progressBar.setPrefWidth(80);
                progressBar.setPrefHeight(15);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Progression prog = getTableView().getItems().get(getIndex());
                    progressBar.setProgress(prog.getPourcentage() / 100.0);
                    label.setText(item);

                    // Couleur selon progression
                    String color = prog.getPourcentage() >= 100 ? "#27ae60" :
                                  prog.getPourcentage() >= 50 ? "#f39c12" : "#3498db";
                    progressBar.setStyle("-fx-accent: " + color + ";");

                    setGraphic(container);
                }
            }
        });

        colXP.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getPointsXP() + " XP"));

        colNiveauCours.setCellValueFactory(cellData ->
            new SimpleStringProperty("Niv. " + cellData.getValue().getNiveau()));

        colEtat.setCellValueFactory(cellData ->
            new SimpleStringProperty(getEtatIcon(cellData.getValue().getEtat()) + " " +
                                    cellData.getValue().getEtat().getDisplayName()));

        // Appliquer le retour à la ligne
        colCours.setCellFactory(new WrappedTextCellFactory<>());

        // Configuration des boutons d'action
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button continueBtn = new Button("▶️");
            private final Button detailsBtn = new Button("📊");
            private final HBox pane = new HBox(5, continueBtn, detailsBtn);

            {
                continueBtn.getStyleClass().add("btn-primary");
                continueBtn.setStyle("-fx-padding: 5 8;");
                continueBtn.setTooltip(new Tooltip("Continuer le cours"));

                detailsBtn.getStyleClass().add("btn-secondary");
                detailsBtn.setStyle("-fx-padding: 5 8;");
                detailsBtn.setTooltip(new Tooltip("Voir les détails"));

                continueBtn.setOnAction(event -> {
                    Progression prog = getTableView().getItems().get(getIndex());
                    continuerCours(prog);
                });

                detailsBtn.setOnAction(event -> {
                    Progression prog = getTableView().getItems().get(getIndex());
                    voirDetails(prog);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Progression prog = getTableView().getItems().get(getIndex());
                    continueBtn.setVisible(!prog.estComplete());
                    continueBtn.setManaged(!prog.estComplete());
                    setGraphic(pane);
                }
            }
        });
    }

    /**
     * Configure les colonnes du leaderboard.
     * Format du leaderboard: [user_id, name, total_xp, cours_completes, niveau_max]
     */
    private void setupLeaderboardColumns() {
        // Note: Le rang n'est pas dans les données, on utilise l'index de la liste + 1
        colRang.setCellValueFactory(cellData -> {
            int index = leaderboardTable.getItems().indexOf(cellData.getValue()) + 1;
            return new javafx.beans.property.SimpleObjectProperty<>(index);
        });

        colNom.setCellValueFactory(cellData ->
            new SimpleStringProperty((String) cellData.getValue()[1]));

        colLeaderXP.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleObjectProperty<>((Integer) cellData.getValue()[2]));

        colLeaderNiveau.setCellValueFactory(cellData -> {
            Object[] row = cellData.getValue();
            // Safely access index 3 (cours_completes) as fallback if index 4 doesn't exist
            int val = row.length > 4 ? ((Number) row[4]).intValue() : (row.length > 3 ? ((Number) row[3]).intValue() : 0);
            return new javafx.beans.property.SimpleObjectProperty<>(val);
        });

        // Styliser le rang
        colRang.setCellFactory(column -> new TableCell<Object[], Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String medal = switch (item) {
                        case 1 -> "🥇";
                        case 2 -> "🥈";
                        case 3 -> "🥉";
                        default -> "#" + item;
                    };
                    setText(medal);
                    setStyle("-fx-font-size: 16px; -fx-alignment: center;");
                }
            }
        });
    }

    /**
     * Configure les ComboBox.
     */
    private void setupComboBoxes() {
        List<String> etatOptions = Arrays.stream(EtatProgression.values())
            .map(EtatProgression::getDisplayName)
            .collect(Collectors.toList());
        etatOptions.add(0, "Tous les états");
        cboEtatFilter.setItems(FXCollections.observableArrayList(etatOptions));
        cboEtatFilter.setValue("Tous les états");
    }

    /**
     * Charge les statistiques de l'utilisateur.
     */
    private void loadUserStats() {
        int[] stats = progressionService.getStatistiquesUtilisateur(currentUserId);

        lblTotalXP.setText(stats[0] + " XP");
        lblNiveau.setText("Niveau " + stats[1]);
        lblCoursCompletes.setText(String.valueOf(stats[2]));

        int nombreBadges = badgeService.compterBadgesUtilisateur(currentUserId);
        lblBadges.setText(String.valueOf(nombreBadges));

        // Calculer la progression vers le prochain niveau
        int totalXP = stats[0];
        int niveau = stats[1];
        if (niveau < Progression.SEUILS_NIVEAU.length) {
            int seuilActuel = Progression.SEUILS_NIVEAU[niveau - 1];
            int seuilProchain = Progression.SEUILS_NIVEAU[niveau];
            double progression = (double) (totalXP - seuilActuel) / (seuilProchain - seuilActuel);
            progressNiveau.setProgress(Math.min(progression, 1.0));
            lblProgressNiveau.setText((seuilProchain - totalXP) + " XP pour niveau " + (niveau + 1));
        } else {
            progressNiveau.setProgress(1.0);
            lblProgressNiveau.setText("Niveau maximum atteint !");
        }
    }

    /**
     * Charge les progressions de l'utilisateur.
     */
    private void loadProgressions() {
        List<Progression> progressions = progressionService.trouverParUtilisateur(currentUserId);
        progressionList = FXCollections.observableArrayList(progressions);
        progressionTable.setItems(progressionList);
    }

    /**
     * Charge les badges disponibles et obtenus.
     */
    private void loadBadges() {
        badgesContainer.getChildren().clear();

        // Récupérer les badges éligibles (débloqués)
        List<Badge> badgesObtenus = badgeService.findBadgesEligibles(currentUserId);

        for (Badge badge : badgesObtenus) {
            VBox badgeCard = createBadgeCard(badge, true);
            badgesContainer.getChildren().add(badgeCard);
        }

        // Afficher un message si aucun badge
        if (badgesObtenus.isEmpty()) {
            Label noBadges = new Label("🎯 Commencez à apprendre pour gagner des badges !");
            noBadges.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            badgesContainer.getChildren().add(noBadges);
        }
    }

    /**
     * Crée une carte pour un badge.
     */
    private VBox createBadgeCard(Badge badge, boolean obtained) {
        VBox card = new VBox(5);
        String bgColor = obtained ? "#f8f9fa" : "#e0e0e0";
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10; " +
                     "-fx-padding: 10; -fx-alignment: center;");
        card.setPrefWidth(100);

        Label iconLabel = new Label(badge.getIcone());
        iconLabel.setStyle("-fx-font-size: 32px;" + (obtained ? "" : " -fx-opacity: 0.5;"));

        Label nameLabel = new Label(badge.getNom());
        nameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #333; -fx-font-weight: bold;");
        nameLabel.setWrapText(true);

        Label descLabel = new Label(obtained ? "✅ Obtenu" : "🔒 Verrouillé");
        descLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #999;");

        card.getChildren().addAll(iconLabel, nameLabel, descLabel);

        // Animation au survol
        String hoverColor = obtained ? "#e3f2fd" : "#d0d0d0";
        card.setOnMouseEntered(e -> 
            card.setStyle("-fx-background-color: " + hoverColor + "; -fx-background-radius: 10; " +
                         "-fx-padding: 10; -fx-alignment: center; -fx-cursor: hand;"));
        card.setOnMouseExited(e ->
            card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10; " +
                         "-fx-padding: 10; -fx-alignment: center;"));

        // Tooltip avec description
        Tooltip tooltip = new Tooltip(badge.getDescription() + "\n" + badge.getCondition());
        Tooltip.install(card, tooltip);

        return card;
    }

    /**
     * Charge le leaderboard.
     */
    private void loadLeaderboard() {
        List<Object[]> leaderboard = progressionService.getLeaderboard(10);
        leaderboardTable.setItems(FXCollections.observableArrayList(leaderboard));
    }

    /**
     * Retourne l'icône correspondant à l'état.
     */
    private String getEtatIcon(EtatProgression etat) {
        return switch (etat) {
            case NON_COMMENCE -> "⚪";
            case EN_COURS -> "🔵";
            case COMPLETE -> "✅";
            case CERTIFIE -> "🏆";
        };
    }

    /**
     * Continue un cours.
     */
    private void continuerCours(Progression progression) {
        AlertUtils.showInfo("Continuer le cours",
            "📚 " + progression.getCoursTitre() + "\n\n" +
            "Progression: " + progression.getPourcentageFormate() + "\n" +
            "Points XP: " + progression.getPointsXP() + "\n\n" +
            "→ Redirection vers le cours...");
        // TODO: Naviguer vers la vue du cours
    }

    /**
     * Affiche les détails d'une progression.
     */
    private void voirDetails(Progression progression) {
        String details = String.format("""
            📚 %s
            
            📊 Progression: %s
            ⭐ Points XP: %d
            📈 Niveau: %d
            📅 Début: %s
            %s
            """,
            progression.getCoursTitre(),
            progression.getPourcentageFormate(),
            progression.getPointsXP(),
            progression.getNiveau(),
            progression.getDateDebut() != null ? 
                progression.getDateDebut().toLocalDate().toString() : "N/A",
            progression.estComplete() ? 
                "✅ Complété le: " + progression.getDateObtentionFormatee() : 
                "🔄 En cours..."
        );

        AlertUtils.showInfo("Détails de la progression", details);
    }

    /**
     * Filtre les progressions.
     */
    @FXML
    public void filterProgressions() {
        String etatFilter = cboEtatFilter.getValue();

        if (etatFilter.equals("Tous les états")) {
            loadProgressions();
        } else {
            List<Progression> filtered = progressionService.trouverParUtilisateur(currentUserId)
                .stream()
                .filter(p -> p.getEtat().getDisplayName().equals(etatFilter))
                .collect(Collectors.toList());
            progressionList = FXCollections.observableArrayList(filtered);
            progressionTable.setItems(progressionList);
        }
    }

    /**
     * Efface les filtres.
     */
    @FXML
    public void clearFilter() {
        cboEtatFilter.setValue("Tous les états");
        loadProgressions();
    }

    /**
     * Actualise les données.
     */
    @FXML
    public void refreshData() {
        loadUserStats();
        loadProgressions();
        loadBadges();
        loadLeaderboard();
        AnimationUtils.playFadeScaleIn(progressionTable, 200, 0);
    }

    /**
     * Vérifie et attribue les nouveaux badges.
     */
    @FXML
    public void checkNewBadges() {
        var nouveauxBadges = badgeService.verifierEtAttribuerBadges(currentUserId);
        if (!nouveauxBadges.isEmpty()) {
            StringBuilder message = new StringBuilder("🎉 Félicitations ! Nouveaux badges obtenus:\n\n");
            for (var badge : nouveauxBadges) {
                message.append(badge.getAffichageComplet()).append("\n");
            }
            AlertUtils.showSuccess(message.toString());
            loadUserStats();
            loadBadges();
        } else {
            AlertUtils.showInfo("Badges", "Continuez à apprendre pour débloquer de nouveaux badges !");
        }
    }

    /**
     * Définit l'utilisateur courant.
     */
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        refreshData();
    }
}
