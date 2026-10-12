package ui.hud;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.Habit;

import java.time.DayOfWeek;
import java.util.*;

/**
 * Diálogo modal para la creación de un nuevo hábito.
 * Valida invariantes de negocio: nombre obligatorio y 1 <= objetivoSemanal <= diasSeleccionados.
 */
public class CreateHabitDialog extends Dialog<Habit> {

    private final TextField nombreField = new TextField();
    private final TextField descripcionField = new TextField();
    private final TextField categoriaField = new TextField();
    private final Spinner<Integer> objetivoSpinner = new Spinner<>(1, 7, 1);
    private final Map<DayOfWeek, ToggleButton> diaButtons = new LinkedHashMap<>();

    public CreateHabitDialog() {
        setTitle("Nuevo Hábito");
        setHeaderText("Configurar nuevo hábito y objetivo semanal");

        DialogPane dialogPane = getDialogPane();
        dialogPane.getStyleClass().add(Styles.DENSE);
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox content = buildContent();
        dialogPane.setContent(content);

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.getStyleClass().addAll(Styles.ACCENT);
        okButton.setText("Crear Hábito");

        // Validación dinámica para habilitar el botón OK
        actualizarEstadoBotonOk(okButton);
        nombreField.textProperty().addListener((obs, o, n) -> actualizarEstadoBotonOk(okButton));

        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                Set<DayOfWeek> seleccionados = getDiasSeleccionados();
                String nombre = nombreField.getText().trim();
                String desc = descripcionField.getText().trim();
                String cat = categoriaField.getText().trim();
                int objetivo = objetivoSpinner.getValue();

                return new Habit(nombre, desc, cat, seleccionados, objetivo);
            }
            return null;
        });
    }

    private VBox buildContent() {
        VBox layout = new VBox(16);
        layout.setPadding(new Insets(16));
        layout.setMinWidth(460);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        // Nombre
        Label nombreLbl = new Label("Nombre (*):");
        nombreLbl.getStyleClass().add(Styles.TEXT_BOLD);
        nombreField.setPromptText("Ej. Leer 30 minutos, Meditar, Gimnasio...");
        grid.add(nombreLbl, 0, 0);
        grid.add(nombreField, 1, 0);

        // Descripción
        Label descLbl = new Label("Meta diaria:");
        descripcionField.setPromptText("Ej. Leer 20 páginas, 2.5 litros, 45 min...");
        grid.add(descLbl, 0, 1);
        grid.add(descripcionField, 1, 1);

        // Categoría
        Label catLbl = new Label("Categoría:");
        categoriaField.setPromptText("Salud, Estudio, Productividad, Bienestar...");
        categoriaField.setText("General");
        grid.add(catLbl, 0, 2);
        grid.add(categoriaField, 1, 2);

        // Días de aparición estilo alarma
        VBox diasBox = new VBox(8);
        Label diasLbl = new Label("Días de aparición en el HUD (*):");
        diasLbl.getStyleClass().add(Styles.TEXT_BOLD);

        HBox togglesBox = new HBox(6);
        togglesBox.setAlignment(Pos.CENTER_LEFT);

        String[] nombresDias = {"L", "M", "X", "J", "V", "S", "D"};
        DayOfWeek[] days = {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        };

        for (int i = 0; i < days.length; i++) {
            DayOfWeek d = days[i];
            ToggleButton tb = new ToggleButton(nombresDias[i]);
            tb.setSelected(true); // Todos seleccionados por defecto
            tb.getStyleClass().add(Styles.BUTTON_CIRCLE);
            tb.setStyle("-fx-min-width: 38px; -fx-min-height: 38px; -fx-max-width: 38px; -fx-max-height: 38px;");
            
            tb.selectedProperty().addListener((obs, o, n) -> ajustarRangoObjetivo());
            diaButtons.put(d, tb);
            togglesBox.getChildren().add(tb);
        }

        diasBox.getChildren().addAll(diasLbl, togglesBox);

        // Objetivo Semanal Mínimo
        VBox objetivoBox = new VBox(6);
        Label objLbl = new Label("Objetivo semanal mínimo (días cumplidos por semana):");
        objLbl.getStyleClass().add(Styles.TEXT_BOLD);

        objetivoSpinner.setEditable(false);
        objetivoSpinner.setStyle("-fx-pref-width: 120px;");
        ajustarRangoObjetivo();

        Label ayudaObj = new Label("1 <= Objetivo Semanal <= Días seleccionados");
        ayudaObj.getStyleClass().addAll(Styles.TEXT_CAPTION, Styles.TEXT_MUTED);

        objetivoBox.getChildren().addAll(objLbl, objetivoSpinner, ayudaObj);

        layout.getChildren().addAll(grid, new Separator(), diasBox, new Separator(), objetivoBox);
        return layout;
    }

    private Set<DayOfWeek> getDiasSeleccionados() {
        Set<DayOfWeek> set = new LinkedHashSet<>();
        diaButtons.forEach((dia, tb) -> {
            if (tb.isSelected()) {
                set.add(dia);
            }
        });
        return set;
    }

    private void ajustarRangoObjetivo() {
        int diasCount = getDiasSeleccionados().size();
        if (diasCount <= 0) {
            diasCount = 1;
        }

        int valorActual = objetivoSpinner.getValue() != null ? objetivoSpinner.getValue() : 1;
        if (valorActual > diasCount) {
            valorActual = diasCount;
        }
        if (valorActual < 1) {
            valorActual = 1;
        }

        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, diasCount, valorActual);
        objetivoSpinner.setValueFactory(factory);

        DialogPane dp = getDialogPane();
        if (dp != null) {
            Button okButton = (Button) dp.lookupButton(ButtonType.OK);
            if (okButton != null) {
                actualizarEstadoBotonOk(okButton);
            }
        }
    }

    private void actualizarEstadoBotonOk(Button okButton) {
        boolean nombreValido = !nombreField.getText().trim().isEmpty();
        boolean diasValidos = !getDiasSeleccionados().isEmpty();
        okButton.setDisable(!nombreValido || !diasValidos);
    }
}
