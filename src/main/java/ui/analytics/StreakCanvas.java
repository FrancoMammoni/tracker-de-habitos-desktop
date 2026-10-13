package ui.analytics;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.StreakInfo;

import java.util.List;
import java.util.Objects;

/**
 * Componente visual que exhibe la línea de tiempo de bloques semanales consecutivos,
 * el récord histórico alcanzado y la racha activa de semanas.
 * Cada bloque representa una semana calendario ISO-8601 evaluada.
 */
public class StreakCanvas extends VBox {

    private final Label currentStreakNumberLabel;
    private final Label maxStreakNumberLabel;
    private final HBox blocksContainer;
    private final Label emptyHistoryLabel;

    public StreakCanvas() {
        super(10);
        getStyleClass().add("streak-canvas-container");

        // 1. Fila de métricas destacadas (Racha Actual vs Récord Histórico)
        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        VBox currentStreakBox = buildStatBox("RACHA ACTUAL", "0 sem");
        currentStreakNumberLabel = (Label) currentStreakBox.getChildren().get(1);
        currentStreakNumberLabel.getStyleClass().add("streak-stat-number-accent");

        VBox maxStreakBox = buildStatBox("RÉCORD HISTÓRICO", "0 sem");
        maxStreakNumberLabel = (Label) maxStreakBox.getChildren().get(1);
        maxStreakNumberLabel.getStyleClass().add("streak-stat-number-record");

        statsRow.getChildren().addAll(currentStreakBox, maxStreakBox);

        // 2. Título de la línea de tiempo
        Label timelineTitle = new Label("Línea de tiempo de semanas:");
        timelineTitle.getStyleClass().add("streak-timeline-title");

        // 3. Contenedor de bloques semanales
        blocksContainer = new HBox(6);
        blocksContainer.setAlignment(Pos.CENTER_LEFT);
        blocksContainer.setPadding(new Insets(4, 2, 6, 2));

        emptyHistoryLabel = new Label("Primera semana de registro. ¡Completa tu meta para inaugurar la racha!");
        emptyHistoryLabel.getStyleClass().add("streak-empty-history");
        emptyHistoryLabel.setVisible(false);
        emptyHistoryLabel.setManaged(false);

        ScrollPane blocksScroll = new ScrollPane(blocksContainer);
        blocksScroll.setFitToHeight(true);
        blocksScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        blocksScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        blocksScroll.getStyleClass().add("streak-blocks-scroll");
        HBox.setHgrow(blocksScroll, Priority.ALWAYS);

        getChildren().addAll(statsRow, timelineTitle, emptyHistoryLabel, blocksScroll);
    }

    private VBox buildStatBox(String title, String initialValue) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("streak-stat-box");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("streak-stat-label");

        Label valueLabel = new Label(initialValue);
        valueLabel.getStyleClass().add("streak-stat-number");

        box.getChildren().addAll(titleLabel, valueLabel);
        return box;
    }

    /**
     * Actualiza las estadísticas y renderiza los bloques de semanas según StreakInfo.
     */
    public void update(StreakInfo info) {
        Objects.requireNonNull(info, "StreakInfo no puede ser nulo");

        // 1. Actualizar números
        String rachaActualText = info.rachaActualSemanas() == 1 ? "1 semana" : info.rachaActualSemanas() + " semanas";
        String recordText = info.recordHistoricoSemanas() == 1 ? "1 semana" : info.recordHistoricoSemanas() + " semanas";

        currentStreakNumberLabel.setText(rachaActualText);
        maxStreakNumberLabel.setText(recordText);

        // 2. Renderizar bloques semanales
        blocksContainer.getChildren().clear();
        List<Boolean> historial = info.historialSemanas();

        if (historial.isEmpty()) {
            emptyHistoryLabel.setVisible(true);
            emptyHistoryLabel.setManaged(true);
        } else {
            emptyHistoryLabel.setVisible(false);
            emptyHistoryLabel.setManaged(false);

            for (int i = 0; i < historial.size(); i++) {
                boolean cumplida = historial.get(i);
                int semanaIndex = i + 1;

                StackPane block = new StackPane();
                block.getStyleClass().add("streak-block");

                if (cumplida) {
                    block.getStyleClass().add("streak-block-success");
                    Tooltip.install(block, new Tooltip(String.format("Semana %d: Objetivo cumplido", semanaIndex)));
                } else {
                    block.getStyleClass().add("streak-block-failed");
                    Tooltip.install(block, new Tooltip(String.format("Semana %d: Objetivo no alcanzado (Racha a 0)", semanaIndex)));
                }

                Label numLabel = new Label(String.valueOf(semanaIndex));
                numLabel.getStyleClass().add("streak-block-num");
                block.getChildren().add(numLabel);

                blocksContainer.getChildren().add(block);
            }
        }

        // 3. Añadir bloque distintivo para la semana en curso (Lunes a Domingo actual)
        StackPane currentWeekBlock = new StackPane();
        currentWeekBlock.getStyleClass().addAll("streak-block", "streak-block-current");

        if (info.metaSemanaActualAlcanzada()) {
            currentWeekBlock.getStyleClass().add("streak-block-current-reached");
            Tooltip.install(currentWeekBlock, new Tooltip("Semana actual: ¡Meta mínima alcanzada!"));
        } else {
            Tooltip.install(currentWeekBlock, new Tooltip("Semana actual: En curso"));
        }

        Label currentLabel = new Label("Hoy");
        currentLabel.getStyleClass().add("streak-block-current-label");
        currentWeekBlock.getChildren().add(currentLabel);

        blocksContainer.getChildren().add(currentWeekBlock);
    }
}
