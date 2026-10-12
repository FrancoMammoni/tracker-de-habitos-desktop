Especificación de Requerimientos — Tracker de Hábitos

## 1. Módulo de Creación y Configuración de Hábitos (ABM y Ciclo de Vida)

### 1.1 Reglas de Negocio y Modelo Conceptual

* **Atributos del Hábito:** Cada hábito contiene únicamente:
  * `nombre` (String, obligatorio).
  * `descripcion` (String, opcional — detalle del objetivo diario, ej. *"Leer 30 páginas"*).
  * `categoria` (String / Relación — etiqueta para agrupar, ej. *"Lectura"*, *"Salud"*, *"Estudio"*).
* **Naturaleza del Registro (Binario con Alcance Parcial):**
  * La evaluación principal del hábito es **binaria** (Cumplido / No cumplido).
  * Si el usuario **no llega a cumplir** el objetivo total del día, el sistema debe permitir registrar de forma opcional **hasta dónde llegó** (progreso parcial / nota de alcance, ej. *"Leí 20 páginas"*), manteniendo el estado del día como no completado respecto a la meta total pero guardando la evidencia del avance.
* **Esquema de Frecuencia (Días de Aparición + Objetivo Semanal):**
  * **Días de aparición (Estilo Alarma):** Selección explícita de los días de la semana (`Lunes`, `Martes`, `Miércoles`, `Jueves`, `Viernes`, `Sábado`, `Domingo`) en los que el hábito debe mostrarse en el HUD diario.
  * **Objetivo semanal mínimo:** Número entero que define cuántas veces como mínimo se debe cumplir el hábito dentro de la semana, sobre el total de días en los que aparece.
  * *Restricción lógica:* `1 <= objetivo_semanal <= cantidad_dias_seleccionados`. (Ejemplo: el hábito se configura para aparecer los 7 días de la semana en el HUD, pero el objetivo semanal se fija en un mínimo de 4 días cumplidos).
* **Ciclo de Vida (Activo -> Archivado -> Eliminado):**
  * Quitar un hábito del HUD activo no destruye sus datos; cambia su estado a `Archivado` y lo mueve a la pestaña de **Archivados**.
  * Desde la pestaña de **Archivados**, el usuario dispone de dos acciones por hábito: **Restaurar** (vuelve al estado `Activo` y reaparece en el HUD) o **Borrar definitivamente** (eliminación física del hábito y todo su historial).

## 2. Módulo de Registro Diario e Interfaz Principal (HUD)

### 2.1 Reglas de Negocio y Comportamiento

* **Estructura Visual del HUD (Simplicidad y Foco):**
  * La pantalla de inicio al abrir la aplicación muestra únicamente los hábitos correspondientes al día en curso (según la configuración de días de aparición de cada uno).
  * **Sección Superior (Por completar):** Lista abierta y visible de los hábitos pendientes del día.
  * **Sección Inferior (Completados - Colapsable):** Pestaña o contenedor retraído por defecto (ej. *"Completados (3)"*). Al hacer clic, se expande para mostrar los hábitos ya realizados durante la jornada, permitiendo mantener la vista limpia y enfocada en lo que falta hacer.
* **Carga de Progreso Numérico:**
  * Para los hábitos o alcances que involucren cantidades, no se utilizan contadores incrementales con pasos (`+` / `-`). El usuario ingresa directamente el **número final alcanzado** en un campo numérico (ej. tipear `"20"` directo).
* **Estados Estrictos:**
  * Solo existen dos estados posibles para un hábito en el día: **Cumplido** o **Fallado**. No existen estados intermedios, neutros ni excusas ("Saltado").
  * Si un hábito no alcanza la meta fijada para el día, su estado computado es **Fallado**, independientemente de si se anotó un valor numérico parcial alcanzado.
* **Inmutabilidad y Cero Edición Retroactiva:**
  * **Regla anti-trampa:** Queda terminantemente prohibida la edición o registro de días anteriores. Los hábitos solo pueden marcarse, modificarse o registrarse durante la fecha del día actual.
  * Una vez finalizada la jornada (cambio de día a las 00:00 hs), los hábitos no completados pasan automáticamente a estado **Fallado** y la fecha queda sellada de forma permanente para garantizar la veracidad de los reportes e históricos.
  * Si el usuario no abre la aplicación durante uno o más días, al volver a abrir la aplicación el sistema sella automáticamente todos los hábitos de esos días anteriores como **Fallados**.

## 3. Lógica de Rachas (Streaks) y Ciclos de Tiempo

### 3.1 Modelo de Rachas: Basadas en Objetivos y Medidas en Semanas

* **Entidad de la Racha:** La racha **no pertenece al hábito individual diario**, sino al **cumplimiento del objetivo semanal**.
* **Unidad de Medida:** Las rachas se miden estrictamente en **semanas consecutivas cumplidas** (ej. *"4 semanas consecutivas"*), nunca en días acumulados.
* **Criterio de Éxito Semanal:** Una semana se considera cumplida si y solo si la cantidad de días completados en la semana calendario (Lunes a Domingo) es mayor o igual al objetivo semanal mínimo fijado (`dias_cumplidos >= objetivo_semanal`). Si no se alcanza la meta al cierre del ciclo, la racha activa se reinicia a 0.

## 4. Diseño de Interfaz (Layout Split) y Tarjeta de Hábito

### 4.1 Distribución de Pantalla (Split Layout)

* **Estructura en dos columnas:**
  * **Columna Lateral (1/3 del ancho de pantalla):** Panel de gestión y check-in diario (HUD de hábitos).
    * Parte superior: Lista de hábitos pendientes del día.
    * Parte inferior: Sección colapsable *"Completados"* con los hábitos terminados en la jornada.
    * Esquina inferior derecha: Botón flotante con un `+` (FAB) para crear nuevos hábitos y gestionar accesos/archivados sin sobrecargar la interfaz.
  * **Área Principal (2/3 del ancho de pantalla):** Panel analítico dedicado exclusivamente al **gráfico de rachas semanales**.
    * Visualización del progreso de cada objetivo semanal (semanas consecutivas alcanzadas).
    * Gráfico comparativo de cumplimiento histórico semana a semana.

##### 4. Módulo de Estadísticas y Visualización (Gráfico de Rachas e Indicadores)

### 4.1 Distribución en Pantalla (Layout Split)

* **Proporción de la Interfaz:** La pantalla se divide de forma fija en dos secciones:
  * **Lateral izquierdo (1/3 del ancho):** Panel de ejecución diaria (lista de hábitos pendientes y contenedor retraído de completados).
  * **Panel central/derecho (2/3 del ancho):** Panel de métricas dedicado exclusivamente a la visualización de los objetivos y el **gráfico de rachas**.
* **Objetivo de diseño:** Mantener el foco operativo en el tercio izquierdo para tildar rápido, teniendo siempre visible en los dos tercios restantes el impacto directo sobre las rachas semanales acumuladas.

### 4.2 Indicadores en la Lista de Hábitos (Minimalismo Absoluto)

* **Cero sobrecarga visual:** Las filas de hábitos en el HUD no muestran métricas, números de racha, porcentajes ni etiquetas de frecuencia.
* **Componentes visuales estrictos:**
  * **Hábito pendiente:** Checkmark vacía interactiva `[ ]` + `Nombre del hábito`.
  * **Hábito completado:** Checkmark marcada `[x]` + `Nombre del hábito`.
* La información analítica no invade la lista diaria; vive únicamente en el panel de visualización de los 2/3 de la pantalla.

### 4.3 Panel y Gráfico de Rachas Semanales

* **Enfoque analítico:** La visualización no utiliza mapas de calor anuales (estilo GitHub) ni calendarios mensuales complejos; se enfoca de forma directa en el rendimiento de los **objetivos semanales**.
* **Componentes del Gráfico:**
  * **Racha Semanal Actual:** Indicador numérico destacado por hábito de la cantidad de semanas consecutivas en las que se cumplió la meta mínima (`semanas_cumplidas_seguidas`).
  * **Récord Histórico:** Muestra de la mejor racha semanal alcanzada (`max_streak`).
  * **Línea de Tiempo / Barras Semanales:** Gráfico de bloques o barras que representa las semanas del año, marcando visualmente qué semanas se logró el objetivo y en cuáles la racha cayó a 0.
  * **Progreso de la Semana en Curso:** Barra de avance que indica cuántos días van cumplidos sobre la meta semanal fijada (ej. `2 / 3 cumplidos este ciclo`) para auditar en tiempo real si el objetivo de la semana sigue vivo o en riesgo.

## 5. Módulo de Datos, Almacenamiento Local y Bloqueo de Privacidad

### 5.1 Persistencia y Base de Datos Local

* **Filosofía 100% Local:**
  * La aplicación no se conecta a ningún servicio externo, API de terceros ni base de datos en la nube.
  * Todo el estado de la aplicación reside físicamente en la máquina local del usuario.
* **Formato de Almacenamiento:**
  * Se gestiona mediante un archivo plano en el sistema de archivos local, a definir en la implementación técnica entre **JSON** o **CSV** (el que resulte más simple y mantenible para la lectura y escritura sincrónica/asincrónica).
  * La estructura almacena: configuración inicial, catálogo de hábitos, registros diarios (cumplido / fallado + valor alcanzado) y el acumulado de rachas semanales.
* **Sin Migración de Históricos:**
  * La app inicia limpia desde el día de su primer uso (Día 0).
  * No se implementan funciones de importación de planillas externas, CSVs antiguos ni asistentes de migración.
* **Respaldo de Datos (Backup):**
  * Al tratarse de un archivo plano local único, el backup manual consiste simplemente en copiar y pegar el archivo `.json` o `.csv` en cualquier otra carpeta o almacenamiento externo.
