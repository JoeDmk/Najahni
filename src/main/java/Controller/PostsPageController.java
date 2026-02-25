    package Controller;
    
    import Entites.Post;
    import Services.PostCRUD;
    import Services.PostReactionCRUD;
    import javafx.animation.FadeTransition;
    import javafx.fxml.FXML;
    import javafx.fxml.FXMLLoader;
    import javafx.scene.Parent;
    import javafx.scene.control.*;
    import javafx.scene.image.Image;
    import javafx.scene.image.ImageView;
    import javafx.scene.layout.*;
    import javafx.stage.FileChooser;
    import javafx.util.Duration;
    
    import java.io.File;
    import java.sql.SQLException;
    import java.time.format.DateTimeFormatter;
    import java.util.ArrayList;
    import java.util.Comparator;
    import java.util.List;
    import java.net.URI;
    import java.net.URLEncoder;
    import java.net.http.HttpClient;
    import java.net.http.HttpRequest;
    import java.net.http.HttpResponse;
    import java.nio.charset.StandardCharsets;
    import java.util.HashMap;
    import java.util.Map;
    public class PostsPageController {
        private final HttpClient http = HttpClient.newHttpClient();
    
        // cacheKey -> translated text  (cacheKey = postId:src>tgt)
        private final Map<String, String> translationCache = new HashMap<>();
        @FXML private Button postBtn;
        @FXML private Label charCountLabel;
        // postId -> original language detected once ("fr"/"en"/"ar")
        private final Map<Integer, String> originalLangByPost = new HashMap<>();
        @FXML private StackPane composerImageWrap;
        @FXML private ImageView composerImagePreview;

        @FXML private Button removeSelectedImageBtn;

        private static final int MAX_POST_LEN = 500;
        private volatile boolean isPosting = false;
    
        // optional: add your email to get higher free limit
        private final String contactEmail = "m.dalilo2016@gmail.com";
        // postId -> original content (for safe restore)
        private final Map<Integer, String> originalByPost = new HashMap<>();
    
        // postId -> currently shown target ("fr"/"en"/"ar") or null if original
        private final Map<Integer, String> shownTargetByPost = new HashMap<>();
    
        // postId -> request version counter (prevents out-of-order updates)
        private final Map<Integer, Integer> requestVersionByPost = new HashMap<>();
    
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

            if (charCountLabel != null) charCountLabel.setText("0 / " + MAX_POST_LEN);

            if (contentArea != null) {
                contentArea.textProperty().addListener((obs, oldV, newV) -> {
                    int len = (newV == null) ? 0 : newV.length();
                    if (charCountLabel != null) charCountLabel.setText(len + " / " + MAX_POST_LEN);

                    if (len > MAX_POST_LEN) {
                        contentArea.setText(newV.substring(0, MAX_POST_LEN));
                        contentArea.positionCaret(MAX_POST_LEN);
                    }
                });
            }

            // ✅ important: hide preview at startup
            if (composerImageWrap != null) { composerImageWrap.setVisible(false); composerImageWrap.setManaged(false); }
            if (removeSelectedImageBtn != null) { removeSelectedImageBtn.setVisible(false); removeSelectedImageBtn.setManaged(false); }

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
            if (file == null) return;

            selectedImageUrl = file.toURI().toString();
            imageStatusLabel.setText("Selected: " + file.getName());

            composerImageWrap.setVisible(true);
            composerImageWrap.setManaged(true);
            removeSelectedImageBtn.setVisible(true);
            removeSelectedImageBtn.setManaged(true);

            try {
                composerImagePreview.setImage(new Image(selectedImageUrl, true));
            } catch (Exception ignored) {
                composerImagePreview.setImage(null);
            }
        }
        @FXML
        private void handleRemoveSelectedImage() {
            selectedImageUrl = null;
            imageStatusLabel.setText("");

            composerImagePreview.setImage(null);
            composerImageWrap.setVisible(false);
            composerImageWrap.setManaged(false);

            removeSelectedImageBtn.setVisible(false);
            removeSelectedImageBtn.setManaged(false);
        }

        @FXML
        private void handleCreatePost() {
            if (isPosting) return;

            String content = contentArea.getText() == null ? "" : contentArea.getText().trim();
            boolean noImg = (selectedImageUrl == null || selectedImageUrl.isBlank());

            if (content.isEmpty() && noImg) {
                showAlert(Alert.AlertType.WARNING, "Empty post", "Write something or add an image.");
                return;
            }

            isPosting = true;
            if (postBtn != null) postBtn.setDisable(true);
            contentArea.setDisable(true);

            try {
                Post p = new Post(currentUserId, content);
                p.setImageUrl(selectedImageUrl);
                postCRUD.ajouter(p);

                // reset composer
                contentArea.clear();
                contentArea.setDisable(false);
                if (postBtn != null) postBtn.setDisable(false);

                handleRemoveSelectedImage(); // ✅ resets image UI nicely

                loadFeedWithFade();

            } catch (SQLException e) {
                contentArea.setDisable(false);
                if (postBtn != null) postBtn.setDisable(false);
                showAlert(Alert.AlertType.ERROR, "DB Error", e.getMessage());
            } finally {
                isPosting = false;
            }
        }
        private void loadFeedWithFade() {
            loadFeed();
            postsContainer.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(220), postsContainer);
            ft.setToValue(1);
            ft.play();
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

            // ===== TRANSLATE (MyMemory FREE) =====
            Button translateBtn = new Button("Translate");
            translateBtn.getStyleClass().add("icon-btn");
            translateBtn.setFocusTraversable(false);
            translateBtn.setMinWidth(140);
            translateBtn.setPrefWidth(140);

            ContextMenu tMenu = new ContextMenu();
            MenuItem toFr = new MenuItem("Translate to French");
            MenuItem toEn = new MenuItem("Translate to English");
            MenuItem toAr = new MenuItem("Translate to Arabic");
            MenuItem showOriginal = new MenuItem("Show original");

            tMenu.getItems().addAll(toFr, toEn, toAr, new SeparatorMenuItem(), showOriginal);

            translateBtn.setOnAction(e -> {
                if (tMenu.isShowing()) tMenu.hide();
                else tMenu.show(translateBtn, javafx.geometry.Side.BOTTOM, 0, 6);
            });

            java.util.function.Consumer<String> doTranslate = (tgt) -> {
                int postId = p.getId();

                // Keep DB original once
                String original = (p.getContent() == null) ? "" : p.getContent();
                originalByPost.putIfAbsent(postId, original);

                // Detect original language ONCE (based on DB original)
                String src = originalLangByPost.computeIfAbsent(postId,
                        id -> detectLang3(originalByPost.get(id)));

                // If user chooses the SAME language already displayed -> DO NOTHING
                String currentlyShown = shownTargetByPost.get(postId); // null = original
                if (tgt.equals(currentlyShown)) {
                    return;
                }

                // If target == original language => show original (do not translate)
                if (tgt.equals(src)) {
                    content.setText(originalByPost.get(postId));
                    shownTargetByPost.put(postId, null);
                    return;
                }

                String cacheKey = postId + ":" + src + ">" + tgt;

                // Cache hit
                if (translationCache.containsKey(cacheKey)) {
                    content.setText(translationCache.get(cacheKey));
                    shownTargetByPost.put(postId, tgt);
                    return;
                }

                // Save scroll position to prevent jumping
                double vBefore = (feedScroll != null) ? feedScroll.getVvalue() : 0;

                // Versioning to avoid out-of-order UI updates
                int version = requestVersionByPost.getOrDefault(postId, 0) + 1;
                requestVersionByPost.put(postId, version);

                // Pretty loading
                translateBtn.setDisable(true);
                ProgressIndicator pi = new ProgressIndicator();
                pi.setPrefSize(14, 14);
                translateBtn.setGraphic(pi);
                translateBtn.setText("Translating...");

                new Thread(() -> {
                    try {
                        String translated = translateWithMyMemory(originalByPost.get(postId), src, tgt);
                        translationCache.put(cacheKey, translated);

                        javafx.application.Platform.runLater(() -> {
                            if (requestVersionByPost.getOrDefault(postId, 0) != version) return;

                            content.setText(translated);
                            shownTargetByPost.put(postId, tgt);

                            translateBtn.setGraphic(null);
                            translateBtn.setText("Translate");
                            translateBtn.setDisable(false);

                            if (feedScroll != null) feedScroll.setVvalue(vBefore);
                        });

                    } catch (Exception ex) {
                        javafx.application.Platform.runLater(() -> {
                            if (requestVersionByPost.getOrDefault(postId, 0) != version) return;

                            translateBtn.setGraphic(null);
                            translateBtn.setText("Translate");
                            translateBtn.setDisable(false);

                            if (feedScroll != null) feedScroll.setVvalue(vBefore);
                            showAlert(Alert.AlertType.ERROR, "Translate error", ex.getMessage());
                        });
                    }
                }).start();
            };

            toFr.setOnAction(e -> doTranslate.accept("fr"));
            toEn.setOnAction(e -> doTranslate.accept("en"));
            toAr.setOnAction(e -> doTranslate.accept("ar"));

            showOriginal.setOnAction(e -> {
                int postId = p.getId();
                String original = originalByPost.getOrDefault(postId, p.getContent() == null ? "" : p.getContent());
                content.setText(original);
                shownTargetByPost.put(postId, null);
            });

            card.getChildren().add(translateBtn);
            // ===== END TRANSLATE =====

            // Image (optional)
            if (p.getImageUrl() != null && !p.getImageUrl().isBlank()) {
                try {
                    ImageView img = new ImageView(new Image(p.getImageUrl(), true));
                    img.setFitWidth(300);
                    img.setPreserveRatio(true);
                    img.setSmooth(true);

                    StackPane imageWrapper = new StackPane(img);
                    imageWrapper.setStyle("""
                -fx-background-color: #f1f5f9;
                -fx-padding: 10;
                -fx-background-radius: 12;
            """);

                    card.getChildren().add(imageWrapper);

                    img.setOnMouseClicked(e -> {
                        if (e.getClickCount() == 2) {
                            showImageZoomDialog(p.getImageUrl());
                        }
                    });

                } catch (Exception ignored) {}
            }

            // Footer (reactions)
            HBox footer = new HBox(10);
            footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Button reactBtn = new Button(reactionLabel(p.getMyReaction()));
            reactBtn.getStyleClass().add("icon-btn");
            reactBtn.setFocusTraversable(false);

            Label countLbl = new Label(p.getReactionsCount() + " reactions");
            countLbl.getStyleClass().add("meta-text");

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

            reactBtn.setOnAction(e -> {
                if (menu.isShowing()) menu.hide();
                else menu.show(reactBtn, javafx.geometry.Side.BOTTOM, 0, 6);
            });

            footer.getChildren().addAll(reactBtn, countLbl);
            card.getChildren().add(footer);

            return card;
        }
        private void showImageZoomDialog(String imageUrl) {
            if (imageUrl == null || imageUrl.isBlank()) return;

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Image");
            dialog.setHeaderText(null);

            DialogPane pane = dialog.getDialogPane();
            try {
                pane.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            } catch (Exception ignored) {}

            ImageView big = new ImageView(new Image(imageUrl, true));
            big.setPreserveRatio(true);
            big.setSmooth(true);
            big.setFitWidth(760);
            big.setFitHeight(520);

            StackPane wrap = new StackPane(big);
            wrap.setStyle("-fx-padding: 14; -fx-background-color: white; -fx-background-radius: 14;");

            pane.setContent(wrap);
            pane.getButtonTypes().add(new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE));

            dialog.setResizable(true);
            pane.setPrefWidth(820);
            pane.setPrefHeight(620);

            dialog.showAndWait();
        }
        private String translateWithMyMemory(String text, String source, String target) throws Exception {
            if (text == null || text.isBlank()) return "";
    
            // Encode | as %7C for Java URI
            String langpair = source + "%7C" + target;
    
            String url = "https://api.mymemory.translated.net/get?q="
                    + URLEncoder.encode(text, StandardCharsets.UTF_8)
                    + "&langpair=" + langpair
                    + "&de=" + URLEncoder.encode(contactEmail, StandardCharsets.UTF_8);
    
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
    
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                throw new RuntimeException("MyMemory failed (" + res.statusCode() + "): " + res.body());
            }
    
            String body = res.body();
    
            int tt = body.indexOf("\"translatedText\":\"");
            if (tt == -1) return body;
    
            int start = tt + "\"translatedText\":\"".length();
            int end = findJsonStringEnd(body, start);
            if (end == -1) return body;
    
            String raw = body.substring(start, end);
    
            return unescapeJsonString(raw)
                    .replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n");
        }
        private String translateTo(String text, String target) throws Exception {
            String source = detectLang3(text);
    
            // if same language, just return original
            if (source.equals(target)) return text;
    
            return translateWithMyMemory(text, source, target);
        }
        private int findJsonStringEnd(String s, int start) {
            boolean escaped = false;
            for (int i = start; i < s.length(); i++) {
                char c = s.charAt(i);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    return i;
                }
            }
            return -1;
        }
        private String detectLang3(String text) {
            if (text == null) return "en";
            String t = text.trim();
            if (t.isEmpty()) return "en";
    
            // Arabic check
            for (int i = 0; i < t.length(); i++) {
                Character.UnicodeBlock b = Character.UnicodeBlock.of(t.charAt(i));
                if (b == Character.UnicodeBlock.ARABIC
                        || b == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A
                        || b == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B
                        || b == Character.UnicodeBlock.ARABIC_SUPPLEMENT) {
                    return "ar";
                }
            }

            // French heuristic (stronger)
            String lower = t.toLowerCase();

// accents already handled above
            int frHits = 0;
            String[] frWords = {
                    "je","tu","il","elle","nous","vous","ils","elles",
                    "le","la","les","un","une","des","du","de",
                    "et","mais","ou","donc","car",
                    "est","suis","es","sommes","êtes","sont",
                    "pas","ne","plus","jamais",
                    "pour","avec","sans","dans","sur","chez",
                    "ce","c'est","ça","que","qui","quoi","dont"
            };

            for (String w : frWords) {
                if (lower.contains(" " + w + " ") || lower.startsWith(w + " ") || lower.endsWith(" " + w)) {
                    frHits++;
                    if (frHits >= 2) return "fr";
                }
            }

// common French apostrophes
            if (lower.contains(" l'") || lower.contains(" d'") || lower.contains(" j'") || lower.contains(" c'")) {
                return "fr";
            }
    
            return "en";
        }
    
        private String unescapeJsonString(String s) {
            StringBuilder out = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (i + 1 >= s.length()) break;
    
                char n = s.charAt(++i);
                switch (n) {
                    case '"': out.append('"'); break;
                    case '\\': out.append('\\'); break;
                    case '/': out.append('/'); break;
                    case 'b': out.append('\b'); break;
                    case 'f': out.append('\f'); break;
                    case 'n': out.append('\n'); break;
                    case 'r': out.append('\r'); break;
                    case 't': out.append('\t'); break;
                    case 'u':
                        if (i + 4 < s.length()) {
                            String hex = s.substring(i + 1, i + 5);
                            try {
                                out.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException e) {
                                out.append("\\u").append(hex);
                                i += 4;
                            }
                        } else {
                            out.append("\\u");
                        }
                        break;
                    default:
                        out.append(n); // unknown escape, keep char
                }
            }
            return out.toString();
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
            dialog.setHeaderText(null);
    
            DialogPane pane = dialog.getDialogPane();
    
            // apply your css
            try {
                pane.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            } catch (Exception ignored) {}
    
            // ----- editable state for image -----
            final String[] editedImageUrl = new String[] { p.getImageUrl() };
    
            // ----- UI -----
            Label title = new Label("Edit your post");
            title.setStyle("-fx-font-size:16px; -fx-font-weight:bold;");
    
            TextArea ta = new TextArea(p.getContent() == null ? "" : p.getContent());
            ta.setWrapText(true);
            ta.setPrefRowCount(6);
            ta.getStyleClass().add("form-field");
    
            // Image preview
            Label imgTitle = new Label("Attached image");
            imgTitle.setStyle("-fx-font-weight: bold;");
    
            ImageView preview = new ImageView();
            preview.setFitWidth(320);
            preview.setFitHeight(220);      // ✅ limite hauteur
            preview.setPreserveRatio(true);
            preview.setSmooth(true);
    
            StackPane previewWrap = new StackPane(preview);
            previewWrap.setStyle("""
            -fx-background-color: #f1f5f9;
            -fx-padding: 10;
            -fx-background-radius: 12;
        """);
            previewWrap.setMaxHeight(260);   // ✅ empêche le dialog d’exploser
    
            Label imgHint = new Label();
            imgHint.getStyleClass().add("muted-text");
    
            Runnable refreshPreview = () -> {
                String url = editedImageUrl[0];
                boolean has = url != null && !url.isBlank();
    
                if (!has) {
                    preview.setImage(null);
                    previewWrap.setVisible(false);
                    previewWrap.setManaged(false);
                    imgHint.setText("No image attached.");
                } else {
                    try {
                        preview.setImage(new Image(url, true));
                        previewWrap.setVisible(true);
                        previewWrap.setManaged(true);
                        imgHint.setText("Image selected.");
                    } catch (Exception e) {
                        preview.setImage(null);
                        previewWrap.setVisible(false);
                        previewWrap.setManaged(false);
                        imgHint.setText("Cannot load image preview.");
                    }
                }
            };
            refreshPreview.run();
    
            Button changeBtn = new Button("Change");
            changeBtn.getStyleClass().add("btn-secondary");
    
            Button removeBtn = new Button("Remove");
            // if you don't have btn-danger in css, keep secondary:
            removeBtn.getStyleClass().add("btn-secondary");
    
            changeBtn.setOnAction(ev -> {
                FileChooser fc = new FileChooser();
                fc.setTitle("Choose an image");
                fc.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
                );
                File file = fc.showOpenDialog(pane.getScene().getWindow());
                if (file != null) {
                    editedImageUrl[0] = file.toURI().toString();
                    refreshPreview.run();
                }
            });
    
            removeBtn.setOnAction(ev -> {
                editedImageUrl[0] = null; // ✅ remove
                refreshPreview.run();
            });
    
            // Hide remove button if no image
            removeBtn.visibleProperty().bind(previewWrap.visibleProperty());
            removeBtn.managedProperty().bind(previewWrap.managedProperty());
    
            HBox imgActions = new HBox(10, changeBtn, removeBtn);
    
            VBox imgBox = new VBox(6, imgTitle, imgHint, previewWrap, imgActions);
    
            VBox root = new VBox(12, title, ta, imgBox);
            root.getStyleClass().add("card");
            root.setStyle("-fx-padding: 18;");
    
            ScrollPane sp = new ScrollPane(root);
            sp.setFitToWidth(true);
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            sp.setStyle("-fx-background-color:transparent; -fx-padding:0;");
    
            pane.setContent(sp);
    
            // Buttons
            ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            pane.getButtonTypes().setAll(cancelType, saveType);
    
            Button saveBtn = (Button) pane.lookupButton(saveType);
            Button cancelBtn = (Button) pane.lookupButton(cancelType);
            saveBtn.getStyleClass().add("btn-primary");
            cancelBtn.getStyleClass().add("btn-secondary");
    
            // Disable Save if both text empty AND no image
            saveBtn.disableProperty().bind(
                    javafx.beans.binding.Bindings.createBooleanBinding(
                            () -> {
                                String txt = ta.getText() == null ? "" : ta.getText().trim();
                                boolean noText = txt.isEmpty();
                                boolean noImg = editedImageUrl[0] == null || editedImageUrl[0].isBlank();
                                return noText && noImg;
                            },
                            ta.textProperty()
                    )
            );
    
            if (dialog.showAndWait().orElse(cancelType) != saveType) return;
    
            String newText = ta.getText() == null ? "" : ta.getText().trim();
    
            try {
                p.setContent(newText);
                p.setImageUrl(editedImageUrl[0]); // ✅ change/remove
                postCRUD.modifier(p);
                loadFeed();
                postsContainer.setOpacity(0);
                FadeTransition ft = new FadeTransition(Duration.millis(300), postsContainer);
                ft.setToValue(1);
                ft.play();
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
