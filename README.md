# Habit Tracker — Desktop Application

Aplicación de escritorio minimalista y de alto enfoque para la gestión y seguimiento de hábitos personales, construida con **Java 21** y **JavaFX**. Diseñada bajo una filosofía **100% local, privada y orientada al cumplimiento de objetivos semanales**.

---

## 🚀 Características Principales

* **HUD Diario Minimalista (1/3 de pantalla):**
  * Vista enfocada exclusivamente en los hábitos programados para la jornada actual.
  * Sección superior para pendientes y contenedor colapsable *"Completados (N)"* para mantener despejada la vista.
  * Interacción limpia (`[ ]` / `[x]`) con acceso sutil en hover y menú contextual para editar, archivar o registrar avance parcial cuantitativo directo (sin pasos `+`/`-`).
* **Panel de Analítica y Rachas Semanales (2/3 de pantalla):**
  * **Rachas basadas en metas semanales:** Evaluadas en semanas calendario ISO-8601 (Lunes a Domingo), consolidando el criterio $\text{días cumplidos} \ge \text{objetivo semanal}$.
  * **Línea de tiempo de bloques discretos:** Visualización de semanas cumplidas, quiebres de racha a 0 y estado de la semana en curso.
  * **Auditoría anti-trampa:** Sellado automático e inmutable de días no abiertos con estado fallado; prohíbe modificaciones retroactivas.
  * **Vista general mensual:** Gráfico interactivo de barras de frecuencias $f(\text{día}) = \text{hábitos completados}$ con **eje Y acotado estrictamente a $[0, N]$** (total de hábitos activos) y tabla de desglose fraccionario diario.
* **Ciclo de vida riguroso:** Gestión de hábitos activos, archivados (con opción de restauración) y eliminación física definitiva en cascada con su historial.
* **Persistencia Atómica Local:** Almacenamiento 100% en disco local (`data/habits_data.json`) mediante volcado atómico (`.tmp` $\rightarrow$ `ATOMIC_MOVE`), garantizando cero corrupción por cierres repentinos. Cero llamadas a APIs externas o bases de datos en la nube.

---

## 🛠️ Stack Tecnológico

* **Lenguaje:** [Java 21 LTS](https://openjdk.org/projects/jdk/21/) (uso extensivo de `record` para modelos inmutables y pattern matching).
* **Framework UI:** [JavaFX 21](https://openjfx.io/) con la suite de estilos [AtlantaFX](https://github.com/mkpaz/atlantafx) (`PrimerDark`) y personalización en CSS (`custom.css`).
* **Persistencia:** [Jackson Databind 2.17+](https://github.com/FasterXML/jackson) con módulo `jackson-datatype-jsr310` para APIs modernas de fecha/hora.
* **Testing:** [JUnit 5 (Jupiter)](https://junit.org/junit5/) para validación exhaustiva de persistencia, invariantes y cálculo de rachas.
* **Build Tool:** [Apache Maven 3.9+](https://maven.apache.org/) (`javafx-maven-plugin`).

---

## 🧠 Arnés de Ingeniería (`rsc-harness`)

Este proyecto está gobernado por el arnés de ingeniería **`rsc-harness`** ([`@ericrisco/rsc`](https://www.npmjs.com/package/@ericrisco/rsc)).

El arnés proporciona un entorno estructurado de acompañamiento y memoria local para el desarrollo asistido por IA:
* **Gobernanza por carriles (Lanes):** Clasificación estricta de tareas en **Answer** (consultas de sólo lectura), **FTD** (Feature Task Document para cambios puntuales comprobables) y **SDD** (Specification-Driven Development para arquitectura crítica).
* **Skills especializadas:** Estandarización de capacidades técnicas (`java` para buenas prácticas y records modernos, `git-workflow` para commits atómicos, `ui-engineering` y agentes de refutación de invariantes).
* **Memoria y continuidad:** Persistencia del contexto del proyecto, perfil técnico y decisiones de ingeniería sesión a sesión sin pérdida de trazabilidad.

---

## 📁 Estructura de Documentación (`02-DOCS/`)

Toda la especificación funcional y técnica del proyecto se encuentra centralizada y versionada en [`02-DOCS/`](02-DOCS/):

* [**`reglasdenegocio.md`**](02-DOCS/reglasdenegocio.md): Especificación funcional detallada, modelo conceptual, esquema de frecuencias (estilo alarma), regla anti-trampa de sellado diario e invariantes de rachas semanales.
* [**`arquitectura.md`**](02-DOCS/arquitectura.md): Estructura de directorios y módulos del proyecto, distribución de pantalla Split (1/3 HUD vs 2/3 Analítica) y responsabilidades por capa.
* [**`core-components.md`**](02-DOCS/core-components.md): Referencia técnica exhaustiva de cada archivo del código fuente (`model/`, `repository/`, `service/`, `ui/`, `ui/hud/`, `ui/analytics/`), detallando sus responsabilidades, invariantes y diseño.
* [**`decisiones.md`**](02-DOCS/decisiones.md): Registro de Decisiones de Arquitectura (ADR) numeradas cronológicamente ([DEC-001] a [DEC-018]) que respaldan cada elección técnica y de interfaz.
* [**`seguimiento.md`**](02-DOCS/seguimiento.md): Bitácora de estado del proyecto, hitos completados y próximas tareas priorizadas.
* [**`wiki/`**](02-DOCS/wiki/): Artículos de gobernanza, perfil técnico del usuario y guía de uso del arnés de ingeniería (`usoDelHarness.md`).

---

## 📦 Compilación y Ejecución

### Requisitos previos
* **JDK 21** o superior instalado y configurado en el `PATH` (`JAVA_HOME`).
* **Apache Maven 3.8+** instalado.

### Comandos principales

```bash
# Compilar el proyecto
mvn clean compile

# Ejecutar la suite completa de pruebas unitarias
mvn test

# Lanzar la aplicación de escritorio
mvn javafx:run

# Generar el paquete JAR distribuible
mvn package
```
