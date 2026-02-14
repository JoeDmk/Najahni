package com.najahni.utils;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.util.Duration;

/**
 * Utility class for JavaFX animations.
 * Provides reusable animation methods for UI elements.
 */
public class AnimationUtils {

    private AnimationUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates a fade-in animation for a node.
     * @param node The node to animate
     * @param durationMs Animation duration in milliseconds
     * @return The FadeTransition animation
     */
    public static FadeTransition fadeIn(Node node, int durationMs) {
        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        return fade;
    }

    /**
     * Creates a fade-out animation for a node.
     * @param node The node to animate
     * @param durationMs Animation duration in milliseconds
     * @return The FadeTransition animation
     */
    public static FadeTransition fadeOut(Node node, int durationMs) {
        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), node);
        fade.setFromValue(1);
        fade.setToValue(0);
        return fade;
    }

    /**
     * Creates a slide-in animation from the bottom.
     * @param node The node to animate
     * @param durationMs Animation duration in milliseconds
     * @param distance The distance to slide from
     * @return The TranslateTransition animation
     */
    public static TranslateTransition slideInFromBottom(Node node, int durationMs, double distance) {
        TranslateTransition slide = new TranslateTransition(Duration.millis(durationMs), node);
        slide.setFromY(distance);
        slide.setToY(0);
        return slide;
    }

    /**
     * Creates a slide-in animation from the right.
     * @param node The node to animate
     * @param durationMs Animation duration in milliseconds
     * @param distance The distance to slide from
     * @return The TranslateTransition animation
     */
    public static TranslateTransition slideInFromRight(Node node, int durationMs, double distance) {
        TranslateTransition slide = new TranslateTransition(Duration.millis(durationMs), node);
        slide.setFromX(distance);
        slide.setToX(0);
        return slide;
    }

    /**
     * Creates a scale animation (zoom effect).
     * @param node The node to animate
     * @param durationMs Animation duration in milliseconds
     * @param fromScale Starting scale
     * @param toScale Ending scale
     * @return The ScaleTransition animation
     */
    public static ScaleTransition scale(Node node, int durationMs, double fromScale, double toScale) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(durationMs), node);
        scale.setFromX(fromScale);
        scale.setFromY(fromScale);
        scale.setToX(toScale);
        scale.setToY(toScale);
        return scale;
    }

    /**
     * Plays a combined fade and slide animation.
     * @param node The node to animate
     * @param durationMs Animation duration in milliseconds
     */
    public static void playFadeSlideIn(Node node, int durationMs) {
        node.setOpacity(0);
        node.setTranslateY(20);

        FadeTransition fade = fadeIn(node, durationMs);
        TranslateTransition slide = slideInFromBottom(node, durationMs, 20);

        ParallelTransition transition = new ParallelTransition(fade, slide);
        transition.play();
    }

    /**
     * Plays a combined fade and scale animation.
     * @param node The node to animate
     * @param durationMs Animation duration in milliseconds
     * @param delayMs Delay before animation starts
     */
    public static void playFadeScaleIn(Node node, int durationMs, int delayMs) {
        node.setOpacity(0);
        node.setScaleX(0.95);
        node.setScaleY(0.95);

        FadeTransition fade = fadeIn(node, durationMs);
        fade.setDelay(Duration.millis(delayMs));

        ScaleTransition scale = scale(node, durationMs, 0.95, 1.0);
        scale.setDelay(Duration.millis(delayMs));

        ParallelTransition transition = new ParallelTransition(fade, scale);
        transition.play();
    }

    /**
     * Creates a pulse animation (subtle scale bounce).
     * @param node The node to animate
     * @return The ScaleTransition animation
     */
    public static ScaleTransition pulse(Node node) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(150), node);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.05);
        scale.setToY(1.05);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        return scale;
    }

    /**
     * Creates a success feedback animation (brief green highlight effect via scale).
     * @param node The node to animate
     */
    public static void playSuccessFeedback(Node node) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(100), node);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.02);
        scale.setToY(1.02);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
    }

    /**
     * Animates a button press effect.
     * @param node The button node
     */
    public static void playButtonPress(Node node) {
        ScaleTransition press = new ScaleTransition(Duration.millis(50), node);
        press.setToX(0.95);
        press.setToY(0.95);

        ScaleTransition release = new ScaleTransition(Duration.millis(50), node);
        release.setToX(1.0);
        release.setToY(1.0);

        SequentialTransition sequence = new SequentialTransition(press, release);
        sequence.play();
    }

    /**
     * Animates table row addition with fade effect.
     * @param table The TableView to animate
     */
    public static void animateTableRefresh(TableView<?> table) {
        table.setOpacity(0.7);
        FadeTransition fade = new FadeTransition(Duration.millis(200), table);
        fade.setFromValue(0.7);
        fade.setToValue(1.0);
        fade.play();
    }

    /**
     * Creates a staggered animation for multiple nodes.
     * @param nodes Array of nodes to animate
     * @param delayBetweenMs Delay between each node's animation
     */
    public static void playStaggeredFadeIn(Node[] nodes, int delayBetweenMs) {
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] != null) {
                playFadeScaleIn(nodes[i], 300, i * delayBetweenMs);
            }
        }
    }
}
