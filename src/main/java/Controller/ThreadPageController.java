package Controller;

import Entites.Comment;
import Entites.Group;
import Entites.Thread;
import Services.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.sql.SQLException;
import java.util.List;

public class ThreadPageController {
    @FXML
    private Label commentCountLabel;

    @FXML
    private Label threadTitle;

    @FXML
    private Label threadContent;

    @FXML
    private VBox commentsContainer;

    @FXML
    private TextArea commentField;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    private Thread thread;
    private Group group;

    private final int currentUserId = 1;
    @FXML
    private Label summaryLabel;

    private final AiSummaryService summaryService = new AiSummaryService();
    private String buildSummaryPrompt(Thread thread, List<Comment> comments) {
        StringBuilder sb = new StringBuilder();

        sb.append("ROLE: You summarize a community discussion thread.\n");
        sb.append("You will receive THREAD INFO and COMMENTS.\n");
        sb.append("THREAD INFO is CONTEXT, NOT comments.\n\n");

        sb.append("THREAD INFO (CONTEXT ONLY)\n");
        sb.append("TITLE: ").append(safe(thread.getTitle())).append("\n");
        sb.append("DESCRIPTION: ").append(safe(thread.getContent())).append("\n\n");

        sb.append("COMMENTS (oldest -> newest)\n");
        sb.append("Each comment has author_firstname, created_at, content.\n\n");

        int i = 1;
        for (Comment c : comments) {
            String name = safe(c.getFirstname());
            String created = (c.getCreatedAt() == null) ? "" : c.getCreatedAt().toString();
            String content = safe(c.getContent()).replace("\r", " ").replace("\n", " ").trim();

            sb.append(i++).append(") author_firstname: ").append(name.isBlank() ? "Unknown" : name).append("\n");
            sb.append("   created_at: ").append(created).append("\n");
            sb.append("   content: ").append(content).append("\n\n");
        }

        sb.append("IMPORTANT RULES (MUST FOLLOW)\n");
        sb.append("1) Treat TITLE/DESCRIPTION as context only.\n");
        sb.append("2) DO NOT mention any usernames/firstnames.\n");
        sb.append("3) DO NOT mention any dates/timestamps.\n");
        sb.append("4) Ignore filler like 'aaa', random letters.\n");
        sb.append("5) Do not invent facts.\n\n");

        sb.append("OUTPUT FORMAT (strict)\n");
        sb.append("Topic: <3-6 words>\n");
        sb.append("Summary: <1-2 sentences>\n");
        sb.append("Key points:\n- <max 3 bullets>\n");

        return sb.toString();
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }



    @FXML
    private void generateSummary() {

        summaryLabel.setText("⏳ Generating summary...");

        EmailAsync.run(() -> {
            try {
                CommentCRUD crud = new CommentCRUD();
                List<Comment> all = crud.afficherByThread(thread.getId());

                // last 20 comments max
                int start = Math.max(0, all.size() - 20);
                List<Comment> comments = all.subList(start, all.size());

                // ✅ build the prompt with title/description + labeled comments
                String prompt = buildSummaryPrompt(thread, comments);

                // ✅ IMPORTANT: call instance method (NOT static)
                String summary = summaryService.summarize(prompt);

                Platform.runLater(() -> summaryLabel.setText(summary));

            } catch (Exception ex) {
                Platform.runLater(() ->
                        summaryLabel.setText("❌ Summary failed: " + ex.getMessage())
                );
            }
        });
    }

    private String freeCensor(String text) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = "https://www.purgomalum.com/service/plain?text=" + encoded;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() >= 300) return text; // fallback if service fails
            return res.body(); // already the censored text

        } catch (Exception e) {
            return text; // fallback
        }
    }

    public void setThread(Thread thread) {
        this.thread = thread;

        threadTitle.setText(thread.getTitle());
        threadContent.setText(thread.getContent());

        if (thread.getUserId() != currentUserId) {
            editButton.setVisible(false);
            deleteButton.setVisible(false);
        }

        loadComments();
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    private void loadComments() {

        commentsContainer.getChildren().clear();

        CommentCRUD crud = new CommentCRUD();

        try {

            List<Comment> comments = crud.afficherByThread(thread.getId());

            commentCountLabel.setText("Comments: " + comments.size());

            for (Comment comment : comments) {

                VBox card = new VBox(8);
                card.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-padding: 15;" +
                                "-fx-background-radius: 14;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 12, 0, 0, 4);"
                );

                Label userLabel = new Label("👤 " + comment.getFirstname());
                userLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

                Label contentLabel = new Label(comment.getContent());
                contentLabel.setWrapText(true);
                contentLabel.setStyle("-fx-text-fill:#111827; -fx-font-size:13px;");

                VBox contentBox = new VBox(6);
                contentBox.getChildren().add(contentLabel);

                Label dateLabel = new Label(
                        comment.getCreatedAt().toLocalDateTime()
                                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                );
                dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");

                card.getChildren().addAll(userLabel, contentBox, dateLabel);

                // 🔴 IF OWNER
                if (comment.getUserId() == currentUserId) {

                    Button editBtn = new Button("✏");
                    editBtn.setStyle(
                            "-fx-background-color:#3b82f6;" +
                                    "-fx-text-fill:white;" +
                                    "-fx-background-radius:6;" +
                                    "-fx-font-size:11px;"
                    );

                    Button deleteBtn = new Button("🗑");
                    deleteBtn.setStyle(
                            "-fx-background-color:#ef4444;" +
                                    "-fx-text-fill:white;" +
                                    "-fx-background-radius:6;" +
                                    "-fx-font-size:11px;"
                    );

                    // Hover effect
                    deleteBtn.setOnMouseEntered(e ->
                            deleteBtn.setStyle(deleteBtn.getStyle() + "-fx-opacity:0.85;")
                    );
                    deleteBtn.setOnMouseExited(e ->
                            deleteBtn.setStyle(deleteBtn.getStyle().replace("-fx-opacity:0.85;", ""))
                    );
                    editBtn.setOnMouseEntered(e ->
                            editBtn.setStyle(editBtn.getStyle() + "-fx-opacity:0.85;")
                    );
                    editBtn.setOnMouseExited(e ->
                            editBtn.setStyle(editBtn.getStyle().replace("-fx-opacity:0.85;", ""))
                    );

                    // Delete
                    deleteBtn.setOnAction(e -> deleteComment(comment.getId()));

                    // Edit (inline + AI censor)
                    editBtn.setOnAction(e -> {

                        TextArea editor = new TextArea(contentLabel.getText());
                        editor.setWrapText(true);
                        editor.setPrefRowCount(3);
                        editor.setStyle("-fx-background-radius: 10;");

                        Button saveBtn = new Button("Save");
                        saveBtn.setStyle(
                                "-fx-background-color:#22c55e;" +
                                        "-fx-text-fill:white;" +
                                        "-fx-background-radius:8;" +
                                        "-fx-font-size:11px;"
                        );

                        Button cancelBtn = new Button("Cancel");
                        cancelBtn.setStyle(
                                "-fx-background-color:#e5e7eb;" +
                                        "-fx-text-fill:#111827;" +
                                        "-fx-background-radius:8;" +
                                        "-fx-font-size:11px;"
                        );

                        HBox editActions = new HBox(8, saveBtn, cancelBtn);
                        editActions.setStyle("-fx-padding: 4 0 0 0;");

                        // swap label -> editor in same place
                        contentBox.getChildren().setAll(editor, editActions);

                        // lock edit/delete while editing
                        editBtn.setDisable(true);
                        deleteBtn.setDisable(true);

                        cancelBtn.setOnAction(ev2 -> {
                            contentBox.getChildren().setAll(contentLabel);
                            editBtn.setDisable(false);
                            deleteBtn.setDisable(false);
                        });

                        saveBtn.setOnAction(ev2 -> {
                            String newText = editor.getText().trim();

                            if (newText.isEmpty()) {
                                showAlert("Comment cannot be empty.");
                                return;
                            }
                            if (newText.length() > 500) {
                                showAlert("Comment too long (max 500 characters).");
                                return;
                            }

                            try {
                                String cleaned = freeCensor(newText);

                                CommentCRUD cCrud = new CommentCRUD();
                                cCrud.modifier(comment.getId(), cleaned);

                                contentLabel.setText(cleaned);
                                contentBox.getChildren().setAll(contentLabel);

                            } catch (SQLException ex) {
                                showAlert("Error updating comment: " + ex.getMessage());
                                contentBox.getChildren().setAll(contentLabel);
                            } finally {
                                editBtn.setDisable(false);
                                deleteBtn.setDisable(false);
                            }

                        });
                    });

                    HBox actions = new HBox(6, editBtn, deleteBtn);
                    actions.setStyle("-fx-padding: 5 0 0 0;");

                    card.getChildren().add(actions);
                }

                commentsContainer.getChildren().add(card);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }



    @FXML
    private void addComment() {

        String text = commentField.getText().trim();

        if (text.isEmpty()) {
            showAlert("Comment cannot be empty.");
            return;
        }
        if (text.length() > 500) {
            showAlert("Comment too long (max 500 characters).");
            return;
        }

        String cleaned = freeCensor(text);

        Comment comment = new Comment(thread.getId(), currentUserId, cleaned);

        CommentCRUD crud = new CommentCRUD();
        try {
            crud.ajouter(comment);

            EmailAsync.run(() -> {
                new NotificationEmailService()
                        .sendThreadCommentMail(thread.getId(), currentUserId, cleaned);
            });

            commentField.clear();
            loadComments();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            showAlert("DB error: " + e.getMessage());
        }


    }


    @FXML
    private void editThread() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/EditThread.fxml"));
            Parent root = loader.load();

            EditThreadController controller = loader.getController();
            controller.setThread(thread);
            controller.setGroup(group);

            threadTitle.getScene().setRoot(root);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @FXML
    private void deleteThread() {

        if (!confirmAction("Delete Thread",
                "Are you sure you want to delete this thread?"))
            return;

        ThreadCRUD crud = new ThreadCRUD();

        try {
            crud.supprimer(thread.getId());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        goBack();
    }

    private void deleteComment(int commentId) {

        if (!confirmAction("Delete Comment",
                "Are you sure you want to delete this comment?"))
            return;

        CommentCRUD crud = new CommentCRUD();

        try {
            crud.supprimer(commentId);
            loadComments();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }




    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GroupDashboard.fxml"));
            Parent root = loader.load();

            GroupDashboardController controller = loader.getController();
            controller.setGroup(group);

            threadTitle.getScene().setRoot(root);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    private boolean confirmAction(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        ButtonType yesBtn = new ButtonType("Yes");
        ButtonType noBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(yesBtn, noBtn);

        return alert.showAndWait().orElse(noBtn) == yesBtn;
    }

}
