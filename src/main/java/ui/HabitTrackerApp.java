package ui;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Punto de entrada principal de la aplicación JavaFX.
 * Inicializa el tema AtlantaFX PrimerDark y configura la ventana principal.
 */
public class HabitTrackerApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Inicializar tema oscuro PrimerDark de AtlantaFX
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        MainController mainController = new MainController();
        Scene scene = new Scene(mainController.getView(), 1100, 720);

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

    public static void main(String[] args) {
        launch(args);
    }
}
