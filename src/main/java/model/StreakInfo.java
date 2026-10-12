package model;

import java.util.List;
import java.util.Objects;

/**
 * Representa las métricas y el estado analítico de rachas para un hábito.
 * Medido estrictamente en semanas calendario consecutivas cumplidas.
 */
public record StreakInfo(
        String habitId,
        int rachaActualSemanas,
        int recordHistoricoSemanas,
        int diasCumplidosSemanaActual,
        int objetivoSemanal,
        List<Boolean> historialSemanas
) {
    public StreakInfo {
        Objects.requireNonNull(habitId, "El habitId no puede ser nulo");
        historialSemanas = (historialSemanas == null) ? List.of() : List.copyOf(historialSemanas);
    }

    /**
     * Determina si el objetivo semanal mínimo ya fue alcanzado en la semana en curso.
     */
    public boolean metaSemanaActualAlcanzada() {
        return diasCumplidosSemanaActual >= objetivoSemanal;
    }

    /**
     * Porcentaje de avance de la semana actual frente al objetivo mínimo (0.0 a 1.0 o superior).
     */
    public double porcentajeAvanceSemanaActual() {
        if (objetivoSemanal <= 0) return 1.0;
        return Math.min(1.0, (double) diasCumplidosSemanaActual / objetivoSemanal);
    }
}
