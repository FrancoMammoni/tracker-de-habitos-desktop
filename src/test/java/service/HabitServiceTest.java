package service;

import model.DailyRecord;
import model.DayCompletion;
import model.Habit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonHabitRepository;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HabitServiceTest {

    @TempDir
    Path tempDir;

    private HabitService habitService;

    @BeforeEach
    void setUp() {
        Path testFile = tempDir.resolve("test_habits_service.json");
        JsonHabitRepository repo = new JsonHabitRepository(testFile);
        habitService = new HabitService(repo);
    }

    @Test
    @DisplayName("crearHabito() valida y persiste el hábito correctamente")
    void testCrearHabitoExitoso() {
        Set<DayOfWeek> dias = Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        Habit creado = habitService.crearHabito(
                "Entrenamiento",
                "Sesión de fuerza",
                "Salud",
                dias,
                2
        );

        assertNotNull(creado.id());
        assertEquals("Entrenamiento", creado.nombre());
        assertEquals(2, creado.objetivoSemanal());
        assertFalse(creado.archivado());

        List<Habit> activos = habitService.getHabitosActivos();
        assertEquals(1, activos.size());
        assertEquals(creado.id(), activos.get(0).id());
    }

    @Test
    @DisplayName("crearHabito() lanza excepción si viola el invariante de objetivo semanal")
    void testCrearHabitoFallaPorInvarianteFrecuencia() {
        Set<DayOfWeek> dias = Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

        // 3 > 2 (cantidad de días seleccionados)
        assertThrows(IllegalArgumentException.class, () ->
                habitService.crearHabito("Invalido", "Desc", "Cat", dias, 3)
        );

        // objetivo < 1
        assertThrows(IllegalArgumentException.class, () ->
                habitService.crearHabito("Invalido", "Desc", "Cat", dias, 0)
        );
    }

    @Test
    @DisplayName("archivarHabito() saca el hábito del HUD pero conserva histórico, y restaurarHabito() lo regresa")
    void testArchivarYRestaurarHabito() {
        Set<DayOfWeek> dias = Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY);
        Habit habit = habitService.crearHabito("Lectura", "Desc", "Estudio", dias, 1);

        habitService.registrarCumplido(habit.id(), LocalDate.now());

        // Archivar
        habitService.archivarHabito(habit.id());

        assertTrue(habitService.getHabitosActivos().isEmpty());
        assertEquals(1, habitService.getHabitosArchivados().size());
        assertEquals(habit.id(), habitService.getHabitosArchivados().get(0).id());

        // El historial diario sigue intacto
        List<DailyRecord> historial = habitService.getHistorialHabito(habit.id());
        assertEquals(1, historial.size());
        assertTrue(historial.get(0).cumplido());

        // Restaurar
        habitService.restaurarHabito(habit.id());
        assertEquals(1, habitService.getHabitosActivos().size());
        assertTrue(habitService.getHabitosArchivados().isEmpty());
    }

    @Test
    @DisplayName("eliminarHabitoDefinitivamente() borra físicamente el hábito y todos sus registros en cascada")
    void testEliminacionFisicaEnCascada() {
        Habit habit = habitService.crearHabito("Correr", "5km", "Cardio", Set.of(DayOfWeek.SATURDAY), 1);
        habitService.registrarCumplido(habit.id(), LocalDate.of(2026, 10, 3));
        habitService.registrarAvanceParcial(habit.id(), LocalDate.of(2026, 10, 10), 3.0);

        assertEquals(2, habitService.getHistorialHabito(habit.id()).size());

        // Eliminación definitiva
        habitService.eliminarHabitoDefinitivamente(habit.id());

        assertTrue(habitService.findById(habit.id()).isEmpty());
        assertTrue(habitService.getHabitosActivos().isEmpty());
        assertTrue(habitService.getHabitosArchivados().isEmpty());
        assertTrue(habitService.getHistorialHabito(habit.id()).isEmpty());
    }

    @Test
    @DisplayName("getHabitosParaFecha() filtra únicamente los activos programados para ese día de la semana")
    void testGetHabitosParaFecha() {
        // Lunes y Miércoles
        habitService.crearHabito("Habito Lun-Mie", "", "General", Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), 1);
        // Sólo Viernes
        habitService.crearHabito("Habito Vie", "", "General", Set.of(DayOfWeek.FRIDAY), 1);

        // 2026-10-12 es Lunes
        LocalDate lunes = LocalDate.of(2026, 10, 12);
        assertEquals(DayOfWeek.MONDAY, lunes.getDayOfWeek());

        List<Habit> paraLunes = habitService.getHabitosParaFecha(lunes);
        assertEquals(1, paraLunes.size());
        assertEquals("Habito Lun-Mie", paraLunes.get(0).nombre());

        // 2026-10-16 es Viernes
        LocalDate viernes = LocalDate.of(2026, 10, 16);
        assertEquals(DayOfWeek.FRIDAY, viernes.getDayOfWeek());

        List<Habit> paraViernes = habitService.getHabitosParaFecha(viernes);
        assertEquals(1, paraViernes.size());
        assertEquals("Habito Vie", paraViernes.get(0).nombre());

        // 2026-10-18 es Domingo (ninguno programado)
        LocalDate domingo = LocalDate.of(2026, 10, 18);
        assertTrue(habitService.getHabitosParaFecha(domingo).isEmpty());
    }

    @Test
    @DisplayName("registrarAvanceParcial() almacena el valor numérico y define cumplido = false")
    void testRegistrarAvanceParcial() {
        Habit habit = habitService.crearHabito("Agua", "2.5L", "Salud", Set.of(DayOfWeek.values()), 5);
        LocalDate hoy = LocalDate.now();

        habitService.registrarAvanceParcial(habit.id(), hoy, 1.8);

        Optional<DailyRecord> recordOpt = habitService.getRegistroDiario(habit.id(), hoy);
        assertTrue(recordOpt.isPresent());

        DailyRecord record = recordOpt.get();
        assertFalse(record.cumplido()); // Regla de negocio: estado formal computable = Fallado
        assertEquals(1.8, record.valorAlcanzado());
        assertFalse(habitService.isHabitoCumplidoHoy(habit.id()));
    }

    @Test
    @DisplayName("eliminarRegistro() desmarca un hábito correctamente")
    void testEliminarRegistro() {
        Habit habit = habitService.crearHabito("Meditación", "", "Bienestar", Set.of(DayOfWeek.values()), 3);
        LocalDate hoy = LocalDate.now();

        habitService.registrarCumplido(habit.id(), hoy);
        assertTrue(habitService.isHabitoCumplidoHoy(habit.id()));

        habitService.eliminarRegistro(habit.id(), hoy);
        assertFalse(habitService.isHabitoCumplidoHoy(habit.id()));
        assertTrue(habitService.getRegistroDiario(habit.id(), hoy).isEmpty());
    }

    @Test
    @DisplayName("Operaciones con IDs inexistentes lanzan NoSuchElementException")
    void testOperacionesConIdInexistenteLanzanExcepcion() {
        assertThrows(NoSuchElementException.class, () ->
                habitService.archivarHabito("no-existe")
        );
        assertThrows(NoSuchElementException.class, () ->
                habitService.eliminarHabitoDefinitivamente("no-existe")
        );
        assertThrows(NoSuchElementException.class, () ->
                habitService.registrarCumplido("no-existe", LocalDate.now())
        );
    }

    @Test
    @DisplayName("getCumplimientosMes() calcula f(dia) = cantidad de habitos cumplidos")
    void testGetCumplimientosMes() {
        Habit h1 = habitService.crearHabito("Leer", "", "General", Set.of(DayOfWeek.values()), 4);
        Habit h2 = habitService.crearHabito("Agua", "", "General", Set.of(DayOfWeek.values()), 4);

        YearMonth oct2026 = YearMonth.of(2026, 10);
        LocalDate dia5 = oct2026.atDay(5);
        LocalDate dia12 = oct2026.atDay(12);

        habitService.registrarCumplido(h1.id(), dia5);
        habitService.registrarCumplido(h2.id(), dia5);
        habitService.registrarCumplido(h1.id(), dia12);

        Map<Integer, DayCompletion> mesData = habitService.getCumplimientosMes(oct2026);
        assertEquals(31, mesData.size());

        assertEquals(2, mesData.get(5).totalCumplidos());
        assertTrue(mesData.get(5).nombresHabitos().contains("Leer"));
        assertTrue(mesData.get(5).nombresHabitos().contains("Agua"));

        assertEquals(1, mesData.get(12).totalCumplidos());
        assertTrue(mesData.get(12).nombresHabitos().contains("Leer"));

        assertEquals(0, mesData.get(1).totalCumplidos());
        assertTrue(mesData.get(1).nombresHabitos().isEmpty());
    }

    @Test
    @DisplayName("Gestión de etiquetas: registro automático, búsqueda por prefijo y normalización")
    void testGestionEtiquetas() {
        habitService.crearHabito("Correr", "", "Salud", Set.of(DayOfWeek.MONDAY), 1);
        habitService.crearHabito("Leer", "", "Estudio", Set.of(DayOfWeek.MONDAY), 1);

        List<String> tags = habitService.obtenerEtiquetas();
        assertTrue(tags.contains("Salud"));
        assertTrue(tags.contains("Estudio"));

        // Registro explícito con normalización
        String nueva = habitService.registrarEtiqueta("  finanzas personales  ");
        assertEquals("Finanzas personales", nueva);
        assertTrue(habitService.obtenerEtiquetas().contains("Finanzas personales"));

        // Búsqueda insensible a mayúsculas
        List<String> resultadosSa = habitService.buscarEtiquetas("sa");
        assertEquals(1, resultadosSa.size());
        assertEquals("Salud", resultadosSa.get(0));

        List<String> resultadosE = habitService.buscarEtiquetas("E");
        assertTrue(resultadosE.contains("Estudio"));
    }
}
