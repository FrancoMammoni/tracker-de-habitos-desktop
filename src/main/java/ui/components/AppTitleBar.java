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

        Button btnMin = createControlButton("—", "title-bar-btn");
        btnMin.setOnAction(e -> stage.setIconified(true));

        this.btnMax = createControlButton("□", "title-bar-btn");
        this.btnMax.setOnAction(e -> toggleMaximize());

        Button btnClose = createControlButton("✕", "title-bar-btn-close");
        btnClose.setOnAction(e -> stage.close());

        controlsBox.getChildren().addAll(btnMin, btnMax, btnClose);

        getChildren().addAll(iconView, titleLabel, spacer, controlsBox);

        // Arrastre fluido de la ventana
        setupWindowDragging();
    }

    private Button createControlButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.setPrefSize(32, 26);
        btn.setMinSize(32, 26);
        btn.setMaxSize(32, 26);
        btn.getStyleClass().add(styleClass);
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
