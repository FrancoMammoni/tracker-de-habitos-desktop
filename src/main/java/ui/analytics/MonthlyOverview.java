package ui.analytics;

import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.DayCompletion;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Componente visual para la analítica mensual general ("Hábitos de Este Mes").
 * Modela y exhibe la función f(día) = cantidad de hábitos completados
 * tanto en un gráfico de barras como en una tabla detallada de frecuencias diarias.
 */
public class MonthlyOverview extends VBox {

    private final Function<YearMonth, Map<Integer, DayCompletion>> dataSupplier;
    private final Supplier<Integer> totalHabitsSupplier;
    private YearMonth currentMonth;

    private final Label monthTitleLabel;
    private final Label totalKpiValue;
    private final Label avgKpiValue;
    private final Label peakKpiValue;

    private final NumberAxis yAxis;
    private final BarChart<String, Number> barChart;
    private final TableColumn<DayCompletion, String> completadosCol;
    private final TableView<DayCompletion> tableView;

    public MonthlyOverview(Function<YearMonth, Map<Integer, DayCompletion>> dataSupplier) {
        this(dataSupplier, () -> 0);
    }

    public MonthlyOverview(Function<YearMonth, Map<Integer, DayCompletion>> dataSupplier,
                           Supplier<Integer> totalHabitsSupplier) {
        super(16);
        this.dataSupplier = Objects.requireNonNull(dataSupplier, "El dataSupplier no puede ser nulo");
        this.totalHabitsSupplier = totalHabitsSupplier != null ? totalHabitsSupplier : () -> 0;
        this.currentMonth = YearMonth.now();

        getStyleClass().add("monthly-overview-container");
        setPadding(new Insets(8, 4, 16, 4));

        // 1. Barra superior: Navegación de mes y botón de hoy
        HBox navRow = new HBox(10);
        navRow.setAlignment(Pos.CENTER_LEFT);

        Button prevBtn = new Button("◀");
        prevBtn.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SMALL);
        prevBtn.setTooltip(new Tooltip("Mes anterior"));
        prevBtn.setOnAction(e -> {
            currentMonth = currentMonth.minusMonths(1);
            refresh();
        });

        monthTitleLabel = new Label();
        monthTitleLabel.getStyleClass().add("monthly-title-label");

        Button nextBtn = new Button("▶");
        nextBtn.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SMALL);
        nextBtn.setTooltip(new Tooltip("Mes siguiente"));
        nextBtn.setOnAction(e -> {
            currentMonth = currentMonth.plusMonths(1);
            refresh();
        });

        Button todayBtn = new Button("Mes Actual");
        todayBtn.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SMALL);
        todayBtn.setOnAction(e -> {
            currentMonth = YearMonth.now();
            refresh();
        });

        navRow.getChildren().addAll(prevBtn, monthTitleLabel, nextBtn, todayBtn);

        // 2. Tarjetas resumen de KPIs (Total, Promedio, Día récord)
        HBox kpiRow = new HBox(12);
        kpiRow.setAlignment(Pos.CENTER_LEFT);

        VBox totalCard = buildKpiCard("TOTAL DEL MES");
        totalKpiValue = (Label) totalCard.getChildren().get(1);

        VBox avgCard = buildKpiCard("PROMEDIO DIARIO");
        avgKpiValue = (Label) avgCard.getChildren().get(1);

        VBox peakCard = buildKpiCard("DÍA RÉCORD");
        peakKpiValue = (Label) peakCard.getChildren().get(1);

        kpiRow.getChildren().addAll(totalCard, avgCard, peakCard);

        // 3. Gráfico de barras: Eje X = Día (1..31), Eje Y = f(día)
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Día del Mes");
        xAxis.setAnimated(false);

        yAxis = new NumberAxis();
        yAxis.setLabel("Hábitos Cumplidos f(día)");
        yAxis.setTickUnit(1);
        yAxis.setMinorTickCount(0);
        yAxis.setAutoRanging(false);
        yAxis.setAnimated(false);

        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        barChart.setTitle("Función f(día) = Cantidad de Hábitos Completados");
        barChart.setMinHeight(260);
        barChart.setPrefHeight(280);
        barChart.getStyleClass().add("monthly-bar-chart");
        VBox.setVgrow(barChart, Priority.SOMETIMES);

        // 4. Tabla de valores: Listado detallado de f(día)
        Label tableTitle = new Label("Tabla de valores f(día):");
        tableTitle.getStyleClass().addAll(Styles.TEXT_BOLD, Styles.TEXT_MUTED);

        tableView = new TableView<>();
        tableView.getStyleClass().addAll(Styles.DENSE, "monthly-table");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setMinHeight(200);
        tableView.setPrefHeight(240);

        TableColumn<DayCompletion, String> diaCol = new TableColumn<>("Día");
        diaCol.setMinWidth(90);
        diaCol.setMaxWidth(120);
        diaCol.setCellValueFactory(cellData -> {
            DayCompletion d = cellData.getValue();
            String diaSemana = d.fecha().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.of("es", "ES"));
            diaSemana = Character.toUpperCase(diaSemana.charAt(0)) + diaSemana.substring(1);
            return new SimpleStringProperty(String.format("%02d (%s)", d.dia(), diaSemana));
        });

        completadosCol = new TableColumn<>("f(día)");
        completadosCol.setMinWidth(90);
        completadosCol.setMaxWidth(110);
        completadosCol.setStyle("-fx-alignment: CENTER;");
        completadosCol.setCellValueFactory(cellData -> {
            int cumplidos = cellData.getValue().totalCumplidos();
            int totalH = totalHabitsSupplier != null ? totalHabitsSupplier.get() : 0;
            String texto = totalH > 0
                    ? String.format("%d / %d", cumplidos, totalH)
                    : String.valueOf(cumplidos);
            return new SimpleStringProperty(texto);
        });

        TableColumn<DayCompletion, String> habitosCol = new TableColumn<>("Hábitos Realizados");
        habitosCol.setCellValueFactory(cellData -> {
            List<String> nombres = cellData.getValue().nombresHabitos();
            return new SimpleStringProperty(nombres.isEmpty() ? "—" : String.join(", ", nombres));
        });

        tableView.getColumns().addAll(diaCol, completadosCol, habitosCol);

        getChildren().addAll(navRow, kpiRow, barChart, tableTitle, tableView);

        // Carga inicial
        refresh();
    }

    private VBox buildKpiCard(String title) {
        VBox card = new VBox(2);
        card.getStyleClass().add("monthly-kpi-card");
        card.setPadding(new Insets(8, 14, 8, 14));
        card.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("monthly-kpi-title");

        Label valueLabel = new Label("0");
        valueLabel.getStyleClass().add("monthly-kpi-value");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    /**
     * Refresca la información del mes actual invocando al proveedor de datos.
     */
    public void refresh() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.of("es", "ES"));
        String mesStr = currentMonth.format(formatter);
        mesStr = Character.toUpperCase(mesStr.charAt(0)) + mesStr.substring(1);
        monthTitleLabel.setText(mesStr);

        Map<Integer, DayCompletion> datos = dataSupplier.apply(currentMonth);

        // 1. Calcular KPIs
        int total = 0;
        int maxVal = 0;
        int mejorDia = 1;

        for (DayCompletion d : datos.values()) {
            total += d.totalCumplidos();
            if (d.totalCumplidos() > maxVal) {
                maxVal = d.totalCumplidos();
                mejorDia = d.dia();
            }
        }

        totalKpiValue.setText(total + " hábitos");

        int diasParaPromedio = currentMonth.equals(YearMonth.now())
                ? Math.max(1, LocalDate.now().getDayOfMonth())
                : currentMonth.lengthOfMonth();
        double promedio = (double) total / diasParaPromedio;
        avgKpiValue.setText(String.format(Locale.US, "%.1f / día", promedio));

        if (maxVal > 0) {
            peakKpiValue.setText(String.format("Día %d (%d)", mejorDia, maxVal));
        } else {
            peakKpiValue.setText("Sin registros");
        }

        // 2. Actualizar Gráfico
        int totalHabitos = totalHabitsSupplier != null ? totalHabitsSupplier.get() : 0;
        int topeY = Math.max(1, totalHabitos);

        yAxis.setLowerBound(0);
        yAxis.setUpperBound(topeY);
        yAxis.setTickUnit(topeY <= 10 ? 1 : Math.ceil(topeY / 10.0));

        if (totalHabitos > 0) {
            completadosCol.setText(String.format("f(día) [de %d]", totalHabitos));
        } else {
            completadosCol.setText("f(día)");
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (DayCompletion d : datos.values()) {
            XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(String.valueOf(d.dia()), d.totalCumplidos());
            series.getData().add(dataPoint);
        }

        barChart.getData().setAll(series);

        // Instalar Tooltips interactivos en las barras generadas
        for (XYChart.Data<String, Number> dataPoint : series.getData()) {
            int dia = Integer.parseInt(dataPoint.getXValue());
            DayCompletion comp = datos.get(dia);
            if (comp != null && dataPoint.getNode() != null) {
                String tooltipText = String.format("Día %d: %d completados%s",
                        comp.dia(),
                        comp.totalCumplidos(),
                        comp.nombresHabitos().isEmpty() ? "" : "\n• " + String.join("\n• ", comp.nombresHabitos())
                );
                Tooltip.install(dataPoint.getNode(), new Tooltip(tooltipText));
            }
        }

        // 3. Actualizar Tabla
        tableView.getItems().setAll(datos.values());
    }

    public YearMonth getCurrentMonth() {
        return currentMonth;
    }
}
