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
  * Agrupa `habits` (`List<Habit>`), `dailyRecords` (`List<DailyRecord>`) y `lastCheckedDate` (`LocalDate`).
  * `lastCheckedDate`: Marca temporal auditada por la regla anti-trampa para sellar días en los que la app no fue abierta.
  * Métodos de transformación funcional inmutable: `withHabitAdded`, `withHabitUpdated`, `withHabitRemoved`, `withRecordAdded`, `withRecordRemoved`, `withLastCheckedDate`.

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

---

## 4. Capa de Interfaz de Usuario (`src/main/java/ui/`)

Interfaz construida 100% de manera declarativa con JavaFX 21 y la biblioteca de temas AtlantaFX (`PrimerDark`), organizada en un `SplitPane` (1/3 HUD lateral y 2/3 Analítica).

### Entrada y Control Principal

#### [`HabitTrackerApp.java`]
* **Responsabilidad:** Punto de entrada de la aplicación JavaFX (`Application.start`).
* **Aspectos Clave:**
  * Inicializa el tema `PrimerDark` de AtlantaFX e inyecta la hoja de estilos personalizada `custom.css`.
  * Establece las dimensiones de ventana: resolución inicial `1100 x 720` px (mínimo garantizado `960 x 600` px).

#### [`MainController.java`]
* **Responsabilidad:** Ensamblar el layout general (`SplitPane` 1/3 y 2/3), selector de modos de visualización y coordinar los eventos de usuario con los servicios.
* **Aspectos Clave:**
  * Ejecuta la auditoría anti-trampa al iniciar.
  * Columna izquierda (HUD): organiza hábitos de hoy en "Por completar" y sección colapsable "Completados", con botón flotante FAB `+` y acciones de edición y archivado.
  * Columna derecha (Analítica): contiene el encabezado con selector desplegable de modo (`"Por Hábitos"` vs `"Hábitos de Este Mes"`), conmutando dinámicamente entre el panel de tarjetas individuales y la vista mensual agregada.
  * Sincronización reactiva: cualquier acción en el HUD (marcar, desmarcar, registrar avance cuantitativo parcial, alta, edición o archivo) actualiza instantáneamente tanto el HUD como ambas vistas analíticas (`refreshAnalytics()`).

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

## 5. Recursos y Sistema de Diseño (`src/main/resources/styles/`)

### [`custom.css`]
* **Responsabilidad:** Sistema de diseño centralizado y paleta cromática corporativa.
* **Paleta estricta aplicada (DEC-010):**
  * **Base:** Jet Black (`#1C2826`).
  * **Superficies Elevadas:** Subtle Black (`#243330`) e Inset (`#16201e`).
  * **Tipografía Principal:** Light Cyan (`#E0FBFC`).
  * **Texto Atenuado:** Muted (`#8d8fa8`).
  * **Bordes y Divisores:** Charcoal (`#505168`).
  * **Acento Principal y Éxito:** Dark Cyan (`#048A81`).
  * **Destructivo y Fallado:** Inferno (`#A50104`).
