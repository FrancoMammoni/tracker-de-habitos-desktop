# Referencia de Componentes Core — Tracker de Hábitos

Este documento describe la responsabilidad, diseño técnico e invariantes de cada archivo central de la aplicación dentro del código fuente (`src/main/`). No incluye suites de prueba ni archivos autogenerados o de configuración redundante.

---

## 1. Capa de Modelo de Dominio (`src/main/java/model/`)

Modelos inmutables implementados como `record` de Java 21, garantizando integridad de datos y thread-safety sin estado mutable oculto.

### [`Habit.java`]
* **Responsabilidad:** Representa la definición conceptual de un hábito y sus reglas de frecuencia semanal.
* **Aspectos Clave:**
  * Invariante de negocio validada en el constructor compacto: $1 \le \text{objetivoSemanal} \le |\text{diasAparicion}|$. Si no se cumple, lanza `IllegalArgumentException`.
  * Nombre obligatorio (no nulo ni en blanco).
  * Soporta patrón *wither* (`withArchivado(boolean)`) para mutaciones inmutables de ciclo de vida.
  * Método helper `apareceEn(LocalDate)` para determinar si el hábito debe mostrarse en el HUD en una fecha dada.

### [`DailyRecord.java`]
* **Responsabilidad:** Modela el resultado de un hábito en una fecha específica.
* **Aspectos Clave:**
  * Evaluación binaria estricta: `cumplido` (`true` o `false`). No existen estados neutros o "saltados".
  * Campo opcional `valorAlcanzado` (`Double`): permite guardar la evidencia cuantitativa de progreso parcial cuando el objetivo total no se alcanzó.
  * Fábricas estáticas:
    * `DailyRecord.cumplido(habitId, fecha)`: `cumplido = true`, sin valor parcial.
    * `DailyRecord.fallado(habitId, fecha)`: `cumplido = false`, sin valor parcial.
    * `DailyRecord.parcial(habitId, fecha, valor)`: fija indefectiblemente `cumplido = false` registrando el avance cuantitativo.

### [`AppData.java`]
* **Responsabilidad:** Contenedor raíz para la serialización y deserialización del estado completo de la aplicación.
* **Aspectos Clave:**
  * Agrupa `habits` (`List<Habit>`), `dailyRecords` (`List<DailyRecord>`), `lastCheckedDate` (`LocalDate`), `passwordHash` (`String`) y `passwordSalt` (`String`).
  * `lastCheckedDate`: Marca temporal auditada por la regla anti-trampa para sellar días en los que la app no fue abierta.
  * `passwordHash` y `passwordSalt`: Credenciales cifradas con PBKDF2 para la autenticación local (nulos en Día 0 o tras reseteo).
  * Métodos de transformación funcional inmutable: `withHabitAdded`, `withHabitUpdated`, `withHabitRemoved`, `withRecordAdded`, `withRecordRemoved`, `withLastCheckedDate`, `withPassword`, `withReset`.

### [`StreakInfo.java`]
* **Responsabilidad:** DTO analítico que encapsula las métricas calculadas para un hábito.
* **Aspectos Clave:**
  * `rachaActualSemanas`: Semanas calendario ISO-8601 consecutivas cumpliendo el objetivo mínimo.
  * `recordHistoricoSemanas`: Récord histórico máximo de semanas consecutivas alcanzado.
  * `diasCumplidosSemanaActual`: Cantidad de días completados en la semana en curso (Lunes a Domingo).
  * `objetivoSemanal`: Meta mínima semanal fijada en el hábito.
  * `historialSemanas`: Lista cronológica booleana de todas las semanas cerradas evaluadas (`true` = meta alcanzada, `false` = meta fallada y racha reiniciada a 0).
  * Métodos calculados: `metaSemanaActualAlcanzada()` y `porcentajeAvanceSemanaActual()`.

### [`DayCompletion.java`]java)
* **Responsabilidad:** Record inmutable que modela el resumen diario para la vista mensual general.
* **Aspectos Clave:**
  * Modela la función matemática $f(\text{día}) = \text{cantidad de hábitos completados en esa jornada}$.
  * Campos: `dia` (int), `fecha` (LocalDate), `totalCumplidos` (int) y `nombresHabitos` (`List<String>`).

---

## 2. Capa de Persistencia Local (`src/main/java/repository/`)

### [`JsonHabitRepository.java`]
* **Responsabilidad:** Gestionar la lectura y escritura atómica en el archivo local `data/habits_data.json`.
* **Aspectos Clave:**
  * Escritura atómica a prueba de fallos: escribe primero en un archivo temporal (`.tmp`) y lo mueve a destino mediante `ATOMIC_MOVE` (con fallback seguro a `REPLACE_EXISTING`), evitando corrupción por cierres abruptos o cortes de energía.
  * Manejo del "Día 0": si el archivo no existe o está vacío, retorna de manera transparente `AppData.empty()` con la fecha actual.
  * Configuración Jackson con `JavaTimeModule`: fechas ISO-8601 legibles (`LocalDate` y `DayOfWeek`).

---

## 3. Capa de Servicios de Negocio (`src/main/java/service/`)

Orquestan las reglas de negocio y actúan como intermediarios entre la persistencia y la interfaz.

### [`HabitService.java`]
* **Responsabilidad:** Gestión del catálogo de hábitos, ciclo de vida (activo, archivado, eliminado), registro diario y agregación mensual.
* **Aspectos Clave:**
  * Operaciones sincronizadas (`synchronized`) y atómicas sobre el repositorio.
  * Ciclo de vida estricto:
    * `archivarHabito`: Oculta el hábito del HUD diario manteniendo su historial íntegro.
    * `restaurarHabito`: Devuelve el hábito al HUD para sus días correspondientes.
    * `actualizarHabito`: Modifica la configuración de un hábito activo preservando su ID y fecha de creación original.
    * `eliminarHabitoDefinitivamente`: Eliminación física del catálogo y borrado en cascada de todos sus `DailyRecord`, impidiendo registros huérfanos.
  * Registro diario: `registrarCumplido`, `registrarAvanceParcial`, `eliminarRegistro` (desmarcar).
  * Filtrado temporal: `getHabitosParaFecha(LocalDate)` devuelve únicamente hábitos activos configurados para aparecer en ese día de la semana.
  * Agregación mensual: `getCumplimientosMes(YearMonth)` calcula la función $f(\text{día}) = \text{completados}$ para cada día del mes.

### [`StreakService.java`]
* **Responsabilidad:** Auditoría anti-trampa retroactiva y cálculo de rachas en semanas calendario ISO-8601.
* **Aspectos Clave:**
  * `auditarYSellarDiasPasados(LocalDate)`: Compara `lastCheckedDate` con la fecha actual. Cualquier día intermedio en que la app no fue abierta y un hábito debía aparecer se sella automáticamente con `DailyRecord.fallado`, haciendo las fechas pasadas inmutables.
  * `calcularRacha(habitId, fechaReferencia)`:
    * Evalúa semanas calendario cerradas (Lunes a Domingo) desde `createdAt`.
    * Criterio de éxito: $\text{dias\_cumplidos} \ge \text{objetivoSemanal}$. Si no se alcanza, la racha consecutiva cae a 0.
    * Suma en tiempo real la semana en curso si el objetivo ya fue satisfecho antes del Domingo.
  * `calcularRachasParaTodosLosHabitos(LocalDate)`: Calcula las métricas para todos los hábitos activos en una sola pasada.

### [`AuthService.java`]
* **Responsabilidad:** Gestión de credenciales locales, hashing con PBKDF2, verificación de autenticación y reseteo de fábrica.
* **Aspectos Clave:**
  * `hasPasswordConfigured()`: Comprueba si existe un hash de contraseña persistido en `AppData`.
  * `configurarPassword(String clave)`: Genera un salt criptográfico aleatorio de 16 bytes (`SecureRandom`), deriva el hash seguro mediante `PBKDF2WithHmacSHA256` (65.536 iteraciones) y lo almacena de forma inmutable en `AppData`.
  * `verificarPassword(String clave)`: Deriva el hash con el salt registrado y lo compara mediante `MessageDigest.isEqual` para evitar ataques de temporización.
  * `restablecerAplicacion()`: Borra atómicamente todos los datos locales y restablece el repositorio al estado de Día 0 (`AppData.empty()`) sin contraseña.

---

## 4. Capa de Interfaz de Usuario (`src/main/java/ui/`)

Interfaz construida 100% de manera declarativa con JavaFX 21 y la biblioteca de temas AtlantaFX (`PrimerDark`), organizada en un `SplitPane` (1/3 HUD lateral y 2/3 Analítica) precedida por una pantalla de autenticación obligatoria.

### Entrada y Control Principal

#### [`HabitTrackerApp.java`]
* **Responsabilidad:** Punto de entrada de la aplicación JavaFX (`Application.start`) y orquestador del marco de ventana sin bordes nativos.
* **Aspectos Clave:**
  * Configura `StageStyle.UNDECORATED` para erradicar las barras de título blancas nativas del sistema operativo.
  * Integra la barra de título superior personalizada (`AppTitleBar`) y asigna el ícono de la aplicación (`/icons/app_icon.png`).
  * Asocia `WindowResizeHelper` para habilitar el redimensionamiento suave y responsivo en los 8 bordes perimetrales.
  * Inicializa el tema `PrimerDark` de AtlantaFX e inyecta la hoja de estilos personalizada `custom.css`.
  * Establece las dimensiones de ventana: resolución inicial `1100 x 720` px (mínimo garantizado `960 x 600` px).
  * Control de flujo: evalúa `authService.hasPasswordConfigured()`. Si no existe clave o tras un reseteo, monta `LockScreenView` en modo configuración; si existe clave, monta `LockScreenView` en modo desbloqueo. Tras autenticar exitosamente, transiciona a `MainController`.

#### [`MainController.java`]
* **Responsabilidad:** Ensamblar el layout general (`SplitPane` 1/3 y 2/3), capa de modales superpuestos (`modalOverlay`), selector de modos de visualización y coordinar los eventos de usuario con los servicios.
* **Aspectos Clave:**
  * Ejecuta la auditoría anti-trampa al iniciar.
  * Estructura raíz en `StackPane` que aloja el `SplitPane` y un `StackPane modalOverlay` (telón translúcido `rgba(0, 0, 0, 0.72)`) para desplegar modales in-app sin generar ventanas secundarias con barras blancas de Windows.
  * Métodos `showModal(Node)` y `closeModal()` para gestionar la pila visual de interacción.
  * Columna izquierda (HUD): organiza hábitos de hoy en "Por completar" y sección colapsable "Completados", con botón flotante FAB `+` y acciones de edición y archivado.
  * Columna derecha (Analítica): contiene el encabezado con selector desplegable de modo (`"Por Hábitos"` vs `"Hábitos de Este Mes"`), conmutando dinámicamente entre el panel de tarjetas individuales y la vista mensual agregada.
  * Sincronización reactiva: cualquier acción en el HUD (marcar, desmarcar, registrar avance cuantitativo parcial, alta, edición o archivo) actualiza instantáneamente tanto el HUD como ambas vistas analíticas (`refreshAnalytics()`).

---

### Componentes de Ventana y Marco (`src/main/java/ui/components/`)

#### [`AppTitleBar.java`]
* **Responsabilidad:** Barra de título personalizada e integrada para ventanas sin decoración nativa.
* **Aspectos Clave:**
  * Estilo Jet Black oscuro (`#16201e`) con borde divisorio Charcoal (`#505168`).
  * Despliega el ícono institucional de la aplicación (`app_icon.png`) y el título del programa.
  * Soporte de arrastre de ventana (`mouseDragged`) y maximizado/restauración inteligente por doble clic.
  * Botones de control con microinteracciones: Minimizar (`—`), Maximizar/Restaurar (`□`) y Cerrar (`✕` con hover destructivo en rojo Inferno `#A50104`).

#### [`WindowResizeHelper.java`]
* **Responsabilidad:** Controlador perimetral para permitir redimensionamiento suave del Stage `UNDECORATED`.
* **Aspectos Clave:**
  * Detecta el cursor en un umbral perimetral de 7 píxeles.
  * Conmuta dinámicamente entre los 8 cursores cardinales e intercardinales (`N`, `S`, `E`, `W`, `NW`, `NE`, `SW`, `SE`).
  * Realiza el cálculo diferencial de geometría en tiempo real asegurando que no se viole el tamaño mínimo de ventana.

---

### Capa de Modales In-App Superpuestos (`src/main/java/ui/modal/`)

#### [`ModalCard.java`]
* **Responsabilidad:** Contenedor base elevado para todas las tarjetas modales in-app.
* **Aspectos Clave:**
  * Fondo Subtle Black (`#243330`), bordes Charcoal (`#505168`), radio de 12px y sombra profunda (`dropshadow`).
  * Encabezado unificado con título, descripción y botón de cierre rápido `✕`.
  * Secciones flexibles para cuerpo (`setBody`) y botones de acción (`setActions`).

#### [`CreateHabitModal.java`]
* **Responsabilidad:** Modal in-app para la creación de nuevos hábitos.
* **Aspectos Clave:**
  * Selector de días de aparición estilo alarma (`L`, `M`, `X`, `J`, `V`, `S`, `D`) con estados activos/inactivos en Dark Cyan.
  * Límite dinámico de meta semanal mínima en `Spinner` vinculado a los días seleccionados.
  * Validación reactiva con mensajes de advertencia visuales.

#### [`EditHabitModal.java`]
* **Responsabilidad:** Modal in-app para la edición de hábitos existentes.
* **Aspectos Clave:**
  * Precarga íntegra de la configuración del hábito preservando metadatos inmutables.
  * Control reactivo de invariantes de negocio y confirmación in-app.

#### [`ArchivedHabitsModal.java`]
* **Responsabilidad:** Modal in-app para la administración y restauración de hábitos archivados.
* **Aspectos Clave:**
  * Listado scrolleable de hábitos archivados con botones para restaurar al HUD o eliminar definitivamente.
  * Flujo de submodal de confirmación integrado ante intentos de borrado definitivo.

#### [`ConfirmModal.java`]
* **Responsabilidad:** Modal in-app para confirmaciones de operaciones críticas (archivado, eliminación de fábrica).
* **Aspectos Clave:**
  * Soporte de variante estándar o destructiva (botón en rojo Inferno `#A50104`).

#### [`PartialProgressModal.java`]
* **Responsabilidad:** Modal in-app para registro cuantitativo de avance diario parcial.
* **Aspectos Clave:**
  * Campo numérico con validación de valores no negativos y retroalimentación contextual in-app.

#### [`NotificationModal.java`]
* **Responsabilidad:** Modal in-app para notificaciones informativas o advertencias, sustituyendo `Alert`.

---

### Módulo de Autenticación y Bloqueo (`src/main/java/ui/auth/`)

#### [`LockScreenView.java`]
* **Responsabilidad:** Pantalla de autenticación y configuración de credenciales previa al HUD.
* **Aspectos Clave:**
  * Modo Setup ("Configurar Contraseña"): solicita contraseña y confirmación. Al validar, guarda la credencial y redirige a la aplicación.
  * Modo Desbloqueo ("Ingresar Contraseña"): campo de contraseña protegido (`PasswordField`), botón "Desbloquear" y botón destructivo "Olvidé mi contraseña / Restablecer aplicación".
  * Confirmación de fábrica in-app integrada mediante tarjeta de advertencia irreversible antes de ejecutar el restablecimiento a Día 0.

---

### Módulo HUD Diario (`src/main/java/ui/hud/`)

#### [`HabitItemCell.java`]
* **Responsabilidad:** Fila interactiva minimalista para representar un hábito en la lista diaria.
* **Aspectos Clave:**
  * Elementos: Checkbox estilizado `[ ]` / `[x]`, nombre del hábito (tachado y atenuado si está completado).
  * Acciones sutiles en hover: despliega botones compactos para avance parcial cuantitativo (`✎`), edición (`✏`) y archivado (`📥`).
  * Menú contextual (clic derecho): ofrece acceso directo a *"Editar hábito..."*, *"Registrar avance parcial..."* y *"Archivar hábito..."*.
  * Cero sobrecarga visual: no invade la fila con números ni métricas analíticas.

#### [`CompletedSection.java`]
* **Responsabilidad:** Contenedor colapsable (`TitledPane`) para los hábitos completados de la jornada.
* **Aspectos Clave:**
  * Retraído por defecto para mantener la vista limpia y enfocada en lo pendiente.
  * Conteo dinámico en el encabezado (ej. *"Completados (3)"*). Se oculta si no hay elementos completados.

#### [`CreateHabitDialog.java`]
* **Responsabilidad:** Diálogo modal para la creación de nuevos hábitos.
* **Aspectos Clave:**
  * Selector de días de aparición estilo alarma (`L`, `M`, `X`, `J`, `V`, `S`, `D`).
  * Control dinámico de meta semanal mínima: ajusta automáticamente el límite superior del `Spinner` para asegurar la regla $1 \le \text{objetivo} \le \text{diasSeleccionados}$.
  * Validación reactiva del botón de confirmación.

#### [`EditHabitDialog.java`]
* **Responsabilidad:** Diálogo modal para la modificación de hábitos existentes.
* **Aspectos Clave:**
  * Precarga íntegramente la configuración actual del hábito (`nombre`, `descripcion`, `categoria`, días de aparición activos y objetivo semanal).
  * Preserva inmutables el `id`, el estado de archivado y la fecha de creación original (`createdAt`).
  * Control y validación dinámica de invariantes de negocio.

#### [`ArchivedHabitsDialog.java`]
* **Responsabilidad:** Diálogo modal de gestión de hábitos archivados.
* **Aspectos Clave:**
  * Lista hábitos con `archivado = true`.
  * Acciones por hábito: **Restaurar** (vuelve al HUD activo) o **Borrar definitivamente** (botón estilo Inferno `#A50104` con confirmación, eliminación física en cascada).

---

### Módulo Analítico y Rachas (`src/main/java/ui/analytics/`)

#### [`CycleProgress.java`]
* **Responsabilidad:** Visualización del progreso de la semana en curso (Lunes a Domingo).
* **Aspectos Clave:**
  * Texto descriptivo de días completados sobre el objetivo.
  * Barra de progreso (`ProgressBar`) estilizada en Dark Cyan (`#048A81`).
  * Indicador de estado inteligente: evalúa matemáticamente si los días restantes de la semana alcanzan para cumplir la meta; si no alcanzan, cambia a color Inferno (`#A50104`) con la alerta *"Meta en riesgo"*. Muestra *"¡Meta Cumplida!"* al lograr el objetivo.

#### [`StreakCanvas.java`]
* **Responsabilidad:** Visualización de métricas históricas y línea de tiempo de bloques semanales.
* **Aspectos Clave:**
  * Muestra superior destacada: **Racha Actual** y **Récord Histórico** en semanas.
  * Línea de tiempo de bloques discretos:
    * Bloque cumplido: Dark Cyan (`#048A81`) con tooltip informativo.
    * Bloque fallado: Inferno / Charcoal (`#A50104` / `#2b1d1f`) señalando el quiebre de racha a 0.
    * Bloque en curso: contorno activo indicando el estado de la semana actual.
  * Mensaje orientativo en hábitos de reciente creación (primera semana de vida).

#### [`MonthlyOverview.java`]
* **Responsabilidad:** Visualización general mensual del rendimiento de hábitos ("Hábitos de Este Mes").
* **Aspectos Clave:**
  * Modela y representa la función matemática $f(\text{día}) = \text{cantidad de hábitos completados}$.
  * Barra de navegación mensual (`◀`, `▶`, `"Mes Actual"`).
  * Tarjetas KPI del mes: total completados, promedio diario y día récord.
  * Gráfico de barras interactivo (`BarChart<String, Number>`) con **eje Y acotado estrictamente a $[0, N]$** (siendo $N$ el total de hábitos registrados), garantizando fidelidad de escala y ticks discretos enteros.
  * Tabla de desglose diario (`TableView<DayCompletion>`) con detalle de hábitos cumplidos por día y relación fraccionaria ($y / N$).

---

## 5. Recursos y Sistema de Diseño (`src/main/resources/`)

### [`custom.css`]
* **Responsabilidad:** Sistema de diseño centralizado, tokens de diseño y reglas CSS modulares con arquitectura Clean Code (10 bloques semánticos).
* **Paleta estricta aplicada — Botanical Forest (DEC-021):**
  * **Base:** Verde Bosque Profundo (`#111815`).
  * **Superficies Elevadas:** Musgo Sutil (`#1A2420`).
  * **Barra de Título e Inset:** Sombra de Bosque (`#0C1210`).
  * **Tipografía Principal:** Blanco Salvia (`#EAF2EC`).
  * **Texto Atenuado:** Gris Vegetal (`#8DA396`).
  * **Bordes y Divisores:** Gris Bosque (`#2C3C35`).
  * **Acento Principal y Hábitos Cumplidos:** Verde Bambú Fresco (`#4EBA6F`).
  * **Acento Secundario y Récord Histórico:** Ámbar Cálido (`#DDA15E`).
  * **Destructivo y Fallado:** Terracota Anaranjado (`#E76F51`).

### [`app_icon.png`]
* **Responsabilidad:** Recurso gráfico institucional (256x256 px) con isotipo de racha cíclica en Verde Bambú (`#4EBA6F`), verificación en Blanco Salvia (`#EAF2EC`) sobre fondo Bosque Profundo (`#111815`).
