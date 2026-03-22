package com.javafx.Clases;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputControl;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Clase de utilidades para animaciones en JavaFX
 * Basada en el TEMA 2-6 ANIMACIONES:
 * 
 * - FadeTransition: animacion de opacidad (aparecer/desaparecer)
 * - ScaleTransition: animacion de escala (agrandar/encoger)
 * - Timeline con KeyFrames: animaciones personalizadas
 * - ParallelTransition: animaciones simultaneas
 * 
 * @author RehabiAPP
 */
public class AnimacionUtil {

    // ==================== FADE TRANSITIONS ====================

    /**
     * Hace que un nodo aparezca gradualmente (FadeTransition)
     * El nodo comienza invisible (opacidad 0) y termina visible (opacidad 1)
     * 
     * @param nodo Nodo a animar
     * @param duracionMs Duracion en milisegundos
     */
    public static void fadeIn(Node nodo, int duracionMs) {
        if (nodo == null) return;

        // Empezar invisible
        nodo.setOpacity(0);

        // Crear FadeTransition
        FadeTransition fade = new FadeTransition(Duration.millis(duracionMs), nodo);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_BOTH);
        fade.play();
    }

    /**
     * Hace que un nodo desaparezca gradualmente (FadeTransition)
     * 
     * @param nodo Nodo a animar
     * @param duracionMs Duracion en milisegundos
     */
    public static void fadeOut(Node nodo, int duracionMs) {
        if (nodo == null) return;

        FadeTransition fade = new FadeTransition(Duration.millis(duracionMs), nodo);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setInterpolator(Interpolator.EASE_BOTH);
        fade.play();
    }

    // ==================== SCALE TRANSITIONS ====================

    /**
     * Anima una ventana/stage para que aparezca aumentando de tamaño
     * Combina ScaleTransition + FadeTransition (ParallelTransition)
     * 
     * @param stage Stage a animar
     * @param duracionMs Duracion en milisegundos
     */
    public static void animarVentana(Stage stage, int duracionMs) {
        if (stage == null || stage.getScene() == null || stage.getScene().getRoot() == null) return;

        Node root = stage.getScene().getRoot();

        // Empezar pequeño e invisible
        root.setScaleX(0.8);
        root.setScaleY(0.8);
        root.setOpacity(0);

        // ScaleTransition - aumentar tamaño
        ScaleTransition scale = new ScaleTransition(Duration.millis(duracionMs), root);
        scale.setFromX(0.8);
        scale.setFromY(0.8);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        // FadeTransition - aparecer
        FadeTransition fade = new FadeTransition(Duration.millis(duracionMs), root);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        // Ejecutar ambas en paralelo
        ParallelTransition parallel = new ParallelTransition(scale, fade);
        parallel.play();
    }

    /**
     * Anima un nodo para que aparezca aumentando de tamaño
     * 
     * @param nodo Nodo a animar
     * @param duracionMs Duracion en milisegundos
     */
    public static void scaleIn(Node nodo, int duracionMs) {
        if (nodo == null) return;

        // Empezar pequeño e invisible
        nodo.setScaleX(0.5);
        nodo.setScaleY(0.5);
        nodo.setOpacity(0);

        // ScaleTransition
        ScaleTransition scale = new ScaleTransition(Duration.millis(duracionMs), nodo);
        scale.setFromX(0.5);
        scale.setFromY(0.5);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        // FadeTransition
        FadeTransition fade = new FadeTransition(Duration.millis(duracionMs), nodo);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        // Paralelo
        ParallelTransition parallel = new ParallelTransition(scale, fade);
        parallel.play();
    }

    // ==================== TIMELINE - EFECTOS PERSONALIZADOS ====================

    /**
     * Crea un efecto de brillo suave pulsante en un campo de texto
     * Usa Timeline con KeyFrames para animar el efecto Glow
     * 
     * @param campo Campo de texto (TextField, PasswordField, etc.)
     * @return Timeline para poder detenerlo cuando sea necesario
     */
    public static Timeline brilloCampoTexto(TextInputControl campo) {
        if (campo == null) return null;

        // Crear efecto Glow
        Glow glow = new Glow(0);
        campo.setEffect(glow);

        // Timeline con KeyFrames para pulsar el brillo
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(glow.levelProperty(), 0.0)),
            new KeyFrame(Duration.millis(1500), 
                new KeyValue(glow.levelProperty(), 0.3, Interpolator.EASE_BOTH)),
            new KeyFrame(Duration.millis(3000), 
                new KeyValue(glow.levelProperty(), 0.0, Interpolator.EASE_BOTH))
        );

        // Repetir indefinidamente
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        return timeline;
    }

    /**
     * Crea un efecto de brillo en hover para botones
     * Usa Timeline para animar DropShadow
     * 
     * @param boton Boton a animar
     * @param colorBrillo Color del brillo
     */
    public static void brilloHover(Button boton, Color colorBrillo) {
        if (boton == null) return;

        // Crear efecto DropShadow
        DropShadow shadow = new DropShadow();
        shadow.setColor(colorBrillo);
        shadow.setRadius(0);
        shadow.setSpread(0);

        // Al entrar el mouse - animar brillo
        boton.setOnMouseEntered(e -> {
            boton.setEffect(shadow);

            Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(shadow.radiusProperty(), 0),
                    new KeyValue(shadow.spreadProperty(), 0)),
                new KeyFrame(Duration.millis(200),
                    new KeyValue(shadow.radiusProperty(), 15, Interpolator.EASE_OUT),
                    new KeyValue(shadow.spreadProperty(), 0.3, Interpolator.EASE_OUT))
            );
            timeline.play();

            // Pequeño aumento de escala
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), boton);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
        });

        // Al salir el mouse - quitar brillo
        boton.setOnMouseExited(e -> {
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(shadow.radiusProperty(), 15),
                    new KeyValue(shadow.spreadProperty(), 0.3)),
                new KeyFrame(Duration.millis(200),
                    new KeyValue(shadow.radiusProperty(), 0, Interpolator.EASE_OUT),
                    new KeyValue(shadow.spreadProperty(), 0, Interpolator.EASE_OUT))
            );
            timeline.setOnFinished(ev -> boton.setEffect(null));
            timeline.play();

            // Volver a escala normal
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), boton);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });
    }

    /**
     * Efecto de "sacudida" para indicar error (util para campos invalidos)
     * Usa Timeline para mover el nodo horizontalmente
     * 
     * @param nodo Nodo a sacudir
     */
    public static void sacudir(Node nodo) {
        if (nodo == null) return;

        Timeline timeline = new Timeline(
            new KeyFrame(Duration.millis(0), new KeyValue(nodo.translateXProperty(), 0)),
            new KeyFrame(Duration.millis(50), new KeyValue(nodo.translateXProperty(), -10)),
            new KeyFrame(Duration.millis(100), new KeyValue(nodo.translateXProperty(), 10)),
            new KeyFrame(Duration.millis(150), new KeyValue(nodo.translateXProperty(), -10)),
            new KeyFrame(Duration.millis(200), new KeyValue(nodo.translateXProperty(), 10)),
            new KeyFrame(Duration.millis(250), new KeyValue(nodo.translateXProperty(), -5)),
            new KeyFrame(Duration.millis(300), new KeyValue(nodo.translateXProperty(), 5)),
            new KeyFrame(Duration.millis(350), new KeyValue(nodo.translateXProperty(), 0))
        );
        timeline.play();
    }

    /**
     * Efecto de pulso (el nodo crece y vuelve a su tamaño)
     * 
     * @param nodo Nodo a pulsar
     */
    public static void pulso(Node nodo) {
        if (nodo == null) return;

        ScaleTransition scale = new ScaleTransition(Duration.millis(150), nodo);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.1);
        scale.setToY(1.1);
        scale.setCycleCount(2);
        scale.setAutoReverse(true);
        scale.play();
    }

    // ==================== ANIMACIONES PARA VENTANAS MODALES ====================

    /**
     * Aplica animacion de entrada a una ventana modal
     * Llamar despues de stage.show() o en setOnShown
     * 
     * @param stage Stage de la ventana modal
     */
    public static void animarVentanaModal(Stage stage) {
        animarVentana(stage, 250);
    }

    /**
     * Aplica animacion de salida a una ventana modal
     *
     * @param stage Stage de la ventana modal
     * @param onFinished Accion a ejecutar cuando termine (ej: stage.close())
     */
    public static void animarCierreVentana(Stage stage, Runnable onFinished) {
        if (stage == null || stage.getScene() == null) {
            if (onFinished != null) onFinished.run();
            return;
        }

        Node root = stage.getScene().getRoot();

        // ScaleTransition - reducir tamaño
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), root);
        scale.setToX(0.8);
        scale.setToY(0.8);

        // FadeTransition - desaparecer
        FadeTransition fade = new FadeTransition(Duration.millis(200), root);
        fade.setToValue(0.0);

        // Paralelo
        ParallelTransition parallel = new ParallelTransition(scale, fade);
        parallel.setOnFinished(e -> {
            if (onFinished != null) onFinished.run();
        });
        parallel.play();
    }

    // ==================== ANIMACIONES PARA TRANSICIONES DE PESTAÑAS ====================

    /**
     * Anima la transición de una pestaña con efecto de desvanecimiento y deslizamiento
     * Combina FadeTransition + TranslateTransition para un efecto suave
     *
     * @param nodo Nodo de la pestaña a animar (generalmente un VBox o BorderPane)
     * @param duracionMs Duración en milisegundos (recomendado: 300-400)
     */
    public static void animarTransicionPestania(Node nodo, int duracionMs) {
        if (nodo == null) return;

        // Configuración inicial: invisible y ligeramente desplazado a la derecha
        nodo.setOpacity(0);
        nodo.setTranslateX(30);

        // FadeTransition - aparecer gradualmente
        FadeTransition fade = new FadeTransition(Duration.millis(duracionMs), nodo);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        // Timeline para deslizar desde la derecha
        Timeline slide = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(nodo.translateXProperty(), 30)),
            new KeyFrame(Duration.millis(duracionMs),
                new KeyValue(nodo.translateXProperty(), 0, Interpolator.EASE_OUT))
        );

        // Ejecutar ambas animaciones en paralelo
        ParallelTransition transition = new ParallelTransition(fade, slide);
        transition.play();
    }

    /**
     * Versión predeterminada de animarTransicionPestania con duración de 350ms
     *
     * @param nodo Nodo de la pestaña a animar
     */
    public static void animarTransicionPestania(Node nodo) {
        animarTransicionPestania(nodo, 350);
    }
}
