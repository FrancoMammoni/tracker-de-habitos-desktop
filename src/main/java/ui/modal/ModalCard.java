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
        getStyleClass().add("modal-card");

        // Encabezado
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("modal-title");
        titleBox.getChildren().add(titleLabel);

        if (subtitle != null && !subtitle.isBlank()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.setWrapText(true);
            subtitleLabel.getStyleClass().add("modal-subtitle");
            titleBox.getChildren().add(subtitleLabel);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerRow.getChildren().addAll(titleBox, spacer);

        if (onClose != null) {
            Button closeBtn = new Button("✕");
            closeBtn.setPrefSize(28, 28);
            closeBtn.getStyleClass().add("modal-close-btn");
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
