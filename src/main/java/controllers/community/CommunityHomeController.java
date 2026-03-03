package controllers.community;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class CommunityHomeController {

    @FXML private BorderPane rootPane;
    @FXML private StackPane communityContent;
    @FXML private Button tabPosts;
    @FXML private Button tabGroups;
    @FXML private Button tabEvents;

    private Button activeTab;

    /* ── Tab actions ── */

    @FXML
    private void goToPosts()  { switchTab(tabPosts,  "/views/community/PostsPage.fxml");  }

    @FXML
    private void goToGroups() { switchTab(tabGroups, "/views/community/GroupsPage.fxml"); }

    @FXML
    private void goToEvents() { switchTab(tabEvents, "/views/community/EventsPage.fxml"); }

    /* ── Internal ── */

    private void switchTab(Button tab, String fxmlPath) {
        if (tab == activeTab) return;          // already showing
        setActiveTab(tab);
        loadContent(fxmlPath);
    }

    private void setActiveTab(Button tab) {
        if (activeTab != null)
            activeTab.getStyleClass().remove("fo-sub-tab-active");
        tab.getStyleClass().add("fo-sub-tab-active");
        activeTab = tab;
    }

    private void loadContent(String path) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource(path));
            communityContent.getChildren().setAll(page);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ── Legacy handlers (kept for compatibility) ── */

    @FXML private void goToHome()     { tools.NavigationHelper.goHome(rootPane); }
    @FXML private void handleGoBack() { goToHome(); }
    @FXML private void handleGoHome() { goToHome(); }

    /* ── Init — default to Posts ── */

    @FXML
    public void initialize() {
        // Show Posts tab by default
        goToPosts();
    }
}
