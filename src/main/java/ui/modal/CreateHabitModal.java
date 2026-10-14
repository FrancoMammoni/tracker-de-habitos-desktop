package ui.modal;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.Habit;

import java.time.DayOfWeek;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Modal in-app superpuesto para la creación de un nuevo hábito con selector interactivo de etiquetas.
 */
public class CreateHabitModal extends ModalCard {

    private final TextField nombreField = new TextField();
    private final TextField descripcionField = new TextField();
    private String categoriaSeleccionada = "General";
    private final Label categoriaChip = new Label("General");
    private final Button btnSeleccionarTag = new Button("Cambiar");
    private final Spinner<Integer> objetivoSpinner = new Spinner<>(1, 7, 1);
    private final Map<DayOfWeek, ToggleButton> diaButtons = new LinkedHashMap<>();

    public CreateHabitModal(
            Consumer<Habit> onSave,
            Runnable onCancel,
            BiConsumer<String, Consumer<String>> onSelectTagRequest
    ) {
        super("Nuevo Hábito", "Configura tu hábito y el objetivo semanal de cumplimiento", onCancel, 480);

        nombreField.setPromptText("Ej. Leer 20 páginas");
        descripcionField.setPromptText("Detalle opcional");

        // Componente selector de categoría/etiqueta
        categoriaChip.getStyleClass().add("tag-chip-preview");
        btnSeleccionarTag.getStyleClass().add("tag-picker-btn");

        if (onSelectTagRequest != null) {
            btnSeleccionarTag.setOnAction(e -> onSelectTagRequest.accept(categoriaSeleccionada, nueva -> {
                categoriaSeleccionada = nueva;
                categoriaChip.setText(nueva);
            }));
        }

        HBox tagBox = new HBox(8, categoriaChip, btnSeleccionarTag);
        tagBox.setAlignment(Pos.CENTER_LEFT);

        // Selector de días de la semana
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
            tb.setSelected(true); // Seleccionado por defecto
            updateToggleStyle(tb);
            tb.setOnAction(e -> {
                updateToggleStyle(tb);
                actualizarLimiteSpinner();
            });
            diaButtons.put(dia, tb);
            diasBox.getChildren().add(tb);
        }

        // Configuración de formulario
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        grid.add(createLabel("Nombre *"), 0, 0);
        grid.add(nombreField, 1, 0);

        grid.add(createLabel("Descripción"), 0, 1);
        grid.add(descripcionField, 1, 1);

        grid.add(createLabel("Etiqueta"), 0, 2);
        grid.add(tagBox, 1, 2);

        VBox diasSection = new VBox(6, createLabel("Días de aparición (estilo alarma) *"), diasBox);
        grid.add(diasSection, 0, 3, 2, 1);

        Label objLabel = createLabel("Objetivo semanal mínimo (días cumplidos):");
        objetivoSpinner.setEditable(false);
        objetivoSpinner.setPrefWidth(100);
        HBox objRow = new HBox(12, objLabel, objetivoSpinner);
        objRow.setAlignment(Pos.CENTER_LEFT);
        grid.add(objRow, 0, 4, 2, 1);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("modal-error-label");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        setBody(grid, errorLabel);

        // Botones de acción
        Button btnCancel = new Button("Cancelar");
        btnCancel.getStyleClass().add("modal-btn-cancel");
        btnCancel.setOnAction(e -> onCancel.run());

        Button btnSave = new Button("Crear Hábito");
        btnSave.getStyleClass().add("modal-btn-primary");
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

            Habit habit = new Habit(
                    nombre,
                    descripcionField.getText().trim(),
                    categoriaSeleccionada,
                    dias,
                    objetivoSpinner.getValue()
            );
            onSave.accept(habit);
        });

        setActions(btnCancel, btnSave);
        actualizarLimiteSpinner();
    }

    public CreateHabitModal(Consumer<Habit> onSave, Runnable onCancel) {
        this(onSave, onCancel, null);
    }

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("modal-label");
        return lbl;
    }

    private void updateToggleStyle(ToggleButton tb) {
        if (tb.isSelected()) {
            if (!tb.getStyleClass().contains("alarm-day-selected")) {
                tb.getStyleClass().add("alarm-day-selected");
            }
        } else {
            tb.getStyleClass().remove("alarm-day-selected");
        }
    }

    private void actualizarLimiteSpinner() {
        int diasSeleccionados = getDiasSeleccionados().size();
        int max = Math.max(1, diasSeleccionados);
        int current = objetivoSpinner.getValue() != null ? objetivoSpinner.getValue() : 1;
        if (current > max) {
            current = max;
        }
        objetivoSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, max, current));
    }

    private Set<DayOfWeek> getDiasSeleccionados() {
        Set<DayOfWeek> seleccionados = new HashSet<>();
        diaButtons.forEach((dia, tb) -> {
            if (tb.isSelected()) {
                seleccionados.add(dia);
            }
        });
        return seleccionados;
    }
}
