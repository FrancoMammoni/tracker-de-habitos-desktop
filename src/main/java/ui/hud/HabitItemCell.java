package ui.hud;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import model.Habit;

import java.util.function.Consumer;

/**
 * Celda visual interactiva y minimalista para un hábito en el HUD diario.
 * Cumple con la regla de cero sobrecarga visual: [ ]/[x] + Nombre.
 * En hover y mediante clic derecho (menú contextual) ofrece acciones sutiles
 * para registrar avance parcial, editar o archivar el hábito.
 */
public class HabitItemCell extends HBox {

    private final Habit habit;
    private final CheckBox checkBox;
    private final Label nameLabel;
    private final HBox actionsBox;
    private final Button partialButton;
    private final Button editButton;
    private final Button archiveButton;

    public HabitItemCell(
            Habit habit,
            boolean completed,
            Consumer<Boolean> onStatusChanged,
            Consumer<Habit> onPartialRequested
    ) {
        this(habit, completed, onStatusChanged, onPartialRequested, null, null);
    }

    public HabitItemCell(
            Habit habit,
            boolean completed,
            Consumer<Boolean> onStatusChanged,
            Consumer<Habit> onPartialRequested,
            Consumer<Habit> onEditRequested,
            Consumer<Habit> onArchiveRequested
    ) {
        this.habit = habit;

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(10);
        setPadding(new Insets(8, 12, 8, 12));
        getStyleClass().add("habit-item-cell");

        // 1. Checkbox principal
        checkBox = new CheckBox();
        checkBox.setSelected(completed);
        checkBox.getStyleClass().add("habit-checkbox");

        // 2. Etiqueta con el nombre del hábito
        nameLabel = new Label(habit.nombre());
        nameLabel.getStyleClass().add("habit-title");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        // 3. Botones sutiles de acción (visibles en hover)
        partialButton = new Button("✎");
        partialButton.getStyleClass().add("btn-partial-progress");
        partialButton.setTooltip(new Tooltip("Registrar avance parcial cuantitativo"));
        partialButton.setOnAction(event -> {
            if (onPartialRequested != null) {
                onPartialRequested.accept(habit);
            }
        });

        editButton = new Button("✏");
        editButton.getStyleClass().add("btn-partial-progress");
        editButton.setTooltip(new Tooltip("Editar configuración del hábito"));
        editButton.setOnAction(event -> {
            if (onEditRequested != null) {
                onEditRequested.accept(habit);
            }
        });

        archiveButton = new Button("📥");
        archiveButton.getStyleClass().add("btn-partial-progress");
        archiveButton.setTooltip(new Tooltip("Archivar hábito"));
        archiveButton.setOnAction(event -> {
            if (onArchiveRequested != null) {
                onArchiveRequested.accept(habit);
            }
        });

        actionsBox = new HBox(4);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);
        actionsBox.getChildren().addAll(partialButton, editButton, archiveButton);
        actionsBox.setVisible(false);
        actionsBox.setManaged(true);

        // Control de visibilidad en hover
        hoverProperty().addListener((obs, oldVal, isHovered) -> {
            actionsBox.setVisible(isHovered);
            // El botón de avance parcial solo aplica si no está ya marcado como cumplido
            partialButton.setVisible(!checkBox.isSelected());
            partialButton.setManaged(!checkBox.isSelected());
        });

        // 4. Menú Contextual (clic derecho) para máxima accesibilidad
        ContextMenu contextMenu = new ContextMenu();

        MenuItem editMenuItem = new MenuItem("Editar hábito...");
        editMenuItem.setOnAction(e -> {
            if (onEditRequested != null) {
                onEditRequested.accept(habit);
            }
        });

        MenuItem partialMenuItem = new MenuItem("Registrar avance parcial...");
        partialMenuItem.setOnAction(e -> {
            if (onPartialRequested != null) {
                onPartialRequested.accept(habit);
            }
        });

        MenuItem archiveMenuItem = new MenuItem("Archivar hábito...");
        archiveMenuItem.setOnAction(e -> {
            if (onArchiveRequested != null) {
                onArchiveRequested.accept(habit);
            }
        });

        contextMenu.getItems().addAll(editMenuItem, partialMenuItem, new SeparatorMenuItem(), archiveMenuItem);
        setOnContextMenuRequested(event -> contextMenu.show(this, event.getScreenX(), event.getScreenY()));

        // Configuración de estilo según completado
        updateCompletedStyle(completed);

        checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            updateCompletedStyle(newVal);
            partialButton.setVisible(!newVal);
            partialButton.setManaged(!newVal);
            if (onStatusChanged != null) {
                onStatusChanged.accept(newVal);
            }
        });

        getChildren().addAll(checkBox, nameLabel, actionsBox);
    }

    private void updateCompletedStyle(boolean completed) {
        if (completed) {
            if (!nameLabel.getStyleClass().contains("habit-title-completed")) {
                nameLabel.getStyleClass().add("habit-title-completed");
            }
        } else {
            nameLabel.getStyleClass().remove("habit-title-completed");
        }
    }

    public Habit getHabit() {
        return habit;
    }

    public boolean isCompleted() {
        return checkBox.isSelected();
    }

    public void setCompleted(boolean completed) {
        checkBox.setSelected(completed);
    }
}
