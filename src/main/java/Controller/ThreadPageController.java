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
import Services.AiReplyService;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class ThreadPageController {


    @FXML private Button addCommentBtn;

    // prevents double-submit even if user spams
    private volatile boolean isPostingComment = false;
    @FXML private Label replyStatusLabel;
    private final AiReplyService replyService = new AiReplyService();
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


    private String buildReplyPrompt(Thread thread, List<Comment> comments) {
        int count = (comments == null) ? 0 : comments.size();

        StringBuilder sb = new StringBuilder();

        sb.append("TASK: Generate 3 READY-TO-POST replies as a comment.\n\n");

        sb.append("ABSOLUTE RULES:\n");
        sb.append("- Each suggestion must be a DIRECT reply message I can post.\n");
        sb.append("- Write in FIRST PERSON (\"I\", \"we\") and address the reader as \"you\".\n");
        sb.append("- DO NOT describe the conversation or participants (NO: \"someone\", \"the person\", \"they\", \"seems\", \"this thread is...\").\n");
        sb.append("- DO NOT invent facts beyond the given text.\n");
        sb.append("- If context is unclear/minimal: output friendly generic replies (greeting + ask clarification).\n");
        sb.append("- No names or dates.\n");
        sb.append("- 1–2 sentences per reply.\n\n");

        sb.append("THREAD:\n");
        sb.append("title: ").append(safe(thread.getTitle())).append("\n");
        sb.append("content: ").append(safe(thread.getContent())).append("\n\n");

        sb.append("COMMENTS_COUNT: ").append(count).append("\n");
        sb.append("COMMENTS (oldest -> newest):\n");

        if (count == 0) {
            sb.append("(none)\n");
            sb.append("IMPORTANT: Since there are no comments, replies MUST be generic and ask for clarification.\n");
        } else {
            int i = 1;
            for (Comment c : comments) {
                String content = safe(c.getContent()).replace("\r", " ").replace("\n", " ").trim();
                if (!content.isBlank()) sb.append(i++).append(") ").append(content).append("\n");
            }
        }

        sb.append("\nOUTPUT FORMAT (STRICT):\n");
        sb.append("1) <ready-to-post reply>\n");
        sb.append("2) <ready-to-post reply>\n");
        sb.append("3) <ready-to-post reply>\n");

        return sb.toString();
    }
    @FXML
    private void onSuggestReply() {

        // quick UI feedback
        replyStatusLabel.setText("⏳ Generating reply suggestions..."); // reuse your label or create a new label if you want

        EmailAsync.run(() -> {
            try {
                CommentCRUD crud = new CommentCRUD();
                List<Comment> all = crud.afficherByThread(thread.getId());

                // last 15 comments max (good context, not too big)
                int start = Math.max(0, all.size() - 15);
                List<Comment> recent = all.subList(start, all.size());

                String prompt = buildReplyPrompt(thread, recent);
                String aiText = replyService.suggestReplies(prompt);

                // parse "1) ... 2) ... 3) ..."
                List<String> options = extractNumberedOptions(aiText);
                if (options.size() < 1) options = List.of(aiText);

                List<String> finalOptions = options;

                Platform.runLater(() -> {
                    replyStatusLabel.setText("✅ Suggestions ready");
                    showReplySuggestionsDialog(finalOptions);
                });

            } catch (Exception ex) {
                Platform.runLater(() -> summaryLabel.setText("❌ Suggest failed: " + ex.getMessage()));
            }
        });
    }
    private void showReplySuggestionsDialog(List<String> options) {

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Reply Suggestions");
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        // --- UI ---
        Label title = new Label("Choose a reply");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        ListView<String> listView = new ListView<>();
        listView.getItems().addAll(options);
        listView.getSelectionModel().selectFirst();
        listView.setPrefWidth(320);

        // show short preview in list
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String s = item.replace("\n", " ").trim();
                    if (s.length() > 70) s = s.substring(0, 70) + "…";
                    setText(s);
                }
            }
        });

        TextArea preview = new TextArea();
        preview.setEditable(false);
        preview.setWrapText(true);
        preview.setPrefWidth(420);
        preview.setPrefHeight(180);
        preview.setStyle(
                "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-font-size: 13px;"
        );

        // bind preview to selection
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            preview.setText(newV == null ? "" : newV);
        });
        preview.setText(options.isEmpty() ? "" : options.get(0));

        HBox body = new HBox(12, listView, preview);
        body.setStyle("-fx-padding: 14;");

        VBox root = new VBox(10, title, body);
        root.setStyle(
                "-fx-background-color: white;" +
                        "-fx-padding: 18;" +
                        "-fx-background-radius: 14;"
        );

        pane.setContent(root);

        // --- Buttons styling ---
        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        okBtn.setText("Insert");
        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-background-color:#3b82f6; -fx-text-fill:white; -fx-background-radius:10;");

        Button cancelBtn = (Button) pane.lookupButton(ButtonType.CANCEL);
        cancelBtn.setStyle("-fx-background-color:#e5e7eb; -fx-text-fill:#111827; -fx-background-radius:10;");

        // disable insert if nothing selected
        okBtn.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) return listView.getSelectionModel().getSelectedItem();
            return null;
        });

        dialog.showAndWait().ifPresent(choice -> {
            commentField.setText(choice);
            commentField.requestFocus();
            commentField.positionCaret(commentField.getText().length());
        });
    }
    private List<String> extractNumberedOptions(String text) {
        List<String> list = new ArrayList<>();

        Pattern p = Pattern.compile("(?m)^\\s*[123]\\)\\s*(.+)$");
        Matcher m = p.matcher(text);

        while (m.find()) {
            String s = m.group(1).trim();
            if (!s.isEmpty()) list.add(s);
        }

        return list;
    }
    private String keepOnlySummaryText(String ai) {
        if (ai == null) return "";
        ai = ai.trim();

        // If model still outputs "Summary: ..."
        int idx = ai.toLowerCase().indexOf("summary:");
        if (idx >= 0) {
            return ai.substring(idx + "summary:".length()).trim();
        }

        // If it outputs Topic/Key points etc, keep first 2 lines max
        String[] lines = ai.split("\\R+");
        if (lines.length >= 2) return (lines[0] + " " + lines[1]).trim();
        return ai;
    }
    private final AiSummaryService summaryService = new AiSummaryService();
    private String buildSummaryPrompt(Thread thread, List<Comment> comments) {
        int count = (comments == null) ? 0 : comments.size();

        StringBuilder sb = new StringBuilder();

        sb.append("TASK: Summarize the discussion in 1-2 sentences.\n\n");

        sb.append("OUTPUT RULES (STRICT):\n");
        sb.append("- Output ONLY the final summary text (no labels, no headings).\n");
        sb.append("- Do NOT include usernames/firstnames.\n");
        sb.append("- Do NOT include dates/timestamps.\n");
        sb.append("- Do NOT quote verbatim.\n");
        sb.append("- Do NOT invent missing info.\n");
        sb.append("- IMPORTANT: COMMENTS_COUNT is authoritative.\n");
        sb.append("- If COMMENTS_COUNT > 0: you MUST produce a summary (never say 'no discussion yet').\n");
        sb.append("- If COMMENTS_COUNT = 0: output exactly: No discussion yet.\n\n");

        sb.append("THREAD:\n");
        sb.append("id: ").append(thread.getId()).append("\n");
        sb.append("title: ").append(safe(thread.getTitle())).append("\n");
        sb.append("content: ").append(safe(thread.getContent())).append("\n\n");

        sb.append("COMMENTS_COUNT: ").append(count).append("\n\n");

        sb.append("COMMENTS (oldest -> newest):\n");
        if (count == 0) {
            sb.append("(none)\n");
        } else {
            int i = 1;
            for (Comment c : comments) {
                sb.append(i++).append(")\n");
                sb.append("   author_firstname: ").append(safe(c.getFirstname())).append("\n");
                sb.append("   created_at: ").append(c.getCreatedAt() == null ? "" : c.getCreatedAt().toString()).append("\n");
                sb.append("   content: ").append(safe(c.getContent()).replace("\r", " ").replace("\n", " ").trim()).append("\n");
            }
        }

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

                String clean = keepOnlySummaryText(summary);
                Platform.runLater(() -> summaryLabel.setText(clean));

            } catch (Exception ex) {
                Platform.runLater(() ->
                        summaryLabel.setText("❌ Summary failed: " + ex.getMessage())
                );
            }
        });
    }

    private static final HttpClient CENSOR_CLIENT = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(1))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private String freeCensorFast(String text) {
        try {
            if (text == null || text.isBlank()) return text;

            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = "https://www.purgomalum.com/service/plain?text=" + encoded;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(3))   // request timeout
                    .GET()
                    .build();

            // HARD CAP: 3 seconds total max
            return CENSOR_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .completeOnTimeout(null, 3, java.util.concurrent.TimeUnit.SECONDS)
                    .thenApply(res -> {
                        if (res == null) return text;               // timed out
                        if (res.statusCode() >= 300) return text;   // bad response
                        String body = res.body();
                        return (body == null || body.isBlank()) ? text : body;
                    })
                    .exceptionally(ex -> text)
                    .join();

        } catch (Exception e) {
            return text;
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

                            // lock while saving
                            saveBtn.setDisable(true);
                            cancelBtn.setDisable(true);

                            EmailAsync.run(() -> {
                                try {
                                    String cleaned = freeCensorFast(newText);

                                    CommentCRUD cCrud = new CommentCRUD();
                                    cCrud.modifier(comment.getId(), cleaned);

                                    Platform.runLater(() -> {
                                        contentLabel.setText(cleaned);
                                        contentBox.getChildren().setAll(contentLabel);

                                        editBtn.setDisable(false);
                                        deleteBtn.setDisable(false);
                                        saveBtn.setDisable(false);
                                        cancelBtn.setDisable(false);
                                    });

                                } catch (Exception ex) {
                                    Platform.runLater(() -> {
                                        showAlert("Error updating comment: " + ex.getMessage());
                                        contentBox.getChildren().setAll(contentLabel);

                                        editBtn.setDisable(false);
                                        deleteBtn.setDisable(false);
                                        saveBtn.setDisable(false);
                                        cancelBtn.setDisable(false);
                                    });
                                }
                            });
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

        // ✅ anti-spam lock
        if (isPostingComment) return;

        String text = commentField.getText().trim();

        // ✅ validate BEFORE locking
        if (text.isEmpty()) {
            showAlert("Comment cannot be empty.");
            return;
        }
        if (text.length() > 500) {
            showAlert("Comment too long (max 500 characters).");
            return;
        }

        // ✅ lock UI once
        isPostingComment = true;
        commentField.setDisable(true);
        if (addCommentBtn != null) addCommentBtn.setDisable(true);

        EmailAsync.run(() -> {
            try {
                // ✅ use FAST censor (hard cap)
                String cleaned = freeCensorFast(text);

                Comment comment = new Comment(thread.getId(), currentUserId, cleaned);
                new CommentCRUD().ajouter(comment);

                // email async (keep it async)
                EmailAsync.run(() -> {
                    new NotificationEmailService()
                            .sendThreadCommentMail(thread.getId(), currentUserId, cleaned);
                });

                Platform.runLater(() -> {
                    commentField.clear();
                    loadComments();

                    // ✅ unlock UI
                    commentField.setDisable(false);
                    if (addCommentBtn != null) addCommentBtn.setDisable(false);
                    isPostingComment = false;
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    // ✅ unlock UI on error too
                    commentField.setDisable(false);
                    if (addCommentBtn != null) addCommentBtn.setDisable(false);
                    isPostingComment = false;

                    showAlert("Error: " + e.getMessage());
                });
            }
        });
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
