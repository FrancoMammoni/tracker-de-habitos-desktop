package model;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contenedor raíz inmutable para el almacenamiento local JSON.
 * Gestiona el catálogo de hábitos, los registros diarios, la fecha de última auditoría,
 * credenciales de seguridad y el registro centralizado de etiquetas (categorías).
 */
public record AppData(
        List<Habit> habits,
        List<DailyRecord> dailyRecords,
        LocalDate lastCheckedDate,
        String passwordHash,
        String passwordSalt,
        List<String> tags
) {
    /**
     * Constructor compacto con normalización y protección contra nulos.
     */
    public AppData {
        habits = (habits == null) ? List.of() : List.copyOf(habits);
        dailyRecords = (dailyRecords == null) ? List.of() : List.copyOf(dailyRecords);
        lastCheckedDate = (lastCheckedDate == null) ? LocalDate.now() : lastCheckedDate;
        passwordHash = (passwordHash == null || passwordHash.isBlank()) ? null : passwordHash;
        passwordSalt = (passwordSalt == null || passwordSalt.isBlank()) ? null : passwordSalt;

        if (tags == null) {
            Set<String> extracted = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (Habit h : habits) {
                if (h.categoria() != null && !h.categoria().isBlank()) {
                    extracted.add(h.categoria().trim());
                }
            }
            tags = List.copyOf(extracted);
        } else {
            Set<String> normalized = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (String t : tags) {
                if (t != null && !t.isBlank()) {
                    normalized.add(t.trim());
                }
            }
            tags = List.copyOf(normalized);
        }
    }

    /**
     * Constructor secundario de compatibilidad con llamadas existentes de 3 argumentos.
     */
    public AppData(List<Habit> habits, List<DailyRecord> dailyRecords, LocalDate lastCheckedDate) {
        this(habits, dailyRecords, lastCheckedDate, null, null, null);
    }

    /**
     * Constructor secundario de compatibilidad con llamadas existentes de 5 argumentos.
     */
    public AppData(List<Habit> habits, List<DailyRecord> dailyRecords, LocalDate lastCheckedDate, String passwordHash, String passwordSalt) {
        this(habits, dailyRecords, lastCheckedDate, passwordHash, passwordSalt, null);
    }

    /**
     * Estado inicial limpio (Día 0) para cuando no existe el archivo de persistencia o tras un restablecimiento.
     */
    public static AppData empty() {
        return new AppData(List.of(), List.of(), LocalDate.now(), null, null, List.of());
    }

    public boolean hasPassword() {
        return passwordHash != null && passwordSalt != null;
    }

    public AppData withPassword(String newHash, String newSalt) {
        return new AppData(habits, dailyRecords, lastCheckedDate, newHash, newSalt, tags);
    }

    public AppData withHabits(List<Habit> newHabits) {
        return new AppData(newHabits, dailyRecords, lastCheckedDate, passwordHash, passwordSalt, tags);
    }

    public AppData withDailyRecords(List<DailyRecord> newRecords) {
        return new AppData(habits, newRecords, lastCheckedDate, passwordHash, passwordSalt, tags);
    }

    public AppData withLastCheckedDate(LocalDate newDate) {
        return new AppData(habits, dailyRecords, newDate, passwordHash, passwordSalt, tags);
    }

    public AppData withTags(List<String> newTags) {
        return new AppData(habits, dailyRecords, lastCheckedDate, passwordHash, passwordSalt, newTags);
    }

    /**
     * Registra una nueva etiqueta en el catálogo si aún no existe (normalizada sin distinción de mayúsculas).
     */
    public AppData withTagAdded(String newTag) {
        if (newTag == null || newTag.isBlank()) {
            return this;
        }
        Set<String> updated = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        updated.addAll(tags);
        updated.add(newTag.trim());
        return new AppData(habits, dailyRecords, lastCheckedDate, passwordHash, passwordSalt, List.copyOf(updated));
    }

    /**
     * Agrega un nuevo hábito al catálogo e incorpora automáticamente su etiqueta si no existe.
     */
    public AppData withHabitAdded(Habit habit) {
        Objects.requireNonNull(habit, "El hábito no puede ser nulo");
        List<Habit> updated = new ArrayList<>(habits);
        updated.add(habit);

        List<String> updatedTags = tags;
        if (habit.categoria() != null && !habit.categoria().isBlank()) {
            Set<String> tagSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            tagSet.addAll(tags);
            tagSet.add(habit.categoria().trim());
            updatedTags = List.copyOf(tagSet);
        }

        return new AppData(updated, dailyRecords, lastCheckedDate, passwordHash, passwordSalt, updatedTags);
    }

    /**
     * Actualiza un hábito existente identificándolo por su ID e incorpora su etiqueta si es nueva.
     */
    public AppData withHabitUpdated(Habit updatedHabit) {
        Objects.requireNonNull(updatedHabit, "El hábito no puede ser nulo");
        List<Habit> updated = habits.stream()
                .map(h -> h.id().equals(updatedHabit.id()) ? updatedHabit : h)
                .toList();

        List<String> updatedTags = tags;
        if (updatedHabit.categoria() != null && !updatedHabit.categoria().isBlank()) {
            Set<String> tagSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            tagSet.addAll(tags);
            tagSet.add(updatedHabit.categoria().trim());
            updatedTags = List.copyOf(tagSet);
        }

        return new AppData(updated, dailyRecords, lastCheckedDate, passwordHash, passwordSalt, updatedTags);
    }

    /**
     * Elimina físicamente un hábito y todos sus registros históricos vinculados preservando credenciales y etiquetas.
     */
    public AppData withHabitRemoved(String habitId) {
        Objects.requireNonNull(habitId, "El habitId no puede ser nulo");
        List<Habit> updatedHabits = habits.stream()
                .filter(h -> !h.id().equals(habitId))
                .toList();
        List<DailyRecord> updatedRecords = dailyRecords.stream()
                .filter(r -> !r.habitId().equals(habitId))
                .toList();
        return new AppData(updatedHabits, updatedRecords, lastCheckedDate, passwordHash, passwordSalt, tags);
    }

    /**
     * Agrega o reemplaza el registro diario para un hábito y fecha determinados preservando credenciales y etiquetas.
     */
    public AppData withRecordAdded(DailyRecord record) {
        Objects.requireNonNull(record, "El registro no puede ser nulo");
        List<DailyRecord> updated = dailyRecords.stream()
                .filter(r -> !(r.habitId().equals(record.habitId()) && r.fecha().equals(record.fecha())))
                .collect(Collectors.toCollection(ArrayList::new));
        updated.add(record);
        return new AppData(habits, updated, lastCheckedDate, passwordHash, passwordSalt, tags);
    }
}
