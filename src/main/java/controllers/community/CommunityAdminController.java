package controllers.community;

import javafx.animation.*;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import models.community.*;
import models.community.Thread;
import services.SessionService;
import services.community.*;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Admin back-office controller for the Community module.
 * Full CRUD with input validation and uniqueness checks.
 */
public class CommunityAdminController {

    private static final Logger LOG = Logger.getLogger(CommunityAdminController.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private StackPane rootStack;
    @FXML private Button tabDashboard, tabGroups, tabPosts, tabThreads, tabEvents;
    @FXML private StackPane contentArea;

    private final GroupCRUD groupCRUD = new GroupCRUD();
    private final PostCRUD postCRUD = new PostCRUD();
    private final ThreadCRUD threadCRUD = new ThreadCRUD();
    private final EventCRUD eventCRUD = new EventCRUD();
    private final CommentCRUD commentCRUD = new CommentCRUD();
    private final GroupMemberCRUD groupMemberCRUD = new GroupMemberCRUD();
    private final EventParticipantCRUD eventParticipantCRUD = new EventParticipantCRUD();

    private Button activeTab;
    private int currentUserId;

    @FXML
    public void initialize() {
        try { currentUserId = SessionService.getInstance().getCurrentUser().getId(); }
        catch (Exception e) { currentUserId = 0; }
        showDashboard();
    }

    // ====== TAB NAVIGATION ======

    @FXML private void showDashboard() { setActiveTab(tabDashboard); loadDashboard(); }
    @FXML private void showGroups()    { setActiveTab(tabGroups);    loadGroupsTable(); }
    @FXML private void showPosts()     { setActiveTab(tabPosts);     loadPostsTable(); }
    @FXML private void showThreads()   { setActiveTab(tabThreads);   loadThreadsTable(); }
    @FXML private void showEvents()    { setActiveTab(tabEvents);    loadEventsTable(); }

    private void setActiveTab(Button tab) {
        if (activeTab != null) activeTab.getStyleClass().remove("community-admin-tab-active");
        activeTab = tab;
        if (activeTab != null && !activeTab.getStyleClass().contains("community-admin-tab-active")) {
            activeTab.getStyleClass().add("community-admin-tab-active");
        }
    }

    private void setContent(javafx.scene.Node node) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(node);
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(200), node);
        ft.setToValue(1);
        ft.play();
    }

    // ====== DASHBOARD ======

    private void loadDashboard() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox dashboard = new VBox(20);
        dashboard.setPadding(new Insets(24, 28, 24, 28));

        Label title = new Label("Tableau de Bord");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setTextFill(Color.web("#2c3e50"));

        Label subtitle = new Label("Vue d'ensemble de l'activite communautaire");
        subtitle.setFont(Font.font("Segoe UI", 12));
        subtitle.setTextFill(Color.web("#7f8c8d"));

        HBox statsRow = new HBox(14);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        int groupCount = 0, postCount = 0, threadCount = 0, eventCount = 0, memberCount = 0;
        try {
            groupCount = groupCRUD.afficher().size();
            postCount = postCRUD.afficher().size();
            threadCount = threadCRUD.afficher().size();
            eventCount = eventCRUD.afficher().size();
            memberCount = groupMemberCRUD.afficher().size();
        } catch (SQLException e) {
            LOG.warning("Stats error: " + e.getMessage());
        }

        statsRow.getChildren().addAll(
                buildStatCard("Groupes", groupCount, "#667eea"),
                buildStatCard("Publications", postCount, "#27ae60"),
                buildStatCard("Threads", threadCount, "#e67e22"),
                buildStatCard("Evenements", eventCount, "#e74c3c"),
                buildStatCard("Membres", memberCount, "#8e44ad")
        );

        for (int i = 0; i < statsRow.getChildren().size(); i++) {
            javafx.scene.Node card = statsRow.getChildren().get(i);
            card.setOpacity(0);
            card.setTranslateY(15);
            PauseTransition pause = new PauseTransition(Duration.millis(i * 60));
            pause.setOnFinished(e -> {
                FadeTransition fade = new FadeTransition(Duration.millis(250), card);
                fade.setToValue(1);
                TranslateTransition slide = new TranslateTransition(Duration.millis(250), card);
                slide.setToY(0);
                slide.setInterpolator(Interpolator.EASE_OUT);
                new ParallelTransition(fade, slide).play();
            });
            pause.play();
        }

        VBox recentSection = new VBox(10);
        recentSection.setPadding(new Insets(12, 0, 0, 0));

        Label recentTitle = new Label("Activite Recente");
        recentTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        recentTitle.setTextFill(Color.web("#2c3e50"));

        VBox activityList = new VBox(6);
        try {
            List<Post> posts = postCRUD.afficher();
            int limit = Math.min(5, posts.size());
            for (int i = posts.size() - 1; i >= Math.max(0, posts.size() - limit); i--) {
                Post p = posts.get(i);
                String author = ((p.getFirstname() != null ? p.getFirstname() : "") + " "
                        + (p.getLastname() != null ? p.getLastname() : "")).trim();
                String date = p.getCreatedAt() != null
                        ? p.getCreatedAt().toLocalDateTime().format(DATE_FMT) : "-";
                String preview = truncate(p.getContent(), 80);

                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 14, 10, 14));
                row.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 4, 0, 0, 1);");

                VBox info = new VBox(2);
                HBox.setHgrow(info, Priority.ALWAYS);
                Label msgLbl = new Label(preview);
                msgLbl.setFont(Font.font("Segoe UI", 12));
                msgLbl.setTextFill(Color.web("#2c3e50"));
                msgLbl.setWrapText(true);
                Label metaLbl = new Label("par " + (author.isEmpty() ? "Anonyme" : author) + " - " + date);
                metaLbl.setFont(Font.font("Segoe UI", 10));
                metaLbl.setTextFill(Color.web("#95a5a6"));
                info.getChildren().addAll(msgLbl, metaLbl);
                row.getChildren().add(info);
                activityList.getChildren().add(row);
            }
        } catch (SQLException e) {
            activityList.getChildren().add(new Label("Erreur de chargement"));
        }

        recentSection.getChildren().addAll(recentTitle, activityList);
        dashboard.getChildren().addAll(title, subtitle, statsRow, recentSection);
        scroll.setContent(dashboard);
        setContent(scroll);
    }

    private VBox buildStatCard(String label, int value, String color) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setMinWidth(130);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2); "
                + "-fx-border-color: " + color + "33; -fx-border-radius: 12; -fx-border-width: 1.5;");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label valueLbl = new Label(String.valueOf(value));
        valueLbl.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 26));
        valueLbl.setTextFill(Color.web(color));
        Label labelLbl = new Label(label);
        labelLbl.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        labelLbl.setTextFill(Color.web("#7f8c8d"));

        card.getChildren().addAll(valueLbl, labelLbl);

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: " + color + "08; -fx-background-radius: 12; "
                + "-fx-effect: dropshadow(gaussian, " + color + "22, 12, 0, 0, 3); "
                + "-fx-border-color: " + color + "55; -fx-border-radius: 12; -fx-border-width: 1.5;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2); "
                + "-fx-border-color: " + color + "33; -fx-border-radius: 12; -fx-border-width: 1.5;"));
        return card;
    }

    // ====== GROUPS TABLE + CRUD ======

    @SuppressWarnings("unchecked")
    private void loadGroupsTable() {
        VBox container = buildTableContainer("Gestion des Groupes", "Gerer tous les groupes de la communaute");

        try {
            List<Group> groups = groupCRUD.afficher();
            ObservableList<Group> data = FXCollections.observableArrayList(groups);
            FilteredList<Group> filtered = new FilteredList<>(data, p -> true);

            Button addBtn = createAddButton("+ Ajouter un Groupe");
            addBtn.setOnAction(e -> openGroupForm(null));

            TextField search = createSearchField("Rechercher un groupe...");
            search.textProperty().addListener((obs, o, n) -> {
                String q = n.toLowerCase().trim();
                filtered.setPredicate(g -> q.isEmpty()
                        || g.getName().toLowerCase().contains(q)
                        || (g.getDescription() != null && g.getDescription().toLowerCase().contains(q)));
            });

            HBox topBar = createTopBar(addBtn, search);

            TableView<Group> table = createStyledTable();
            VBox.setVgrow(table, Priority.ALWAYS);

            TableColumn<Group, Integer> colId = new TableColumn<>("ID");
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colId.setMaxWidth(60); colId.setMinWidth(60);

            TableColumn<Group, String> colName = new TableColumn<>("Nom");
            colName.setCellValueFactory(new PropertyValueFactory<>("name"));

            TableColumn<Group, String> colDesc = new TableColumn<>("Description");
            colDesc.setCellValueFactory(cd -> new SimpleStringProperty(truncate(cd.getValue().getDescription(), 60)));

            TableColumn<Group, String> colPrivacy = new TableColumn<>("Visibilite");
            colPrivacy.setCellValueFactory(cd -> new SimpleStringProperty(
                    cd.getValue().getIsPrivate() ? "Prive" : "Public"));
            colPrivacy.setMaxWidth(90); colPrivacy.setMinWidth(90);

            TableColumn<Group, String> colDate = new TableColumn<>("Date creation");
            colDate.setCellValueFactory(cd -> new SimpleStringProperty(formatDate(cd.getValue().getCreatedAt())));
            colDate.setMaxWidth(140); colDate.setMinWidth(140);

            TableColumn<Group, Void> colActions = new TableColumn<>("Actions");
            colActions.setMaxWidth(170); colActions.setMinWidth(170);
            colActions.setCellFactory(col -> new TableCell<>() {
                private final HBox box = new HBox(6);
                private final Button editBtn = createEditButton();
                private final Button delBtn = createDeleteButton();
                {
                    editBtn.setOnAction(e -> {
                        Group g = getTableView().getItems().get(getIndex());
                        openGroupForm(g);
                    });
                    delBtn.setOnAction(e -> {
                        Group g = getTableView().getItems().get(getIndex());
                        confirmAndDelete("le groupe '" + g.getName() + "'", () -> {
                            try { groupCRUD.supprimer(g.getId()); data.remove(g); showToast("Groupe supprime"); }
                            catch (SQLException ex) { showError(ex.getMessage()); }
                        });
                    });
                    box.setAlignment(Pos.CENTER);
                    box.getChildren().addAll(editBtn, delBtn);
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });

            table.getColumns().addAll(colId, colName, colDesc, colPrivacy, colDate, colActions);
            table.setItems(filtered);

            Label countLbl = createCountLabel(groups.size() + " groupes au total");
            container.getChildren().addAll(topBar, countLbl, table);
        } catch (SQLException e) {
            container.getChildren().add(errorLabel(e.getMessage()));
        }
        setContent(container);
    }

    private void openGroupForm(Group existing) {
        boolean isEdit = existing != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier le Groupe" : "Nouveau Groupe");
        dialog.setHeaderText(null);

        DialogPane dp = dialog.getDialogPane();
        dp.setPrefWidth(440);
        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dp.getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        styleDialogPane(dp);

        VBox form = new VBox(8);
        form.setPadding(new Insets(16));

        TextField nameField = createFormField(isEdit ? existing.getName() : "", "Nom du groupe");
        Label nameErr = createErrLabel();

        TextArea descField = new TextArea(isEdit && existing.getDescription() != null ? existing.getDescription() : "");
        descField.setPromptText("Description du groupe");
        descField.setPrefRowCount(3);
        descField.setWrapText(true);
        descField.setStyle(fieldStyle());

        CheckBox privateBox = new CheckBox("Groupe prive");
        if (isEdit) privateBox.setSelected(existing.getIsPrivate());

        form.getChildren().addAll(formLabel("Nom *"), nameField, nameErr,
                formLabel("Description"), descField, privateBox);
        dp.setContent(form);

        final boolean[] saved = {false};
        dp.lookupButton(saveType).addEventFilter(ActionEvent.ACTION, evt -> {
            String name = nameField.getText().trim();
            nameErr.setText(""); nameField.setStyle(fieldStyle());

            if (name.isEmpty()) {
                nameErr.setText("Le nom est obligatoire");
                nameField.setStyle(errFieldStyle()); evt.consume(); return;
            }
            if (name.length() < 3) {
                nameErr.setText("Minimum 3 caracteres");
                nameField.setStyle(errFieldStyle()); evt.consume(); return;
            }
            try {
                if (isEdit) {
                    existing.setName(name);
                    existing.setDescription(descField.getText().trim());
                    groupCRUD.modifier(existing);
                } else {
                    Group g = new Group(name, descField.getText().trim(), currentUserId, privateBox.isSelected());
                    groupCRUD.ajouter(g);
                }
                saved[0] = true;
            } catch (SQLException ex) {
                String msg = ex.getMessage();
                if (msg != null && msg.contains("GROUP_NAME_EXISTS")) {
                    nameErr.setText("Ce nom de groupe existe deja");
                    nameField.setStyle(errFieldStyle());
                } else {
                    nameErr.setText("Erreur: " + msg);
                }
                evt.consume();
            }
        });

        dialog.showAndWait();
        if (saved[0]) { showToast(isEdit ? "Groupe modifie" : "Groupe ajoute"); showGroups(); }
    }

    // ====== POSTS TABLE + CRUD ======

    @SuppressWarnings("unchecked")
    private void loadPostsTable() {
        VBox container = buildTableContainer("Gestion des Publications", "Moderer et gerer toutes les publications");

        try {
            List<Post> posts = postCRUD.afficher();
            ObservableList<Post> data = FXCollections.observableArrayList(posts);
            FilteredList<Post> filtered = new FilteredList<>(data, p -> true);

            Button addBtn = createAddButton("+ Ajouter une Publication");
            addBtn.setOnAction(e -> openPostForm(null));

            TextField search = createSearchField("Rechercher une publication...");
            search.textProperty().addListener((obs, o, n) -> {
                String q = n.toLowerCase().trim();
                filtered.setPredicate(p -> q.isEmpty()
                        || (p.getContent() != null && p.getContent().toLowerCase().contains(q))
                        || (p.getFirstname() != null && p.getFirstname().toLowerCase().contains(q))
                        || (p.getLastname() != null && p.getLastname().toLowerCase().contains(q)));
            });

            HBox topBar = createTopBar(addBtn, search);

            TableView<Post> table = createStyledTable();
            VBox.setVgrow(table, Priority.ALWAYS);

            TableColumn<Post, Integer> colId = new TableColumn<>("ID");
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colId.setMaxWidth(60); colId.setMinWidth(60);

            TableColumn<Post, String> colAuthor = new TableColumn<>("Auteur");
            colAuthor.setCellValueFactory(cd -> {
                String name = ((cd.getValue().getFirstname() != null ? cd.getValue().getFirstname() : "") + " "
                        + (cd.getValue().getLastname() != null ? cd.getValue().getLastname() : "")).trim();
                return new SimpleStringProperty(name.isEmpty() ? "Utilisateur #" + cd.getValue().getUserId() : name);
            });

            TableColumn<Post, String> colContent = new TableColumn<>("Contenu");
            colContent.setCellValueFactory(cd -> new SimpleStringProperty(truncate(cd.getValue().getContent(), 80)));

            TableColumn<Post, String> colImage = new TableColumn<>("Image");
            colImage.setCellValueFactory(cd -> new SimpleStringProperty(
                    cd.getValue().getImageUrl() != null && !cd.getValue().getImageUrl().isBlank() ? "Oui" : "-"));
            colImage.setMaxWidth(60); colImage.setMinWidth(60);

            TableColumn<Post, Integer> colReactions = new TableColumn<>("Reactions");
            colReactions.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getReactionsCount()).asObject());
            colReactions.setMaxWidth(80); colReactions.setMinWidth(80);

            TableColumn<Post, String> colDate = new TableColumn<>("Date");
            colDate.setCellValueFactory(cd -> new SimpleStringProperty(formatDate(cd.getValue().getCreatedAt())));
            colDate.setMaxWidth(140); colDate.setMinWidth(140);

            TableColumn<Post, Void> colActions = new TableColumn<>("Actions");
            colActions.setMaxWidth(170); colActions.setMinWidth(170);
            colActions.setCellFactory(col -> new TableCell<>() {
                private final HBox box = new HBox(6);
                private final Button editBtn = createEditButton();
                private final Button delBtn = createDeleteButton();
                {
                    editBtn.setOnAction(e -> {
                        Post p = getTableView().getItems().get(getIndex());
                        openPostForm(p);
                    });
                    delBtn.setOnAction(e -> {
                        Post p = getTableView().getItems().get(getIndex());
                        confirmAndDelete("la publication #" + p.getId(), () -> {
                            try { postCRUD.supprimer(p.getId()); data.remove(p); showToast("Publication supprimee"); }
                            catch (SQLException ex) { showError(ex.getMessage()); }
                        });
                    });
                    box.setAlignment(Pos.CENTER);
                    box.getChildren().addAll(editBtn, delBtn);
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });

            table.getColumns().addAll(colId, colAuthor, colContent, colImage, colReactions, colDate, colActions);
            table.setItems(filtered);

            Label countLbl = createCountLabel(posts.size() + " publications au total");
            container.getChildren().addAll(topBar, countLbl, table);
        } catch (SQLException e) {
            container.getChildren().add(errorLabel(e.getMessage()));
        }
        setContent(container);
    }

    private void openPostForm(Post existing) {
        boolean isEdit = existing != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier la Publication" : "Nouvelle Publication");
        dialog.setHeaderText(null);

        DialogPane dp = dialog.getDialogPane();
        dp.setPrefWidth(460);
        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dp.getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        styleDialogPane(dp);

        VBox form = new VBox(8);
        form.setPadding(new Insets(16));

        TextArea contentField = new TextArea(isEdit ? existing.getContent() : "");
        contentField.setPromptText("Contenu de la publication (min 10 caracteres)");
        contentField.setPrefRowCount(5);
        contentField.setWrapText(true);
        contentField.setStyle(fieldStyle());
        Label contentErr = createErrLabel();

        TextField imageField = createFormField(
                isEdit && existing.getImageUrl() != null ? existing.getImageUrl() : "",
                "URL de l'image (optionnel)");
        Label imageErr = createErrLabel();

        form.getChildren().addAll(formLabel("Contenu *"), contentField, contentErr,
                formLabel("Image URL"), imageField, imageErr);
        dp.setContent(form);

        final boolean[] saved = {false};
        dp.lookupButton(saveType).addEventFilter(ActionEvent.ACTION, evt -> {
            String content = contentField.getText().trim();
            String imgUrl = imageField.getText().trim();
            contentErr.setText(""); imageErr.setText("");
            contentField.setStyle(fieldStyle()); imageField.setStyle(fieldStyle());

            boolean valid = true;
            if (content.isEmpty()) {
                contentErr.setText("Le contenu est obligatoire");
                contentField.setStyle(errFieldStyle()); valid = false;
            } else if (content.length() < 10) {
                contentErr.setText("Minimum 10 caracteres");
                contentField.setStyle(errFieldStyle()); valid = false;
            }
            if (!imgUrl.isEmpty() && !imgUrl.startsWith("http://") && !imgUrl.startsWith("https://")) {
                imageErr.setText("L'URL doit commencer par http:// ou https://");
                imageField.setStyle(errFieldStyle()); valid = false;
            }
            if (!valid) { evt.consume(); return; }

            try {
                if (isEdit) {
                    existing.setContent(content);
                    existing.setImageUrl(imgUrl.isEmpty() ? null : imgUrl);
                    postCRUD.modifier(existing);
                } else {
                    Post p = new Post(currentUserId, content);
                    p.setImageUrl(imgUrl.isEmpty() ? null : imgUrl);
                    postCRUD.ajouter(p);
                }
                saved[0] = true;
            } catch (SQLException ex) {
                contentErr.setText("Erreur: " + ex.getMessage());
                evt.consume();
            }
        });

        dialog.showAndWait();
        if (saved[0]) { showToast(isEdit ? "Publication modifiee" : "Publication ajoutee"); showPosts(); }
    }

    // ====== THREADS TABLE + CRUD ======

    @SuppressWarnings("unchecked")
    private void loadThreadsTable() {
        VBox container = buildTableContainer("Gestion des Threads", "Moderer et gerer toutes les discussions");

        try {
            List<Thread> threads = threadCRUD.afficher();
            ObservableList<Thread> data = FXCollections.observableArrayList(threads);
            FilteredList<Thread> filtered = new FilteredList<>(data, p -> true);

            Button addBtn = createAddButton("+ Ajouter un Thread");
            addBtn.setOnAction(e -> openThreadForm(null));

            TextField search = createSearchField("Rechercher un thread...");
            search.textProperty().addListener((obs, o, n) -> {
                String q = n.toLowerCase().trim();
                filtered.setPredicate(t -> q.isEmpty()
                        || (t.getTitle() != null && t.getTitle().toLowerCase().contains(q))
                        || (t.getContent() != null && t.getContent().toLowerCase().contains(q))
                        || (t.getFirstname() != null && t.getFirstname().toLowerCase().contains(q)));
            });

            HBox topBar = createTopBar(addBtn, search);

            TableView<Thread> table = createStyledTable();
            VBox.setVgrow(table, Priority.ALWAYS);

            TableColumn<Thread, Integer> colId = new TableColumn<>("ID");
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colId.setMaxWidth(60); colId.setMinWidth(60);

            TableColumn<Thread, String> colTitle = new TableColumn<>("Titre");
            colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));

            TableColumn<Thread, String> colAuthor = new TableColumn<>("Auteur");
            colAuthor.setCellValueFactory(cd -> new SimpleStringProperty(
                    cd.getValue().getFirstname() != null ? cd.getValue().getFirstname() : "Utilisateur #" + cd.getValue().getUserId()));

            TableColumn<Thread, Integer> colGroup = new TableColumn<>("Groupe ID");
            colGroup.setCellValueFactory(new PropertyValueFactory<>("groupId"));
            colGroup.setMaxWidth(80); colGroup.setMinWidth(80);

            TableColumn<Thread, String> colContent = new TableColumn<>("Contenu");
            colContent.setCellValueFactory(cd -> new SimpleStringProperty(truncate(cd.getValue().getContent(), 60)));

            TableColumn<Thread, String> colDate = new TableColumn<>("Date");
            colDate.setCellValueFactory(cd -> new SimpleStringProperty(formatDate(cd.getValue().getCreatedAt())));
            colDate.setMaxWidth(140); colDate.setMinWidth(140);

            TableColumn<Thread, Void> colActions = new TableColumn<>("Actions");
            colActions.setMaxWidth(170); colActions.setMinWidth(170);
            colActions.setCellFactory(col -> new TableCell<>() {
                private final HBox box = new HBox(6);
                private final Button editBtn = createEditButton();
                private final Button delBtn = createDeleteButton();
                {
                    editBtn.setOnAction(e -> {
                        Thread t = getTableView().getItems().get(getIndex());
                        openThreadForm(t);
                    });
                    delBtn.setOnAction(e -> {
                        Thread t = getTableView().getItems().get(getIndex());
                        confirmAndDelete("le thread '" + truncate(t.getTitle(), 30) + "'", () -> {
                            try { threadCRUD.supprimer(t.getId()); data.remove(t); showToast("Thread supprime"); }
                            catch (SQLException ex) { showError(ex.getMessage()); }
                        });
                    });
                    box.setAlignment(Pos.CENTER);
                    box.getChildren().addAll(editBtn, delBtn);
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });

            table.getColumns().addAll(colId, colTitle, colAuthor, colGroup, colContent, colDate, colActions);
            table.setItems(filtered);

            Label countLbl = createCountLabel(threads.size() + " threads au total");
            container.getChildren().addAll(topBar, countLbl, table);
        } catch (SQLException e) {
            container.getChildren().add(errorLabel(e.getMessage()));
        }
        setContent(container);
    }

    private void openThreadForm(Thread existing) {
        boolean isEdit = existing != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier le Thread" : "Nouveau Thread");
        dialog.setHeaderText(null);

        DialogPane dp = dialog.getDialogPane();
        dp.setPrefWidth(460);
        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dp.getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        styleDialogPane(dp);

        VBox form = new VBox(8);
        form.setPadding(new Insets(16));

        // Group dropdown
        ComboBox<String> groupCombo = new ComboBox<>();
        groupCombo.setPromptText("Selectionnez un groupe");
        groupCombo.setMaxWidth(Double.MAX_VALUE);
        groupCombo.setStyle(fieldStyle());
        Label groupErr = createErrLabel();

        List<Group> allGroups = List.of();
        try { allGroups = groupCRUD.afficher(); } catch (SQLException e) { LOG.warning(e.getMessage()); }
        for (Group g : allGroups) {
            groupCombo.getItems().add(g.getId() + " - " + g.getName());
        }
        if (isEdit) {
            for (int i = 0; i < allGroups.size(); i++) {
                if (allGroups.get(i).getId() == existing.getGroupId()) {
                    groupCombo.getSelectionModel().select(i); break;
                }
            }
        }
        final List<Group> groupList = allGroups;

        TextField titleField = createFormField(isEdit ? existing.getTitle() : "", "Titre du thread");
        Label titleErr = createErrLabel();

        TextArea contentField = new TextArea(isEdit ? existing.getContent() : "");
        contentField.setPromptText("Contenu (min 10 caracteres)");
        contentField.setPrefRowCount(4);
        contentField.setWrapText(true);
        contentField.setStyle(fieldStyle());
        Label contentErr = createErrLabel();

        form.getChildren().addAll(formLabel("Groupe *"), groupCombo, groupErr,
                formLabel("Titre *"), titleField, titleErr,
                formLabel("Contenu *"), contentField, contentErr);
        dp.setContent(form);

        final boolean[] saved = {false};
        dp.lookupButton(saveType).addEventFilter(ActionEvent.ACTION, evt -> {
            String title = titleField.getText().trim();
            String content = contentField.getText().trim();
            int groupIdx = groupCombo.getSelectionModel().getSelectedIndex();

            groupErr.setText(""); titleErr.setText(""); contentErr.setText("");
            groupCombo.setStyle(fieldStyle()); titleField.setStyle(fieldStyle()); contentField.setStyle(fieldStyle());

            boolean valid = true;
            if (!isEdit && groupIdx < 0) {
                groupErr.setText("Selectionnez un groupe");
                groupCombo.setStyle(errFieldStyle()); valid = false;
            }
            if (title.isEmpty()) {
                titleErr.setText("Le titre est obligatoire");
                titleField.setStyle(errFieldStyle()); valid = false;
            } else if (title.length() < 3) {
                titleErr.setText("Minimum 3 caracteres");
                titleField.setStyle(errFieldStyle()); valid = false;
            }
            if (content.isEmpty()) {
                contentErr.setText("Le contenu est obligatoire");
                contentField.setStyle(errFieldStyle()); valid = false;
            } else if (content.length() < 10) {
                contentErr.setText("Minimum 10 caracteres");
                contentField.setStyle(errFieldStyle()); valid = false;
            }
            if (!valid) { evt.consume(); return; }

            try {
                if (isEdit) {
                    existing.setTitle(title);
                    existing.setContent(content);
                    if (groupIdx >= 0) existing.setGroupId(groupList.get(groupIdx).getId());
                    threadCRUD.modifier(existing);
                } else {
                    int groupId = groupList.get(groupIdx).getId();
                    Thread t = new Thread(groupId, currentUserId, title, content);
                    threadCRUD.ajouter(t);
                }
                saved[0] = true;
            } catch (SQLException ex) {
                contentErr.setText("Erreur: " + ex.getMessage());
                evt.consume();
            }
        });

        dialog.showAndWait();
        if (saved[0]) { showToast(isEdit ? "Thread modifie" : "Thread ajoute"); showThreads(); }
    }

    // ====== EVENTS TABLE + CRUD ======

    @SuppressWarnings("unchecked")
    private void loadEventsTable() {
        VBox container = buildTableContainer("Gestion des Evenements", "Gerer tous les evenements communautaires");

        try {
            List<Event> events = eventCRUD.afficher();
            ObservableList<Event> data = FXCollections.observableArrayList(events);
            FilteredList<Event> filtered = new FilteredList<>(data, p -> true);

            Button addBtn = createAddButton("+ Ajouter un Evenement");
            addBtn.setOnAction(e -> openEventForm(null));

            TextField search = createSearchField("Rechercher un evenement...");
            search.textProperty().addListener((obs, o, n) -> {
                String q = n.toLowerCase().trim();
                filtered.setPredicate(ev -> q.isEmpty()
                        || (ev.getTitle() != null && ev.getTitle().toLowerCase().contains(q))
                        || (ev.getDescription() != null && ev.getDescription().toLowerCase().contains(q)));
            });

            HBox topBar = createTopBar(addBtn, search);

            TableView<Event> table = createStyledTable();
            VBox.setVgrow(table, Priority.ALWAYS);

            TableColumn<Event, Integer> colId = new TableColumn<>("ID");
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colId.setMaxWidth(60); colId.setMinWidth(60);

            TableColumn<Event, String> colTitle = new TableColumn<>("Titre");
            colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));

            TableColumn<Event, String> colDesc = new TableColumn<>("Description");
            colDesc.setCellValueFactory(cd -> new SimpleStringProperty(truncate(cd.getValue().getDescription(), 80)));

            TableColumn<Event, Integer> colCapacity = new TableColumn<>("Capacite");
            colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
            colCapacity.setMaxWidth(80); colCapacity.setMinWidth(80);

            TableColumn<Event, String> colEventDate = new TableColumn<>("Date Evenement");
            colEventDate.setCellValueFactory(cd -> new SimpleStringProperty(formatDate(cd.getValue().getEventDate())));
            colEventDate.setMaxWidth(140); colEventDate.setMinWidth(140);

            TableColumn<Event, String> colCreated = new TableColumn<>("Cree le");
            colCreated.setCellValueFactory(cd -> new SimpleStringProperty(formatDate(cd.getValue().getCreatedAt())));
            colCreated.setMaxWidth(140); colCreated.setMinWidth(140);

            TableColumn<Event, Void> colActions = new TableColumn<>("Actions");
            colActions.setMaxWidth(170); colActions.setMinWidth(170);
            colActions.setCellFactory(col -> new TableCell<>() {
                private final HBox box = new HBox(6);
                private final Button editBtn = createEditButton();
                private final Button delBtn = createDeleteButton();
                {
                    editBtn.setOnAction(e -> {
                        Event ev = getTableView().getItems().get(getIndex());
                        openEventForm(ev);
                    });
                    delBtn.setOnAction(e -> {
                        Event ev = getTableView().getItems().get(getIndex());
                        confirmAndDelete("l'evenement '" + ev.getTitle() + "'", () -> {
                            try { eventCRUD.supprimer(ev.getId()); data.remove(ev); showToast("Evenement supprime"); }
                            catch (SQLException ex) { showError(ex.getMessage()); }
                        });
                    });
                    box.setAlignment(Pos.CENTER);
                    box.getChildren().addAll(editBtn, delBtn);
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });

            table.getColumns().addAll(colId, colTitle, colDesc, colCapacity, colEventDate, colCreated, colActions);
            table.setItems(filtered);

            Label countLbl = createCountLabel(events.size() + " evenements au total");
            container.getChildren().addAll(topBar, countLbl, table);
        } catch (SQLException e) {
            container.getChildren().add(errorLabel(e.getMessage()));
        }
        setContent(container);
    }

    private void openEventForm(Event existing) {
        boolean isEdit = existing != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier l'Evenement" : "Nouvel Evenement");
        dialog.setHeaderText(null);

        DialogPane dp = dialog.getDialogPane();
        dp.setPrefWidth(460);
        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dp.getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        styleDialogPane(dp);

        VBox form = new VBox(8);
        form.setPadding(new Insets(16));

        TextField titleField = createFormField(isEdit ? existing.getTitle() : "", "Titre de l'evenement");
        Label titleErr = createErrLabel();

        TextArea descField = new TextArea(isEdit && existing.getDescription() != null ? existing.getDescription() : "");
        descField.setPromptText("Description (min 5 caracteres)");
        descField.setPrefRowCount(3);
        descField.setWrapText(true);
        descField.setStyle(fieldStyle());
        Label descErr = createErrLabel();

        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Date de l'evenement");
        datePicker.setMaxWidth(Double.MAX_VALUE);
        if (isEdit && existing.getEventDate() != null) {
            datePicker.setValue(existing.getEventDate().toLocalDateTime().toLocalDate());
        }
        Label dateErr = createErrLabel();

        TextField timeField = createFormField(
                isEdit && existing.getEventDate() != null
                        ? existing.getEventDate().toLocalDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                        : "",
                "Heure (HH:mm)");
        Label timeErr = createErrLabel();

        TextField capField = createFormField(
                isEdit ? String.valueOf(existing.getCapacity()) : "", "Capacite (nombre entier > 0)");
        Label capErr = createErrLabel();

        form.getChildren().addAll(
                formLabel("Titre *"), titleField, titleErr,
                formLabel("Description *"), descField, descErr,
                formLabel("Date *"), datePicker, dateErr,
                formLabel("Heure *"), timeField, timeErr,
                formLabel("Capacite *"), capField, capErr);
        dp.setContent(form);

        final boolean[] saved = {false};
        dp.lookupButton(saveType).addEventFilter(ActionEvent.ACTION, evt -> {
            String title = titleField.getText().trim();
            String desc = descField.getText().trim();
            LocalDate date = datePicker.getValue();
            String timeStr = timeField.getText().trim();
            String capStr = capField.getText().trim();

            titleErr.setText(""); descErr.setText(""); dateErr.setText("");
            timeErr.setText(""); capErr.setText("");
            titleField.setStyle(fieldStyle()); descField.setStyle(fieldStyle());
            capField.setStyle(fieldStyle()); timeField.setStyle(fieldStyle());

            boolean valid = true;
            if (title.isEmpty()) {
                titleErr.setText("Le titre est obligatoire");
                titleField.setStyle(errFieldStyle()); valid = false;
            } else if (title.length() < 3) {
                titleErr.setText("Minimum 3 caracteres");
                titleField.setStyle(errFieldStyle()); valid = false;
            }
            if (desc.isEmpty()) {
                descErr.setText("La description est obligatoire");
                descField.setStyle(errFieldStyle()); valid = false;
            } else if (desc.length() < 5) {
                descErr.setText("Minimum 5 caracteres");
                descField.setStyle(errFieldStyle()); valid = false;
            }
            if (date == null) {
                dateErr.setText("La date est obligatoire");
                valid = false;
            }
            LocalTime time = null;
            if (timeStr.isEmpty()) {
                timeErr.setText("L'heure est obligatoire");
                timeField.setStyle(errFieldStyle()); valid = false;
            } else {
                try { time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm")); }
                catch (Exception e) { timeErr.setText("Format invalide (HH:mm)"); timeField.setStyle(errFieldStyle()); valid = false; }
            }
            int cap = 0;
            if (capStr.isEmpty()) {
                capErr.setText("La capacite est obligatoire");
                capField.setStyle(errFieldStyle()); valid = false;
            } else {
                try {
                    cap = Integer.parseInt(capStr);
                    if (cap <= 0) { capErr.setText("Doit etre > 0"); capField.setStyle(errFieldStyle()); valid = false; }
                } catch (NumberFormatException e) {
                    capErr.setText("Nombre entier requis"); capField.setStyle(errFieldStyle()); valid = false;
                }
            }
            if (!valid) { evt.consume(); return; }

            try {
                Timestamp eventTs = Timestamp.valueOf(LocalDateTime.of(date, time));
                if (isEdit) {
                    existing.setTitle(title);
                    existing.setDescription(desc);
                    existing.setEventDate(eventTs);
                    existing.setCapacity(cap);
                    eventCRUD.modifier(existing);
                } else {
                    Event ev = new Event();
                    ev.setTitle(title);
                    ev.setDescription(desc);
                    ev.setEventDate(eventTs);
                    ev.setCapacity(cap);
                    ev.setUserId(currentUserId);
                    eventCRUD.ajouter(ev);
                }
                saved[0] = true;
            } catch (SQLException ex) {
                String msg = ex.getMessage();
                if (msg != null && msg.contains("EVENT_TITLE_EXISTS")) {
                    titleErr.setText("Ce titre d'evenement existe deja");
                    titleField.setStyle(errFieldStyle());
                } else {
                    titleErr.setText("Erreur: " + msg);
                }
                evt.consume();
            }
        });

        dialog.showAndWait();
        if (saved[0]) { showToast(isEdit ? "Evenement modifie" : "Evenement ajoute"); showEvents(); }
    }

    // ====== SHARED UTILITY METHODS ======

    private VBox buildTableContainer(String title, String subtitle) {
        VBox container = new VBox(10);
        container.setPadding(new Insets(20, 24, 20, 24));
        VBox.setVgrow(container, Priority.ALWAYS);

        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        titleLbl.setTextFill(Color.web("#2c3e50"));

        Label subLbl = new Label(subtitle);
        subLbl.setFont(Font.font("Segoe UI", 12));
        subLbl.setTextFill(Color.web("#7f8c8d"));

        container.getChildren().addAll(titleLbl, subLbl);
        return container;
    }

    private <T> TableView<T> createStyledTable() {
        TableView<T> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
                + "-fx-border-color: #e8e8e8; -fx-border-radius: 8; -fx-border-width: 1;");
        table.setFixedCellSize(40);
        table.setPlaceholder(new Label("Aucune donnee"));
        return table;
    }

    private TextField createSearchField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(380);
        field.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
                + "-fx-border-color: #ddd; -fx-border-radius: 8; -fx-border-width: 1; "
                + "-fx-padding: 8 14; -fx-font-size: 13;");
        field.focusedProperty().addListener((obs, o, n) -> {
            field.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
                    + "-fx-border-color: " + (n ? "#667eea" : "#ddd") + "; -fx-border-radius: 8; "
                    + "-fx-border-width: " + (n ? "1.5" : "1") + "; -fx-padding: 8 14; -fx-font-size: 13;");
        });
        return field;
    }

    private HBox createTopBar(Button addBtn, TextField search) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(12, addBtn, spacer, search);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private Button createAddButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; "
                + "-fx-background-radius: 8; -fx-font-size: 12; -fx-font-weight: bold; "
                + "-fx-cursor: hand; -fx-padding: 8 18;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #5a6fd6; -fx-text-fill: white; "
                + "-fx-background-radius: 8; -fx-font-size: 12; -fx-font-weight: bold; "
                + "-fx-cursor: hand; -fx-padding: 8 18;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; "
                + "-fx-background-radius: 8; -fx-font-size: 12; -fx-font-weight: bold; "
                + "-fx-cursor: hand; -fx-padding: 8 18;"));
        return btn;
    }

    private Button createEditButton() {
        Button btn = new Button("Modifier");
        btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; "
                + "-fx-background-radius: 6; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 4 10;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; "
                + "-fx-background-radius: 6; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 4 10;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; "
                + "-fx-background-radius: 6; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 4 10;"));
        return btn;
    }

    private Button createDeleteButton() {
        Button btn = new Button("Supprimer");
        btn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; "
                + "-fx-background-radius: 6; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 4 10;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; "
                + "-fx-background-radius: 6; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 4 10;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; "
                + "-fx-background-radius: 6; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 4 10;"));
        return btn;
    }

    private Label createCountLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", 11));
        lbl.setTextFill(Color.web("#95a5a6"));
        return lbl;
    }

    private Label errorLabel(String msg) {
        Label lbl = new Label("Erreur: " + msg);
        lbl.setTextFill(Color.web("#e74c3c"));
        lbl.setFont(Font.font("Segoe UI", 13));
        return lbl;
    }

    private String truncate(String text, int max) {
        if (text == null || text.isBlank()) return "-";
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }

    private String formatDate(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime().format(DATE_FMT) : "-";
    }

    // ====== FORM UTILITY METHODS ======

    private void styleDialogPane(DialogPane dp) {
        dp.setStyle("-fx-background-color: #f8f9fa; -fx-font-family: 'Segoe UI';");
    }

    private Label formLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        lbl.setTextFill(Color.web("#2c3e50"));
        return lbl;
    }

    private TextField createFormField(String value, String prompt) {
        TextField field = new TextField(value);
        field.setPromptText(prompt);
        field.setStyle(fieldStyle());
        return field;
    }

    private Label createErrLabel() {
        Label lbl = new Label();
        lbl.setTextFill(Color.web("#e74c3c"));
        lbl.setFont(Font.font("Segoe UI", 10));
        lbl.setWrapText(true);
        return lbl;
    }

    private String fieldStyle() {
        return "-fx-background-color: white; -fx-background-radius: 6; "
                + "-fx-border-color: #ddd; -fx-border-radius: 6; -fx-border-width: 1; "
                + "-fx-padding: 6 10; -fx-font-size: 13;";
    }

    private String errFieldStyle() {
        return "-fx-background-color: #fff5f5; -fx-background-radius: 6; "
                + "-fx-border-color: #e74c3c; -fx-border-radius: 6; -fx-border-width: 1.5; "
                + "-fx-padding: 6 10; -fx-font-size: 13;";
    }

    // ====== TOAST / DIALOGS ======

    private void confirmAndDelete(String item, Runnable action) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer " + item + " ?");
        alert.setContentText("Cette action est irreversible.");
        alert.getButtonTypes().setAll(
                new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE),
                new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE));
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            action.run();
        }
    }

    private void showToast(String message) {
        Label toast = new Label(message);
        toast.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        toast.setTextFill(Color.WHITE);
        toast.setStyle("-fx-background-color: #27ae60; -fx-background-radius: 8; "
                + "-fx-padding: 10 24; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);");
        StackPane.setAlignment(toast, Pos.TOP_CENTER);
        StackPane.setMargin(toast, new Insets(10, 0, 0, 0));

        rootStack.getChildren().add(toast);
        toast.setOpacity(0);
        toast.setTranslateY(-15);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setToValue(1);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(200), toast);
        slideIn.setToY(0);
        new ParallelTransition(fadeIn, slideIn).play();

        PauseTransition hold = new PauseTransition(Duration.seconds(2));
        hold.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> rootStack.getChildren().remove(toast));
            fadeOut.play();
        });
        hold.play();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
