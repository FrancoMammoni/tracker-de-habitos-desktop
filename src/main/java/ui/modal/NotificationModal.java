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
        messageLabel.getStyleClass().add("modal-message");

        setBody(messageLabel);

        Button btnAccept = new Button("Aceptar");
        btnAccept.getStyleClass().add("modal-btn-primary");
        btnAccept.setOnAction(e -> onAccept.run());

        setActions(btnAccept);
    }
}
