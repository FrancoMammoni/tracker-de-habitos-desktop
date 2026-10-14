package repository;

import model.AppData;
import model.DailyRecord;
import model.Habit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JsonHabitRepositoryTest {

    @TempDir
    Path tempDir;

    private Path testFilePath;
    private JsonHabitRepository repository;

    @BeforeEach
    void setUp() {
        testFilePath = tempDir.resolve("test_habits.json");
        repository = new JsonHabitRepository(testFilePath);
    }

    @Test
    @DisplayName("load() debe retornar AppData.empty() si el archivo no existe")
    void testLoadWhenFileDoesNotExistReturnsEmptyAppData() {
        assertFalse(Files.exists(testFilePath));

        AppData data = repository.load();

        assertNotNull(data);
        assertTrue(data.habits().isEmpty());
        assertTrue(data.dailyRecords().isEmpty());
        assertEquals(LocalDate.now(), data.lastCheckedDate());
    }

    @Test
    @DisplayName("load() debe retornar AppData.empty() si el archivo existe pero está vacío")
    void testLoadWhenFileIsEmptyReturnsEmptyAppData() throws IOException {
        Files.createFile(testFilePath);
        assertTrue(Files.exists(testFilePath));
        assertEquals(0, Files.size(testFilePath));

        AppData data = repository.load();

        assertNotNull(data);
        assertTrue(data.habits().isEmpty());
        assertTrue(data.dailyRecords().isEmpty());
    }

    @Test
    @DisplayName("save() y load() deben preservar con fidelidad hábitos y registros con avance parcial")
    void testSaveAndLoadRoundtrip() {
        Habit habit1 = new Habit(
                "h-1",
                "Lectura Diaria",
                "Leer 30 páginas",
                "Estudio",
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                2,
                false,
                LocalDate.of(2026, 9, 1)
        );

        Habit habit2 = new Habit(
                "h-2",
                "Ejercicio",
                "Rutina de fuerza",
                "Salud",
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                3,
                true,
                LocalDate.of(2026, 9, 5)
        );

        DailyRecord recCumplido = DailyRecord.cumplido("h-1", LocalDate.of(2026, 9, 29));
        DailyRecord recParcial = DailyRecord.parcial("h-1", LocalDate.of(2026, 9, 30), 18.5);

        AppData original = new AppData(
                List.of(habit1, habit2),
                List.of(recCumplido, recParcial),
                LocalDate.of(2026, 9, 30)
        );

        repository.save(original);

        assertTrue(Files.exists(testFilePath));

        AppData loaded = repository.load();

        assertNotNull(loaded);
        assertEquals(2, loaded.habits().size());
        assertEquals(2, loaded.dailyRecords().size());
        assertEquals(LocalDate.of(2026, 9, 30), loaded.lastCheckedDate());

        // Verificar datos del primer hábito
        Habit loadedHabit1 = loaded.habits().get(0);
        assertEquals("h-1", loadedHabit1.id());
        assertEquals("Lectura Diaria", loadedHabit1.nombre());
        assertEquals("Leer 30 páginas", loadedHabit1.descripcion());
        assertEquals("Estudio", loadedHabit1.categoria());
        assertEquals(3, loadedHabit1.diasAparicion().size());
        assertTrue(loadedHabit1.diasAparicion().contains(DayOfWeek.MONDAY));
        assertEquals(2, loadedHabit1.objetivoSemanal());
        assertFalse(loadedHabit1.archivado());

        // Verificar datos del registro parcial (regla de negocio: cumplido = false, valor conservado)
        DailyRecord loadedParcial = loaded.dailyRecords().stream()
                .filter(r -> r.habitId().equals("h-1") && r.fecha().equals(LocalDate.of(2026, 9, 30)))
                .findFirst()
                .orElseThrow();
        assertFalse(loadedParcial.cumplido());
        assertEquals(18.5, loadedParcial.valorAlcanzado());
    }

    @Test
    @DisplayName("save() debe crear el directorio padre si no existe")
    void testSaveCreatesParentDirectories() {
        Path nestedPath = tempDir.resolve("subfolder").resolve("data").resolve("habits.json");
        JsonHabitRepository nestedRepo = new JsonHabitRepository(nestedPath);

        AppData data = AppData.empty();
        nestedRepo.save(data);

        assertTrue(Files.exists(nestedPath));
    }

    @Test
    @DisplayName("save() atómico debe sobrescribir el archivo existente correctamente")
    void testAtomicSaveOverwritesExistingFile() {
        Habit h1 = new Habit("Hábito A", "Desc", "Cat", Set.of(DayOfWeek.MONDAY), 1);
        AppData inicial = AppData.empty().withHabitAdded(h1);

        repository.save(inicial);
        assertEquals(1, repository.load().habits().size());

        Habit h2 = new Habit("Hábito B", "Desc", "Cat", Set.of(DayOfWeek.FRIDAY), 1);
        AppData actualizado = inicial.withHabitAdded(h2);

        repository.save(actualizado);

        AppData resultado = repository.load();
        assertEquals(2, resultado.habits().size());
        assertTrue(resultado.habits().stream().anyMatch(h -> h.nombre().equals("Hábito A")));
        assertTrue(resultado.habits().stream().anyMatch(h -> h.nombre().equals("Hábito B")));
    }

    @Test
    @DisplayName("save() y load() deben persistir y normalizar correctamente el catálogo de tags")
    void testTagsPersistence() {
        Habit h1 = new Habit("Hábito A", "Desc", "Salud", Set.of(DayOfWeek.MONDAY), 1);
        AppData data = AppData.empty()
                .withHabitAdded(h1)
                .withTagAdded("Estudio")
                .withTagAdded("Finanzas");

        repository.save(data);
        AppData loaded = repository.load();

        assertNotNull(loaded.tags());
        assertTrue(loaded.tags().contains("Salud"));
        assertTrue(loaded.tags().contains("Estudio"));
        assertTrue(loaded.tags().contains("Finanzas"));
        assertEquals(3, loaded.tags().size());
    }
}
