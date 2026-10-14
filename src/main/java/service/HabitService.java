package service;

import model.AppData;
import model.DailyRecord;
import model.DayCompletion;
import model.Habit;
import repository.JsonHabitRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Servicio de negocio para la gestión del catálogo de hábitos y registros diarios.
 * Orquesta las transformaciones inmutables sobre AppData y su persistencia atómica.
 */
public class HabitService {

    private final JsonHabitRepository repository;

    public HabitService() {
        this(new JsonHabitRepository());
    }

    public HabitService(JsonHabitRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repositorio no puede ser nulo");
    }

    /**
     * Crea un nuevo hábito activo, validando invariantes y persistiendo atómicamente.
     */
    public synchronized Habit crearHabito(
            String nombre,
            String descripcion,
            String categoria,
            Set<DayOfWeek> diasAparicion,
            int objetivoSemanal
    ) {
        String catNormalizada = normalizarEtiqueta(categoria);
        Habit nuevo = new Habit(nombre, descripcion, catNormalizada, diasAparicion, objetivoSemanal);
        AppData data = repository.load();
        AppData actualizado = data.withHabitAdded(nuevo);
        repository.save(actualizado);
        return nuevo;
    }

    /**
     * Actualiza un hábito existente en el catálogo.
     */
    public synchronized Habit actualizarHabito(Habit habitActualizado) {
        Objects.requireNonNull(habitActualizado, "El hábito no puede ser nulo");
        AppData data = repository.load();
        
        boolean existe = data.habits().stream().anyMatch(h -> h.id().equals(habitActualizado.id()));
        if (!existe) {
            throw new NoSuchElementException("No existe el hábito con ID: " + habitActualizado.id());
        }

        AppData actualizado = data.withHabitUpdated(habitActualizado);
        repository.save(actualizado);
        return habitActualizado;
    }

    /**
     * Cambia el estado del hábito a archivado.
     * Sale del HUD diario pero conserva todo su historial para reportes.
     */
    public synchronized void archivarHabito(String habitId) {
        Objects.requireNonNull(habitId, "El habitId no puede ser nulo");
        Habit habit = findByIdOrThrow(habitId);
        if (!habit.archivado()) {
            Habit archivado = habit.withArchivado(true);
            actualizarHabito(archivado);
        }
    }

    /**
     * Restaura un hábito archivado al estado activo para que reaparezca en el HUD.
     */
    public synchronized void restaurarHabito(String habitId) {
        Objects.requireNonNull(habitId, "El habitId no puede ser nulo");
        Habit habit = findByIdOrThrow(habitId);
        if (habit.archivado()) {
            Habit restaurado = habit.withArchivado(false);
            actualizarHabito(restaurado);
        }
    }

    /**
     * Eliminación física definitiva del hábito y todo su historial de registros diarios vinculados.
     */
    public synchronized void eliminarHabitoDefinitivamente(String habitId) {
        Objects.requireNonNull(habitId, "El habitId no puede ser nulo");
        AppData data = repository.load();
        
        boolean existe = data.habits().stream().anyMatch(h -> h.id().equals(habitId));
        if (!existe) {
            throw new NoSuchElementException("No existe el hábito con ID: " + habitId);
        }

        AppData actualizado = data.withHabitRemoved(habitId);
        repository.save(actualizado);
    }

    /**
     * Obtiene todos los hábitos actualmente activos (no archivados).
     */
    public synchronized List<Habit> getHabitosActivos() {
        return repository.load().habits().stream()
                .filter(h -> !h.archivado())
                .toList();
    }

    /**
     * Obtiene todos los hábitos archivados.
     */
    public synchronized List<Habit> getHabitosArchivados() {
        return repository.load().habits().stream()
                .filter(Habit::archivado)
                .toList();
    }

    /**
     * Retorna los hábitos activos que deben mostrarse en el HUD para una fecha específica.
     */
    public synchronized List<Habit> getHabitosParaFecha(LocalDate fecha) {
        Objects.requireNonNull(fecha, "La fecha no puede ser nula");
        return repository.load().habits().stream()
                .filter(h -> h.apareceEn(fecha))
                .toList();
    }

    /**
     * Busca un hábito por su identificador único.
     */
    public synchronized Optional<Habit> findById(String habitId) {
        Objects.requireNonNull(habitId, "El habitId no puede ser nulo");
        return repository.load().habits().stream()
                .filter(h -> h.id().equals(habitId))
                .findFirst();
    }

    /**
     * Registra un hábito como cumplido para una fecha dada (sin valor numérico).
     */
    public synchronized void registrarCumplido(String habitId, LocalDate fecha) {
        validarExistencia(habitId);
        DailyRecord record = DailyRecord.cumplido(habitId, fecha);
        guardarRegistro(record);
    }

    /**
     * Registra un hábito como cumplido para una fecha dada especificando la cantidad lograda.
     */
    public synchronized void registrarCumplido(String habitId, LocalDate fecha, double valor) {
        validarExistencia(habitId);
        DailyRecord record = DailyRecord.cumplido(habitId, fecha, valor);
        guardarRegistro(record);
    }

    /**
     * Registra un avance parcial para el hábito.
     * Según la regla de negocio, computa como Fallado (cumplido = false) pero preserva el valor.
     */
    public synchronized void registrarAvanceParcial(String habitId, LocalDate fecha, double valorAlcanzado) {
        validarExistencia(habitId);
        DailyRecord record = DailyRecord.parcial(habitId, fecha, valorAlcanzado);
        guardarRegistro(record);
    }

    /**
     * Elimina el registro del día (utilizado cuando el usuario desmarca un hábito en el HUD).
     */
    public synchronized void eliminarRegistro(String habitId, LocalDate fecha) {
        Objects.requireNonNull(habitId, "El habitId no puede ser nulo");
        Objects.requireNonNull(fecha, "La fecha no puede ser nula");

        AppData data = repository.load();
        List<DailyRecord> filtrados = data.dailyRecords().stream()
                .filter(r -> !(r.habitId().equals(habitId) && r.fecha().equals(fecha)))
                .toList();

        repository.save(data.withDailyRecords(filtrados));
    }

    /**
     * Retorna el registro diario para un hábito y fecha específicos, si existe.
     */
    public synchronized Optional<DailyRecord> getRegistroDiario(String habitId, LocalDate fecha) {
        Objects.requireNonNull(habitId, "El habitId no puede ser nulo");
        Objects.requireNonNull(fecha, "La fecha no puede ser nula");

        return repository.load().dailyRecords().stream()
                .filter(r -> r.habitId().equals(habitId) && r.fecha().equals(fecha))
                .findFirst();
    }

    /**
     * Determina si el hábito ya fue marcado como cumplido en el día en curso.
     */
    public synchronized boolean isHabitoCumplidoHoy(String habitId) {
        return getRegistroDiario(habitId, LocalDate.now())
                .map(DailyRecord::cumplido)
                .orElse(false);
    }

    /**
     * Retorna todos los registros históricos de un hábito.
     */
    public synchronized List<DailyRecord> getHistorialHabito(String habitId) {
        Objects.requireNonNull(habitId, "El habitId no puede ser nulo");
        return repository.load().dailyRecords().stream()
                .filter(r -> r.habitId().equals(habitId))
                .toList();
    }

    /**
     * Calcula la función de cumplimiento f(día) = cantidad de hábitos completados
     * para cada día del mes especificado.
     */
    public synchronized Map<Integer, DayCompletion> getCumplimientosMes(YearMonth mes) {
        Objects.requireNonNull(mes, "El mes no puede ser nulo");
        AppData data = repository.load();

        Map<String, Habit> mapaHabitos = new HashMap<>();
        for (Habit h : data.habits()) {
            mapaHabitos.put(h.id(), h);
        }

        Map<Integer, DayCompletion> resultado = new LinkedHashMap<>();
        int diasEnMes = mes.lengthOfMonth();

        for (int d = 1; d <= diasEnMes; d++) {
            LocalDate fecha = mes.atDay(d);
            List<String> nombresCumplidos = new ArrayList<>();

            for (DailyRecord r : data.dailyRecords()) {
                if (r.fecha().equals(fecha) && r.cumplido()) {
                    Habit h = mapaHabitos.get(r.habitId());
                    if (h != null) {
                        nombresCumplidos.add(h.nombre());
                    }
                }
            }

            resultado.put(d, new DayCompletion(d, fecha, nombresCumplidos.size(), nombresCumplidos));
        }

        return Collections.unmodifiableMap(resultado);
    }

    /**
     * Retorna el catálogo completo de etiquetas (categorías) ordenadas alfabéticamente.
     */
    public synchronized List<String> obtenerEtiquetas() {
        AppData data = repository.load();
        return data.tags();
    }

    /**
     * Busca y filtra etiquetas que contengan la consulta (ignorando mayúsculas y minúsculas).
     */
    public synchronized List<String> buscarEtiquetas(String query) {
        if (query == null || query.isBlank()) {
            return obtenerEtiquetas();
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        return obtenerEtiquetas().stream()
                .filter(tag -> tag.toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    /**
     * Registra explícitamente una nueva etiqueta en el catálogo.
     * Retorna la etiqueta normalizada.
     */
    public synchronized String registrarEtiqueta(String etiqueta) {
        if (etiqueta == null || etiqueta.isBlank()) {
            throw new IllegalArgumentException("El nombre de la etiqueta no puede estar vacío");
        }
        String normalizada = normalizarEtiqueta(etiqueta);
        AppData data = repository.load();
        AppData actualizado = data.withTagAdded(normalizada);
        repository.save(actualizado);
        return normalizada;
    }

    /**
     * Normaliza una etiqueta eliminando espacios en los extremos y asegurando mayúscula inicial.
     */
    public static String normalizarEtiqueta(String tag) {
        if (tag == null) return "General";
        String trimmed = tag.trim();
        if (trimmed.isEmpty()) return "General";
        if (trimmed.length() == 1) return trimmed.toUpperCase(Locale.ROOT);
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    private Habit findByIdOrThrow(String habitId) {
        return findById(habitId)
                .orElseThrow(() -> new NoSuchElementException("No existe el hábito con ID: " + habitId));
    }

    private void validarExistencia(String habitId) {
        if (findById(habitId).isEmpty()) {
            throw new NoSuchElementException("No existe el hábito con ID: " + habitId);
        }
    }

    private void guardarRegistro(DailyRecord record) {
        AppData data = repository.load();
        AppData actualizado = data.withRecordAdded(record);
        repository.save(actualizado);
    }
}
