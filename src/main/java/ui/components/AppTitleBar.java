package ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.Objects;

/**
 * Barra de título oscura integrada y personalizada para la ventana principal.
 * Reemplaza la barra nativa de Windows, ofreciendo botones de control (minimizar,
 * maximizar y cerrar), arrastre con el mouse y soporte para doble clic.
 */
public class AppTitleBar extends HBox {

    private final Stage stage;
    private final Button btnMax;
    private double xOffset = 0;
    private double yOffset = 0;

    public AppTitleBar(Stage stage) {
        this.stage = Objects.requireNonNull(stage, "El Stage no puede ser nulo");

        setPrefHeight(38);
        setMinHeight(38);
        setMaxHeight(38);
        setAlignment(Pos.CENTER_LEFT);
        setStyle(
                "-fx-background-color: #16201e; " +
                "-fx-border-color: #505168; " +
                "-fx-border-width: 0 0 1 0; " +
                "-fx-padding: 0 8 0 12;"
        );

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
        titleLabel.setStyle(
                "-fx-text-fill: #E0FBFC; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 0 0 0 8;"
        );

        // Espaciador flexible
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Botones de control de ventana
        HBox controlsBox = new HBox(4);
        controlsBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnMin = createControlButton("—", 12);
        btnMin.setOnAction(e -> stage.setIconified(true));

        this.btnMax = createControlButton("□", 13);
        this.btnMax.setOnAction(e -> toggleMaximize());

        Button btnClose = createControlButton("✕", 12);
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(
                "-fx-background-color: #A50104; -fx-text-fill: #FFFFFF; " +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4px;"
        ));
        btnClose.setOnMouseExited(e -> btnClose.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #8d8fa8; " +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4px;"
        ));
        btnClose.setOnAction(e -> stage.close());

        controlsBox.getChildren().addAll(btnMin, btnMax, btnClose);

        getChildren().addAll(iconView, titleLabel, spacer, controlsBox);

        // Arrastre fluido de la ventana
        setupWindowDragging();
    }

    private Button createControlButton(String text, int fontSize) {
        Button btn = new Button(text);
        btn.setPrefSize(32, 26);
        btn.setMinSize(32, 26);
        btn.setMaxSize(32, 26);
        btn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #8d8fa8; " +
                "-fx-font-size: " + fontSize + "px; -fx-font-weight: bold; " +
                "-fx-cursor: hand; -fx-background-radius: 4px; -fx-padding: 0;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #243330; -fx-text-fill: #E0FBFC; " +
                "-fx-font-size: " + fontSize + "px; -fx-font-weight: bold; " +
                "-fx-cursor: hand; -fx-background-radius: 4px; -fx-padding: 0;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #8d8fa8; " +
                "-fx-font-size: " + fontSize + "px; -fx-font-weight: bold; " +
                "-fx-cursor: hand; -fx-background-radius: 4px; -fx-padding: 0;"
        ));
        return btn;
    }

    private void toggleMaximize() {
        stage.setMaximized(!stage.isMaximized());
        btnMax.setText(stage.isMaximized() ? "❐" : "□");
    }

    private void setupWindowDragging() {
        setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        setOnMouseDragged(event -> {
            if (!stage.isMaximized()) {
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
