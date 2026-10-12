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


- Accepted plan `16f5920728eb97c688252d3941dc746db3142c948a87948df3c587acef6aafe4`.
- Project kind: software.
- SDD: selected.
