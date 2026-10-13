package ui.modal;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Modal in-app superpuesto para confirmaciones de acciones críticas o destructivas.
 */
public class ConfirmModal extends ModalCard {

    public ConfirmModal(
            String title,
            String message,
            String confirmButtonText,
            boolean isDestructive,
            Runnable onConfirm,
            Runnable onCancel
    ) {
        super(title, null, onCancel, 440);

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("modal-message");

        setBody(messageLabel);

        Button btnCancel = new Button("Cancelar");
        btnCancel.getStyleClass().add("modal-btn-cancel");
        btnCancel.setOnAction(e -> onCancel.run());

        Button btnConfirm = new Button(confirmButtonText);
        if (isDestructive) {
            btnConfirm.getStyleClass().add("modal-btn-destructive");
        } else {
            btnConfirm.getStyleClass().add("modal-btn-primary");
        }
        btnConfirm.setOnAction(e -> onConfirm.run());

        setActions(btnCancel, btnConfirm);
    }
}
