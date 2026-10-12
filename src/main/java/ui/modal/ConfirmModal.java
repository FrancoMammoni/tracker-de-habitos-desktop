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
        messageLabel.setStyle("-fx-text-fill: #E0FBFC; -fx-font-size: 13px; -fx-line-spacing: 4px;");

        setBody(messageLabel);

        Button btnCancel = new Button("Cancelar");
        btnCancel.setStyle(
                "-fx-background-color: #505168; -fx-text-fill: #E0FBFC; " +
                "-fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 8 16;"
        );
        btnCancel.setOnAction(e -> onCancel.run());

        Button btnConfirm = new Button(confirmButtonText);
        if (isDestructive) {
            btnConfirm.setStyle(
                    "-fx-background-color: #A50104; -fx-text-fill: #FFFFFF; " +
                    "-fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 8 16;"
            );
        } else {
            btnConfirm.setStyle(
                    "-fx-background-color: #048A81; -fx-text-fill: #E0FBFC; " +
                    "-fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 8 16;"
            );
        }
        btnConfirm.setOnAction(e -> onConfirm.run());

        setActions(btnCancel, btnConfirm);
    }
}
