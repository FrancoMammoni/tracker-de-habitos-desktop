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
import model.DailyRecord;
import model.Habit;
import model.StreakInfo;
import service.HabitService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Objects;

/**
 * Componente visual que exhibe la línea de tiempo de la semana actual (Lunes a Domingo),
 * el récord histórico alcanzado y la racha activa de semanas consecutivas.
 */
public class StreakCanvas extends VBox {

    private final Label currentStreakNumberLabel;
    private final Label maxStreakNumberLabel;
    private final HBox blocksContainer;

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
        Label timelineTitle = new Label("Línea de tiempo de la semana:");
        timelineTitle.getStyleClass().add("streak-timeline-title");

        // 3. Contenedor de bloques semanales
        blocksContainer = new HBox(6);
        blocksContainer.setAlignment(Pos.CENTER_LEFT);
        blocksContainer.setPadding(new Insets(4, 2, 6, 2));

        ScrollPane blocksScroll = new ScrollPane(blocksContainer);
        blocksScroll.setFitToHeight(true);
        blocksScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        blocksScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        blocksScroll.getStyleClass().add("streak-blocks-scroll");
        HBox.setHgrow(blocksScroll, Priority.ALWAYS);

        getChildren().addAll(statsRow, timelineTitle, blocksScroll);
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
     * Actualiza las estadísticas de racha y renderiza los 7 días de la semana actual (Lunes a Domingo).
     */
    public void update(StreakInfo info, Habit habit, HabitService habitService, LocalDate hoy) {
        Objects.requireNonNull(info, "StreakInfo no puede ser nulo");
        Objects.requireNonNull(habit, "Habit no puede ser nulo");
        Objects.requireNonNull(habitService, "HabitService no puede ser nulo");
        LocalDate fechaRef = (hoy != null) ? hoy : LocalDate.now();

        // 1. Actualizar números
        String rachaActualText = info.rachaActualSemanas() == 1 ? "1 semana" : info.rachaActualSemanas() + " semanas";
        String recordText = info.recordHistoricoSemanas() == 1 ? "1 semana" : info.recordHistoricoSemanas() + " semanas";

        currentStreakNumberLabel.setText(rachaActualText);
        maxStreakNumberLabel.setText(recordText);

        // 2. Renderizar los 7 días de la semana en curso (Lunes a Domingo)
        blocksContainer.getChildren().clear();
        LocalDate lunesSemana = fechaRef.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        String[] etiquetasDias = { "L", "M", "X", "J", "V", "S", "D" };
        DateTimeFormatter tooltipDateFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.of("es", "ES"));

        for (int i = 0; i < 7; i++) {
            LocalDate dia = lunesSemana.plusDays(i);
            boolean esHoy = dia.isEqual(fechaRef);
            boolean esFuturo = dia.isAfter(fechaRef);
            boolean cumplido = habitService.getRegistroDiario(habit.id(), dia)
                    .map(DailyRecord::cumplido)
                    .orElse(false);

            StackPane block = new StackPane();
            block.getStyleClass().add("streak-block");

            String diaFormateado = dia.format(tooltipDateFormatter);
            diaFormateado = Character.toUpperCase(diaFormateado.charAt(0)) + diaFormateado.substring(1);

            if (esHoy) {
                block.getStyleClass().add("streak-block-current");
                if (cumplido) {
                    block.getStyleClass().add("streak-block-current-reached");
                    Tooltip.install(block, new Tooltip(diaFormateado + " (Hoy): ¡Hábito cumplido!"));
                } else {
                    Tooltip.install(block, new Tooltip(diaFormateado + " (Hoy): En curso (Pendiente)"));
                }

                Label currentLabel = new Label("Hoy");
                currentLabel.getStyleClass().add("streak-block-current-label");
                block.getChildren().add(currentLabel);
            } else if (esFuturo) {
                block.getStyleClass().add("streak-block-future");
                Tooltip.install(block, new Tooltip(diaFormateado + ": Próximo"));

                Label dayLabel = new Label(etiquetasDias[i]);
                dayLabel.getStyleClass().add("streak-block-num");
                block.getChildren().add(dayLabel);
            } else {
                // Día pasado de la semana
                boolean asignado = habit.apareceEn(dia);
                if (cumplido) {
                    block.getStyleClass().add("streak-block-success");
                    Tooltip.install(block, new Tooltip(diaFormateado + ": Cumplido"));
                } else if (!asignado) {
                    block.getStyleClass().add("streak-block-unassigned");
                    Tooltip.install(block, new Tooltip(diaFormateado + ": Día no asignado"));
                } else {
                    block.getStyleClass().add("streak-block-failed");
                    Tooltip.install(block, new Tooltip(diaFormateado + ": No cumplido"));
                }

                Label dayLabel = new Label(etiquetasDias[i]);
                dayLabel.getStyleClass().add("streak-block-num");
                block.getChildren().add(dayLabel);
            }

            blocksContainer.getChildren().add(block);
        }
    }
}
