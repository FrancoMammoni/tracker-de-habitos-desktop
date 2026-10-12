package service;

import model.AppData;
import model.Habit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonHabitRepository;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    @TempDir
    Path tempDir;

    private JsonHabitRepository repository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        Path testFile = tempDir.resolve("test_habits.json");
        repository = new JsonHabitRepository(testFile);
        authService = new AuthService(repository);
    }

    @Test
    void testInitialStateHasNoPassword() {
        assertFalse(authService.hasPasswordConfigured(), "Inicialmente no debe haber contraseña configurada");
        assertFalse(authService.verificarPassword("cualquiera"), "Cualquier verificación debe fallar si no hay contraseña");
    }

    @Test
    void testConfigureAndVerifyPasswordSuccess() {
        authService.configurarPassword("claveSegura123");

        assertTrue(authService.hasPasswordConfigured(), "Debe reportar que la contraseña está configurada");
        assertTrue(authService.verificarPassword("claveSegura123"), "La clave correcta debe validarse exitosamente");
        assertFalse(authService.verificarPassword("claveErronea"), "Una clave distinta debe ser rechazada");
        assertFalse(authService.verificarPassword(""), "Una clave vacía debe ser rechazada");
        assertFalse(authService.verificarPassword(null), "Una clave nula debe ser rechazada");
    }

    @Test
    void testConfigurePasswordInvalid() {
        assertThrows(IllegalArgumentException.class, () -> authService.configurarPassword(null),
                "Clave nula debe lanzar excepción");
        assertThrows(IllegalArgumentException.class, () -> authService.configurarPassword("123"),
                "Clave menor a 4 caracteres debe lanzar excepción");
        assertThrows(IllegalArgumentException.class, () -> authService.configurarPassword("   "),
                "Clave en blanco debe lanzar excepción");
    }

    @Test
    void testRestablecerAplicacionWipesDataAndCredentials() {
        // Configuramos contraseña y agregamos datos
        authService.configurarPassword("miPassword1");
        Habit habito = new Habit("Lectura", "Leer 20 pags", "Estudio", Set.of(DayOfWeek.MONDAY), 1);
        repository.save(repository.load().withHabitAdded(habito));

        assertTrue(authService.hasPasswordConfigured());
        assertEquals(1, repository.load().habits().size());

        // Ejecutamos restablecimiento destructivo
        authService.restablecerAplicacion();

        assertFalse(authService.hasPasswordConfigured(), "Tras el reseteo no debe quedar contraseña");
        assertTrue(repository.load().habits().isEmpty(), "Tras el reseteo los hábitos deben eliminarse");
        assertTrue(repository.load().dailyRecords().isEmpty(), "Tras el reseteo los registros diarios deben eliminarse");
    }

    @Test
    void testPreserveExistingHabitsWhenConfiguringPassword() {
        Habit habito = new Habit("Gimnasio", null, "Salud", Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), 2);
        repository.save(AppData.empty().withHabitAdded(habito));

        authService.configurarPassword("superClave2026");

        assertTrue(authService.verificarPassword("superClave2026"));
        assertEquals(1, repository.load().habits().size(), "El hábito preexistente debe conservarse intacto");
        assertEquals("Gimnasio", repository.load().habits().getFirst().nombre());
    }
}
