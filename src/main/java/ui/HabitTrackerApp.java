package ui;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import repository.JsonHabitRepository;
import service.AuthService;
import service.HabitService;
import service.StreakService;
import ui.auth.LockScreenView;
import ui.components.AppTitleBar;
import ui.components.WindowResizeHelper;

import java.io.InputStream;
import java.net.URL;

/**
 * Punto de entrada principal de la aplicación JavaFX.
 * Inicializa el tema AtlantaFX PrimerDark, ventana sin bordes blancos de sistema (UNDECORATED),
 * barra de título oscura integrada, ícono de aplicación, verificación de autenticación y enrutamiento
 * entre la pantalla de bloqueo y la vista principal.
 */
public class HabitTrackerApp extends Application {

    private JsonHabitRepository repository;
    private AuthService authService;
    private Stage primaryStage;
    private VBox rootContainer;
    private AppTitleBar titleBar;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        // Inicializar tema oscuro PrimerDark de AtlantaFX
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        this.repository = new JsonHabitRepository();
        this.authService = new AuthService(repository);

        // Configurar ventana sin decoración nativa del SO para evitar bordes blancos
        primaryStage.initStyle(StageStyle.UNDECORATED);

        // Asignar ícono de la aplicación
        InputStream iconStream = getClass().getResourceAsStream("/icons/app_icon.png");
        if (iconStream != null) {
            primaryStage.getIcons().add(new Image(iconStream));
        }

        // Construir barra de título oscura integrada y contenedor raíz
        this.titleBar = new AppTitleBar(primaryStage);
        this.rootContainer = new VBox();
        this.rootContainer.setStyle("-fx-background-color: #1C2826;");

        // Inicializar con la pantalla de bloqueo o configuración inicial
        LockScreenView lockScreenView = new LockScreenView(authService, this::showMainApp);
        VBox.setVgrow(lockScreenView, Priority.ALWAYS);

        rootContainer.getChildren().addAll(titleBar, lockScreenView);

        Scene scene = new Scene(rootContainer, 1100, 720);

        // Cargar hoja de estilos personalizada si existe
        URL cssResource = getClass().getResource("/styles/custom.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        // Habilitar redimensionamiento interactivo en ventana sin marco nativo
        WindowResizeHelper.attach(primaryStage, scene);

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

        var mainView = mainController.getView();
        VBox.setVgrow(mainView, Priority.ALWAYS);

        rootContainer.getChildren().setAll(titleBar, mainView);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
