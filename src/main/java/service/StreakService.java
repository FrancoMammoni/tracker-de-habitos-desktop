package service;

import model.AppData;
import model.DailyRecord;
import model.Habit;
import model.StreakInfo;
import repository.JsonHabitRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Servicio de negocio para la auditoría anti-trampa y el cálculo de rachas semanales.
 * Las rachas se miden estrictamente en semanas calendario consecutivas (Lunes a Domingo).
 */
public class StreakService {

    private final JsonHabitRepository repository;

    public StreakService() {
        this(new JsonHabitRepository());
    }

    public StreakService(JsonHabitRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repositorio no puede ser nulo");
    }

    /**
     * Regla anti-trampa: Audita y sella automáticamente como Fallado cualquier día anterior
     * que no haya sido registrado por el usuario si el hábito debía aparecer en ese día.
     * Actualiza lastCheckedDate en AppData a la fecha de referencia.
     */
    public synchronized void auditarYSellarDiasPasados(LocalDate fechaReferencia) {
        Objects.requireNonNull(fechaReferencia, "La fecha de referencia no puede ser nula");
        AppData data = repository.load();
        LocalDate lastChecked = data.lastCheckedDate();

        // Si ya está al día o la fecha de referencia es anterior, solo asegura la fecha
        if (!lastChecked.isBefore(fechaReferencia)) {
            if (!lastChecked.equals(fechaReferencia)) {
                repository.save(data.withLastCheckedDate(fechaReferencia));
            }
            return;
        }

        // Auditar el rango [lastChecked, fechaReferencia - 1 dia]
        LocalDate finAuditoria = fechaReferencia.minusDays(1);
        List<DailyRecord> nuevosRegistros = new ArrayList<>(data.dailyRecords());
        Set<String> registrosExistentes = new HashSet<>();
        for (DailyRecord r : data.dailyRecords()) {
            registrosExistentes.add(r.habitId() + "#" + r.fecha());
        }

        boolean huboCambios = false;
        LocalDate cursor = lastChecked;
        while (!cursor.isAfter(finAuditoria)) {
            for (Habit habit : data.habits()) {
                // Solo hábitos no archivados y que hayan sido creados en o antes de esa fecha
                if (!habit.archivado() && !cursor.isBefore(habit.createdAt()) && habit.apareceEn(cursor)) {
                    String clave = habit.id() + "#" + cursor;
                    if (!registrosExistentes.contains(clave)) {
                        nuevosRegistros.add(DailyRecord.fallado(habit.id(), cursor));
                        registrosExistentes.add(clave);
                        huboCambios = true;
                    }
                }
            }
            cursor = cursor.plusDays(1);
        }

        AppData actualizado = data.withDailyRecords(nuevosRegistros).withLastCheckedDate(fechaReferencia);
        repository.save(actualizado);
    }

    /**
     * Calcula las métricas y la racha semanal completa para un hábito.
     * Evalúa las semanas cerradas (Lunes a Domingo) desde la creación del hábito
     * hasta la semana de la fecha de referencia.
     */
    public synchronized StreakInfo calcularRacha(String habitId, LocalDate fechaReferencia) {
        Objects.requireNonNull(habitId, "El habitId no puede ser nulo");
        Objects.requireNonNull(fechaReferencia, "La fecha de referencia no puede ser nula");

        AppData data = repository.load();
        Habit habit = data.habits().stream()
                .filter(h -> h.id().equals(habitId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No existe el hábito con ID: " + habitId));

        // Obtener el Lunes de la semana actual de referencia
        LocalDate lunesSemanaActual = fechaReferencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // Filtrar registros cumplidos del hábito
        Map<LocalDate, Boolean> registrosCumplidos = new HashMap<>();
        for (DailyRecord r : data.dailyRecords()) {
            if (r.habitId().equals(habitId) && r.cumplido()) {
                registrosCumplidos.put(r.fecha(), true);
            }
        }

        // 1. Calcular el progreso de la semana en curso (Lunes a Domingo)
        int diasCumplidosSemanaActual = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate dia = lunesSemanaActual.plusDays(i);
            if (registrosCumplidos.getOrDefault(dia, false)) {
                diasCumplidosSemanaActual++;
            }
        }

        // 2. Determinar la primera semana calendario a evaluar (desde createdAt)
        LocalDate lunesInicio = habit.createdAt().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<Boolean> historialSemanas = new ArrayList<>();
        int rachaConsecutiva = 0;
        int maxRacha = 0;

        // Evaluar todas las semanas cerradas (anteriores a la semana actual)
        LocalDate lunesCursor = lunesInicio;
        while (lunesCursor.isBefore(lunesSemanaActual)) {
            int cumplidosEnSemana = 0;
            for (int i = 0; i < 7; i++) {
                LocalDate diaSemana = lunesCursor.plusDays(i);
                if (registrosCumplidos.getOrDefault(diaSemana, false)) {
                    cumplidosEnSemana++;
                }
            }

            boolean semanaCumplida = cumplidosEnSemana >= habit.objetivoSemanal();
            historialSemanas.add(semanaCumplida);

            if (semanaCumplida) {
                rachaConsecutiva++;
                if (rachaConsecutiva > maxRacha) {
                    maxRacha = rachaConsecutiva;
                }
            } else {
                rachaConsecutiva = 0; // Regla: al fallar la semana, la racha activa cae a 0
            }

            lunesCursor = lunesCursor.plusWeeks(1);
        }

        // Si en la semana actual ya cumplió la meta mínima, suma a la racha activa en tiempo real
        int rachaEfectiva = rachaConsecutiva;
        if (diasCumplidosSemanaActual >= habit.objetivoSemanal()) {
            rachaEfectiva++;
            if (rachaEfectiva > maxRacha) {
                maxRacha = rachaEfectiva;
            }
        }

        return new StreakInfo(
                habitId,
                rachaEfectiva,
                maxRacha,
                diasCumplidosSemanaActual,
                habit.objetivoSemanal(),
                historialSemanas
        );
    }

    /**
     * Calcula las métricas de racha para todos los hábitos activos.
     */
    public synchronized Map<String, StreakInfo> calcularRachasParaTodosLosHabitos(LocalDate fechaReferencia) {
        AppData data = repository.load();
        Map<String, StreakInfo> mapa = new LinkedHashMap<>();
        for (Habit h : data.habits()) {
            if (!h.archivado()) {
                mapa.put(h.id(), calcularRacha(h.id(), fechaReferencia));
            }
        }
        return mapa;
    }
}
