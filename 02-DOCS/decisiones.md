# Harness decisions

- Accepted plan `fecab56faa3b1e736246f10331f25fd72e06079cb10eb9ba9729c700e4e8234a`.
- Project kind: software.
- SDD: deferred.

# Registro de Decisiones de Arquitectura y Negocio

Este documento registra las decisiones tomadas durante el desarrollo del proyecto **Tracker de Hábitos**, alineando las reglas funcionales, el diseño de interacción y la arquitectura técnica.

---

### [DEC-001] Stack Tecnológico y Entorno de Ejecución
* **Fecha:** 2026-09-28
* **Estado:** Aprobado
* **Decisión:**
  * **Lenguaje:** Java 21 LTS, utilizando características modernas como `record` para modelos inmutables y `switch` con pattern matching.
  * **Framework UI:** JavaFX 21 complementado con la biblioteca de temas **AtlantaFX** (utilizando `PrimerDark` o `PrimerLight`).
  * **Persistencia:** Local mediante archivo plano JSON (`./data/habits_data.json`) gestionado por **Jackson Databind** (con módulo `jackson-datatype-jsr310` para APIs de fecha/hora de Java 8+).
  * **Construcción y Dependencias:** Apache Maven (`javafx-maven-plugin` para ejecución).

---

### [DEC-002] Criterio de Evaluación y Éxito de Rachas Semanales
* **Fecha:** 2026-09-28
* **Estado:** Aprobado
* **Decisión:**
  * La unidad de consolidación de racha es la semana calendario (Lunes a Domingo, estándar ISO-8601).
  * Una semana se considera **Cumplida** si y solo si la cantidad de días en los que el hábito fue completado durante esa semana es mayor o igual al objetivo semanal configurado:
    $$\text{dias\_cumplidos\_en\_semana} \ge \text{objetivo\_semanal}$$
  * Si la semana finaliza y no se alcanzó el objetivo mínimo, la racha activa del hábito cae a 0.
  * Durante la semana en curso, se muestra en tiempo real el progreso de días cumplidos frente al objetivo.

---

### [DEC-003] Regla Anti-trampa y Sellado Automático de Días No Abiertos
* **Fecha:** 2026-09-28
* **Estado:** Aprobado
* **Decisión:**
  * Queda estrictamente prohibida la edición o registro retroactivo de días anteriores.
  * Al abrir la aplicación, el sistema audita la fecha del último registro y, si el usuario no abrió la app durante uno o varios días intermedios, todos los hábitos asignados a esos días que no cuenten con registro explícito se sellan automáticamente en estado **Fallado** (`cumplido = false`).
  * Las fechas pasadas quedan inmutables una vez selladas.

---

### [DEC-004] Acceso y Creación de Hábitos (HUD y Botón Flotante)
* **Fecha:** 2026-09-28
* **Estado:** Aprobado
* **Decisión:**
  * Para mantener el minimalismo visual del HUD (1/3 de pantalla) sin saturar la barra superior, la acción principal para crear hábitos se ubica como un **botón flotante con ícono `+` (Floating Action Button)** en la esquina inferior derecha de la columna lateral.
  * Al pulsar el botón flotante se despliega el diálogo/modal de alta de hábito (y acceso a la gestión de hábitos archivados).

---

### [DEC-005] Registro de Progreso Parcial Numérico
* **Fecha:** 2026-09-28
* **Estado:** Aprobado
* **Decisión:**
  * Cuando un usuario no completa la meta total diaria pero desea asentar su progreso (ej. leyó 20 de 30 páginas), se permite registrar de forma opcional un valor numérico alcanzado.
  * El estado formal del día se computa y almacena como **Fallado** (`cumplido = false`), conservando el valor numérico parcial como evidencia de alcance.

---

### [DEC-006] Implementación del Modelo Habit como Record Inmutable
* **Fecha:** 2026-09-29
* **Estado:** Aprobado
* **Decisión:**
  * Se implementa `Habit` como un `record` de Java 21 con inmutabilidad estricta.
  * En el constructor compacto se valida la obligatoriedad del nombre y la regla $1 \le \text{objetivoSemanal} \le |\text{diasAparicion}|$, lanzando `IllegalArgumentException` ante infracciones.
  * Se incluye el patrón *wither* (`withArchivado`) para soportar transiciones de ciclo de vida sin mutar estado.

---

### [DEC-007] Implementación del Modelo DailyRecord como Record Inmutable
* **Fecha:** 2026-09-29
* **Estado:** Aprobado
* **Decisión:**
  * Se implementa `DailyRecord` como un `record` de Java 21 con inmutabilidad.
  * Modela la evaluación binaria estricta (`cumplido = true/false`) y progreso numérico opcional (`valorAlcanzado >= 0`).
  * Incorpora métodos fábrica estáticos (`cumplido`, `fallado`, `parcial`) garantizando que cualquier registro parcial configure indefectiblemente `cumplido = false`.

---

### [DEC-008] Implementación del Modelo AppData como Contenedor Raíz
* **Fecha:** 2026-09-29
* **Estado:** Aprobado
* **Decisión:**
  * Se implementa `AppData` como un `record` inmutable con `habits`, `dailyRecords` y `lastCheckedDate`.
  * La propiedad `lastCheckedDate` permite auditar días no ejecutados para la regla anti-trampa al iniciar la aplicación.
  * Soporta el patrón wither para transformaciones inmutables (`withHabitAdded`, `withHabitUpdated`, `withHabitRemoved`, `withRecordAdded`).

---

### [DEC-009] Definiciones de Diseño de UI, Interacción del HUD y Dimensiones
* **Fecha:** 2026-09-29
* **Estado:** Aprobado
* **Decisión:**
  * **Tema Visual:** Inicialización con tema oscuro (`PrimerDark` de AtlantaFX).
  * **Interacción para Progreso Parcial Cuantitativo:** Se adopta un botón de acción sutil visible al hacer *hover* sobre la fila del hábito en el HUD (Opción B), preservando la vista limpia y minimalista por defecto y desplegando un diálogo de entrada numérica directa cuando el usuario lo solicite.
  * **Arquitectura de Interfaz:** Implementación mediante código Java 100% declarativo, fuertemente tipado y modular (sin archivos `.fxml`), aprovechando directamente la jerarquía de componentes y estilos de AtlantaFX.
  * **Dimensiones de Ventana:** Resolución inicial configurada en `1100 x 720` px, con límites mínimos que aseguren la correcta proporción visual de la división 1/3 (HUD lateral) y 2/3 (panel analítico).

---

### [DEC-010] Sistema de Diseño y Paleta de Colores
* **Fecha:** 2026-09-29
* **Estado:** Aprobado
* **Decisión:**
  * Se establece la paleta cromática personalizada de la aplicación sobrescribiendo las variables raíz de AtlantaFX y estilos en `custom.css`:
    * **Fondo Base:** Jet Black (`#1C2826`).
    * **Tipografía Principal y Alto Contraste:** Light Cyan (`#E0FBFC`).
    * **Acento Primario y Éxito:** Dark Cyan (`#048A81`) para el botón FAB `+`, checkboxes activos y hover acentuado.
    * **Bordes y Secundarios:** Charcoal (`#505168`) para divisores, bordes y texto atenuado (`#8d8fa8`).
    * **Destructivo y Fallado:** Inferno (`#A50104`) para acciones de eliminación y badges del estado computable Fallado.
    * **Superficie Sutil:** `#243330` para tarjetas elevadas y hover en celdas de hábitos.

---

### [DEC-011] Persistencia Atómica Local y Tolerancia a Fallos con Jackson
* **Fecha:** 2026-10-11
* **Estado:** Aprobado
* **Decisión:**
  * Se implementa `JsonHabitRepository` gestionando lectura/escritura atómica sobre `data/habits_data.json`.
  * La escritura se ejecuta mediante volcado en un archivo temporal (`.tmp`) seguido de un movimiento atómico (`ATOMIC_MOVE` con fallback a `REPLACE_EXISTING`), evitando corrupción de datos ante cierres abruptos.
  * Si el archivo no existe o está vacío (Día 0), retorna un estado inicial limpio inmutable (`AppData.empty()`).
  * Se configura `ObjectMapper` con `JavaTimeModule`, deshabilitando timestamps numéricos para garantizar formato ISO-8601 legible en fechas (`LocalDate`) y días de la semana (`DayOfWeek`).

---

### [DEC-012] Implementación de HabitService y Orquestación del Ciclo de Vida
* **Fecha:** 2026-10-11
* **Estado:** Aprobado
* **Decisión:**
  * Se implementa `HabitService` como único punto de entrada para el catálogo de hábitos y registros diarios.
  * Cada operación de negocio orquesta transformaciones funcionales inmutables sobre el record `AppData` y las persiste atómicamente a través de `JsonHabitRepository`.
  * El ciclo de vida de tres estados se modela estrictamente:
    * **Archivado:** Modifica la propiedad booleana `archivado = true` para salir del HUD diario, preservando íntegros todos sus registros históricos.
    * **Restaurado:** Modifica `archivado = false`, reintegrando el hábito al HUD para sus días correspondientes.
    * **Eliminación definitiva:** Remoción física del catálogo y eliminación en cascada de todos los registros diarios asociados (`DailyRecord`), previniendo registros huérfanos.
  * El registro de avance parcial cuantitativo se registra mediante `DailyRecord.parcial()`, asegurando que el estado computable del día quede fijado como `cumplido = false` y preservando el valor numérico alcanzado.

---

### [DEC-013] Implementación de StreakService, Rachas Semanales y Regla Anti-trampa
* **Fecha:** 2026-10-11
* **Estado:** Aprobado
* **Decisión:**
  * Las rachas se evalúan y miden estrictamente en semanas calendario ISO-8601 (Lunes a Domingo), consolidando el criterio:
    $$\text{dias\_cumplidos} \ge \text{objetivo\_semanal}$$
  * Si una semana cerrada no alcanza el objetivo mínimo, la racha activa del hábito cae a 0, preservando el récord histórico máximo (`max_streak`).
  * En la semana en curso se computa el progreso en tiempo real (`diasCumplidosSemanaActual` / `objetivoSemanal`), sumando a la racha activa de forma reactiva si el objetivo ya fue alcanzado antes del cierre de semana.
  * **Regla anti-trampa:** El método `auditarYSellarDiasPasados` audita el intervalo desde `lastCheckedDate` hasta `hoy - 1` y sella automáticamente con `DailyRecord.fallado` cualquier hábito que debió aparecer y no cuente con registro explícito, actualizando `lastCheckedDate`.

---

### [DEC-014] Integración del HUD con Persistencia Real y Diálogos de Gestión
* **Fecha:** 2026-10-11
* **Estado:** Aprobado
* **Decisión:**
  * `MainController` se conecta de forma reactiva a `HabitService` y `StreakService`, ejecutando la auditoría anti-trampa al inicializarse.
  * El botón flotante `+` (FAB) abre `CreateHabitDialog`, que valida los invariantes de frecuencia ($1 \le \text{objetivoSemanal} \le |\text{diasSeleccionados}|$) antes de permitir la creación y refresca el HUD.
  * Se implementa `ArchivedHabitsDialog` accesible desde el encabezado del HUD para restaurar hábitos al estado activo o eliminarlos definitivamente en cascada con su historial.
  * Al marcar/desmarcar celdas en el HUD se actualiza de inmediato el registro en el repositorio y la celda se transfiere automáticamente entre las secciones "Por completar" y "Completados".

---

### [DEC-015] Implementación del Panel Analítico y Visualización de Rachas (2/3 de Pantalla)
* **Fecha:** 2026-10-11
* **Estado:** Aprobado
* **Decisión:**
  * Se implementan los componentes visuales dedicados `CycleProgress` y `StreakCanvas` en el paquete `ui.analytics`.
  * `CycleProgress` audita en tiempo real el ciclo semanal en curso (Lunes a Domingo), calculando si el objetivo mínimo está cumplido, en progreso o en riesgo matemático según los días restantes de la semana.
  * `StreakCanvas` renderiza las métricas clave (racha actual y récord histórico de semanas consecutivas) y una línea de tiempo compuesta por bloques discretos para cada semana calendario evaluada, diferenciando semanas cumplidas (`#048A81`), semanas falladas con quiebre de racha (`#A50104`) y la semana activa.
  * `MainController` reemplaza el placeholder analítico por un contenedor scrolleable de tarjetas analíticas para todos los hábitos activos.
  * Se establece sincronización reactiva inmediata (`refreshAnalytics()`): cada cambio de estado en el HUD (marcar/desmarcar, avance cuantitativo parcial, alta de hábito o restauración/archivo) actualiza instantáneamente el panel analítico.

---

### [DEC-016] Modificación y Archivado de Hábitos desde HUD y Analítica
* **Fecha:** 2026-10-11
* **Estado:** Aprobado
* **Decisión:**
  * **Modificación de Hábitos:** Se crea `EditHabitDialog` precargando los valores existentes del hábito (`nombre`, `descripcion`, `categoria`, `diasAparicion` y `objetivoSemanal`), preservando de forma inmutable su `id`, `archivado` y `createdAt`. La persistencia se efectúa a través de `habitService.actualizarHabito(habit)`.
  * **Archivado de Hábitos:** Se implementa confirmación modal antes de invocar `habitService.archivarHabito(id)`. El hábito sale del HUD diario y del panel analítico activo, conservando intacto su historial de registros y permitiendo su restauración o borrado definitivo en `ArchivedHabitsDialog`.
  * **Puntos de Interacción Duales:**
    1. En el HUD (`HabitItemCell`): Clic derecho abre `ContextMenu` con opciones (*"Editar hábito..."*, *"Archivar hábito..."*, *"Registrar avance parcial..."*); adicionalmente, al hacer *hover* sobre la fila se despliegan botones sutiles (`✏` para editar y `📥` para archivar).
    2. En el Panel Analítico (`MainController`): Cada tarjeta de hábito incluye botones directos *"Editar"* y *"Archivar"* en su encabezado.
  * Tras cada operación de edición o archivo, se disparan sincrónicamente `refreshTodayHabits()` y `refreshAnalytics()`.

---

### [DEC-017] Selector de Vistas y Módulo de Analítica Mensual General
* **Fecha:** 2026-10-11
* **Estado:** Aprobado
* **Decisión:**
  * **Selector de Vistas:** Se incorpora en el encabezado del panel derecho (2/3 de pantalla) un control desplegable (`ComboBox`) que permite alternar dinámicamente entre dos modos de visualización:
    1. *"Por Hábitos"*: Tarjetas individuales con progreso de ciclo semanal (`CycleProgress`) y línea de tiempo de bloques de rachas (`StreakCanvas`).
    2. *"Hábitos de Este Mes"*: Vista general mensual agregada (`MonthlyOverview`).
  * **Modelo de Datos Mensual:** Se introduce el record inmutable `DayCompletion` y el método `habitService.getCumplimientosMes(YearMonth)` para calcular la función:
    $$f(\text{día}) = \text{cantidad de hábitos completados en esa jornada}$$
  * **Componente `MonthlyOverview`:**
    * **Navegación:** Botones para navegar meses anteriores/posteriores y volver al mes en curso.
    * **Métricas KPI:** Total acumulado del mes ($\sum f(d)$), promedio diario y día récord de cumplimiento.
    * **Gráfico de Barras ($f(d)$):** Implementado con `BarChart<String, Number>` estilizado en Dark Cyan (`#048A81`) con tooltips interactivos que detallan los hábitos cumplidos por fecha.
    * **Tabla de Valores:** `TableView<DayCompletion>` con columnas estructuradas (`Día`, `f(día)` y `Hábitos Realizados`).
  * **Sincronización:** Ambas vistas se mantienen actualizadas reactivamente ante cualquier cambio de estado en el HUD diario.

---

### [DEC-018] Acotamiento Estricto del Eje Y al Catálogo de Hábitos en la Analítica Mensual
* **Fecha:** 2026-10-11
* **Estado:** Aprobado
* **Decisión:**
  * **Eje Y Acotado ($[0, N]$):** El eje de ordenadas (`NumberAxis yAxis`) del componente [MonthlyOverview](../src/main/java/ui/analytics/MonthlyOverview.java) se fija con `autoRanging = false`, teniendo como límite superior el total de hábitos registrados y activos en el sistema ($N = \max(1, |\text{hábitos activos}|)$).
  * **Fidelidad de Escala:** Se evita la distorsión del auto-escalado donde un solo hábito completado ocupaba el 100% de la altura visual. Con el tope en $N$, cada barra representa de manera proporcional el porcentaje de la meta global cumplida ese día ($y / N$).
  * **Discretización Entera:** Los ticks se configuran estrictamente en números enteros (`tickUnit = 1`, `minorTickCount = 0`), eliminando divisiones decimales irreales.
  * **Sincronización en Tabla:** La columna de frecuencia en la `TableView` refleja el formato fraccionario exacto (`cumplidos / N`) y ajusta dinámicamente su encabezado (`f(día) [de N]`).
  * **Reactividad:** [MainController](../src/main/java/ui/MainController.java) inyecta el proveedor dinámico `() -> habitService.getHabitosActivos().size()`, recalculando el tope automáticamente ante creaciones, modificaciones o archivados de hábitos.

---

### [DEC-019] Pantalla de Bloqueo, Autenticación Local con PBKDF2 y Reseteo por Olvido
* **Fecha:** 2026-10-12
* **Estado:** Aprobado
* **Decisión:**
  * **Pantalla de Bloqueo Inicial:** Al arrancar la aplicación (`HabitTrackerApp`), se comprueba si existe una contraseña configurada antes de montar la vista principal (`MainController`).
  * **Algoritmo Criptográfico:** Se utiliza derivación de claves mediante `PBKDF2WithHmacSHA256` nativo de Java (`javax.crypto.SecretKeyFactory`) con 65.536 iteraciones y salt criptográfico aleatorio de 16 bytes (`SecureRandom`). La comparación de hashes se efectúa con `MessageDigest.isEqual` para prevenir ataques de temporización (*timing attacks*).
  * **Persistencia Integrada en AppData:** Los campos `passwordHash` y `passwordSalt` residen dentro del record inmutable `AppData`, manteniéndose en `data/habits_data.json`.
  * **Estados de la Pantalla de Autenticación (`LockScreenView`):**
    * *Modo Setup ("Configurar Contraseña"):* Se activa si `passwordHash` es nulo/vacío (Día 0 o tras reseteo). Solicita ingreso y confirmación de contraseña.
    * *Modo Desbloqueo ("Ingresar Contraseña"):* Solicita la clave actual para habilitar el HUD y la analítica.
  * **Política de Cero Recuperación y Borrado Total:** Ante olvido de contraseña, no existen mecanismos de recuperación. La acción "Olvidé mi contraseña / Restablecer aplicación" exige confirmación modal y ejecuta el restablecimiento atómico de `habits_data.json` al estado de fábrica (`AppData.empty()`), borrando el catálogo, registros históricos y credenciales, regresando al Modo Setup.

---

### [DEC-020] Barra de Título Oscura Integrada, Modales In-App Superpuestos e Ícono de Aplicación
* **Fecha:** 2026-10-12
* **Estado:** Aprobado
* **Decisión:**
  * **Ventana sin Marco Nativo (`StageStyle.UNDECORATED`):** Se elimina la barra de título blanca nativa de Windows en el `primaryStage`, logrando una estética visual homogénea, moderna e inmersiva integrada con el tema oscuro de la aplicación.
  * **Barra de Título Oscura Integrada (`AppTitleBar`):** Componente visual superior (`HBox`) con estilo Jet Black oscuro (`#16201e`), borde inferior Charcoal (`#505168`), ícono gráfico, título "Tracker de Hábitos", zona de arrastre con mouse (`mouseDragged`), atajo de maximizar/restaurar por doble clic, y botones de control personalizados (minimizar `—`, maximizar/restaurar `□` y cerrar `✕` con hover destructivo `#A50104`).
  * **Redimensionamiento Perimetral Suave (`WindowResizeHelper`):** Manejador perimetral de eventos de cursor en un umbral de 7 píxeles que restaura el redimensionamiento fluido del `Stage` en sus 8 direcciones cardinales e intercardinales (`N`, `S`, `E`, `W`, `NW`, `NE`, `SW`, `SE`).
  * **Modales In-App Superpuestos (`ui.modal.*`):** Reemplazo absoluto de los diálogos nativos del sistema operativo (`Dialog`, `Alert`, `TextInputDialog`) por una capa de superposición in-app (`modalOverlay`) dentro de un `StackPane` raíz con telón translúcido (`rgba(0, 0, 0, 0.72)`). Todo el flujo de interacción ocurre dentro de la misma ventana con tarjetas modales elevadas (`ModalCard`) que respetan rigurosamente la paleta y tipografía del sistema:
    * `CreateHabitModal`: Alta de hábitos con selección de días L-D y spinner de meta acotado.
    * `EditHabitModal`: Modificación de configuración con precarga de parámetros existentes.
    * `ArchivedHabitsModal`: Inspección, restauración y eliminación definitiva con advertencia en cascada.
    * `ConfirmModal`: Diálogo de confirmación para archivado y operaciones irreversibles.
    * `PartialProgressModal`: Entrada de progreso numérico diario sin romper el encapsulamiento.
    * `NotificationModal`: Avisos de retroalimentación e informativos sin recurrir a alertas del SO.
  * **Identidad Visual e Ícono:** Incorporación de un ícono de alta resolución (`/icons/app_icon.png`, 256x256 px) con isotipo de racha cíclica en Dark Cyan (`#048A81`) y verificación en Light Cyan (`#E0FBFC`), asignado tanto a la barra de título como a la barra de tareas del sistema operativo.

---

### [DEC-021] Paleta Botanical Forest y Refactorización Clean Code de Estilos
* **Fecha:** 2026-10-13
* **Estado:** Aprobado
* **Decisión:**
  * **Nueva Paleta Cromática "Botanical Forest":** Se adopta una estética orgánica de bienestar para reforzar la constancia sin fatiga visual:
    * *Base / Fondo Principal:* `#111815` (Verde bosque profundo).
    * *Superficies Elevadas / Tarjetas:* `#1A2420` (Musgo sutil).
    * *Barra de Título e Insets:* `#0C1210` (Sombra de bosque).
    * *Acento Principal y Hábitos Cumplidos:* `#4EBA6F` (Verde bambú fresco).
    * *Acento Secundario y Récord Histórico:* `#DDA15E` (Ámbar cálido / arcilla dorada).
    * *Bordes y Divisores:* `#2C3C35` (Gris bosque).
    * *Tipografía Principal:* `#EAF2EC` (Blanco salvia suave, contraste WCAG AAA).
    * *Tipografía Secundaria:* `#8DA396` (Gris vegetal).
    * *Estado Peligro / Fallado:* `#E76F51` (Terracota anaranjado).
  * **Refactorización Clean Code de `custom.css`:** Reorganización modular integral en 10 bloques semánticos (Tokens de Diseño, Shell, Barra de Título, HUD, Modales In-App, Autenticación, Analítica, Rachas, Vista Mensual y Componentes Globales).
  * **Separación de Responsabilidades Absoluta (Cero Colores Hardcodeados en Java):** Se erradicaron todas las cadenas de estilos inline con colores fijos en los controladores y vistas Java (`ModalCard`, `AppTitleBar`, `LockScreenView`, `CreateHabitModal`, `EditHabitModal`, `ArchivedHabitsModal`, `ConfirmModal`, `PartialProgressModal`, `NotificationModal`, `StreakCanvas`), asignando clases CSS semánticas para que el 100% del estilo dependa exclusivamente de `custom.css`.
  * **Regeneración del Ícono Institucional:** Se actualizó el recurso gráfico de alta definición [`app_icon.png`](../src/main/resources/icons/app_icon.png) sincronizándolo con los colores de la paleta Botanical Forest.

---

### [DEC-022] Corrección y Refinamiento Ergonómico de la UI (Íconos SVG, Redimensionamiento y Modales Adaptativos)
* **Fecha:** 2026-10-13
* **Estado:** Aprobado
* **Decisión:**
  * **Íconos Vectoriales SVG en Barra de Título (`AppTitleBar`):** Sustitución de los glifos de texto Unicode por trazados vectoriales geométricos `SVGPath` de alta definición (línea horizontal para minimizar, recuadro de trazo nítido para maximizar, doble recuadro solapado para restaurar y cruz simétrica para cerrar). Se implementó un observador reactivo sobre `stage.maximizedProperty()` para asegurar la coherencia del ícono ante maximizado por doble clic o atajos del sistema.
  * **Captura Temprana de Redimensionamiento (`WindowResizeHelper`):** Migración del esquema de eventos a `EventFilter` en la escena JavaFX (fase de captura) con margen perimetral aumentado a 8 px. Al activarse el arrastre en los bordes, el evento se consume inmediatamente (`event.consume()`), erradicando interferencias con el arrastre de `AppTitleBar` y los paneles internos del `SplitPane`.
  * **Ajuste Intrínseco de Altura en Modales (`ModalCard`):** Asignación explícita de `setMaxHeight(Region.USE_PREF_SIZE)` en `ModalCard` para evitar la expansión vertical involuntaria en el `StackPane` de superposición (`modalOverlay`). Las tarjetas modales in-app ahora ajustan su altura de manera estricta y compacta a su contenido, permaneciendo centradas en pantalla.

---

### [DEC-023] Maximizado Respetando la Barra de Tareas de Windows (VisualBounds)
* **Fecha:** 2026-10-13
* **Estado:** Aprobado
* **Decisión:**
  * **Uso de `VisualBounds` en Reemplazo de Pantalla Completa:** Al utilizar ventanas sin marco nativo (`StageStyle.UNDECORATED`), el método tradicional `stage.setMaximized(true)` cubría por completo la barra de tareas de Windows. Se implementó una lógica de maximizado personalizado en `AppTitleBar` que consulta los límites útiles de la pantalla mediante `Screen.getScreensForRectangle(...).get(0).getVisualBounds()`.
  * **Preservación y Restauración de Dimensiones:** El sistema almacena las coordenadas y dimensiones previas (`prevX`, `prevY`, `prevWidth`, `prevHeight`) antes de maximizar, permitiendo restaurar la ventana al tamaño definido por el usuario al pulsar restaurar, hacer doble clic o arrastrar la barra de título.
  * **Arrastre Fluido con Restauración Proporcional:** Si el usuario arrastra la barra de título cuando la ventana está maximizada, esta se restaura automáticamente calculando la posición horizontal relativa del cursor sobre el ancho restaurado, permitiendo un arrastre natural idéntico al comportamiento nativo de Windows.
  * **Sincronización con `WindowResizeHelper`:** Se vinculó el estado a través de `stage.getProperties().put("custom-maximized", ...)`, garantizando que el asistente de redimensionamiento desactive automáticamente los cursores y el arrastre de bordes mientras la ventana se encuentre maximizada.

---

### [DEC-024] Hotfix de Estabilidad Ergonómica: Flechas de Mes Fijas y Botones Vectorizados (FAB y Modal Close)
* **Fecha:** 2026-10-13
* **Estado:** Aprobado
* **Decisión:**
  * **Estabilidad del Selector de Mes (`MonthlyOverview`):** Se definió un ancho fijo de 190 px y alineación centrada para `monthTitleLabel`, erradicando el desplazamiento horizontal de las flechas `[◀]` y `[▶]` al transitar entre meses con diferente longitud de caracteres y evitando *misclicks* en navegación rápida.
  * **Centrado Matemático del FAB (`MainController`):** Sustitución del texto plano `"+"` en el botón flotante por un vector `SVGPath` geométrico (`M 0 6 H 12 M 6 0 V 12`) con grosor de 2.2 px y `padding: 0`, logrando un centrado geométrico perfecto e inmune a variaciones de tipografía o fuentes de sistema.
  * **Erradicación de Ellipsis en Cierre de Modales (`ModalCard`):** Sustitución del glifo de texto plano `"✕"` por un vector `SVGPath` idéntico al de la barra de título (`M 0.5 0.5 L 8.5 8.5 M 8.5 0.5 L 0.5 8.5`), eliminando el truncamiento a puntos suspensivos (`...`) causado por el padding y font-size en botones compactos de 28x28 px.

---

### [DEC-025] Simplificación del Panel Mensual: Enfoque Exclusivo en Gráfico y Ensanchamiento de Barras
* **Fecha:** 2026-10-13
* **Estado:** Aprobado
* **Decisión:**
  * **Eliminación de la Tabla de Valores:** Se retiró el componente `TableView` y la etiqueta "Tabla de valores f(día)" en `MonthlyOverview`, eliminando la sobrecarga cognitiva y concentrando toda la analítica mensual en las tarjetas KPI y el gráfico de barras.
  * **Limpieza de Nomenclatura del Eje Y y Título:** Se cambió la etiqueta del eje vertical de `"Hábitos Cumplidos f(día)"` a `"Hábitos Cumplidos"`, y el título del gráfico a `"Hábitos Completados por Día"`, adoptando un lenguaje directo y limpio.
  * **Ensanchamiento y Proporción del Gráfico de Barras (`BarChart`):** Se redujo `categoryGap` a 4 px y `barGap` a 0 px, permitiendo que cada una de las 31 barras aproveche al máximo el espacio horizontal. Se incrementó la altura preferida a 440 px con `Priority.ALWAYS` para ocupar todo el espacio vertical liberado por la tabla.

---

### [DEC-026] Catálogo Centralizado y Selector Interactivo de Etiquetas (SelectTagModal)
* **Fecha:** 2026-10-14
* **Estado:** Aprobado
* **Decisión:**
  * **Persistencia Centralizada en AppData:** Se incorporó el campo `List<String> tags` en `AppData`, con normalización automática (alfabética, sin duplicados case-insensitive) y retrocompatibilidad total mediante extracción de etiquetas existentes de los hábitos cuando `tags == null`.
  * **Servicios de Gestión en HabitService:** Métodos `obtenerEtiquetas()`, `buscarEtiquetas(query)` y `registrarEtiqueta(tag)` con normalización automática de mayúscula inicial y recorte de espacios.
  * **Modal Selector Interactivo (SelectTagModal):** Modal in-app con buscador reactivo en tiempo real, grilla de chips seleccionables y botón dinámico `+ Crear etiqueta "..."` cuando no hay coincidencia exacta.
  * **Apilamiento de Modales en MainController:** Actualización de `showModal` y `closeModal` para soportar una pila de modales superpuestos en `modalOverlay`. Esto permite abrir `SelectTagModal` desde `CreateHabitModal` o `EditHabitModal` y regresar de forma transparente con el estado y los campos del formulario intactos.
  * **Estilos Botanical Forest:** Integración en `custom.css` de los estilos modulares para preview de etiquetas (`tag-chip-preview`), botones de chip (`tag-chip-button`, `tag-chip-selected`) y botón de creación rápida.

---

### [DEC-027] Corrección de Flujo de Desapilado en Submodales In-App (ArchivedHabitsModal y ConfirmModal)
* **Fecha:** 2026-10-14
* **Estado:** Aprobado
* **Decisión:**
  * **Causa del Defecto (Duplicate Children en JavaFX):** En `ArchivedHabitsModal`, al interactuar con el diálogo de confirmación `ConfirmModal` para eliminar definitivamente un hábito, los callbacks de confirmación y cancelación invocaban erróneamente `showSubModal.accept(this)`. Esto intentaba reinsertar la instancia existente de `ArchivedHabitsModal` en el contenedor `modalOverlay` del `StackPane`, desencadenando `java.lang.IllegalArgumentException: Children: duplicate children added: parent = StackPane`.
  * **Solución de Desapilado LIFO:** Se almacenó la referencia al callback de cierre en un campo `private final Runnable closeModal;` en `ArchivedHabitsModal`. Tanto al cancelar como al confirmar la acción destructiva en `ConfirmModal`, se invoca `closeModal.run()`, retirando el modal superior de la pila de `modalOverlay` y revelando fluidamente `ArchivedHabitsModal` que permanecía debajo.
---

### [DEC-028] Línea de Tiempo Semanal Diaria (L a D) en Tarjetas de Hábitos
* **Fecha:** 2026-10-16
* **Estado:** Aprobado
* **Decisión:**
  * **Evolución de la Línea de Tiempo Semanal:** Se sustituyó la visualización de bloques históricos por una matriz fija de 7 días que representa la semana calendario en curso (Lunes a Domingo), brindando visibilidad inmediata del estado de cumplimiento diario de cada hábito.
  * **Nomenclatura Simplificada (L, M, X, J, V, S, D):** Se adoptaron iniciales concisas de un solo carácter en sintonía directa con los botones de selección de días del sistema (`CreateHabitModal` y `EditHabitModal`).
  * **Semántica Visual de Estados en `custom.css`:**
    * *Días pasados cumplidos:* Fondo verde bambú y borde de contraste (`.streak-block-success`).
    * *Días pasados no cumplidos:* Fondo rojo oscuro y borde terracota (`.streak-block-failed`).
    * *Días pasados no programados:* Tono neutro apagado (`.streak-block-unassigned`).
    * *Día actual ("Hoy"):* Conserva el indicador `"Hoy"` con borde punteado de enfoque (`.streak-block-current`), adquiriendo fondo verde sólido al completarse (`.streak-block-current-reached`).
    * *Días futuros:* Fondo gris oscuro atenuado y opacidad reducida (`.streak-block-future`), comunicando visualmente que aún no han ocurrido.
  * **Tooltips Descriptivos:** Tooltip informativo con nombre completo del día, fecha exacta y estado (ej. *"Lunes 12 de octubre: Cumplido"*, *"Hoy (Viernes): ¡Hábito cumplido!"*, *"Sábado 17 de octubre: Próximo"*).



















