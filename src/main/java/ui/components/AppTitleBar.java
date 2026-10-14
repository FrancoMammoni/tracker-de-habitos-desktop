package ui.components;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.Objects;

/**
 * Barra de título oscura integrada y personalizada para la ventana principal.
 * Reemplaza la barra nativa de Windows, ofreciendo botones de control (minimizar,
 * maximizar y cerrar), arrastre con el mouse y soporte para doble clic respetando
 * la barra de tareas de Windows mediante el uso de VisualBounds.
 */
public class AppTitleBar extends HBox {

    private final Stage stage;
    private final Button btnMax;
    private final SVGPath maxIcon;
    private boolean isCustomMaximized = false;
    private double prevX = 0;
    private double prevY = 0;
    private double prevWidth = 0;
    private double prevHeight = 0;
    private double xOffset = 0;
    private double yOffset = 0;

    public AppTitleBar(Stage stage) {
        this.stage = Objects.requireNonNull(stage, "El Stage no puede ser nulo");

        setPrefHeight(38);
        setMinHeight(38);
        setMaxHeight(38);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("app-title-bar");

        // Ícono de la aplicación
        ImageView iconView = new ImageView();
        InputStream iconStream = getClass().getResourceAsStream("/icons/app_icon.png");
        if (iconStream != null) {
            Image icon = new Image(iconStream);
            iconView.setImage(icon);
            iconView.setFitWidth(20);
            iconView.setFitHeight(20);
            iconView.setSmooth(true);
        }

        // Título de la aplicación
        Label titleLabel = new Label("Tracker de Hábitos");
        titleLabel.getStyleClass().add("app-title-label");

        // Espaciador flexible
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Botones de control de ventana
        HBox controlsBox = new HBox(4);
        controlsBox.setAlignment(Pos.CENTER_RIGHT);

        SVGPath minIcon = new SVGPath();
        minIcon.setContent("M 0 5 H 10");
        minIcon.getStyleClass().add("title-bar-icon");
        Button btnMin = createControlButton(minIcon, "title-bar-btn");
        btnMin.setOnAction(e -> stage.setIconified(true));

        this.maxIcon = new SVGPath();
        this.maxIcon.getStyleClass().add("title-bar-icon");
        updateMaximizeIcon();
        this.btnMax = createControlButton(maxIcon, "title-bar-btn");
        this.btnMax.setOnAction(e -> toggleMaximize());

        SVGPath closeIcon = new SVGPath();
        closeIcon.setContent("M 0.5 0.5 L 9.5 9.5 M 9.5 0.5 L 0.5 9.5");
        closeIcon.getStyleClass().add("title-bar-icon");
        Button btnClose = createControlButton(closeIcon, "title-bar-btn-close");
        btnClose.setOnAction(e -> stage.close());

        controlsBox.getChildren().addAll(btnMin, btnMax, btnClose);

        getChildren().addAll(iconView, titleLabel, spacer, controlsBox);

        // Arrastre fluido de la ventana
        setupWindowDragging();
    }

    private Button createControlButton(javafx.scene.Node graphic, String styleClass) {
        Button btn = new Button();
        btn.setGraphic(graphic);
        btn.setPrefSize(34, 26);
        btn.setMinSize(34, 26);
        btn.setMaxSize(34, 26);
        btn.getStyleClass().add(styleClass);
        return btn;
    }

    public void toggleMaximize() {
        if (isCustomMaximized) {
            restoreWindow();
        } else {
            maximizeWindow();
        }
    }

    private void maximizeWindow() {
        prevX = stage.getX();
        prevY = stage.getY();
        prevWidth = stage.getWidth();
        prevHeight = stage.getHeight();

        ObservableList<Screen> screens = Screen.getScreensForRectangle(prevX, prevY, prevWidth, prevHeight);
        Screen currentScreen = screens.isEmpty() ? Screen.getPrimary() : screens.get(0);
        Rectangle2D visualBounds = currentScreen.getVisualBounds();

        stage.setX(visualBounds.getMinX());
        stage.setY(visualBounds.getMinY());
        stage.setWidth(visualBounds.getWidth());
        stage.setHeight(visualBounds.getHeight());

        isCustomMaximized = true;
        stage.getProperties().put("custom-maximized", true);
        updateMaximizeIcon();
    }

    private void restoreWindow() {
        if (prevWidth > 0 && prevHeight > 0) {
            stage.setX(prevX);
            stage.setY(prevY);
            stage.setWidth(prevWidth);
            stage.setHeight(prevHeight);
        }

        isCustomMaximized = false;
        stage.getProperties().put("custom-maximized", false);
        updateMaximizeIcon();
    }

    private void updateMaximizeIcon() {
        if (isCustomMaximized) {
            maxIcon.setContent("M 2.5 2.5 V 0.5 H 9.5 V 7.5 H 7.5 M 0.5 2.5 H 7.5 V 9.5 H 0.5 Z");
        } else {
            maxIcon.setContent("M 0.5 0.5 H 9.5 V 9.5 H 0.5 Z");
        }
    }

    private void setupWindowDragging() {
        setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        setOnMouseDragged(event -> {
            if (isCustomMaximized) {
                double ratioX = event.getSceneX() / stage.getWidth();
                restoreWindow();
                xOffset = prevWidth * ratioX;
                yOffset = event.getSceneY();
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            } else {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });

        setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                toggleMaximize();
            }
        });
    }
}
