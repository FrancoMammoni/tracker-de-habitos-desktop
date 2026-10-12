package ui.modal;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.Habit;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Modal in-app superpuesto para registrar avance cuantitativo parcial de una jornada.
 */
public class PartialProgressModal extends ModalCard {

    public PartialProgressModal(
            Habit habit,
            Double currentValue,
            Consumer<Double> onSave,
            Runnable onCancel
    ) {
        super(
                "Avance Parcial — " + habit.nombre(),
                "Registra el valor cuantitativo alcanzado hoy.\n(Nota: La jornada computará como Fallado respecto a la meta total)",
                onCancel,
                420
        );

        Label promptLabel = new Label("Valor numérico alcanzado:");
        promptLabel.setStyle("-fx-text-fill: #E0FBFC; -fx-font-weight: bold; -fx-font-size: 13px;");

        TextField valueField = new TextField();
        valueField.setPromptText("Ej. 15, 2.5, 40");
        if (currentValue != null) {
            valueField.setText(String.valueOf(currentValue));
        }

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #A50104; -fx-font-size: 12px; -fx-font-weight: bold;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        setBody(promptLabel, valueField, errorLabel);

        Button btnCancel = new Button("Cancelar");
        btnCancel.setStyle(
                "-fx-background-color: #505168; -fx-text-fill: #E0FBFC; " +
                "-fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 8 16;"
        );
        btnCancel.setOnAction(e -> onCancel.run());

        Button btnSave = new Button("Guardar Avance");
        btnSave.setStyle(
                "-fx-background-color: #048A81; -fx-text-fill: #E0FBFC; " +
                "-fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 8 18;"
        );

        Runnable handleSave = () -> {
            String text = valueField.getText().trim();
            if (text.isEmpty()) {
                errorLabel.setText("Por favor ingresa un valor numérico.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            try {
                double parsed = Double.parseDouble(text.replace(',', '.'));
                if (parsed < 0) {
                    errorLabel.setText("El valor ingresado no puede ser negativo.");
                    errorLabel.setVisible(true);
                    errorLabel.setManaged(true);
                    return;
                }
                onSave.accept(parsed);
            } catch (NumberFormatException e) {
                errorLabel.setText("Por favor ingresa un número válido (ej. 15 o 3.5).");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        };

        btnSave.setOnAction(e -> handleSave.run());
        valueField.setOnAction(e -> handleSave.run());

        setActions(btnCancel, btnSave);
    }
}
