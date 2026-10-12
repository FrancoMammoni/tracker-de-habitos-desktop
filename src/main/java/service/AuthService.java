package service;

import model.AppData;
import repository.JsonHabitRepository;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Objects;

/**
 * Servicio de seguridad y autenticación local.
 * Gestiona el ciclo de vida de la contraseña mediante derivación criptográfica PBKDF2 con salting
 * y provee el restablecimiento de fábrica (Día 0) ante olvido de credenciales.
 */
public class AuthService {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_BYTES = 16;   // 128 bits de sal aleatoria

    private final JsonHabitRepository repository;
    private final SecureRandom secureRandom;

    public AuthService(JsonHabitRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repositorio no puede ser nulo");
        this.secureRandom = new SecureRandom();
    }

    /**
     * Comprueba si la aplicación ya tiene una contraseña configurada y persistida.
     */
    public synchronized boolean hasPasswordConfigured() {
        AppData data = repository.load();
        return data.hasPassword();
    }

    /**
     * Establece una nueva contraseña para la aplicación.
     * Genera un salt criptográfico aleatorio, deriva el hash con PBKDF2 y persiste atómicamente en AppData.
     *
     * @param rawPassword Contraseña en texto plano (mínimo 4 caracteres).
     * @throws IllegalArgumentException Si la contraseña es nula o menor a 4 caracteres.
     */
    public synchronized void configurarPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().length() < 4) {
            throw new IllegalArgumentException("La contraseña debe contener al menos 4 caracteres");
        }

        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);

        byte[] hash = hashPassword(rawPassword, salt);

        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = Base64.getEncoder().encodeToString(hash);

        AppData current = repository.load();
        AppData updated = current.withPassword(hashBase64, saltBase64);
        repository.save(updated);
    }

    /**
     * Valida si la contraseña ingresada coincide con el hash persistido.
     * Utiliza MessageDigest.isEqual para garantizar tiempo constante y prevenir timing attacks.
     *
     * @param rawPassword Contraseña ingresada por el usuario.
     * @return true si la contraseña es correcta; false si no coincide o no hay clave configurada.
     */
    public synchronized boolean verificarPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return false;
        }

        AppData data = repository.load();
        if (!data.hasPassword()) {
            return false;
        }

        byte[] salt = Base64.getDecoder().decode(data.passwordSalt());
        byte[] expectedHash = Base64.getDecoder().decode(data.passwordHash());
        byte[] computedHash = hashPassword(rawPassword, salt);

        return MessageDigest.isEqual(expectedHash, computedHash);
    }

    /**
     * Restablece la aplicación a su estado de fábrica (Día 0).
     * Borra de forma atómica e irreversible todos los hábitos, registros históricos y la contraseña.
     */
    public synchronized void restablecerAplicacion() {
        AppData resetData = AppData.empty();
        repository.save(resetData);
    }

    /**
     * Deriva el hash criptográfico a partir de la contraseña y el salt.
     */
    private byte[] hashPassword(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Error al generar hash criptográfico PBKDF2", e);
        } finally {
            spec.clearPassword();
        }
    }
}
