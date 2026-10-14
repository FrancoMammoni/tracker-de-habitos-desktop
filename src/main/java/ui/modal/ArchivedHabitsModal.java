package ui.modal;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.Habit;
import service.HabitService;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Modal in-app superpuesto para la administración de hábitos archivados.
 * Permite restaurar hábitos al HUD o eliminarlos definitivamente con su historial.
 */
public class ArchivedHabitsModal extends ModalCard {

    private final HabitService habitService;
    private final Runnable onDataChanged;
    private final Consumer<Node> showSubModal;
    private final Runnable closeModal;
    private final VBox listContainer;

    public ArchivedHabitsModal(
            HabitService habitService,
            Runnable onDataChanged,
            Consumer<Node> showSubModal,
            Runnable onClose
    ) {
        super("Hábitos Archivados", "Gestión de hábitos fuera del HUD activo", onClose, 600);

        this.habitService = Objects.requireNonNull(habitService);
        this.onDataChanged = Objects.requireNonNull(onDataChanged);
        this.showSubModal = Objects.requireNonNull(showSubModal);
        this.closeModal = Objects.requireNonNull(onClose);

        this.listContainer = new VBox(8);
        this.listContainer.setPadding(new Insets(4));

        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(320);
        scrollPane.setMaxHeight(360);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        setBody(scrollPane);

        Button btnClose = new Button("Cerrar");
        btnClose.getStyleClass().add("modal-btn-cancel");
        btnClose.setOnAction(e -> onClose.run());

        setActions(btnClose);

        refreshList();
    }

    private void refreshList() {
        listContainer.getChildren().clear();
        List<Habit> archivados = habitService.getHabitosArchivados();

        if (archivados.isEmpty()) {
            Label emptyLbl = new Label("No tienes hábitos archivados actualmente.");
            emptyLbl.getStyleClass().add("modal-subtitle");
            emptyLbl.setStyle("-fx-padding: 24;");
            listContainer.getChildren().add(emptyLbl);
            return;
        }

        for (Habit habit : archivados) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 14, 10, 14));
            row.getStyleClass().add("archived-item-card");

            VBox infoBox = new VBox(2);
            Label nameLbl = new Label(habit.nombre());
            nameLbl.getStyleClass().add("modal-label");

            String cat = (habit.categoria() != null && !habit.categoria().isBlank()) ? habit.categoria() : "General";
            Label metaLbl = new Label("Categoría: " + cat + " • Meta semanal: " + habit.objetivoSemanal() + " días");
            metaLbl.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 11px;");

            infoBox.getChildren().addAll(nameLbl, metaLbl);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnRestore = new Button("Restaurar");
            btnRestore.getStyleClass().add("btn-primary");
            btnRestore.setOnAction(e -> {
                habitService.restaurarHabito(habit.id());
                onDataChanged.run();
                refreshList();
            });

            Button btnDelete = new Button("Eliminar");
            btnDelete.getStyleClass().add("btn-inferno");
            btnDelete.setOnAction(e -> {
                ConfirmModal confirmModal = new ConfirmModal(
                        "Eliminar Hábito Definitivamente",
                        "⚠️ Esta acción es IRREVERSIBLE.\n\n" +
                        "Se eliminará \"" + habit.nombre() + "\" y todos sus registros diarios históricos en cascada.\n\n" +
                        "¿Deseas continuar?",
                        "Eliminar Definitivamente",
                        true,
                        () -> {
                            habitService.eliminarHabitoDefinitivamente(habit.id());
                            onDataChanged.run();
                            closeModal.run();
                            refreshList();
                        },
                        closeModal
                );
                showSubModal.accept(confirmModal);
            });

            row.getChildren().addAll(infoBox, spacer, btnRestore, btnDelete);
            listContainer.getChildren().add(row);
        }
    }
}
