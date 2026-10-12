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

        this.listContainer = new VBox(8);
        this.listContainer.setPadding(new Insets(4));

        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(320);
        scrollPane.setMaxHeight(360);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        setBody(scrollPane);

        Button btnClose = new Button("Cerrar");
        btnClose.setStyle(
                "-fx-background-color: #505168; -fx-text-fill: #E0FBFC; " +
                "-fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 8 18;"
        );
        btnClose.setOnAction(e -> onClose.run());

        setActions(btnClose);

        refreshList();
    }

    private void refreshList() {
        listContainer.getChildren().clear();
        List<Habit> archivados = habitService.getHabitosArchivados();

        if (archivados.isEmpty()) {
            Label emptyLbl = new Label("No tienes hábitos archivados actualmente.");
            emptyLbl.setStyle("-fx-text-fill: #8d8fa8; -fx-font-size: 13px; -fx-padding: 24;");
            listContainer.getChildren().add(emptyLbl);
            return;
        }

        for (Habit habit : archivados) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 14, 10, 14));
            row.setStyle(
                    "-fx-background-color: #16201e; -fx-background-radius: 8px; " +
                    "-fx-border-color: #505168; -fx-border-radius: 8px; -fx-border-width: 1px;"
            );

            VBox infoBox = new VBox(2);
            Label nameLbl = new Label(habit.nombre());
            nameLbl.setStyle("-fx-text-fill: #E0FBFC; -fx-font-weight: bold; -fx-font-size: 14px;");

            String cat = (habit.categoria() != null && !habit.categoria().isBlank()) ? habit.categoria() : "General";
            Label metaLbl = new Label("Categoría: " + cat + " • Meta semanal: " + habit.objetivoSemanal() + " días");
            metaLbl.setStyle("-fx-text-fill: #8d8fa8; -fx-font-size: 11px;");

            infoBox.getChildren().addAll(nameLbl, metaLbl);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnRestore = new Button("Restaurar");
            btnRestore.setStyle(
                    "-fx-background-color: #048A81; -fx-text-fill: #E0FBFC; " +
                    "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4px; -fx-padding: 6 12;"
            );
            btnRestore.setOnAction(e -> {
                habitService.restaurarHabito(habit.id());
                onDataChanged.run();
                refreshList();
            });

            Button btnDelete = new Button("Eliminar");
            btnDelete.setStyle(
                    "-fx-background-color: #A50104; -fx-text-fill: #FFFFFF; " +
                    "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4px; -fx-padding: 6 12;"
            );
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
                            showSubModal.accept(this); // Volver a mostrar este modal
                            refreshList();
                        },
                        () -> showSubModal.accept(this)
                );
                showSubModal.accept(confirmModal);
            });

            row.getChildren().addAll(infoBox, spacer, btnRestore, btnDelete);
            listContainer.getChildren().add(row);
        }
    }
}
