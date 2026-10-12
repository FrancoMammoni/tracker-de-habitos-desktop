package ui.hud;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Sección colapsable que agrupa los hábitos completados durante la jornada.
 * Inicia retraída por defecto para mantener el foco en los hábitos pendientes.
 */
public class CompletedSection extends TitledPane {

    private final VBox container;

    public CompletedSection() {
        container = new VBox(4);
        container.setPadding(new Insets(6, 0, 6, 0));

        setContent(container);
        setExpanded(false); // Colapsado por defecto según requerimiento de negocio
        setAnimated(true);
        getStyleClass().addAll(Styles.DENSE);

        updateTitle(0);
    }

    public void updateTitle(int count) {
        setText("Completados (" + count + ")");
    }

    public void clear() {
        container.getChildren().clear();
        updateTitle(0);
    }

    public void addCell(Node cell) {
        container.getChildren().add(cell);
        updateTitle(container.getChildren().size());
    }

    public void removeCell(Node cell) {
        container.getChildren().remove(cell);
        updateTitle(container.getChildren().size());
    }

    public void setCells(List<Node> cells) {
        container.getChildren().setAll(cells);
        updateTitle(cells.size());
    }

    public int getCompletedCount() {
        return container.getChildren().size();
    }
}
