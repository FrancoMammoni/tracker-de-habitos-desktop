package ui.modal;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Contenedor base de tarjeta elevada para modales in-app tipo overlay.
 * Provee encabezado con título, subtítulo, botón de cierre y contenedor para cuerpo y botones.
 */
public class ModalCard extends VBox {

    private final VBox bodyContainer;
    private final HBox footerContainer;

    public ModalCard(String title, String subtitle, Runnable onClose, double preferredWidth) {
        Objects.requireNonNull(title, "El título no puede ser nulo");

        setMaxWidth(preferredWidth);
        setMinWidth(Math.min(preferredWidth, 380));
        setAlignment(Pos.TOP_LEFT);
        setSpacing(16);
        setPadding(new Insets(24));
        setStyle(
                "-fx-background-color: #243330; " +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: #505168; " +
                "-fx-border-radius: 12px; " +
                "-fx-border-width: 1px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 32, 0, 0, 10);"
        );

        // Encabezado
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #E0FBFC;");
        titleBox.getChildren().add(titleLabel);

        if (subtitle != null && !subtitle.isBlank()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.setWrapText(true);
            subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8d8fa8;");
            titleBox.getChildren().add(subtitleLabel);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerRow.getChildren().addAll(titleBox, spacer);

        if (onClose != null) {
            Button closeBtn = new Button("✕");
            closeBtn.setPrefSize(28, 28);
            closeBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #8d8fa8; " +
                    "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4px;"
            );
            closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                    "-fx-background-color: #A50104; -fx-text-fill: white; " +
                    "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4px;"
            ));
            closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #8d8fa8; " +
                    "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4px;"
            ));
            closeBtn.setOnAction(e -> onClose.run());
            headerRow.getChildren().add(closeBtn);
        }

        // Cuerpo dinámico
        this.bodyContainer = new VBox(12);
        this.bodyContainer.setAlignment(Pos.TOP_LEFT);

        // Pie de página para botones de acción
        this.footerContainer = new HBox(12);
        this.footerContainer.setAlignment(Pos.CENTER_RIGHT);
        this.footerContainer.setPadding(new Insets(8, 0, 0, 0));

        getChildren().addAll(headerRow, bodyContainer, footerContainer);
    }

    public void setBody(Node... nodes) {
        bodyContainer.getChildren().setAll(nodes);
    }

    public void setActions(Node... buttons) {
        footerContainer.getChildren().setAll(buttons);
    }

    public VBox getBodyContainer() {
        return bodyContainer;
    }
}
