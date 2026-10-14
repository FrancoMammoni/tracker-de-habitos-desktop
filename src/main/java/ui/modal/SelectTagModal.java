package ui.modal;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import service.HabitService;

import java.util.List;
import java.util.function.Consumer;

/**
 * Modal in-app interactivo para buscar, seleccionar y registrar etiquetas de hábitos.
 */
public class SelectTagModal extends ModalCard {

    private final TextField searchField = new TextField();
    private final FlowPane tagsFlowPane = new FlowPane(8, 8);
    private final VBox createActionBox = new VBox();
    private final Label emptyLabel = new Label("No hay etiquetas que coincidan con la búsqueda.");
    private final HabitService habitService;
    private final Consumer<String> onTagSelected;
    private final String selectedTag;

    public SelectTagModal(
            HabitService habitService,
            String etiquetaActual,
            Consumer<String> onTagSelected,
            Runnable onCancel
    ) {
        super("Etiquetas del Hábito", "Selecciona una etiqueta existente o busca/crea una nueva", onCancel, 460);
        this.habitService = habitService;
        this.selectedTag = (etiquetaActual == null || etiquetaActual.isBlank()) ? "General" : etiquetaActual.trim();
        this.onTagSelected = onTagSelected;

        searchField.setPromptText("Buscar o escribir nueva etiqueta (ej. Salud)...");
        searchField.getStyleClass().add("tag-search-field");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> renderTags(newVal));
        searchField.setOnAction(e -> handleEnterAction());

        tagsFlowPane.setPadding(new Insets(6, 6, 6, 6));
        tagsFlowPane.setAlignment(Pos.CENTER_LEFT);

        emptyLabel.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-style: italic; -fx-padding: 8px;");
        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        VBox flowContainer = new VBox(tagsFlowPane, emptyLabel);

        ScrollPane scrollPane = new ScrollPane(flowContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(150);
        scrollPane.setMaxHeight(180);
        scrollPane.getStyleClass().add("tag-scroll-pane");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        createActionBox.setAlignment(Pos.CENTER_LEFT);

        VBox contentBox = new VBox(12, searchField, scrollPane, createActionBox);
        contentBox.setFillWidth(true);

        setBody(contentBox);

        Button btnCancel = new Button("Volver");
        btnCancel.getStyleClass().add("modal-btn-cancel");
        btnCancel.setOnAction(e -> onCancel.run());

        HBox footer = new HBox(btnCancel);
        footer.setAlignment(Pos.CENTER_RIGHT);
        contentBox.getChildren().add(footer);

        renderTags("");
    }

    private void renderTags(String query) {
        tagsFlowPane.getChildren().clear();
        createActionBox.getChildren().clear();

        List<String> tags = habitService.buscarEtiquetas(query);

        emptyLabel.setVisible(tags.isEmpty());
        emptyLabel.setManaged(tags.isEmpty());

        for (String tag : tags) {
            Button chip = new Button(tag);
            chip.getStyleClass().add("tag-chip-button");
            if (tag.equalsIgnoreCase(selectedTag)) {
                chip.getStyleClass().add("tag-chip-selected");
            }
            chip.setOnAction(e -> chooseTag(tag));
            tagsFlowPane.getChildren().add(chip);
        }

        String trimmed = (query == null) ? "" : query.trim();
        boolean exactMatch = tags.stream().anyMatch(t -> t.equalsIgnoreCase(trimmed));

        if (!trimmed.isEmpty() && !exactMatch) {
            String candidate = HabitService.normalizarEtiqueta(trimmed);
            Button btnCreate = new Button("+ Crear etiqueta \"" + candidate + "\"");
            btnCreate.getStyleClass().add("tag-create-button");
            btnCreate.setOnAction(e -> {
                String creada = habitService.registrarEtiqueta(candidate);
                chooseTag(creada);
            });
            createActionBox.getChildren().add(btnCreate);
        }
    }

    private void handleEnterAction() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        List<String> matches = habitService.buscarEtiquetas(text);
        if (!matches.isEmpty() && matches.get(0).equalsIgnoreCase(text)) {
            chooseTag(matches.get(0));
        } else {
            String creada = habitService.registrarEtiqueta(text);
            chooseTag(creada);
        }
    }

    private void chooseTag(String tag) {
        if (onTagSelected != null) {
            onTagSelected.accept(tag);
        }
    }
}
