package service;

import model.AppData;
import model.DailyRecord;
import model.Habit;
import model.StreakInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonHabitRepository;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StreakServiceTest {

    @TempDir
    Path tempDir;

    private JsonHabitRepository repository;
    private StreakService streakService;

    @BeforeEach
    void setUp() {
        Path testFile = tempDir.resolve("test_streaks.json");
        repository = new JsonHabitRepository(testFile);
        streakService = new StreakService(repository);
    }

    @Test
    @DisplayName("auditarYSellarDiasPasados() sella como Fallado días previos no abiertos si el hábito debía aparecer")
    void testAuditoriaYSelladoDiasPasados() {
        // Hábito que aparece Lunes, Miércoles y Viernes, creado el 2026-10-01
        Habit habit = new Habit(
                "h-audit",
                "Estudiar",
                "Leer material",
                "General",
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                2,
                false,
                LocalDate.of(2026, 10, 1)
        );

        // Supongamos que la última apertura fue el Lunes 2026-10-05 y hoy es Viernes 2026-10-09
        // El usuario solo cumplió el Lunes 2026-10-05
        DailyRecord recordLunes = DailyRecord.cumplido("h-audit", LocalDate.of(2026, 10, 5));
        AppData initialData = new AppData(
                List.of(habit),
                List.of(recordLunes),
                LocalDate.of(2026, 10, 5)
        );
        repository.save(initialData);

        // Ejecutar auditoría para el Viernes 2026-10-09
        LocalDate hoy = LocalDate.of(2026, 10, 9);
        streakService.auditarYSellarDiasPasados(hoy);

        AppData updatedData = repository.load();
        assertEquals(hoy, updatedData.lastCheckedDate());

        // El Miércoles 2026-10-07 el hábito debía aparecer pero no fue abierto -> debe estar sellado como Fallado
        List<DailyRecord> records = updatedData.dailyRecords();
        assertTrue(records.stream().anyMatch(r ->
                r.habitId().equals("h-audit") &&
                r.fecha().equals(LocalDate.of(2026, 10, 7)) &&
                !r.cumplido()
        ));

        // El Martes 2026-10-06 y Jueves 2026-10-08 NO debía aparecer -> NO debe haberse generado registro
        assertFalse(records.stream().anyMatch(r -> r.fecha().equals(LocalDate.of(2026, 10, 6))));
        assertFalse(records.stream().anyMatch(r -> r.fecha().equals(LocalDate.of(2026, 10, 8))));
    }

    @Test
    @DisplayName("calcularRacha() acumula semanas consecutivas que alcanzan el objetivo mínimo")
    void testRachaSemanasConsecutivasCumplidas() {
        // Hábito creado hace 2 semanas (Lunes 2026-09-28), objetivo semanal: 3
        Habit habit = new Habit(
                "h-streak",
                "Entrenamiento",
                "Rutina de fuerza",
                "Salud",
                Set.of(DayOfWeek.values()),
                3,
                false,
                LocalDate.of(2026, 9, 28)
        );

        // Semana 1 (2026-09-28 al 2026-10-04): 3 días cumplidos -> Cumplida
        DailyRecord s1d1 = DailyRecord.cumplido("h-streak", LocalDate.of(2026, 9, 28));
        DailyRecord s1d2 = DailyRecord.cumplido("h-streak", LocalDate.of(2026, 9, 30));
        DailyRecord s1d3 = DailyRecord.cumplido("h-streak", LocalDate.of(2026, 10, 2));

        // Semana 2 (2026-10-05 al 2026-10-11): 3 días cumplidos -> Cumplida
        DailyRecord s2d1 = DailyRecord.cumplido("h-streak", LocalDate.of(2026, 10, 5));
        DailyRecord s2d2 = DailyRecord.cumplido("h-streak", LocalDate.of(2026, 10, 6));
        DailyRecord s2d3 = DailyRecord.cumplido("h-streak", LocalDate.of(2026, 10, 7));

        AppData data = new AppData(
                List.of(habit),
                List.of(s1d1, s1d2, s1d3, s2d1, s2d2, s2d3),
                LocalDate.of(2026, 10, 11)
        );
        repository.save(data);

        // Evaluamos en la semana 3 (Lunes 2026-10-12)
        LocalDate fechaRef = LocalDate.of(2026, 10, 12);
        StreakInfo info = streakService.calcularRacha("h-streak", fechaRef);

        assertEquals(2, info.rachaActualSemanas());
        assertEquals(2, info.recordHistoricoSemanas());
        assertEquals(2, info.historialSemanas().size());
        assertEquals(List.of(true, true), info.historialSemanas());
        assertEquals(0, info.diasCumplidosSemanaActual());
    }

    @Test
    @DisplayName("calcularRacha() resetea la racha a cero si una semana cerrada no alcanzó el objetivo")
    void testRachaCaeACeroSiFallaUnaSemanaCerrada() {
        Habit habit = new Habit(
                "h-streak-reset",
                "Lectura",
                "Leer 30 min",
                "General",
                Set.of(DayOfWeek.values()),
                3,
                false,
                LocalDate.of(2026, 9, 28)
        );

        // Semana 1: Cumple 3 días -> Éxito
        DailyRecord s1d1 = DailyRecord.cumplido("h-streak-reset", LocalDate.of(2026, 9, 28));
        DailyRecord s1d2 = DailyRecord.cumplido("h-streak-reset", LocalDate.of(2026, 9, 29));
        DailyRecord s1d3 = DailyRecord.cumplido("h-streak-reset", LocalDate.of(2026, 9, 30));

        // Semana 2: Solo cumple 1 día -> Fallada (meta era 3)
        DailyRecord s2d1 = DailyRecord.cumplido("h-streak-reset", LocalDate.of(2026, 10, 5));

        AppData data = new AppData(
                List.of(habit),
                List.of(s1d1, s1d2, s1d3, s2d1),
                LocalDate.of(2026, 10, 11)
        );
        repository.save(data);

        // Evaluamos al comenzar la semana 3 (2026-10-12)
        StreakInfo info = streakService.calcularRacha("h-streak-reset", LocalDate.of(2026, 10, 12));

        assertEquals(0, info.rachaActualSemanas()); // Racha reiniciada a 0
        assertEquals(1, info.recordHistoricoSemanas()); // Conserva el pico histórico de 1 semana
        assertEquals(List.of(true, false), info.historialSemanas());
    }

    @Test
    @DisplayName("calcularRacha() contabiliza el avance en vivo de la semana en curso")
    void testProgresoSemanaEnCursoYSumatoriaEnTiempoReal() {
        // Hábito creado en la semana actual (Lunes 2026-10-12), objetivo: 2
        Habit habit = new Habit(
                "h-curso",
                "Agua",
                "Beber 2L",
                "Salud",
                Set.of(DayOfWeek.values()),
                2,
                false,
                LocalDate.of(2026, 10, 12)
        );

        // Martes 2026-10-13: 1 día cumplido
        DailyRecord rec1 = DailyRecord.cumplido("h-curso", LocalDate.of(2026, 10, 13));

        AppData data = new AppData(List.of(habit), List.of(rec1), LocalDate.of(2026, 10, 13));
        repository.save(data);

        LocalDate miercoles = LocalDate.of(2026, 10, 14);
        StreakInfo info1 = streakService.calcularRacha("h-curso", miercoles);

        assertEquals(1, info1.diasCumplidosSemanaActual());
        assertEquals(2, info1.objetivoSemanal());
        assertFalse(info1.metaSemanaActualAlcanzada());
        assertEquals(0.5, info1.porcentajeAvanceSemanaActual());
        assertEquals(0, info1.rachaActualSemanas());

        // Jueves 2026-10-15: cumple el segundo día (alcanza el objetivo de 2)
        DailyRecord rec2 = DailyRecord.cumplido("h-curso", LocalDate.of(2026, 10, 15));
        repository.save(data.withDailyRecords(List.of(rec1, rec2)));

        StreakInfo info2 = streakService.calcularRacha("h-curso", LocalDate.of(2026, 10, 15));
        assertEquals(2, info2.diasCumplidosSemanaActual());
        assertTrue(info2.metaSemanaActualAlcanzada());
        assertEquals(1.0, info2.porcentajeAvanceSemanaActual());
        // Al haber alcanzado ya la meta en la semana actual, se computa racha 1 en tiempo real
        assertEquals(1, info2.rachaActualSemanas());
        assertEquals(1, info2.recordHistoricoSemanas());
    }

    @Test
    @DisplayName("calcularRachasParaTodosLosHabitos() retorna el mapa de métricas para todos los hábitos activos")
    void testCalcularRachasParaTodosLosHabitos() {
        Habit h1 = new Habit("H1", "Desc", "Cat", Set.of(DayOfWeek.MONDAY), 1);
        Habit h2 = new Habit("H2", "Desc", "Cat", Set.of(DayOfWeek.TUESDAY), 1);
        Habit hArchivado = new Habit("H3", "Desc", "Cat", Set.of(DayOfWeek.WEDNESDAY), 1).withArchivado(true);

        AppData data = new AppData(List.of(h1, h2, hArchivado), List.of(), LocalDate.now());
        repository.save(data);

        Map<String, StreakInfo> resultado = streakService.calcularRachasParaTodosLosHabitos(LocalDate.now());

        assertEquals(2, resultado.size());
        assertTrue(resultado.containsKey(h1.id()));
        assertTrue(resultado.containsKey(h2.id()));
        assertFalse(resultado.containsKey(hArchivado.id()));
    }

    @Test
    @DisplayName("calcularRacha() lanza NoSuchElementException si el hábito no existe")
    void testCalcularRachaLanzaExcepcionSiNoExiste() {
        assertThrows(NoSuchElementException.class, () ->
                streakService.calcularRacha("id-fantasma", LocalDate.now())
        );
    }
}
