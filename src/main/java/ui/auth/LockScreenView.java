package ui.auth;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import service.AuthService;

import java.util.Objects;

/**
 * Pantalla de seguridad para autenticación obligatoria y configuración de contraseña.
 * Incluye modo de configuración inicial (Día 0) y modo de desbloqueo, con soporte de
 * restablecimiento irreversible de fábrica ante olvido de contraseña.
 */
public class LockScreenView extends StackPane {

    private final AuthService authService;
    private final Runnable onUnlockSuccess;
    private final VBox cardContainer;

    public LockScreenView(AuthService authService, Runnable onUnlockSuccess) {
        this.authService = Objects.requireNonNull(authService, "AuthService no puede ser nulo");
        this.onUnlockSuccess = Objects.requireNonNull(onUnlockSuccess, "onUnlockSuccess no puede ser nulo");

        getStyleClass().add("lock-screen-root");
        setAlignment(Pos.CENTER);

        this.cardContainer = new VBox();
        this.cardContainer.setMaxWidth(440);
        this.cardContainer.getStyleClass().add("lock-screen-card");

        getChildren().add(cardContainer);

        // Renderizar vista inicial según si existe contraseña configurada
        refreshView();
    }

    /**
     * Refresca la vista eligiendo el modo correspondiente.
     */
    public void refreshView() {
        if (!authService.hasPasswordConfigured()) {
            showSetupView();
        } else {
            showUnlockView();
        }
    }

    /**
     * Modo A: Configurar contraseña por primera vez (Día 0 o tras reseteo).
     */
    private void showSetupView() {
        cardContainer.getChildren().clear();

        Label iconLabel = new Label("🔐");
        iconLabel.setStyle("-fx-font-size: 38px;");

        Label titleLabel = new Label("Configurar Contraseña");
        titleLabel.getStyleClass().add("lock-screen-title");

        Label subtitleLabel = new Label(
                "Crea una contraseña maestra para proteger el acceso a tus hábitos.\n\n" +
                "⚠️ Importante: Si olvidas tu clave, no se podrá recuperar. " +
                "La única alternativa será restablecer la aplicación borrando todos los datos guardados."
        );
        subtitleLabel.setWrapText(true);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().add("lock-screen-subtitle");

        Label passLabel = new Label("Nueva Contraseña");
        passLabel.setMaxWidth(Double.MAX_VALUE);
        passLabel.getStyleClass().add("modal-label");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Mínimo 4 caracteres");
        passField.setMaxWidth(Double.MAX_VALUE);

        Label confirmLabel = new Label("Confirmar Contraseña");
        confirmLabel.setMaxWidth(Double.MAX_VALUE);
        confirmLabel.getStyleClass().add("modal-label");

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Repite la contraseña");
        confirmField.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.getStyleClass().add("lock-screen-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button saveButton = new Button("Guardar Contraseña y Continuar");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.getStyleClass().add("lock-screen-btn-primary");

        Runnable handleSetup = () -> {
            String p1 = passField.getText();
            String p2 = confirmField.getText();

            if (p1 == null || p1.trim().length() < 4) {
                errorLabel.setText("La contraseña debe tener al menos 4 caracteres.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                passField.requestFocus();
                return;
            }

            if (!p1.equals(p2)) {
                errorLabel.setText("Las contraseñas no coinciden.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                confirmField.requestFocus();
                return;
            }

            try {
                authService.configurarPassword(p1);
                onUnlockSuccess.run();
            } catch (Exception e) {
                errorLabel.setText("Error al guardar: " + e.getMessage());
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        };

        saveButton.setOnAction(e -> handleSetup.run());
        passField.setOnAction(e -> confirmField.requestFocus());
        confirmField.setOnAction(e -> handleSetup.run());

        cardContainer.getChildren().addAll(
                iconLabel,
                titleLabel,
                subtitleLabel,
                passLabel,
                passField,
                confirmLabel,
                confirmField,
                errorLabel,
                saveButton
        );
    }

    /**
     * Modo B: Ingresar contraseña para desbloquear la aplicación.
     */
    private void showUnlockView() {
        cardContainer.getChildren().clear();

        Label iconLabel = new Label("🔒");
        iconLabel.setStyle("-fx-font-size: 38px;");

        Label titleLabel = new Label("Tracker de Hábitos");
        titleLabel.getStyleClass().add("lock-screen-title");

        Label subtitleLabel = new Label("Introduce tu contraseña maestra para ingresar.");
        subtitleLabel.getStyleClass().add("lock-screen-subtitle");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Contraseña");
        passField.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.getStyleClass().add("lock-screen-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button unlockButton = new Button("Desbloquear");
        unlockButton.setMaxWidth(Double.MAX_VALUE);
        unlockButton.getStyleClass().add("lock-screen-btn-primary");

        Separator separator = new Separator();

        Button forgotButton = new Button("Olvidé mi contraseña / Restablecer aplicación");
        forgotButton.getStyleClass().add("lock-screen-btn-reset");

        Runnable handleUnlock = () -> {
            String p = passField.getText();
            if (authService.verificarPassword(p)) {
                onUnlockSuccess.run();
            } else {
                errorLabel.setText("Contraseña incorrecta. Inténtalo de nuevo.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                passField.selectAll();
                passField.requestFocus();
            }
        };

        unlockButton.setOnAction(e -> handleUnlock.run());
        passField.setOnAction(e -> handleUnlock.run());

        forgotButton.setOnAction(e -> handleForgotPassword());

        cardContainer.getChildren().addAll(
                iconLabel,
                titleLabel,
                subtitleLabel,
                passField,
                errorLabel,
                unlockButton,
                separator,
                forgotButton
        );
    }

    /**
     * Muestra la vista in-app de advertencia destructiva para restablecimiento irreversible.
     */
    private void handleForgotPassword() {
        cardContainer.getChildren().clear();

        Label iconLabel = new Label("⚠️");
        iconLabel.setStyle("-fx-font-size: 38px;");

        Label titleLabel = new Label("Restablecer Aplicación");
        titleLabel.getStyleClass().add("lock-screen-title");

        Label warningLabel = new Label(
                "Esta acción es IRREVERSIBLE y no se puede deshacer.\n\n" +
                "• Se eliminarán definitivamente todos los hábitos registrados.\n" +
                "• Se borrarán los registros diarios y notas de avance.\n" +
                "• Se reiniciarán las rachas y récords históricos acumulados.\n\n" +
                "La aplicación volverá a su estado inicial de fábrica (Día 0) y deberás configurar una nueva contraseña.\n\n" +
                "¿Confirmas el restablecimiento total?"
        );
        warningLabel.setWrapText(true);
        warningLabel.getStyleClass().add("modal-message");
        warningLabel.setStyle("-fx-text-alignment: center;");

        Button btnReset = new Button("Sí, Restablecer y Borrar Datos");
        btnReset.setMaxWidth(Double.MAX_VALUE);
        btnReset.getStyleClass().add("modal-btn-destructive");
        btnReset.setOnAction(e -> {
            authService.restablecerAplicacion();
            showSetupView();
        });

        Button btnCancel = new Button("Cancelar y Volver");
        btnCancel.setMaxWidth(Double.MAX_VALUE);
        btnCancel.getStyleClass().add("modal-btn-cancel");
        btnCancel.setOnAction(e -> showUnlockView());

        cardContainer.getChildren().addAll(
                iconLabel,
                titleLabel,
                warningLabel,
                btnReset,
                btnCancel
        );
    }
}
