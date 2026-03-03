package util;

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

    // ═══════════════════════════════════════════════════════
    //  PREMIUM ANIMATIONS v3.0
    // ═══════════════════════════════════════════════════════

    /**
     * Elastic bounce-in: node drops in with an elastic overshoot.
     */
    public static void playElasticBounceIn(Node node, int durationMs, int delayMs) {
        node.setOpacity(0);
        node.setScaleX(0.3);
        node.setScaleY(0.3);

        FadeTransition fade = fadeIn(node, durationMs / 2);
        fade.setDelay(Duration.millis(delayMs));

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(durationMs * 0.6), node);
        scaleUp.setDelay(Duration.millis(delayMs));
        scaleUp.setFromX(0.3);
        scaleUp.setFromY(0.3);
        scaleUp.setToX(1.08);
        scaleUp.setToY(1.08);
        scaleUp.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleBack = new ScaleTransition(Duration.millis(durationMs * 0.3), node);
        scaleBack.setToX(0.96);
        scaleBack.setToY(0.96);
        scaleBack.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition settleIn = new ScaleTransition(Duration.millis(durationMs * 0.2), node);
        settleIn.setToX(1.0);
        settleIn.setToY(1.0);
        settleIn.setInterpolator(Interpolator.EASE_IN);

        SequentialTransition bounce = new SequentialTransition(scaleUp, scaleBack, settleIn);
        new ParallelTransition(fade, bounce).play();
    }

    /**
     * Slide-in from left with fade.
     */
    public static void playSlideInFromLeft(Node node, int durationMs, int delayMs) {
        node.setOpacity(0);
        node.setTranslateX(-40);

        FadeTransition fade = fadeIn(node, durationMs);
        fade.setDelay(Duration.millis(delayMs));

        TranslateTransition slide = new TranslateTransition(Duration.millis(durationMs), node);
        slide.setDelay(Duration.millis(delayMs));
        slide.setFromX(-40);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));

        new ParallelTransition(fade, slide).play();
    }

    /**
     * Continuous subtle floating animation (great for hero elements).
     */
    public static TranslateTransition floatAnimation(Node node) {
        TranslateTransition floatAnim = new TranslateTransition(Duration.millis(2500), node);
        floatAnim.setFromY(0);
        floatAnim.setToY(-6);
        floatAnim.setAutoReverse(true);
        floatAnim.setCycleCount(Animation.INDEFINITE);
        floatAnim.setInterpolator(Interpolator.EASE_BOTH);
        return floatAnim;
    }

    /**
     * Shimmer / glow pulse for attention-grabbing elements.
     */
    public static void playShimmer(Node node) {
        FadeTransition shimmer = new FadeTransition(Duration.millis(1200), node);
        shimmer.setFromValue(0.7);
        shimmer.setToValue(1.0);
        shimmer.setAutoReverse(true);
        shimmer.setCycleCount(4);
        shimmer.setInterpolator(Interpolator.EASE_BOTH);
        shimmer.play();
    }

    /**
     * Ripple press effect — shrinks then expands slightly beyond, then settles.
     */
    public static void playRipplePress(Node node) {
        ScaleTransition shrink = new ScaleTransition(Duration.millis(80), node);
        shrink.setToX(0.92);
        shrink.setToY(0.92);
        shrink.setInterpolator(Interpolator.EASE_IN);

        ScaleTransition expand = new ScaleTransition(Duration.millis(120), node);
        expand.setToX(1.04);
        expand.setToY(1.04);
        expand.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition settle = new ScaleTransition(Duration.millis(100), node);
        settle.setToX(1.0);
        settle.setToY(1.0);
        settle.setInterpolator(Interpolator.EASE_BOTH);

        new SequentialTransition(shrink, expand, settle).play();
    }

    /**
     * Staggered card entrance — cards slide up + fade in with increasing delays.
     * Great for FlowPane/VBox children.
     */
    public static void playStaggeredCardEntrance(javafx.scene.Parent container, int staggerMs) {
        var children = container.getChildrenUnmodifiable();
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            child.setOpacity(0);
            child.setTranslateY(30);

            FadeTransition fade = new FadeTransition(Duration.millis(400), child);
            fade.setDelay(Duration.millis(i * staggerMs));
            fade.setFromValue(0);
            fade.setToValue(1);

            TranslateTransition slide = new TranslateTransition(Duration.millis(400), child);
            slide.setDelay(Duration.millis(i * staggerMs));
            slide.setFromY(30);
            slide.setToY(0);
            slide.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));

            new ParallelTransition(fade, slide).play();
        }
    }

    /**
     * Typewriter reveal — gradually increases a label's opacity char by char effect.
     * (Approximated with scale + fade since JavaFX doesn't support per-char animation natively)
     */
    public static void playTypewriterReveal(Node node, int durationMs) {
        node.setOpacity(0);
        node.setScaleX(0.97);

        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.millis(durationMs), node);
        scale.setFromX(0.97);
        scale.setToX(1.0);

        new ParallelTransition(fade, scale).play();
    }

    /**
     * Shake animation for error feedback.
     */
    public static void playShake(Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), node);
        shake.setFromX(0);
        shake.setByX(8);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> node.setTranslateX(0));
        shake.play();
    }

    /**
     * Countdown scale pop for stat numbers.
     */
    public static void playCountPop(Node node) {
        ScaleTransition pop = new ScaleTransition(Duration.millis(200), node);
        pop.setFromX(0.6);
        pop.setFromY(0.6);
        pop.setToX(1.12);
        pop.setToY(1.12);
        pop.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition settle = new ScaleTransition(Duration.millis(150), node);
        settle.setToX(1.0);
        settle.setToY(1.0);
        settle.setInterpolator(Interpolator.EASE_IN);

        new SequentialTransition(pop, settle).play();
    }
}
