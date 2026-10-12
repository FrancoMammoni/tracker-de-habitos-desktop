package ui.modal;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Modal in-app superpuesto para avisos y notificaciones informativas o de advertencia.
 */
public class NotificationModal extends ModalCard {

    public NotificationModal(String title, String message, boolean isWarning, Runnable onAccept) {
        super(title, null, onAccept, 400);

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle(
                isWarning
                        ? "-fx-text-fill: #E0FBFC; -fx-font-size: 13px; -fx-line-spacing: 3px;"
                        : "-fx-text-fill: #E0FBFC; -fx-font-size: 13px; -fx-line-spacing: 3px;"
        );

        setBody(messageLabel);

        Button btnAccept = new Button("Aceptar");
        btnAccept.setStyle(
                "-fx-background-color: #048A81; -fx-text-fill: #E0FBFC; " +
                "-fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 8 20;"
        );
        btnAccept.setOnAction(e -> onAccept.run());

        setActions(btnAccept);
    }
}
