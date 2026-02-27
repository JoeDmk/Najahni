package com.najahni.utils;

import javafx.animation.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Composant réutilisable de visualisation PDF dans l'application JavaFX.
 *
 * Affiche un aperçu du PDF avec navigation entre pages,
 * bouton "Enregistrer" et bouton "Imprimer".
 *
 * Utilise le même pattern d'overlay que les popups existants (StackPane + GaussianBlur).
 * Rendu des pages via Apache PDFBox (PDFRenderer → BufferedImage → JavaFX Image).
 */
public class PDFPreviewPopup {

    private static final Logger LOG = Logger.getLogger(PDFPreviewPopup.class.getName());
    private static final double RENDER_DPI = 150.0;

    /**
     * Affiche un aperçu PDF dans un popup overlay.
     *
     * @param pdfBytes       Le contenu du PDF en bytes
     * @param title          Titre du document affiché dans le header
     * @param defaultFileName Nom de fichier par défaut pour l'enregistrement
     * @param rootStack      Le StackPane racine de la scène (pour l'overlay)
     */
    public static void show(byte[] pdfBytes, String title, String defaultFileName, StackPane rootStack) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            LOG.warning("PDF vide — impossible d'afficher l'aperçu");
            return;
        }
        if (rootStack == null) {
            LOG.warning("StackPane racine null — impossible d'afficher l'aperçu");
            return;
        }

        // ── Render PDF pages to images ──
        List<Image> pageImages = renderPDFPages(pdfBytes);
        if (pageImages.isEmpty()) {
            LOG.warning("Aucune page rendue depuis le PDF");
            return;
        }

        // ── Overlay ──
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.6);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setOpacity(0);

        // ── Popup container ──
        VBox popup = new VBox(0);
        popup.setStyle("-fx-background-color: white; -fx-background-radius: 18;");
        popup.setMaxWidth(680);
        popup.setMinWidth(560);
        popup.setMaxHeight(820);
        popup.setAlignment(Pos.TOP_CENTER);
        StackPane.setAlignment(popup, Pos.CENTER);
        popup.setScaleX(0.85);
        popup.setScaleY(0.85);
        popup.setOpacity(0);

        popup.setEffect(new DropShadow(30, Color.rgb(0, 0, 0, 0.35)));

        Rectangle clip = new Rectangle();
        clip.setArcWidth(36);
        clip.setArcHeight(36);
        clip.widthProperty().bind(popup.widthProperty());
        clip.heightProperty().bind(popup.heightProperty());
        popup.setClip(clip);

        // ── Header ──
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 24, 14, 24));
        header.setStyle("-fx-background-color: linear-gradient(to right, #2c3e50, #3498db); "
                + "-fx-background-radius: 18 18 0 0;");

        Label iconLabel = new Label("📄");
        iconLabel.setStyle("-fx-font-size: 26;");

        VBox titleBox = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subtitleLabel = new Label(pageImages.size() + " page" + (pageImages.size() > 1 ? "s" : ""));
        subtitleLabel.setStyle("-fx-font-size: 11; -fx-text-fill: rgba(255,255,255,0.7);");
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; "
                + "-fx-font-size: 16; -fx-font-weight: bold; -fx-background-radius: 50; "
                + "-fx-cursor: hand; -fx-min-width: 36; -fx-min-height: 36;");
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(btnClose.getStyle().replace("0.15", "0.3")));
        btnClose.setOnMouseExited(e -> btnClose.setStyle(btnClose.getStyle().replace("0.3", "0.15")));

        header.getChildren().addAll(iconLabel, titleBox, btnClose);

        // ── Page display area ──
        VBox pagesContainer = new VBox(16);
        pagesContainer.setAlignment(Pos.TOP_CENTER);
        pagesContainer.setPadding(new Insets(20, 24, 20, 24));
        pagesContainer.setStyle("-fx-background-color: #ecf0f1;");

        for (int i = 0; i < pageImages.size(); i++) {
            Image img = pageImages.get(i);
            ImageView imageView = new ImageView(img);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(560);
            imageView.setSmooth(true);

            StackPane pageWrapper = new StackPane(imageView);
            pageWrapper.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 2);");
            pageWrapper.setPadding(new Insets(0));

            if (pageImages.size() > 1) {
                Label pageNum = new Label("Page " + (i + 1) + " / " + pageImages.size());
                pageNum.setStyle("-fx-font-size: 10; -fx-text-fill: #95a5a6;");
                VBox pageBox = new VBox(6, pageWrapper, pageNum);
                pageBox.setAlignment(Pos.CENTER);
                pagesContainer.getChildren().add(pageBox);
            } else {
                pagesContainer.getChildren().add(pageWrapper);
            }
        }

        ScrollPane scrollPane = new ScrollPane(pagesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: #ecf0f1; -fx-background: #ecf0f1;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // ── Footer with actions ──
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(16, 24, 20, 24));
        footer.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #ecf0f1; "
                + "-fx-border-width: 1 0 0 0;");

        Button btnSave = new Button("💾  Enregistrer");
        btnSave.setStyle("-fx-background-color: linear-gradient(to bottom, #27ae60, #229954); "
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13; "
                + "-fx-background-radius: 10; -fx-padding: 10 28; -fx-cursor: hand;");
        btnSave.setOnMouseEntered(e -> btnSave.setStyle(btnSave.getStyle().replace("#27ae60", "#2ecc71")));
        btnSave.setOnMouseExited(e -> btnSave.setStyle(btnSave.getStyle().replace("#2ecc71", "#27ae60")));

        Button btnPrint = new Button("🖨  Imprimer");
        btnPrint.setStyle("-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9); "
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13; "
                + "-fx-background-radius: 10; -fx-padding: 10 28; -fx-cursor: hand;");
        btnPrint.setOnMouseEntered(e -> btnPrint.setStyle(btnPrint.getStyle().replace("#3498db", "#5dade2")));
        btnPrint.setOnMouseExited(e -> btnPrint.setStyle(btnPrint.getStyle().replace("#5dade2", "#3498db")));

        Button btnCloseFooter = new Button("Fermer");
        btnCloseFooter.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #7f8c8d; "
                + "-fx-font-weight: bold; -fx-font-size: 13; -fx-background-radius: 10; "
                + "-fx-padding: 10 28; -fx-cursor: hand;");

        footer.getChildren().addAll(btnSave, btnPrint, btnCloseFooter);

        // ── Actions ──
        Runnable closeAction = () -> closePopup(overlay, popup, rootStack);

        btnClose.setOnAction(e -> closeAction.run());
        btnCloseFooter.setOnAction(e -> closeAction.run());

        btnSave.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le PDF");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Document PDF", "*.pdf"));
            fileChooser.setInitialFileName(defaultFileName);

            Stage stage = (Stage) rootStack.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(pdfBytes);
                    fos.flush();
                    LOG.info("✓ PDF enregistré : " + file.getAbsolutePath());

                    // Success feedback
                    btnSave.setText("✅  Enregistré !");
                    btnSave.setStyle(btnSave.getStyle().replace("#27ae60", "#1abc9c"));
                    PauseTransition pause = new PauseTransition(Duration.seconds(2));
                    pause.setOnFinished(ev -> {
                        btnSave.setText("💾  Enregistrer");
                        btnSave.setStyle(btnSave.getStyle().replace("#1abc9c", "#27ae60"));
                    });
                    pause.play();
                } catch (Exception ex) {
                    LOG.severe("✗ Erreur sauvegarde PDF: " + ex.getMessage());
                    btnSave.setText("❌ Erreur");
                    PauseTransition pause = new PauseTransition(Duration.seconds(2));
                    pause.setOnFinished(ev -> btnSave.setText("💾  Enregistrer"));
                    pause.play();
                }
            }
        });

        btnPrint.setOnAction(e -> {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null) {
                Stage stage = (Stage) rootStack.getScene().getWindow();
                boolean proceed = job.showPrintDialog(stage);
                if (proceed) {
                    boolean success = true;
                    for (Image pageImg : pageImages) {
                        ImageView printView = new ImageView(pageImg);
                        printView.setPreserveRatio(true);
                        printView.setFitWidth(job.getJobSettings().getPageLayout().getPrintableWidth());
                        if (!job.printPage(printView)) {
                            success = false;
                            break;
                        }
                    }
                    job.endJob();

                    if (success) {
                        btnPrint.setText("✅  Envoyé !");
                        PauseTransition pause = new PauseTransition(Duration.seconds(2));
                        pause.setOnFinished(ev -> btnPrint.setText("🖨  Imprimer"));
                        pause.play();
                    }
                }
            }
        });

        // ── Keyboard ──
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) closeAction.run();
        });
        overlay.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) closeAction.run();
        });

        popup.getChildren().addAll(header, scrollPane, footer);
        overlay.getChildren().add(popup);

        rootStack.getChildren().add(overlay);
        overlay.requestFocus();

        // Apply blur to background
        if (rootStack.getChildren().size() > 1) {
            rootStack.getChildren().get(0).setEffect(new GaussianBlur(8));
        }

        // ── Animate in ──
        FadeTransition fadeOverlay = new FadeTransition(Duration.millis(300), overlay);
        fadeOverlay.setFromValue(0);
        fadeOverlay.setToValue(1);
        FadeTransition fadePopup = new FadeTransition(Duration.millis(400), popup);
        fadePopup.setFromValue(0);
        fadePopup.setToValue(1);
        ScaleTransition scalePopup = new ScaleTransition(Duration.millis(450), popup);
        scalePopup.setFromX(0.85);
        scalePopup.setFromY(0.85);
        scalePopup.setToX(1.0);
        scalePopup.setToY(1.0);
        scalePopup.setInterpolator(Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0));
        TranslateTransition slidePopup = new TranslateTransition(Duration.millis(400), popup);
        slidePopup.setFromY(40);
        slidePopup.setToY(0);
        slidePopup.setInterpolator(Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0));
        new ParallelTransition(fadeOverlay, fadePopup, scalePopup, slidePopup).play();
    }

    // ─── PDF → Images rendering ─────────────────────────────

    private static List<Image> renderPDFPages(byte[] pdfBytes) {
        List<Image> images = new ArrayList<>();
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFRenderer renderer = new PDFRenderer(doc);
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage bimg = renderer.renderImageWithDPI(i, (float) RENDER_DPI);
                Image fxImage = SwingFXUtils.toFXImage(bimg, null);
                images.add(fxImage);
            }
            LOG.info("✓ " + images.size() + " page(s) PDF rendues pour l'aperçu");
        } catch (Exception e) {
            LOG.severe("✗ Erreur rendu PDF: " + e.getMessage());
            e.printStackTrace();
        }
        return images;
    }

    // ─── Close animation ────────────────────────────────────

    private static void closePopup(StackPane overlay, VBox popup, StackPane root) {
        // Remove blur
        if (root.getChildren().size() > 1) {
            root.getChildren().get(0).setEffect(null);
        }

        FadeTransition fade = new FadeTransition(Duration.millis(250), overlay);
        fade.setFromValue(1);
        fade.setToValue(0);
        ScaleTransition scale = new ScaleTransition(Duration.millis(250), popup);
        scale.setToX(0.9);
        scale.setToY(0.9);
        TranslateTransition slide = new TranslateTransition(Duration.millis(250), popup);
        slide.setToY(20);

        ParallelTransition closeAnim = new ParallelTransition(fade, scale, slide);
        closeAnim.setOnFinished(e -> root.getChildren().remove(overlay));
        closeAnim.play();
    }
}
