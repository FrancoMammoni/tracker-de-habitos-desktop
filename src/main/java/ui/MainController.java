package ui;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import model.DailyRecord;
import model.Habit;
import model.StreakInfo;
import service.HabitService;
import service.StreakService;
import ui.analytics.CycleProgress;
import ui.analytics.MonthlyOverview;
import ui.analytics.StreakCanvas;
import ui.hud.ArchivedHabitsDialog;
import ui.hud.CompletedSection;
import ui.hud.CreateHabitDialog;
import ui.hud.EditHabitDialog;
import ui.hud.HabitItemCell;
import ui.modal.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controlador principal de la interfaz de usuario.
 * Ensambla el SplitPane (1/3 HUD lateral y 2/3 Panel Analítico).
 * Conecta los eventos del usuario con HabitService y StreakService.
 */
public class MainController {

    private final HabitService habitService;
    private final StreakService streakService;

    private final StackPane rootStackPane;
    private final SplitPane rootSplitPane;
    private final StackPane modalOverlay;
    private final VBox pendingContainer;
    private final CompletedSection completedSection;
    private final Label emptyHudLabel;
    private final Map<String, HabitItemCell> cellRegistry = new LinkedHashMap<>();

    private final VBox analyticsCardsContainer;
    private final Label emptyAnalyticsLabel;
    private final MonthlyOverview monthlyOverview;
    private final ScrollPane habitsScrollPane;
    private final ScrollPane monthlyScrollPane;
    private final StackPane analyticsContentPane;
    private final ComboBox<String> viewModeSelector;

    public MainController() {
        this(new HabitService(), new StreakService());
    }

    public MainController(HabitService habitService, StreakService streakService) {
        this.habitService = Objects.requireNonNull(habitService);
        this.streakService = Objects.requireNonNull(streakService);

        // 1. Regla anti-trampa al iniciar la aplicación
        streakService.auditarYSellarDiasPasados(LocalDate.now());

        rootSplitPane = new SplitPane();
        rootSplitPane.setOrientation(Orientation.HORIZONTAL);

        pendingContainer = new VBox(6);
        completedSection = new CompletedSection();
        emptyHudLabel = new Label("No hay hábitos programados para hoy.\n¡Pulsa el botón + para crear uno!");
        emptyHudLabel.getStyleClass().addAll(Styles.TEXT_MUTED);
        emptyHudLabel.setStyle("-fx-text-alignment: center; -fx-padding: 24px 0 0 0;");
        emptyHudLabel.setVisible(false);
        emptyHudLabel.setManaged(false);

        analyticsCardsContainer = new VBox(16);
        analyticsCardsContainer.setPadding(new Insets(12, 16, 24, 16));

        emptyAnalyticsLabel = new Label("No hay hábitos activos para analizar.\n¡Pulsa el botón + para crear tu primer hábito!");
        emptyAnalyticsLabel.getStyleClass().addAll(Styles.TEXT_MUTED);
        emptyAnalyticsLabel.setStyle("-fx-text-alignment: center; -fx-padding: 32px 0 0 0;");
        emptyAnalyticsLabel.setVisible(false);
        emptyAnalyticsLabel.setManaged(false);

        monthlyOverview = new MonthlyOverview(
                habitService::getCumplimientosMes,
                () -> habitService.getHabitosActivos().size()
        );

        VBox habitsContentBox = new VBox(12);
        habitsContentBox.getChildren().addAll(emptyAnalyticsLabel, analyticsCardsContainer);

        habitsScrollPane = new ScrollPane(habitsContentBox);
        habitsScrollPane.setFitToWidth(true);
        habitsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        habitsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        habitsScrollPane.getStyleClass().add(Styles.DENSE);

        monthlyScrollPane = new ScrollPane(monthlyOverview);
        monthlyScrollPane.setFitToWidth(true);
        monthlyScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        monthlyScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        monthlyScrollPane.getStyleClass().add(Styles.DENSE);

        analyticsContentPane = new StackPane(habitsScrollPane, monthlyScrollPane);
        monthlyScrollPane.setVisible(false);
        monthlyScrollPane.setManaged(false);

        viewModeSelector = new ComboBox<>();
        viewModeSelector.getItems().addAll("Por Hábitos", "Hábitos de Este Mes");
        viewModeSelector.setValue("Por Hábitos");
        viewModeSelector.getStyleClass().addAll(Styles.DENSE);
        viewModeSelector.setOnAction(e -> {
            boolean esMensual = "Hábitos de Este Mes".equals(viewModeSelector.getValue());
            habitsScrollPane.setVisible(!esMensual);
            habitsScrollPane.setManaged(!esMensual);
            monthlyScrollPane.setVisible(esMensual);
            monthlyScrollPane.setManaged(esMensual);
            if (esMensual) {
                monthlyOverview.refresh();
            }
        });

        // 2. Construcción de los paneles
        Node hudNode = buildHudPanel();
        Node analyticsNode = buildAnalyticsPanel();

        rootSplitPane.getItems().addAll(hudNode, analyticsNode);
        rootSplitPane.setDividerPositions(0.33);

        modalOverlay = new StackPane();
        modalOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.72);");
        modalOverlay.setAlignment(Pos.CENTER);
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);

        rootStackPane = new StackPane(rootSplitPane, modalOverlay);

        // 3. Cargar datos reales de la persistencia para el día de hoy y analítica
        refreshTodayHabits();
        refreshAnalytics();
    }

    public Parent getView() {
        return rootStackPane;
    }

    public void showModal(Node modalContent) {
        modalOverlay.getChildren().setAll(modalContent);
        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);
    }

    public void closeModal() {
        modalOverlay.getChildren().clear();
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);
    }

    private Node buildHudPanel() {
        BorderPane hudLayout = new BorderPane();
        hudLayout.setPadding(new Insets(16, 16, 16, 16));

        // Encabezado del HUD: fecha de hoy y acceso a archivados
        VBox headerBox = new VBox(4);
        headerBox.setPadding(new Insets(0, 0, 12, 0));

        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.of("es", "ES"));
        String dateText = today.format(formatter);
        dateText = Character.toUpperCase(dateText.charAt(0)) + dateText.substring(1);

        Label dateLabel = new Label(dateText);
        dateLabel.getStyleClass().add("hud-date-label");

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Hábitos de Hoy");
        titleLabel.getStyleClass().add("hud-main-title");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button archivadosBtn = new Button("Archivados");
        archivadosBtn.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SMALL);
        archivadosBtn.setTooltip(new Tooltip("Ver y gestionar hábitos archivados"));
        archivadosBtn.setOnAction(e -> openArchivedHabitsDialog());

        titleRow.getChildren().addAll(titleLabel, archivadosBtn);

        headerBox.getChildren().addAll(dateLabel, titleRow, new Separator());
        hudLayout.setTop(headerBox);

        // Contenedor scrolleable central
        VBox listContainer = new VBox(14);
        listContainer.setPadding(new Insets(8, 4, 8, 4));

        Label pendingTitle = new Label("Por completar");
        pendingTitle.getStyleClass().add("section-subtitle");

        listContainer.getChildren().addAll(pendingTitle, pendingContainer, emptyHudLabel, completedSection);

        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add(Styles.DENSE);

        hudLayout.setCenter(scrollPane);

        // Botón Flotante de Acción (+) en esquina inferior derecha
        SVGPath plusIcon = new SVGPath();
        plusIcon.setContent("M 0 6 H 12 M 6 0 V 12");
        plusIcon.getStyleClass().add("fab-icon");

        Button fabButton = new Button();
        fabButton.setGraphic(plusIcon);
        fabButton.getStyleClass().add("fab-button");
        fabButton.setTooltip(new Tooltip("Crear nuevo hábito"));
        fabButton.setOnAction(event -> openCreateHabitDialog());

        StackPane hudStack = new StackPane();
        hudStack.getStyleClass().add("hud-panel");
        hudStack.getChildren().addAll(hudLayout, fabButton);
        StackPane.setAlignment(fabButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(fabButton, new Insets(0, 24, 24, 0));
        hudStack.setMinWidth(300);

        return hudStack;
    }

    private Node buildAnalyticsPanel() {
        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("analytics-panel");
        layout.setPadding(new Insets(16, 20, 16, 20));

        // Encabezado
        VBox headerBox = new VBox(8);
        headerBox.setPadding(new Insets(0, 0, 12, 0));

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleCol = new VBox(2);
        Label titleLabel = new Label("Analítica y Rendimiento");
        titleLabel.getStyleClass().add("analytics-header-title");

        Label descLabel = new Label("Visualización por objetivos semanales o función de cumplimiento mensual");
        descLabel.getStyleClass().add("analytics-header-desc");
        titleCol.getChildren().addAll(titleLabel, descLabel);
        HBox.setHgrow(titleCol, Priority.ALWAYS);

        HBox selectorBox = new HBox(8);
        selectorBox.setAlignment(Pos.CENTER_RIGHT);
        Label selectorLabel = new Label("Modo:");
        selectorLabel.getStyleClass().addAll(Styles.TEXT_BOLD, Styles.TEXT_MUTED);
        selectorBox.getChildren().addAll(selectorLabel, viewModeSelector);

        titleRow.getChildren().addAll(titleCol, selectorBox);

        headerBox.getChildren().addAll(titleRow, new Separator());
        layout.setTop(headerBox);

        layout.setCenter(analyticsContentPane);
        return layout;
    }

    /**
     * Recalcula y actualiza las métricas y tarjetas analíticas de todos los hábitos activos.
     */
    public void refreshAnalytics() {
        LocalDate hoy = LocalDate.now();
        List<Habit> habitosActivos = habitService.getHabitosActivos();
        Map<String, StreakInfo> rachas = streakService.calcularRachasParaTodosLosHabitos(hoy);

        analyticsCardsContainer.getChildren().clear();

        if (habitosActivos.isEmpty()) {
            emptyAnalyticsLabel.setVisible(true);
            emptyAnalyticsLabel.setManaged(true);
        } else {
            emptyAnalyticsLabel.setVisible(false);
            emptyAnalyticsLabel.setManaged(false);

            for (Habit habit : habitosActivos) {
                StreakInfo streakInfo = rachas.get(habit.id());
                if (streakInfo == null) {
                    streakInfo = streakService.calcularRacha(habit.id(), hoy);
                }

                VBox card = new VBox(10);
                card.getStyleClass().add("analytics-card");

                // Fila superior: Título, categoría, meta y acciones directas
                HBox topRow = new HBox(8);
                topRow.setAlignment(Pos.CENTER_LEFT);

                Label nameLabel = new Label(habit.nombre());
                nameLabel.getStyleClass().add("analytics-card-title");
                HBox.setHgrow(nameLabel, Priority.ALWAYS);

                Label catLabel = new Label(habit.categoria());
                catLabel.getStyleClass().add("analytics-card-category");

                Button editCardBtn = new Button("Editar");
                editCardBtn.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SMALL);
                editCardBtn.setTooltip(new Tooltip("Editar configuración de \"" + habit.nombre() + "\""));
                editCardBtn.setOnAction(e -> openEditHabitDialog(habit));

                Button archiveCardBtn = new Button("Archivar");
                archiveCardBtn.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SMALL);
                archiveCardBtn.setTooltip(new Tooltip("Archivar \"" + habit.nombre() + "\""));
                archiveCardBtn.setOnAction(e -> confirmarYArchivar(habit));

                topRow.getChildren().addAll(nameLabel, catLabel, editCardBtn, archiveCardBtn);

                Label goalLabel = new Label(String.format("Meta semanal mínima: %d de %d días asignados",
                        habit.objetivoSemanal(), habit.diasAparicion().size()));
                goalLabel.getStyleClass().add("analytics-card-goal");

                // Componentes especializados: CycleProgress y StreakCanvas
                CycleProgress cycleProgress = new CycleProgress();
                cycleProgress.update(streakInfo, hoy);

                StreakCanvas streakCanvas = new StreakCanvas();
                streakCanvas.update(streakInfo);

                card.getChildren().addAll(
                        topRow,
                        goalLabel,
                        new Separator(),
                        cycleProgress,
                        new Separator(),
                        streakCanvas
                );

                analyticsCardsContainer.getChildren().add(card);
            }
        }

        // Sincronizar también la vista analítica mensual general
        if (monthlyOverview != null) {
            monthlyOverview.refresh();
        }
    }

    /**
     * Carga y sincroniza los hábitos correspondientes al día de hoy desde la persistencia real.
     */
    public void refreshTodayHabits() {
        LocalDate hoy = LocalDate.now();
        List<Habit> habitosHoy = habitService.getHabitosParaFecha(hoy);

        pendingContainer.getChildren().clear();
        completedSection.clear();
        cellRegistry.clear();

        if (habitosHoy.isEmpty()) {
            emptyHudLabel.setVisible(true);
            emptyHudLabel.setManaged(true);
            return;
        } else {
            emptyHudLabel.setVisible(false);
            emptyHudLabel.setManaged(false);
        }

        for (Habit habit : habitosHoy) {
            boolean isCumplido = habitService.isHabitoCumplidoHoy(habit.id());

            HabitItemCell cell = new HabitItemCell(
                    habit,
                    isCumplido,
                    completed -> handleStatusChanged(habit.id(), completed),
                    this::handlePartialProgressRequested,
                    this::openEditHabitDialog,
                    this::confirmarYArchivar
            );

            cellRegistry.put(habit.id(), cell);

            if (isCumplido) {
                completedSection.addCell(cell);
            } else {
                pendingContainer.getChildren().add(cell);
            }
        }
    }

    private void handleStatusChanged(String habitId, boolean completed) {
        LocalDate hoy = LocalDate.now();
        HabitItemCell cell = cellRegistry.get(habitId);
        if (cell == null) return;

        if (completed) {
            habitService.registrarCumplido(habitId, hoy);
            pendingContainer.getChildren().remove(cell);
            completedSection.addCell(cell);
        } else {
            habitService.eliminarRegistro(habitId, hoy);
            completedSection.removeCell(cell);
            pendingContainer.getChildren().add(cell);
        }
        refreshAnalytics();
    }

    private void handlePartialProgressRequested(Habit habit) {
        Optional<DailyRecord> actual = habitService.getRegistroDiario(habit.id(), LocalDate.now());
        Double currentVal = actual.map(DailyRecord::valorAlcanzado).orElse(null);

        PartialProgressModal modal = new PartialProgressModal(
                habit,
                currentVal,
                val -> {
                    habitService.registrarAvanceParcial(habit.id(), LocalDate.now(), val);
                    closeModal();
                    refreshAnalytics();
                    showInfoAlert(
                            "Progreso Registrado",
                            "Se asentó un alcance de " + val + " para \"" + habit.nombre() + "\".\n" +
                            "Según la regla de negocio, el registro del día se computa como Fallado con evidencia de avance."
                    );
                },
                this::closeModal
        );
        showModal(modal);
    }

    private void openCreateHabitDialog() {
        CreateHabitModal modal = new CreateHabitModal(
                nuevoHabit -> {
                    habitService.crearHabito(
                            nuevoHabit.nombre(),
                            nuevoHabit.descripcion(),
                            nuevoHabit.categoria(),
                            nuevoHabit.diasAparicion(),
                            nuevoHabit.objetivoSemanal()
                    );
                    closeModal();
                    refreshTodayHabits();
                    refreshAnalytics();
                },
                this::closeModal
        );
        showModal(modal);
    }

    private void openEditHabitDialog(Habit habit) {
        EditHabitModal modal = new EditHabitModal(
                habit,
                habitActualizado -> {
                    habitService.actualizarHabito(habitActualizado);
                    closeModal();
                    refreshTodayHabits();
                    refreshAnalytics();
                },
                this::closeModal
        );
        showModal(modal);
    }

    private void confirmarYArchivar(Habit habit) {
        ConfirmModal modal = new ConfirmModal(
                "Archivar Hábito",
                "¿Archivar \"" + habit.nombre() + "\"?\n\n" +
                "El hábito dejará de mostrarse en el HUD diario y en el panel de analítica activa.\n" +
                "Todo su historial de registros se conservará intacto y podrás restaurarlo cuando desees desde el botón 'Archivados'.",
                "Sí, archivar",
                false,
                () -> {
                    habitService.archivarHabito(habit.id());
                    closeModal();
                    refreshTodayHabits();
                    refreshAnalytics();
                },
                this::closeModal
        );
        showModal(modal);
    }

    private void openArchivedHabitsDialog() {
        ArchivedHabitsModal modal = new ArchivedHabitsModal(
                habitService,
                () -> {
                    refreshTodayHabits();
                    refreshAnalytics();
                },
                this::showModal,
                this::closeModal
        );
        showModal(modal);
    }

    private void showInfoAlert(String title, String message) {
        NotificationModal modal = new NotificationModal(title, message, false, this::closeModal);
        showModal(modal);
    }

    private void showWarningAlert(String message) {
        NotificationModal modal = new NotificationModal("Atención", message, true, this::closeModal);
        showModal(modal);
    }
}
