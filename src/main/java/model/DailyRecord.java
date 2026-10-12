package model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Registro diario inmutable de un hábito.
 * Cumple con la regla de evaluación binaria estricta (cumplido / fallado)
 * y almacenamiento de progreso numérico parcial opcional.
 */
public record DailyRecord(
        String habitId,
        LocalDate fecha,
        boolean cumplido,
        Double valorAlcanzado
) {
    /**
     * Constructor compacto con validación de invariantes de negocio.
     */
    public DailyRecord {
        Objects.requireNonNull(habitId, "El habitId no puede ser nulo");
        if (habitId.isBlank()) {
            throw new IllegalArgumentException("El habitId no puede estar vacío");
        }
        Objects.requireNonNull(fecha, "La fecha no puede ser nula");

        if (valorAlcanzado != null && valorAlcanzado < 0) {
            throw new IllegalArgumentException("El valor alcanzado no puede ser negativo");
        }
    }

    /**
     * Crea un registro marcado como cumplido sin valor numérico.
     */
    public static DailyRecord cumplido(String habitId, LocalDate fecha) {
        return new DailyRecord(habitId, fecha, true, null);
    }

    /**
     * Crea un registro marcado como cumplido con valor cuantitativo alcanzado.
     */
    public static DailyRecord cumplido(String habitId, LocalDate fecha, double valor) {
        return new DailyRecord(habitId, fecha, true, valor);
    }

    /**
     * Crea un registro fallado sin valor numérico (utilizado para sellado automático).
     */
    public static DailyRecord fallado(String habitId, LocalDate fecha) {
        return new DailyRecord(habitId, fecha, false, null);
    }

    /**
     * Crea un registro de avance parcial.
     * Según la regla de negocio, el estado computado es Fallado (cumplido = false),
     * pero se preserva el valor alcanzado como evidencia de alcance.
     */
    public static DailyRecord parcial(String habitId, LocalDate fecha, double valorAlcanzado) {
        return new DailyRecord(habitId, fecha, false, valorAlcanzado);
    }
}
