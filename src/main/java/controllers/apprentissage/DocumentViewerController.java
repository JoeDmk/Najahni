package controllers.apprentissage;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Embedded document/video viewer that opens content inside the application
 * using a JavaFX WebView. Supports:
 * <ul>
 *   <li>Web URLs (YouTube, Vimeo, Google Forms, etc.)</li>
 *   <li>Local PDF files (rendered via the browser engine)</li>
 *   <li>Local text/HTML files</li>
 * </ul>
 */
public class DocumentViewerController {

    @FXML private WebView webView;
    @FXML private Label lblTitle;
    @FXML private BorderPane root;

    private WebEngine webEngine;

    @FXML
    public void initialize() {
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);
    }

    /**
     * Loads a URL (web link or local file) into the embedded viewer.
     */
    public void loadContent(String url, String title) {
        lblTitle.setText(title);
        webEngine.load(url);
    }

    /**
     * Loads a local file into the viewer.
     */
    public void loadFile(File file, String title) {
        lblTitle.setText(title);
        String uri = file.toURI().toString();
        webEngine.load(uri);
    }

    @FXML
    public void closeViewer() {
        Stage stage = (Stage) root.getScene().getWindow();
        stage.close();
    }

    // -----------------------------------------------------------------------
    // Static helper – open the viewer from anywhere
    // -----------------------------------------------------------------------

    /**
     * Opens a modal viewer window for a web URL (video, quiz, etc.).
     *
     * @param url   the URL to display
     * @param title window title
     * @param owner the owner stage (for modality), may be null
     */
    public static void openUrl(String url, String title, Stage owner) {
        openViewer(url, null, title, owner);
    }

    /**
     * Opens a modal viewer window for a local file.
     *
     * @param file  the file to display
     * @param title window title
     * @param owner the owner stage (for modality), may be null
     */
    public static void openFile(File file, String title, Stage owner) {
        openViewer(null, file, title, owner);
    }

    private static void openViewer(String url, File file, String title, Stage owner) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DocumentViewerController.class.getResource("/views/apprentissage/document-viewer.fxml"));
            BorderPane viewerRoot = loader.load();
            DocumentViewerController ctrl = loader.getController();

            if (file != null) {
                ctrl.loadFile(file, title);
            } else if (url != null) {
                // Transform YouTube watch URLs to embed URLs for better in-app playback
                String embedUrl = toEmbedUrl(url);
                ctrl.loadContent(embedUrl, title);
            }

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.initModality(Modality.NONE);
            if (owner != null) {
                stage.initOwner(owner);
            }

            Scene scene = new Scene(viewerRoot, 960, 680);
            stage.setScene(scene);
            stage.setMinWidth(640);
            stage.setMinHeight(480);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            // Fallback: open externally
            try {
                if (file != null) {
                    java.awt.Desktop.getDesktop().open(file);
                } else if (url != null) {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Converts standard video URLs to their embeddable versions.
     * YouTube watch?v=ID  →  youtube.com/embed/ID
     * Vimeo vimeo.com/ID  →  player.vimeo.com/video/ID
     */
    private static String toEmbedUrl(String url) {
        if (url == null) return url;

        // YouTube: https://www.youtube.com/watch?v=VIDEO_ID  or  https://youtu.be/VIDEO_ID
        if (url.contains("youtube.com/watch")) {
            String videoId = extractParam(url, "v");
            if (videoId != null) {
                return "https://www.youtube.com/embed/" + videoId + "?autoplay=1&rel=0";
            }
        }
        if (url.contains("youtu.be/")) {
            String videoId = url.substring(url.lastIndexOf("youtu.be/") + 9).split("[?&]")[0];
            return "https://www.youtube.com/embed/" + videoId + "?autoplay=1&rel=0";
        }

        // Vimeo: https://vimeo.com/VIDEO_ID
        if (url.contains("vimeo.com/") && !url.contains("player.vimeo.com")) {
            String videoId = url.substring(url.lastIndexOf("vimeo.com/") + 10).split("[?&#/]")[0];
            return "https://player.vimeo.com/video/" + videoId + "?autoplay=1";
        }

        return url;
    }

    private static String extractParam(String url, String param) {
        try {
            String query = url.substring(url.indexOf('?') + 1);
            for (String pair : query.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && kv[0].equals(param)) {
                    return kv[1];
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
