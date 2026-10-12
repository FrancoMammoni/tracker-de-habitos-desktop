package model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Representa el resumen de cumplimiento de hábitos para un día específico del mes.
 * Modela la función f(día) = cantidad de hábitos completados en esa jornada.
 */
public record DayCompletion(
        int dia,
        LocalDate fecha,
        int totalCumplidos,
        List<String> nombresHabitos
) {
    public DayCompletion {
        Objects.requireNonNull(fecha, "La fecha no puede ser nula");
        nombresHabitos = (nombresHabitos == null) ? List.of() : List.copyOf(nombresHabitos);
    }
}
