# Seguimiento del Proyecto — Tracker de Hábitos

## Estado al 2026-10-11

### 1. Completado hasta la fecha
* **Entorno y Tooling:**
  * Instalado y configurado **Eclipse Temurin JDK 21** y **Apache Maven 3.9.9** en el sistema (`PATH` y `JAVA_HOME`).
  * Compilación y ejecución de tests validadas con Maven.
  * Arnés de ingeniería `rsc-harness` configurado para **Gemini** con 32 skills (incluyendo `java` y `git-workflow`) y guía de uso en [usoDelHarness.md](./wiki/usoDelHarness.md).
* **Configuración del Proyecto:**
  * Generado [pom.xml](../pom.xml) con Java 21, JavaFX 21, tema AtlantaFX (2.0.1), Jackson (con JSR310) y JUnit 5.
* **Capa de Modelo de Dominio (`model/`):**
  * [`Habit.java`](../src/main/java/model/Habit.java): Record inmutable con validación de invariantes de negocio ($1 \le \text{objetivoSemanal} \le |\text{diasAparicion}|$) y patrón *wither*.
  * [`DailyRecord.java`](../src/main/java/model/DailyRecord.java): Record inmutable con evaluación binaria estricta, avance parcial opcional y fábricas estáticas.
  * [`AppData.java`](../src/main/java/model/AppData.java): Record contenedor raíz para Jackson con transformaciones inmutables y control de última auditoría.
* **Capa de Persistencia Local (`repository/`):**
  * [`JsonHabitRepository.java`](../src/main/java/repository/JsonHabitRepository.java): Persistencia atómica local en `data/habits_data.json` con Jackson Databind, JSR-310, manejo de Día 0 y tolerancia a fallos.
  * [`JsonHabitRepositoryTest.java`](../src/test/java/repository/JsonHabitRepositoryTest.java): Suite de pruebas unitarias cubriendo Día 0, archivo vacío, serialización/deserialización completa con avances parciales y escrituras atómicas (5/5 tests superados).
* **Capa de Interfaz de Usuario — HUD y Scaffold (`ui/`):**
  * [`HabitItemCell.java`](../src/main/java/ui/hud/HabitItemCell.java): Celda minimalista (`[ ]` / `[x]` + nombre) con estilo tachado al completarse y botón sutil en hover (`✎`) para registrar avance parcial.
  * [`CompletedSection.java`](../src/main/java/ui/hud/CompletedSection.java): Contenedor colapsable retraído por defecto con conteo dinámico (`Completados (N)`).
  * [`MainController.java`](../src/main/java/ui/MainController.java): Layout con `SplitPane` (1/3 lateral izquierdo para HUD, 2/3 derecho para analítica), scroll para tareas del día y botón flotante de acción `+` (FAB) en esquina inferior derecha.
  * [`HabitTrackerApp.java`](../src/main/java/ui/HabitTrackerApp.java): Inicializador `Application` con tema `PrimerDark` de AtlantaFX y dimensiones de ventana fijadas en `1100 x 720` px (mínimo `960 x 600` px).
  * [`custom.css`](../src/main/resources/styles/custom.css): Sistema completo de diseño y paleta de colores personalizada (Jet Black `#1C2826`, Light Cyan `#E0FBFC`, Dark Cyan `#048A81`, Charcoal `#505168` e Inferno `#A50104`).
* **Capa de Servicios de Negocio (`service/`):**
  * [`HabitService.java`](../src/main/java/service/HabitService.java): ABM del catálogo, ciclo de vida estricto (activo, archivado, restauración y eliminación física en cascada) y registro diario binario con soporte de alcance parcial.
  * [`HabitServiceTest.java`](../src/test/java/service/HabitServiceTest.java): Suite de pruebas unitarias para `HabitService` (8/8 tests superados).
  * [`StreakService.java`](../src/main/java/service/StreakService.java) y [`StreakInfo.java`](../src/main/java/model/StreakInfo.java): Auditoría anti-trampa con sellado automático retroactivo, cálculo de rachas en semanas calendario ISO-8601 (Lunes a Domingo), avance en tiempo real de la semana en curso y récords históricos.
  * [`StreakServiceTest.java`](../src/test/java/service/StreakServiceTest.java): Suite de pruebas unitarias para `StreakService` (6/6 tests superados).
* **Integración del HUD y Diálogos Interactivos (`ui/`):**
  * [`CreateHabitDialog.java`](../src/main/java/ui/hud/CreateHabitDialog.java): Diálogo modal para alta de hábitos con selector de días estilo alarma y control dinámico del objetivo semanal mínimo ($1 \le \text{objetivo} \le \text{dias}$).
  * [`EditHabitDialog.java`](../src/main/java/ui/hud/EditHabitDialog.java): Diálogo modal para modificar hábitos existentes preservando su ID e historial de cumplimiento.
  * [`ArchivedHabitsDialog.java`](../src/main/java/ui/hud/ArchivedHabitsDialog.java): Gestión de hábitos archivados con opciones de restauración y eliminación física definitiva en cascada con botón `Inferno`.
  * [`HabitItemCell.java`](../src/main/java/ui/hud/HabitItemCell.java): Celda del HUD con soporte dual de acciones (botones sutiles en hover `✎`, `✏`, `📥` y menú contextual de clic derecho).
  * [`MainController.java`](../src/main/java/ui/MainController.java): Conexión completa con `HabitService` y `StreakService`, auditoría anti-trampa, persistencia al tildar/destildar, avance parcial cuantitativo, edición en vivo y archivado inmediato con confirmación tanto desde el HUD como desde las tarjetas de analítica.
* **Capa de Analítica y Visualización de Rachas (2/3 de pantalla) (`ui/analytics/`):**
  * [`CycleProgress.java`](../src/main/java/ui/analytics/CycleProgress.java): Componente visual que audita en tiempo real el ciclo semanal en curso (Lunes a Domingo), calculando si el objetivo mínimo está cumplido, en progreso o en riesgo matemático según los días restantes de la semana.
  * [`StreakCanvas.java`](../src/main/java/ui/analytics/StreakCanvas.java): Línea de tiempo de bloques semanales discretos (`#048A81` para semanas cumplidas, quiebre a 0 en `#A50104` para falladas y bloque en curso) junto a las métricas de racha activa y récord histórico.
  * [`MonthlyOverview.java`](../src/main/java/ui/analytics/MonthlyOverview.java): Vista general mensual ("Hábitos de Este Mes") con función de frecuencia $f(\text{día}) = \text{hábitos completados}$, gráfico de barras `BarChart` con eje Y acotado estrictamente al total de hábitos registrados $[0, N]$, tarjetas KPI y tabla de desglose diario `TableView` en formato fraccionario ($y / N$).
  * [`MainController.java`](../src/main/java/ui/MainController.java): Selector desplegable en cabecera para conmutar dinámicamente entre `"Por Hábitos"` y `"Hábitos de Este Mes"` con actualización reactiva instantánea y sincronización de escala.
* **Capa de Seguridad y Pantalla de Bloqueo (`auth/` y `service/`):**
  * [`AppData.java`](../src/main/java/model/AppData.java): Integración de `passwordHash` y `passwordSalt` con constructores inmutables y soporte Día 0.
  * [`AuthService.java`](../src/main/java/service/AuthService.java): Algoritmo de derivación de claves PBKDF2 (`PBKDF2WithHmacSHA256`, 65.536 iteraciones, salt criptográfico de 16 bytes), validación con `MessageDigest.isEqual` y restablecimiento atómico a Día 0.
  * [`AuthServiceTest.java`](../src/test/java/service/AuthServiceTest.java): Suite de pruebas unitarias para el servicio de autenticación y reseteo destructivo (5/5 tests superados).
  * [`LockScreenView.java`](../src/main/java/ui/auth/LockScreenView.java): Interfaz visual de autenticación obligatoria con conmutación entre modo configuración ("Configurar Contraseña") y modo desbloqueo ("Ingresar Contraseña"), incluyendo modal de confirmación irreversible.
  * [`HabitTrackerApp.java`](../src/main/java/ui/HabitTrackerApp.java): Enrutador dinámico de escenas JavaFX que protege y bloquea la interfaz hasta validar credenciales.
* **Modernización Visual, Marco de Ventana y Modales In-App (`ui/components/`, `ui/modal/`):**
  * [`HabitTrackerApp.java`](../src/main/java/ui/HabitTrackerApp.java): Configuración de ventana `StageStyle.UNDECORATED` para erradicar las barras de título blancas de Windows, asociación de la barra de título superior personalizada y anclaje de `WindowResizeHelper`.
  * [`AppTitleBar.java`](../src/main/java/ui/components/AppTitleBar.java): Barra de título superior con estilo Jet Black oscuro (`#16201e`), ícono institucional de la aplicación (`app_icon.png`), título "Tracker de Hábitos", soporte de arrastre (`mouseDragged`), doble clic para maximizar/restaurar y botones oscuros de control de ventana (`—`, `□`, `✕`).
  * [`WindowResizeHelper.java`](../src/main/java/ui/components/WindowResizeHelper.java): Manejador perimetral de cursor que habilita el redimensionamiento suave y responsivo en las 8 direcciones cardinales e intercardinales en ventanas sin marco nativo.
  * [`ModalCard.java`](../src/main/java/ui/modal/ModalCard.java): Tarjeta base elevada para la presentación de modales in-app.
  * Modales in-app superpuestos: [`CreateHabitModal.java`](../src/main/java/ui/modal/CreateHabitModal.java), [`EditHabitModal.java`](../src/main/java/ui/modal/EditHabitModal.java), [`ArchivedHabitsModal.java`](../src/main/java/ui/modal/ArchivedHabitsModal.java), [`ConfirmModal.java`](../src/main/java/ui/modal/ConfirmModal.java), [`PartialProgressModal.java`](../src/main/java/ui/modal/PartialProgressModal.java) y [`NotificationModal.java`](../src/main/java/ui/modal/NotificationModal.java).
  * [`MainController.java`](../src/main/java/ui/MainController.java): Capa `modalOverlay` integrada con fondo translúcido (`rgba(0, 0, 0, 0.72)`) que reemplaza la totalidad de popups nativos del SO (`Dialog`, `Alert`, `TextInputDialog`), manteniendo el 100% de la experiencia dentro de la ventana de la app.
  * [`app_icon.png`](../src/main/resources/icons/app_icon.png): Ícono institucional 256x256 px asignado al Stage y a la barra de título.
* **Nueva Paleta "Botanical Forest" y Refactorización Clean Code de Estilos (`DEC-021`):**
  * [`custom.css`](../src/main/resources/styles/custom.css): Reestructuración Clean Code completa en 10 bloques modulares aplicando la paleta Botanical Forest (Verde Bosque `#111815`, Musgo `#1A2420`, Verde Bambú `#4EBA6F`, Ámbar Récord `#DDA15E`, Blanco Salvia `#EAF2EC` y Terracota `#E76F51`).
  * Desacoplamiento total de estilos en controladores Java: erradicados al 100% los estilos inline y colores fijos en `AppTitleBar`, `ModalCard`, `LockScreenView`, `CreateHabitModal`, `EditHabitModal`, `ArchivedHabitsModal`, `ConfirmModal`, `PartialProgressModal`, `NotificationModal` y `StreakCanvas`.
  * [`app_icon.png`](../src/main/resources/icons/app_icon.png): Ícono institucional 256x256 px regenerado y armonizado con la paleta Botanical Forest.
* **Catálogo Centralizado y Selector Interactivo de Etiquetas (`DEC-026`):**
  * [`AppData.java`](../src/main/java/model/AppData.java): Persistencia atómica de `List<String> tags` con normalización case-insensitive y retrocompatibilidad.
  * [`HabitService.java`](../src/main/java/service/HabitService.java): Búsqueda reactiva por prefijo, registro y normalización de etiquetas.
  * [`SelectTagModal.java`](../src/main/java/ui/modal/SelectTagModal.java): Modal in-app con buscador en tiempo real, chips interactivos y creación dinámica `+ Crear etiqueta "..."`.
  * [`CreateHabitModal.java`](../src/main/java/ui/modal/CreateHabitModal.java) y [`EditHabitModal.java`](../src/main/java/ui/modal/EditHabitModal.java): Integración del badge de etiqueta con botón de selección.
  * [`MainController.java`](../src/main/java/ui/MainController.java): Soporte para apilamiento de modales en `modalOverlay`.
  * [`custom.css`](../src/main/resources/styles/custom.css): Estilos modulares (Sección 11) con paleta Botanical Forest.
* **Corrección de Desapilado en Submodales In-App (`DEC-027`):**
  * [`ArchivedHabitsModal.java`](../src/main/java/ui/modal/ArchivedHabitsModal.java): Corrección en los callbacks de `ConfirmModal` para desapilar correctamente mediante `closeModal.run()`, eliminando la excepción `IllegalArgumentException: Children: duplicate children added` al pulsar "Eliminar" en hábitos archivados.
* **Línea de Tiempo Semanal Diaria (L a D) en Analítica (`DEC-028`):**
  * [`StreakCanvas.java`](../src/main/java/ui/analytics/StreakCanvas.java): Línea de tiempo semanal fija de 7 días (L, M, X, J, V, S, D) con evaluación diaria reactiva.
  * [`custom.css`](../src/main/resources/styles/custom.css): Nuevas clases `.streak-block-future`, `.streak-block-unassigned` y ajuste de dimensiones y estados activos para "Hoy".
  * [`MainController.java`](../src/main/java/ui/MainController.java): Integración del contexto del hábito y fecha para el cálculo semanal.
* **Documentación y Decisiones:**
  * Sincronizadas las decisiones **[DEC-001]** a **[DEC-028]** en [decisiones.md](./decisiones.md).
  * 27/27 pruebas unitarias automatizadas superadas con éxito sin regresiones.

---

### 2. Tareas pendientes ordenadas por prioridad

1. **Próxima Inmediata — Verificación Integral de Experiencia de Usuario y Empaquetado:**
   - Ejecutar la aplicación completa (`mvn javafx:run`) para validar visualmente el flujo completo: Configurar clave inicial $\rightarrow$ Desbloqueo $\rightarrow$ HUD $\rightarrow$ Analítica $\rightarrow$ Prueba de reseteo destructivo.
   - Validar casos de borde (ej. Día 0 sin hábitos, hábitos recién creados en su primera semana, cambio de días de la semana y cálculo de riesgo semanal).
   - Generar el empaquetado final o script de distribución (`mvn package`).







