package model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad inmutable que representa un Hábito y sus reglas de frecuencia y ciclo de vida.
 */
public record Habit(
        String id,
        String nombre,
        String descripcion,
        String categoria,
        Set<DayOfWeek> diasAparicion,
        int objetivoSemanal,
        boolean archivado,
        LocalDate createdAt
) {
    /**
     * Constructor compacto con validación de invariantes de negocio.
     */
    public Habit {
        Objects.requireNonNull(id, "El ID no puede ser nulo");
        if (id.isBlank()) {
            throw new IllegalArgumentException("El ID no puede estar vacío");
        }

        Objects.requireNonNull(nombre, "El nombre no puede ser nulo");
        if (nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del hábito es obligatorio");
        }

        descripcion = (descripcion == null) ? "" : descripcion.trim();
        categoria = (categoria == null || categoria.isBlank()) ? "General" : categoria.trim();

        Objects.requireNonNull(diasAparicion, "Los días de aparición no pueden ser nulos");
        if (diasAparicion.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un día de aparición");
        }
        diasAparicion = Collections.unmodifiableSet(Set.copyOf(diasAparicion));

        // Regla de negocio: 1 <= objetivo_semanal <= cantidad_dias_seleccionados
        if (objetivoSemanal < 1 || objetivoSemanal > diasAparicion.size()) {
            throw new IllegalArgumentException(
                    "El objetivo semanal debe estar entre 1 y la cantidad de días seleccionados (" + diasAparicion.size() + ")"
            );
        }

        Objects.requireNonNull(createdAt, "La fecha de creación no puede ser nula");
    }

    /**
     * Constructor de conveniencia para la creación inicial de un hábito activo.
     */
    public Habit(
            String nombre,
            String descripcion,
            String categoria,
            Set<DayOfWeek> diasAparicion,
            int objetivoSemanal
    ) {
        this(
                UUID.randomUUID().toString(),
                nombre,
                descripcion,
                categoria,
                diasAparicion,
                objetivoSemanal,
                false,
                LocalDate.now()
        );
    }

    /**
     * Determina si el hábito debe mostrarse en el HUD en un día de la semana específico.
     */
    public boolean apareceEn(DayOfWeek dia) {
        return !archivado && diasAparicion.contains(dia);
    }

    /**
     * Determina si el hábito debe mostrarse en el HUD para una fecha calendario.
     */
    public boolean apareceEn(LocalDate fecha) {
        return apareceEn(fecha.getDayOfWeek());
    }

    /**
     * Genera una nueva instancia con el estado de archivado modificado.
     */
    public Habit withArchivado(boolean nuevoArchivado) {
        return new Habit(
                id,
                nombre,
                descripcion,
                categoria,
                diasAparicion,
                objetivoSemanal,
                nuevoArchivado,
                createdAt
        );
    }
}
