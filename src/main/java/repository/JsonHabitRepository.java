package repository;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.AppData;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Repositorio de persistencia local para el estado de la aplicación.
 * Gestiona lectura y escritura atómica en archivo plano JSON local con Jackson.
 */
public class JsonHabitRepository {

    private static final Path DEFAULT_PATH = Path.of("data", "habits_data.json");

    private final Path filePath;
    private final ObjectMapper objectMapper;

    /**
     * Constructor por defecto que apunta a la ruta estándar de la aplicación: data/habits_data.json.
     */
    public JsonHabitRepository() {
        this(DEFAULT_PATH);
    }

    /**
     * Constructor con inyección de ruta (permite tests aislados sin tocar el archivo de producción).
     */
    public JsonHabitRepository(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "La ruta del archivo no puede ser nula");
        this.objectMapper = createConfiguredMapper();
    }

    private static ObjectMapper createConfiguredMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    /**
     * Carga el estado de la aplicación desde el archivo plano local.
     * Si el archivo no existe o está vacío, retorna un estado inicial limpio (Día 0).
     */
    public synchronized AppData load() {
        if (!Files.exists(filePath)) {
            return AppData.empty();
        }

        try {
            if (Files.size(filePath) == 0) {
                return AppData.empty();
            }
            return objectMapper.readValue(filePath.toFile(), AppData.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Error al leer el archivo de datos en: " + filePath, e);
        }
    }

    /**
     * Guarda atómicamente el estado de la aplicación en el archivo local.
     * Utiliza un archivo temporal (.tmp) y reemplazo atómico para evitar corrupción en caídas.
     */
    public synchronized void save(AppData data) {
        Objects.requireNonNull(data, "El objeto AppData no puede ser nulo");

        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Path tempFile = filePath.resolveSibling(filePath.getFileName().toString() + ".tmp");
            objectMapper.writeValue(tempFile.toFile(), data);

            try {
                Files.move(tempFile, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                // Fallback en sistemas de archivos o unidades donde ATOMIC_MOVE no está soportado
                Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Error al guardar el estado en: " + filePath, e);
        }
    }

    public Path getFilePath() {
        return filePath;
    }
}
