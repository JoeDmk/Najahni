package com.najahni.controllers;

import com.najahni.services.GeminiService;
import com.najahni.services.SessionManager;
import com.najahni.models.User;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

/**
 * Floating AI Chatbot widget — adds a chat bubble to any StackPane.
 *
 * <p>Usage: {@code AIChatWidget.attachTo(stackPane);}
 *
 * <p>Features:
 * <ul>
 *   <li>Floating toggle button (bottom-right)</li>
 *   <li>Animated open/close with scale + fade</li>
 *   <li>Conversation history with Gemini AI</li>
 *   <li>Typing indicator while waiting for response</li>
 *   <li>Quick action buttons for common queries</li>
 * </ul>
 */
public class AIChatWidget {

    private static final double CHAT_WIDTH = 380;
    private static final double CHAT_HEIGHT = 520;

    private final GeminiService gemini;
    private final StackPane hostPane;

    private VBox chatPanel;
    private VBox messagesContainer;
    private ScrollPane messagesScroll;
    private TextField inputField;
    private Button sendBtn;
    private StackPane toggleButton;
    private boolean isOpen = false;
    private boolean firstOpen = true;

    /** Singleton per host-pane — avoids duplicates */
    private static AIChatWidget currentInstance;

    private AIChatWidget(StackPane hostPane) {
        this.hostPane = hostPane;
        this.gemini = new GeminiService();
        buildWidget();
    }

    /**
     * Attach the AI chat widget to a StackPane.
     * Call this once (e.g. from the FrontOfficeController after loading the main view).
     */
    public static void attachTo(StackPane hostPane) {
        if (currentInstance != null && currentInstance.hostPane == hostPane) return;
        if (currentInstance != null) currentInstance.detach();
        currentInstance = new AIChatWidget(hostPane);
    }

    /** Remove widget from its host. */
    public void detach() {
        hostPane.getChildren().removeAll(toggleButton, chatPanel);
        currentInstance = null;
    }

    // ═══════════════════════════════════════════════════════════
    //  BUILD UI
    // ═══════════════════════════════════════════════════════════

    private void buildWidget() {
        buildToggleButton();
        buildChatPanel();

        // Add to host (toggle always visible, chat initially hidden)
        hostPane.getChildren().addAll(chatPanel, toggleButton);
        StackPane.setAlignment(toggleButton, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(chatPanel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(toggleButton, new Insets(0, 25, 25, 0));
        StackPane.setMargin(chatPanel, new Insets(0, 25, 90, 0));

        chatPanel.setVisible(false);
        chatPanel.setManaged(false);
    }

    private void buildToggleButton() {
        toggleButton = new StackPane();
        toggleButton.setPrefSize(60, 60);
        toggleButton.setMaxSize(60, 60);
        toggleButton.setPickOnBounds(true);

        Circle circle = new Circle(30);
        circle.setFill(Color.web("#e94560"));
        circle.setEffect(new DropShadow(15, Color.rgb(233, 69, 96, 0.5)));

        Label icon = new Label("🤖");
        icon.setFont(Font.font(26));

        toggleButton.getChildren().addAll(circle, icon);
        toggleButton.setCursor(javafx.scene.Cursor.HAND);

        // Hover animation
        toggleButton.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), toggleButton);
            st.setToX(1.1); st.setToY(1.1);
            st.play();
        });
        toggleButton.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), toggleButton);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
        });
        toggleButton.setOnMouseClicked(e -> toggleChat());

        // Start with a subtle floating animation
        TranslateTransition floatAnim = new TranslateTransition(Duration.millis(2000), toggleButton);
        floatAnim.setByY(-6);
        floatAnim.setAutoReverse(true);
        floatAnim.setCycleCount(Animation.INDEFINITE);
        floatAnim.play();
    }

    private void buildChatPanel() {
        chatPanel = new VBox(0);
        chatPanel.setPrefSize(CHAT_WIDTH, CHAT_HEIGHT);
        chatPanel.setMaxSize(CHAT_WIDTH, CHAT_HEIGHT);
        chatPanel.getStyleClass().add("ai-chat-panel");
        chatPanel.setEffect(new DropShadow(25, Color.rgb(0, 0, 0, 0.3)));

        // ── Header ──
        HBox header = buildHeader();

        // ── Messages area ──
        messagesContainer = new VBox(10);
        messagesContainer.setPadding(new Insets(15));
        messagesContainer.getStyleClass().add("ai-chat-messages");

        messagesScroll = new ScrollPane(messagesContainer);
        messagesScroll.setFitToWidth(true);
        messagesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messagesScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        messagesScroll.getStyleClass().add("ai-chat-scroll");
        VBox.setVgrow(messagesScroll, Priority.ALWAYS);

        // ── Quick actions ──
        HBox quickActions = buildQuickActions();

        // ── Input area ──
        HBox inputArea = buildInputArea();

        chatPanel.getChildren().addAll(header, messagesScroll, quickActions, inputArea);

        // Clip for rounded corners
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(CHAT_WIDTH, CHAT_HEIGHT);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        chatPanel.setClip(clip);
    }

    private HBox buildHeader() {
        HBox header = new HBox(10);
        header.setPadding(new Insets(14, 18, 14, 18));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("ai-chat-header");

        VBox titleBox = new VBox(2);
        Label title = new Label("🤖 NAJAHNI AI");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label(gemini.isUsingHuggingFace()
                ? "🟢 " + gemini.getBackendName()
                : "☁️ Gemini Cloud");
        subtitle.setFont(Font.font("Segoe UI", 11));
        subtitle.setTextFill(Color.web("#a0d2ff"));

        titleBox.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Close / minimize button
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("ai-chat-close-btn");
        closeBtn.setOnAction(e -> toggleChat());

        // Clear history button
        Button clearBtn = new Button("🗑");
        clearBtn.getStyleClass().add("ai-chat-close-btn");
        clearBtn.setTooltip(new Tooltip("Nouvelle conversation"));
        clearBtn.setOnAction(e -> {
            gemini.clearHistory();
            messagesContainer.getChildren().clear();
            addWelcomeMessage();
        });

        header.getChildren().addAll(titleBox, clearBtn, closeBtn);
        return header;
    }

    private HBox buildQuickActions() {
        HBox actions = new HBox(6);
        actions.setPadding(new Insets(8, 12, 4, 12));
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("ai-chat-quick-actions");

        String[][] quickButtons = {
                {"💰 Conseils", "Donne-moi des conseils pour bien investir sur NAJAHNI"},
                {"📊 Risques", "Quels sont les principaux risques d'investissement à surveiller ?"},
                {"🇹🇳 Marché TN", "Comment se porte le marché d'investissement en Tunisie actuellement ?"},
        };

        for (String[] qb : quickButtons) {
            Button btn = new Button(qb[0]);
            btn.getStyleClass().add("ai-quick-btn");
            btn.setOnAction(e -> sendMessage(qb[1]));
            actions.getChildren().add(btn);
        }

        return actions;
    }

    private HBox buildInputArea() {
        HBox inputArea = new HBox(8);
        inputArea.setPadding(new Insets(10, 12, 12, 12));
        inputArea.setAlignment(Pos.CENTER);
        inputArea.getStyleClass().add("ai-chat-input-area");

        inputField = new TextField();
        inputField.setPromptText("Posez votre question...");
        inputField.getStyleClass().add("ai-chat-input");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputField.setOnAction(e -> handleSend());

        sendBtn = new Button("➤");
        sendBtn.getStyleClass().add("ai-chat-send-btn");
        sendBtn.setOnAction(e -> handleSend());

        inputArea.getChildren().addAll(inputField, sendBtn);
        return inputArea;
    }

    // ═══════════════════════════════════════════════════════════
    //  CHAT LOGIC
    // ═══════════════════════════════════════════════════════════

    private void toggleChat() {
        if (isOpen) {
            closeChat();
        } else {
            openChat();
        }
    }

    private void openChat() {
        chatPanel.setVisible(true);
        chatPanel.setManaged(true);
        chatPanel.setScaleX(0.5);
        chatPanel.setScaleY(0.5);
        chatPanel.setOpacity(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(250), chatPanel);
        scale.setToX(1); scale.setToY(1);
        scale.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.millis(200), chatPanel);
        fade.setToValue(1);

        new ParallelTransition(scale, fade).play();

        isOpen = true;

        if (firstOpen) {
            addWelcomeMessage();
            firstOpen = false;
        }

        // Focus input
        Platform.runLater(() -> inputField.requestFocus());
    }

    private void closeChat() {
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), chatPanel);
        scale.setToX(0.5); scale.setToY(0.5);

        FadeTransition fade = new FadeTransition(Duration.millis(150), chatPanel);
        fade.setToValue(0);

        ParallelTransition pt = new ParallelTransition(scale, fade);
        pt.setOnFinished(e -> {
            chatPanel.setVisible(false);
            chatPanel.setManaged(false);
        });
        pt.play();

        isOpen = false;
    }

    private void addWelcomeMessage() {
        User user = SessionManager.getInstance().getCurrentUser();
        String name = user != null ? user.getName() : "investisseur";

        String welcome = String.format(
                "👋 Bonjour %s ! Je suis **NAJAHNI AI**, votre conseiller financier intelligent.\n\n" +
                "Je peux vous aider avec :\n" +
                "• 📊 Analyse de risques d'investissement\n" +
                "• 💰 Conseils sur vos offres\n" +
                "• 🇹🇳 Informations sur le marché tunisien\n" +
                "• 🎯 Stratégies d'investissement\n\n" +
                "Posez-moi une question ! 🚀", name);

        addBotMessage(welcome);
    }

    private void handleSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        sendMessage(text);
    }

    private void sendMessage(String text) {
        inputField.clear();
        inputField.setDisable(true);
        sendBtn.setDisable(true);

        // Add user message bubble
        addUserMessage(text);

        // Add typing indicator
        HBox typingIndicator = buildTypingIndicator();
        messagesContainer.getChildren().add(typingIndicator);
        scrollToBottom();

        // Call Gemini AI
        gemini.chat(text).thenAccept(response -> Platform.runLater(() -> {
            messagesContainer.getChildren().remove(typingIndicator);
            addBotMessage(response);
            inputField.setDisable(false);
            sendBtn.setDisable(false);
            inputField.requestFocus();
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                messagesContainer.getChildren().remove(typingIndicator);
                addBotMessage("⚠️ Erreur : " + ex.getMessage());
                inputField.setDisable(false);
                sendBtn.setDisable(false);
            });
            return null;
        });
    }

    // ═══════════════════════════════════════════════════════════
    //  MESSAGE BUBBLES
    // ═══════════════════════════════════════════════════════════

    private void addUserMessage(String text) {
        HBox wrapper = new HBox();
        wrapper.setAlignment(Pos.CENTER_RIGHT);
        wrapper.setPadding(new Insets(2, 0, 2, 40));

        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("ai-msg-user");
        bubble.setPadding(new Insets(10, 14, 10, 14));

        Label msgLabel = new Label(text);
        msgLabel.setWrapText(true);
        msgLabel.setTextFill(Color.WHITE);
        msgLabel.setFont(Font.font("Segoe UI", 13));

        bubble.getChildren().add(msgLabel);
        wrapper.getChildren().add(bubble);

        // Animate in
        wrapper.setOpacity(0);
        wrapper.setTranslateX(20);
        messagesContainer.getChildren().add(wrapper);

        FadeTransition fade = new FadeTransition(Duration.millis(200), wrapper);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(200), wrapper);
        slide.setToX(0);
        new ParallelTransition(fade, slide).play();

        scrollToBottom();
    }

    private void addBotMessage(String text) {
        HBox wrapper = new HBox(8);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(2, 40, 2, 0));

        // Bot avatar
        StackPane avatar = new StackPane();
        Circle avatarCircle = new Circle(16);
        avatarCircle.setFill(Color.web("#e94560"));
        Label avatarIcon = new Label("🤖");
        avatarIcon.setFont(Font.font(12));
        avatar.getChildren().addAll(avatarCircle, avatarIcon);

        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("ai-msg-bot");
        bubble.setPadding(new Insets(10, 14, 10, 14));

        // Parse markdown-like bold (**text**) into styled text
        TextFlow textFlow = parseFormattedText(text);
        textFlow.setMaxWidth(CHAT_WIDTH - 120);

        bubble.getChildren().add(textFlow);
        wrapper.getChildren().addAll(avatar, bubble);

        // Animate in
        wrapper.setOpacity(0);
        wrapper.setTranslateX(-20);
        messagesContainer.getChildren().add(wrapper);

        FadeTransition fade = new FadeTransition(Duration.millis(300), wrapper);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), wrapper);
        slide.setToX(0);
        new ParallelTransition(fade, slide).play();

        scrollToBottom();
    }

    /**
     * Parse simple markdown (**bold**) and newlines into a TextFlow.
     */
    private TextFlow parseFormattedText(String text) {
        TextFlow flow = new TextFlow();
        flow.setLineSpacing(2);

        String[] parts = text.split("\\*\\*");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;

            // Handle newlines within each part
            String[] lines = parts[i].split("\n");
            for (int j = 0; j < lines.length; j++) {
                Text t = new Text(lines[j]);
                t.setFill(Color.web("#2c3e50"));
                t.setFont(i % 2 == 1
                        ? Font.font("Segoe UI", FontWeight.BOLD, 13)
                        : Font.font("Segoe UI", 13));
                flow.getChildren().add(t);

                if (j < lines.length - 1) {
                    flow.getChildren().add(new Text("\n"));
                }
            }
        }

        return flow;
    }

    private HBox buildTypingIndicator() {
        HBox wrapper = new HBox(8);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(2, 40, 2, 0));

        StackPane avatar = new StackPane();
        Circle avatarCircle = new Circle(16);
        avatarCircle.setFill(Color.web("#e94560"));
        Label avatarIcon = new Label("🤖");
        avatarIcon.setFont(Font.font(12));
        avatar.getChildren().addAll(avatarCircle, avatarIcon);

        HBox dotsBox = new HBox(5);
        dotsBox.setAlignment(Pos.CENTER);
        dotsBox.setPadding(new Insets(12, 18, 12, 18));
        dotsBox.getStyleClass().add("ai-msg-bot");

        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(4);
            dot.setFill(Color.web("#7f8c8d"));

            // Staggered bounce animation
            TranslateTransition bounce = new TranslateTransition(Duration.millis(400), dot);
            bounce.setByY(-6);
            bounce.setAutoReverse(true);
            bounce.setCycleCount(Animation.INDEFINITE);
            bounce.setDelay(Duration.millis(i * 150));
            bounce.play();

            dotsBox.getChildren().add(dot);
        }

        wrapper.getChildren().addAll(avatar, dotsBox);
        return wrapper;
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            messagesScroll.applyCss();
            messagesScroll.layout();
            messagesScroll.setVvalue(1.0);
        });
    }
}
