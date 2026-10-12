package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Contenedor raíz inmutable para el almacenamiento local JSON.
 * Gestiona el catálogo de hábitos, los registros diarios y la fecha de última auditoría.
 */
public record AppData(
        List<Habit> habits,
        List<DailyRecord> dailyRecords,
        LocalDate lastCheckedDate,
        String passwordHash,
        String passwordSalt
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
    }

    /**
     * Constructor secundario de compatibilidad con llamadas existentes de 3 argumentos.
     */
    public AppData(List<Habit> habits, List<DailyRecord> dailyRecords, LocalDate lastCheckedDate) {
        this(habits, dailyRecords, lastCheckedDate, null, null);
    }

    /**
     * Estado inicial limpio (Día 0) para cuando no existe el archivo de persistencia o tras un restablecimiento.
     */
    public static AppData empty() {
        return new AppData(List.of(), List.of(), LocalDate.now(), null, null);
    }

    public boolean hasPassword() {
        return passwordHash != null && passwordSalt != null;
    }

    public AppData withPassword(String newHash, String newSalt) {
        return new AppData(habits, dailyRecords, lastCheckedDate, newHash, newSalt);
    }

    public AppData withHabits(List<Habit> newHabits) {
        return new AppData(newHabits, dailyRecords, lastCheckedDate, passwordHash, passwordSalt);
    }

    public AppData withDailyRecords(List<DailyRecord> newRecords) {
        return new AppData(habits, newRecords, lastCheckedDate, passwordHash, passwordSalt);
    }

    public AppData withLastCheckedDate(LocalDate newDate) {
        return new AppData(habits, dailyRecords, newDate, passwordHash, passwordSalt);
    }

    /**
     * Agrega un nuevo hábito al catálogo preservando las credenciales.
     */
    public AppData withHabitAdded(Habit habit) {
        Objects.requireNonNull(habit, "El hábito no puede ser nulo");
        List<Habit> updated = new ArrayList<>(habits);
        updated.add(habit);
        return new AppData(updated, dailyRecords, lastCheckedDate, passwordHash, passwordSalt);
    }

    /**
     * Actualiza un hábito existente identificándolo por su ID preservando las credenciales.
     */
    public AppData withHabitUpdated(Habit updatedHabit) {
        Objects.requireNonNull(updatedHabit, "El hábito no puede ser nulo");
        List<Habit> updated = habits.stream()
                .map(h -> h.id().equals(updatedHabit.id()) ? updatedHabit : h)
                .toList();
        return new AppData(updated, dailyRecords, lastCheckedDate, passwordHash, passwordSalt);
    }

    /**
     * Elimina físicamente un hábito y todos sus registros históricos vinculados preservando las credenciales.
     */
    public AppData withHabitRemoved(String habitId) {
        Objects.requireNonNull(habitId, "El habitId no puede ser nulo");
        List<Habit> updatedHabits = habits.stream()
                .filter(h -> !h.id().equals(habitId))
                .toList();
        List<DailyRecord> updatedRecords = dailyRecords.stream()
                .filter(r -> !r.habitId().equals(habitId))
                .toList();
        return new AppData(updatedHabits, updatedRecords, lastCheckedDate, passwordHash, passwordSalt);
    }

    /**
     * Agrega o reemplaza el registro diario para un hábito y fecha determinados preservando las credenciales.
     */
    public AppData withRecordAdded(DailyRecord record) {
        Objects.requireNonNull(record, "El registro no puede ser nulo");
        List<DailyRecord> updated = dailyRecords.stream()
                .filter(r -> !(r.habitId().equals(record.habitId()) && r.fecha().equals(record.fecha())))
                .collect(Collectors.toCollection(ArrayList::new));
        updated.add(record);
        return new AppData(habits, updated, lastCheckedDate, passwordHash, passwordSalt);
    }
}
