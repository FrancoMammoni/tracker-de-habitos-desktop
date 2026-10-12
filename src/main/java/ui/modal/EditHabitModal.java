package ui.modal;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.Habit;

import java.time.DayOfWeek;
import java.util.*;
import java.util.function.Consumer;

/**
 * Modal in-app superpuesto para la edición de un hábito existente.
 */
public class EditHabitModal extends ModalCard {

    private final TextField nombreField = new TextField();
    private final TextField descripcionField = new TextField();
    private final TextField categoriaField = new TextField();
    private final Spinner<Integer> objetivoSpinner;
    private final Map<DayOfWeek, ToggleButton> diaButtons = new LinkedHashMap<>();

    public EditHabitModal(Habit habit, Consumer<Habit> onSave, Runnable onCancel) {
        super("Editar Hábito", "Modifica la configuración de \"" + habit.nombre() + "\"", onCancel, 480);

        nombreField.setText(habit.nombre());
        descripcionField.setText(habit.descripcion() == null ? "" : habit.descripcion());
        categoriaField.setText(habit.categoria() == null ? "" : habit.categoria());

        this.objetivoSpinner = new Spinner<>(1, Math.max(1, habit.diasAparicion().size()), habit.objetivoSemanal());

        // Selector de días
        HBox diasBox = new HBox(6);
        diasBox.setAlignment(Pos.CENTER_LEFT);

        DayOfWeek[] ordenDias = {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        };
        String[] etiquetas = { "L", "M", "X", "J", "V", "S", "D" };

        for (int i = 0; i < ordenDias.length; i++) {
            DayOfWeek dia = ordenDias[i];
            ToggleButton tb = new ToggleButton(etiquetas[i]);
            tb.setPrefSize(38, 38);
            tb.setSelected(habit.diasAparicion().contains(dia));
            updateToggleStyle(tb);
            tb.setOnAction(e -> {
                updateToggleStyle(tb);
                actualizarLimiteSpinner();
            });
            diaButtons.put(dia, tb);
            diasBox.getChildren().add(tb);
        }

        // Formulario
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        grid.add(createLabel("Nombre *"), 0, 0);
        grid.add(nombreField, 1, 0);

        grid.add(createLabel("Descripción"), 0, 1);
        grid.add(descripcionField, 1, 1);

        grid.add(createLabel("Categoría"), 0, 2);
        grid.add(categoriaField, 1, 2);

        VBox diasSection = new VBox(6, createLabel("Días de aparición (estilo alarma) *"), diasBox);
        grid.add(diasSection, 0, 3, 2, 1);

        Label objLabel = createLabel("Objetivo semanal mínimo (días cumplidos):");
        objetivoSpinner.setEditable(false);
        objetivoSpinner.setPrefWidth(100);
        HBox objRow = new HBox(12, objLabel, objetivoSpinner);
        objRow.setAlignment(Pos.CENTER_LEFT);
        grid.add(objRow, 0, 4, 2, 1);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #A50104; -fx-font-size: 12px; -fx-font-weight: bold;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        setBody(grid, errorLabel);

        // Botones de acción
        Button btnCancel = new Button("Cancelar");
        btnCancel.setStyle(
                "-fx-background-color: #505168; -fx-text-fill: #E0FBFC; " +
                "-fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 8 16;"
        );
        btnCancel.setOnAction(e -> onCancel.run());

        Button btnSave = new Button("Guardar Cambios");
        btnSave.setStyle(
                "-fx-background-color: #048A81; -fx-text-fill: #E0FBFC; " +
                "-fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand; " +
                "-fx-background-radius: 6px; -fx-padding: 8 18;"
        );
        btnSave.setOnAction(e -> {
            String nombre = nombreField.getText().trim();
            Set<DayOfWeek> dias = getDiasSeleccionados();

            if (nombre.isBlank()) {
                errorLabel.setText("El nombre del hábito es obligatorio.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                nombreField.requestFocus();
                return;
            }

            if (dias.isEmpty()) {
                errorLabel.setText("Debes seleccionar al menos un día de aparición.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            Habit updated = new Habit(
                    habit.id(),
                    nombre,
                    descripcionField.getText().trim(),
                    categoriaField.getText().trim(),
                    dias,
                    objetivoSpinner.getValue(),
                    habit.archivado(),
                    habit.createdAt()
            );
            onSave.accept(updated);
        });

        setActions(btnCancel, btnSave);
        actualizarLimiteSpinner();
    }

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #E0FBFC; -fx-font-weight: bold; -fx-font-size: 13px;");
        return lbl;
    }

    private void updateToggleStyle(ToggleButton tb) {
        if (tb.isSelected()) {
            tb.setStyle(
                    "-fx-background-color: #048A81; -fx-text-fill: #E0FBFC; " +
                    "-fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;"
            );
        } else {
            tb.setStyle(
                    "-fx-background-color: #16201e; -fx-text-fill: #8d8fa8; " +
                    "-fx-background-radius: 6px; -fx-cursor: hand; -fx-border-color: #505168; -fx-border-radius: 6px;"
            );
        }
    }

    private Set<DayOfWeek> getDiasSeleccionados() {
        Set<DayOfWeek> set = new HashSet<>();
        diaButtons.forEach((dia, btn) -> {
            if (btn.isSelected()) set.add(dia);
        });
        return set;
    }

    private void actualizarLimiteSpinner() {
        int maxDias = Math.max(1, getDiasSeleccionados().size());
        int actual = Math.min(objetivoSpinner.getValue(), maxDias);
        objetivoSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, maxDias, actual));
    }
}
