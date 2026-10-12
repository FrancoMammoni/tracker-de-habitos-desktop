package ui.hud;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.Habit;
import service.HabitService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Diálogo modal para la gestión de hábitos archivados.
 * Permite restaurar un hábito al HUD activo o borrarlo definitivamente con todo su historial.
 */
public class ArchivedHabitsDialog extends Dialog<Void> {

    private final HabitService habitService;
    private final Runnable onDataChanged;
    private final VBox listContainer = new VBox(8);

    public ArchivedHabitsDialog(HabitService habitService, Runnable onDataChanged) {
        this.habitService = Objects.requireNonNull(habitService);
        this.onDataChanged = onDataChanged;

        setTitle("Hábitos Archivados");
        setHeaderText("Gestión de hábitos archivados fuera del HUD diario");

        DialogPane dialogPane = getDialogPane();
        dialogPane.getStyleClass().add(Styles.DENSE);
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setMinWidth(480);
        content.setMinHeight(300);

        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add(Styles.DENSE);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        content.getChildren().add(scroll);
        dialogPane.setContent(content);

        refreshList();
    }

    private void refreshList() {
        listContainer.getChildren().clear();
        List<Habit> archivados = habitService.getHabitosArchivados();

        if (archivados.isEmpty()) {
            Label vacioLbl = new Label("No tienes hábitos archivados.");
            vacioLbl.getStyleClass().addAll(Styles.TEXT_MUTED);
            listContainer.getChildren().add(vacioLbl);
            return;
        }

        for (Habit habit : archivados) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.getStyleClass().addAll(Styles.DENSE);
            row.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 8px;");

            VBox infoBox = new VBox(2);
            Label nombreLbl = new Label(habit.nombre());
            nombreLbl.getStyleClass().addAll(Styles.TEXT_BOLD);

            Label metaLbl = new Label(habit.categoria() + " • " + habit.objetivoSemanal() + " días/semana");
            metaLbl.getStyleClass().addAll(Styles.TEXT_CAPTION, Styles.TEXT_MUTED);
            infoBox.getChildren().addAll(nombreLbl, metaLbl);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            // Botón Restaurar (Dark Cyan)
            Button restaurarBtn = new Button("Restaurar");
            restaurarBtn.getStyleClass().addAll(Styles.SUCCESS, Styles.SMALL);
            restaurarBtn.setOnAction(e -> {
                habitService.restaurarHabito(habit.id());
                refreshList();
                if (onDataChanged != null) {
                    onDataChanged.run();
                }
            });

            // Botón Borrar Definitivamente (Inferno)
            Button borrarBtn = new Button("Eliminar");
            borrarBtn.getStyleClass().addAll("btn-inferno", Styles.SMALL);
            borrarBtn.setOnAction(e -> confirmarYEliminar(habit));

            row.getChildren().addAll(infoBox, restaurarBtn, borrarBtn);
            listContainer.getChildren().add(row);
        }
    }

    private void confirmarYEliminar(Habit habit) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación Definitiva");
        alert.setHeaderText("¿Eliminar \"" + habit.nombre() + "\" permanentemente?");
        alert.setContentText(
                "Esta acción destruirá el hábito y todo su historial de registros diarios.\n" +
                "Esta operación no se puede deshacer."
        );

        Button okBtn = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.getStyleClass().add("btn-inferno");
        okBtn.setText("Sí, eliminar definitivamente");

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            habitService.eliminarHabitoDefinitivamente(habit.id());
            refreshList();
            if (onDataChanged != null) {
                onDataChanged.run();
            }
        }
    }
}
