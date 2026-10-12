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

        setStyle("-fx-background-color: #1C2826;");
        setAlignment(Pos.CENTER);

        this.cardContainer = new VBox();
        this.cardContainer.setMaxWidth(440);
        this.cardContainer.setAlignment(Pos.CENTER);
        this.cardContainer.setSpacing(16);
        this.cardContainer.setPadding(new Insets(36));
        this.cardContainer.setStyle(
                "-fx-background-color: #243330; " +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: #505168; " +
                "-fx-border-radius: 12px; " +
                "-fx-border-width: 1px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 24, 0, 0, 8);"
        );

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
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #E0FBFC;");

        Label subtitleLabel = new Label(
                "Crea una contraseña maestra para proteger el acceso a tus hábitos.\n\n" +
                "⚠️ Importante: Si olvidas tu clave, no se podrá recuperar. " +
                "La única alternativa será restablecer la aplicación borrando todos los datos guardados."
        );
        subtitleLabel.setWrapText(true);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setStyle("-fx-text-fill: #8d8fa8; -fx-font-size: 12px; -fx-text-alignment: center;");

        Label passLabel = new Label("Nueva Contraseña");
        passLabel.setMaxWidth(Double.MAX_VALUE);
        passLabel.setStyle("-fx-text-fill: #E0FBFC; -fx-font-weight: bold; -fx-font-size: 13px;");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Mínimo 4 caracteres");
        passField.setMaxWidth(Double.MAX_VALUE);

        Label confirmLabel = new Label("Confirmar Contraseña");
        confirmLabel.setMaxWidth(Double.MAX_VALUE);
        confirmLabel.setStyle("-fx-text-fill: #E0FBFC; -fx-font-weight: bold; -fx-font-size: 13px;");

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Repite la contraseña");
        confirmField.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.setStyle("-fx-text-fill: #A50104; -fx-font-size: 12px; -fx-font-weight: bold;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button saveButton = new Button("Guardar Contraseña y Continuar");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setStyle(
                "-fx-background-color: #048A81; -fx-text-fill: #E0FBFC; " +
                "-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20; " +
                "-fx-background-radius: 6px; -fx-cursor: hand;"
        );

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
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #E0FBFC;");

        Label subtitleLabel = new Label("Introduce tu contraseña maestra para ingresar.");
        subtitleLabel.setStyle("-fx-text-fill: #8d8fa8; -fx-font-size: 13px;");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Contraseña");
        passField.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.setStyle("-fx-text-fill: #A50104; -fx-font-size: 12px; -fx-font-weight: bold;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button unlockButton = new Button("Desbloquear");
        unlockButton.setMaxWidth(Double.MAX_VALUE);
        unlockButton.setStyle(
                "-fx-background-color: #048A81; -fx-text-fill: #E0FBFC; " +
                "-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20; " +
                "-fx-background-radius: 6px; -fx-cursor: hand;"
        );

        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #505168;");

        Button forgotButton = new Button("Olvidé mi contraseña / Restablecer aplicación");
        forgotButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #A50104; " +
                "-fx-font-size: 12px; -fx-underline: true; -fx-cursor: hand;"
        );

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
     * Muestra el modal de advertencia destructiva para restablecimiento irreversible.
     */
    private void handleForgotPassword() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Restablecer Aplicación");
        alert.setHeaderText("⚠️ ¿Deseas restablecer completamente la aplicación?");
        alert.setContentText(
                "Esta acción es IRREVERSIBLE y no se puede deshacer.\n\n" +
                "• Se eliminarán definitivamente todos los hábitos registrados.\n" +
                "• Se borrarán todos los registros diarios y notas de avance.\n" +
                "• Se reiniciarán las rachas y récords históricos acumulados.\n\n" +
                "La aplicación volverá a su estado inicial de fábrica (Día 0) " +
                "y deberás configurar una nueva contraseña.\n\n" +
                "¿Confirmas el restablecimiento?"
        );

        ButtonType btnReset = new ButtonType("Restablecer y Borrar Datos", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnReset, btnCancel);

        if (getScene() != null && getScene().getWindow() != null) {
            alert.initOwner(getScene().getWindow());
            alert.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        }

        alert.showAndWait().ifPresent(choice -> {
            if (choice == btnReset) {
                authService.restablecerAplicacion();
                showSetupView();
            }
        });
    }
}
