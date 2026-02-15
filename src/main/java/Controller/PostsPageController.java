package Controller;

import Entites.Post;
import Services.PostCRUD;
import Services.PostReactionCRUD;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PostsPageController {

    @FXML private ScrollPane feedScroll;

    @FXML private VBox postsContainer;

    @FXML private TextArea contentArea;
    @FXML private Label imageStatusLabel;

    @FXML private ComboBox<String> sortComboBox;
    @FXML private TextField searchField;

    private final PostCRUD postCRUD = new PostCRUD();
    private final PostReactionCRUD reactionCRUD = new PostReactionCRUD();

    // Change this to your real session getter
    private final int currentUserId = 1;  // TEMP: will be replaced by Session later


    private String selectedImageUrl = null;
    private List<Post> cachedFeed = new ArrayList<>();

    @FXML
    public void initialize() {
        sortComboBox.getItems().addAll("Newest", "Oldest", "Most reacted");
        sortComboBox.setValue("Newest");
        loadFeed();
    }

    @FXML
    private void handleRefresh() {
        loadFeed();
    }

    @FXML
    private void handleSortChange() {
        applyFiltersAndRender();
    }

    @FXML
    private void handleSearch() {
        applyFiltersAndRender();
    }

    @FXML
    private void focusComposer() {
        contentArea.requestFocus();
    }

    @FXML
    private void handlePickImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose an image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        File file = fc.showOpenDialog(contentArea.getScene().getWindow());
        if (file != null) {
            // For now store local file url. Later you can copy it to uploads folder.
            selectedImageUrl = file.toURI().toString();
            imageStatusLabel.setText("Selected: " + file.getName());
        }
    }

    @FXML
    private void handleCreatePost() {
        String content = contentArea.getText() == null ? "" : contentArea.getText().trim();
        if (content.isEmpty() && (selectedImageUrl == null || selectedImageUrl.isBlank())) {
            showAlert(Alert.AlertType.WARNING, "Empty post", "Write something or add an image.");
            return;
        }

        try {
            Post p = new Post(currentUserId, content);
            p.setImageUrl(selectedImageUrl);
            postCRUD.ajouter(p);

            // reset composer
            contentArea.clear();
            selectedImageUrl = null;
            imageStatusLabel.setText("");

            loadFeed();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "DB Error", e.getMessage());
        }
    }

    private void loadFeed() {
        try {
            // Use your feed method (recommended). If you don't have it yet, tell me and I’ll adapt.
            cachedFeed = postCRUD.afficherFeed(currentUserId);
            applyFiltersAndRender();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "DB Error", e.getMessage());
        }
    }

    private void applyFiltersAndRender() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String sort = sortComboBox.getValue();

        List<Post> filtered = new ArrayList<>();
        for (Post p : cachedFeed) {
            String hay = (p.getFirstname() + " " + (p.getLastname() == null ? "" : p.getLastname()) + " " + p.getContent())
                    .toLowerCase();
            if (q.isEmpty() || hay.contains(q)) filtered.add(p);
        }

        if ("Oldest".equals(sort)) {
            filtered.sort(Comparator.comparing(Post::getCreatedAt));
        } else if ("Most reacted".equals(sort)) {
            filtered.sort(Comparator.comparingInt(Post::getReactionsCount).reversed());
        }
        else { // Newest
            filtered.sort(Comparator.comparing(Post::getCreatedAt).reversed());
        }

        renderFeed(filtered);
    }

    private void renderFeed(List<Post> posts) {
        postsContainer.getChildren().clear();

        if (posts.isEmpty()) {
            Label empty = new Label("No posts yet. Be the first to post!");
            empty.setStyle("-fx-text-fill:#64748b; -fx-padding: 20;");
            postsContainer.getChildren().add(empty);
            return;
        }

        for (Post p : posts) {
            postsContainer.getChildren().add(buildPostCard(p));
        }
    }

    private VBox buildPostCard(Post p) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        // Header (author + date + actions)
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox left = new VBox(2);
        Label author = new Label(p.getFirstname() + " " + (p.getLastname() == null ? "" : p.getLastname()));
        author.getStyleClass().add("post-author");

        String dateText = (p.getCreatedAt() == null)
                ? ""
                : p.getCreatedAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm"));

        Label meta = new Label(dateText);
        meta.getStyleClass().add("meta-text");

        left.getChildren().addAll(author, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Right actions (edit/delete only for owner)
        HBox actions = new HBox(8);

        if (p.getUserId() == currentUserId) {
            Button editBtn = new Button("Edit");
            editBtn.getStyleClass().add("icon-btn");
            editBtn.setOnAction(e -> openEditDialog(p));

            Button delBtn = new Button("Delete");
            delBtn.getStyleClass().add("danger-btn");
            delBtn.setOnAction(e -> handleDeletePost(p));

            actions.getChildren().addAll(editBtn, delBtn);
        }

        header.getChildren().addAll(left, spacer, actions);

        // Content
        Label content = new Label(p.getContent() == null ? "" : p.getContent());
        content.setWrapText(true);
        content.getStyleClass().add("post-content");

        card.getChildren().addAll(header, content);

        // Image (optional)
        if (p.getImageUrl() != null && !p.getImageUrl().isBlank()) {
            try {
                ImageView img = new ImageView(new Image(p.getImageUrl(), true));
                img.setFitWidth(300);        // nice clean size
                img.setPreserveRatio(true);
                img.setSmooth(true);

                StackPane imageWrapper = new StackPane(img);
                imageWrapper.setStyle("""
            -fx-background-color: #f1f5f9;
            -fx-padding: 10;
            -fx-background-radius: 12;
        """);

                card.getChildren().add(imageWrapper);

            } catch (Exception ignored) {}
        }


        // Footer (reactions)
        HBox footer = new HBox(10);
        footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Button reactBtn = new Button(reactionLabel(p.getMyReaction()));
        reactBtn.getStyleClass().add("icon-btn");   // reuse your nice button style

        Label countLbl = new Label(p.getReactionsCount() + " reactions");
        countLbl.getStyleClass().add("meta-text");

// context menu (styled)
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("reaction-menu");

        menu.getItems().addAll(
                reactionItem("👍 Like", "LIKE", p, reactBtn, countLbl),
                reactionItem("❤️ Love", "LOVE", p, reactBtn, countLbl),
                reactionItem("😂 Haha", "HAHA", p, reactBtn, countLbl),
                reactionItem("😮 Wow",  "WOW",  p, reactBtn, countLbl),
                reactionItem("😢 Sad",  "SAD",  p, reactBtn, countLbl),
                reactionItem("😡 Angry","ANGRY",p, reactBtn, countLbl),
                new SeparatorMenuItem(),
                clearReactionItem(p, reactBtn, countLbl)
        );

// open menu on click (nice & stable)
        reactBtn.setOnAction(e -> {
            if (menu.isShowing()) menu.hide();
            else menu.show(reactBtn, javafx.geometry.Side.BOTTOM, 0, 6);
        });

        footer.getChildren().addAll(reactBtn, countLbl);
        card.getChildren().add(footer);



        return card;
    }
    private MenuItem reactionItem(String label, String type, Post p, Button btn, Label countLbl) {
        MenuItem item = new MenuItem(label);
        item.setOnAction(e -> setReactionNoRefresh(p, type, btn, countLbl));
        return item;
    }

    private MenuItem clearReactionItem(Post p, Button btn, Label countLbl) {
        MenuItem item = new MenuItem("Remove reaction");
        item.setOnAction(e -> clearReactionNoRefresh(p, btn, countLbl));
        return item;
    }

    private void setReactionNoRefresh(Post p, String newType, Button btn, Label countLbl) {
        String oldType = p.getMyReaction();
        int oldCount = p.getReactionsCount();

        // optimistic
        if (oldType == null) p.setReactionsCount(oldCount + 1); // first time reacting
        p.setMyReaction(newType);

        btn.setText(reactionLabel(p.getMyReaction()));
        countLbl.setText(p.getReactionsCount() + " reactions");

        btn.setDisable(true);
        try {
            reactionCRUD.setReaction(p.getId(), currentUserId,
                    PostReactionCRUD.ReactionType.valueOf(newType));
        } catch (SQLException ex) {
            // rollback exact
            p.setMyReaction(oldType);
            p.setReactionsCount(oldCount);

            btn.setText(reactionLabel(p.getMyReaction()));
            countLbl.setText(p.getReactionsCount() + " reactions");

            showAlert(Alert.AlertType.ERROR, "DB Error", ex.getMessage());
        } finally {
            btn.setDisable(false);
        }
    }

    private void clearReactionNoRefresh(Post p, Button btn, Label countLbl) {
        String oldType = p.getMyReaction();
        int oldCount = p.getReactionsCount();
        if (oldType == null) return;

        // optimistic
        p.setMyReaction(null);
        p.setReactionsCount(Math.max(0, oldCount - 1));

        btn.setText(reactionLabel(p.getMyReaction()));
        countLbl.setText(p.getReactionsCount() + " reactions");

        btn.setDisable(true);
        try {
            reactionCRUD.removeReaction(p.getId(), currentUserId);
        } catch (SQLException ex) {
            // rollback exact
            p.setMyReaction(oldType);
            p.setReactionsCount(oldCount);

            btn.setText(reactionLabel(p.getMyReaction()));
            countLbl.setText(p.getReactionsCount() + " reactions");

            showAlert(Alert.AlertType.ERROR, "DB Error", ex.getMessage());
        } finally {
            btn.setDisable(false);
        }
    }




    private String reactionLabel(String myReaction) {
        if (myReaction == null) return "React";
        return switch (myReaction) {
            case "LIKE" -> "👍 Like";
            case "LOVE" -> "❤️ Love";
            case "HAHA" -> "😂 Haha";
            case "WOW"  -> "😮 Wow";
            case "SAD"  -> "😢 Sad";
            case "ANGRY"-> "😡 Angry";
            default -> "React";
        };
    }

    private void handleDeletePost(Post p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete post");
        confirm.setHeaderText("Delete this post?");
        confirm.setContentText("This action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            postCRUD.supprimer(p.getId());
            loadFeed();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "DB Error", e.getMessage());
        }
    }

    private void openEditDialog(Post p) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit post");

        TextArea ta = new TextArea(p.getContent());
        ta.setWrapText(true);
        ta.setPrefRowCount(5);

        VBox box = new VBox(10, new Label("Update your post:"), ta);
        box.setStyle("-fx-padding: 15;");

        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        String newText = ta.getText() == null ? "" : ta.getText().trim();
        if (newText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Invalid", "Post content cannot be empty.");
            return;
        }

        try {
            p.setContent(newText);
            postCRUD.modifier(p);
            loadFeed();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "DB Error", e.getMessage());
        }
    }

    @FXML
    private void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/CommunityHomePage.fxml"));
            postsContainer.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace(); // important to see the real error
        }
    }


    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
