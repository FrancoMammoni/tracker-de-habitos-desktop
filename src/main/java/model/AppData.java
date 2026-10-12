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
        LocalDate lastCheckedDate
) {
    /**
     * Constructor compacto con normalización y protección contra nulos.
     */
    public AppData {
        habits = (habits == null) ? List.of() : List.copyOf(habits);
        dailyRecords = (dailyRecords == null) ? List.of() : List.copyOf(dailyRecords);
        lastCheckedDate = (lastCheckedDate == null) ? LocalDate.now() : lastCheckedDate;
    }

    /**
     * Estado inicial limpio (Día 0) para cuando no existe el archivo de persistencia.
     */
    public static AppData empty() {
        return new AppData(List.of(), List.of(), LocalDate.now());
    }

    public AppData withHabits(List<Habit> newHabits) {
        return new AppData(newHabits, dailyRecords, lastCheckedDate);
    }

    public AppData withDailyRecords(List<DailyRecord> newRecords) {
        return new AppData(habits, newRecords, lastCheckedDate);
    }

    public AppData withLastCheckedDate(LocalDate newDate) {
        return new AppData(habits, dailyRecords, newDate);
    }

    /**
     * Agrega un nuevo hábito al catálogo.
     */
    public AppData withHabitAdded(Habit habit) {
        Objects.requireNonNull(habit, "El hábito no puede ser nulo");
        List<Habit> updated = new ArrayList<>(habits);
        updated.add(habit);
        return new AppData(updated, dailyRecords, lastCheckedDate);
    }

    /**
     * Actualiza un hábito existente identificándolo por su ID.
     */
    public AppData withHabitUpdated(Habit updatedHabit) {
        Objects.requireNonNull(updatedHabit, "El hábito no puede ser nulo");
        List<Habit> updated = habits.stream()
                .map(h -> h.id().equals(updatedHabit.id()) ? updatedHabit : h)
                .toList();
        return new AppData(updated, dailyRecords, lastCheckedDate);
    }

    /**
     * Elimina físicamente un hábito y todos sus registros históricos vinculados.
     */
    public AppData withHabitRemoved(String habitId) {
        Objects.requireNonNull(habitId, "El habitId no puede ser nulo");
        List<Habit> updatedHabits = habits.stream()
                .filter(h -> !h.id().equals(habitId))
                .toList();
        List<DailyRecord> updatedRecords = dailyRecords.stream()
                .filter(r -> !r.habitId().equals(habitId))
                .toList();
        return new AppData(updatedHabits, updatedRecords, lastCheckedDate);
    }

    /**
     * Agrega o reemplaza el registro diario para un hábito y fecha determinados.
     */
    public AppData withRecordAdded(DailyRecord record) {
        Objects.requireNonNull(record, "El registro no puede ser nulo");
        List<DailyRecord> updated = dailyRecords.stream()
                .filter(r -> !(r.habitId().equals(record.habitId()) && r.fecha().equals(record.fecha())))
                .collect(Collectors.toCollection(ArrayList::new));
        updated.add(record);
        return new AppData(habits, updated, lastCheckedDate);
    }
}
