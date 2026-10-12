package ui;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import repository.JsonHabitRepository;
import service.AuthService;
import service.HabitService;
import service.StreakService;
import ui.auth.LockScreenView;

import java.net.URL;

/**
 * Punto de entrada principal de la aplicación JavaFX.
 * Inicializa el tema AtlantaFX PrimerDark, verifica la autenticación y enruta
 * entre la pantalla de bloqueo y la vista principal de la aplicación.
 */
public class HabitTrackerApp extends Application {

    private JsonHabitRepository repository;
    private AuthService authService;
    private Scene scene;

    @Override
    public void start(Stage primaryStage) {
        // Inicializar tema oscuro PrimerDark de AtlantaFX
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        this.repository = new JsonHabitRepository();
        this.authService = new AuthService(repository);

        // Inicializar con la pantalla de bloqueo o configuración inicial
        LockScreenView lockScreenView = new LockScreenView(authService, this::showMainApp);
        this.scene = new Scene(lockScreenView, 1100, 720);

        // Cargar hoja de estilos personalizada si existe
        URL cssResource = getClass().getResource("/styles/custom.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        primaryStage.setTitle("Tracker de Hábitos");
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showMainApp() {
        HabitService habitService = new HabitService(repository);
        StreakService streakService = new StreakService(repository);
        MainController mainController = new MainController(habitService, streakService);
        scene.setRoot(mainController.getView());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
