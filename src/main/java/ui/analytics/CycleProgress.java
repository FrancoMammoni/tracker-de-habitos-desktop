package ui.analytics;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.StreakInfo;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Componente visual que exhibe el avance del ciclo semanal en curso (Lunes a Domingo).
 * Muestra días completados frente al objetivo semanal mínimo y evalúa si la meta
 * está cumplida, en camino o en riesgo de quiebre.
 */
public class CycleProgress extends VBox {

    private final Label progressTextLabel;
    private final Label badgeStatusLabel;
    private final ProgressBar progressBar;

    public CycleProgress() {
        super(6);
        getStyleClass().add("cycle-progress-container");

        // Fila de encabezado: texto de conteo y badge de estado
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        progressTextLabel = new Label("Progreso semana en curso: 0 / 0 días");
        progressTextLabel.getStyleClass().add("cycle-progress-text");
        HBox.setHgrow(progressTextLabel, Priority.ALWAYS);

        badgeStatusLabel = new Label("En progreso");
        badgeStatusLabel.getStyleClass().add("badge-info-pill");

        headerRow.getChildren().addAll(progressTextLabel, badgeStatusLabel);

        // Barra de progreso interactiva
        progressBar = new ProgressBar(0.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("cycle-progress-bar");

        getChildren().addAll(headerRow, progressBar);
    }

    /**
     * Actualiza la visualización de avance según la información analítica y la fecha actual.
     */
    public void update(StreakInfo info, LocalDate hoy) {
        Objects.requireNonNull(info, "StreakInfo no puede ser nulo");
        LocalDate fecha = (hoy != null) ? hoy : LocalDate.now();

        int cumplidos = info.diasCumplidosSemanaActual();
        int objetivo = info.objetivoSemanal();
        double progreso = info.porcentajeAvanceSemanaActual();

        progressTextLabel.setText(String.format("Semana en curso: %d de %d días completados", cumplidos, objetivo));
        progressBar.setProgress(progreso);

        // Limpiar clases de estado previas
        badgeStatusLabel.getStyleClass().removeAll("badge-success-pill", "badge-warning-pill", "badge-info-pill");
        progressBar.getStyleClass().removeAll(Styles.SUCCESS, Styles.DANGER);

        if (info.metaSemanaActualAlcanzada()) {
            badgeStatusLabel.setText("¡Meta Cumplida!");
            badgeStatusLabel.getStyleClass().add("badge-success-pill");
            progressBar.getStyleClass().add(Styles.SUCCESS);
        } else {
            // Calcular días restantes de la semana (ISO-8601: Lunes = 1, Domingo = 7)
            int diasRestantesEnSemana = 7 - fecha.getDayOfWeek().getValue();
            int faltantes = objetivo - cumplidos;

            if (faltantes > diasRestantesEnSemana) {
                // Matemáticamente no es posible alcanzar la meta en los días que restan
                badgeStatusLabel.setText("Meta en riesgo");
                badgeStatusLabel.getStyleClass().add("badge-warning-pill");
                progressBar.getStyleClass().add(Styles.DANGER);
            } else {
                badgeStatusLabel.setText(String.format("Faltan %d", faltantes));
                badgeStatusLabel.getStyleClass().add("badge-info-pill");
            }
        }
    }
}
