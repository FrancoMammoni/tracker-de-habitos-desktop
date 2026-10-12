# Guía de Uso del Harness (`rsc-harness`)

Esta guía resume cómo funciona el arnés instalado para el proyecto **Tracker de Hábitos**, cómo interactúa con el agente de IA y las mejores prácticas para sacarle el máximo partido.

---

## 1. ¿Qué es el Harness y qué hace?

`rsc-harness` es un sistema de gobernanza y acompañamiento local que equipa al asistente (en este caso **Gemini**) con un conjunto estructurado de habilidades (skills), roles de agente (refuters) y flujos de trabajo de ingeniería.

### Beneficios principales:
- **Cero improvisación:** El agente sigue flujos de trabajo claros y validados.
- **Memoria y continuidad:** El progreso y las decisiones de sesión persisten localmente en `.rsc/` y `02-DOCS/wiki/harness/`.
- **Compuertas de calidad (SDD):** El desarrollo de funcionalidades complejas o de persistencia se hace guiado por especificación antes de escribir código.
- **Revisión por refutación:** Dispone de roles para auditar la corrección lógica (`refuter-correctness`), la seguridad/invariantes (`refuter-security`) y los tests (`refuter-tests`).

---

## 2. Los Flujos de Trabajo (Lanes)

Cada petición que haces al agente se clasifica internamente en uno de estos tres caminos:

1. **Answer (Consulta / Exploración):**
   - Preguntas explicativas, comparaciones, auditorías de código o revisiones.
   - Es de sólo lectura: no modifica código ni altera especificaciones.
2. **FTD (Feature Task Document - Tareas Simples):**
   - Cambios directos, pequeñas mejoras de UI o ajustes puntuales.
   - Se crea un documento ligero en `02-DOCS/` con checklist de tareas y evidencia de pruebas.
3. **SDD (Specification-Driven Development - Tareas Estructurales):**
   - Nuevos módulos, persistencia, lógica de negocio crítica o cambios que afectan múltiples capas.
   - **Ciclo:** `specify` (especificar requisitos) $\rightarrow$ `clarify` (resolver dudas) $\rightarrow$ `implement` (desarrollar) $\rightarrow$ `verify` (comprobar tests/evidencia) $\rightarrow$ `review` / `ship`.

---

## 3. Agentes Especializados Disponibles

Cuando necesites revisiones profundas, puedes pedirle al agente explícitamente:
- **`@refuter-correctness`**: Para auditar si la lógica de rachas, la regla anti-trampa o el cálculo semanal cumple al 100% las reglas de negocio.
- **`@refuter-security`**: Para auditar la inmutabilidad de los records Java (`Habit`, `DailyRecord`, `AppData`) y validación de entradas.
- **`@refuter-tests`**: Para revisar que los casos de prueba unitarios en JUnit cubran casos límite (bordes de semana, años bisiestos, ausencias de uso).

---

## 4. Skills Clave Instaladas en este Proyecto

- **Generales de ciclo de vida:** `plan`, `specify`, `implement`, `verify`, `review`, `ship`, `tasks`.
- **Calidad y depuración:** `debug`, `unslop` (redacción humana limpia), `simplify-code`, `decision-challenge`.
- **Específicas de la tecnología:**
  - `java`: Buenas prácticas en Java moderno (Java 21, inmutabilidad, records, pattern matching).
  - `git-workflow`: Gestión limpia de commits y cambios de código.
  - `ui-engineering`: Patrones de desarrollo de interfaces fluidas.

---

## 5. Comandos y Utilidades en Terminal

Si alguna vez deseas auditar o administrar el arnés desde tu consola, puedes ejecutar:

```bash
# Auditar la salud del arnés
npx @ericrisco/rsc doctor

# Listar skills activas
npx @ericrisco/rsc list

# Ver todas las habilidades disponibles en el catálogo
npx @ericrisco/rsc capabilities

# Instalar una habilidad adicional si el proyecto la necesita
npx @ericrisco/rsc add <nombre-skill> --target gemini

# Reevaluar el arnés según el crecimiento del código
npx @ericrisco/rsc reassess
```

---

## 6. Tips para una Sesión Exitosa

1. **Usa lenguaje natural y sé específico:** No necesitas aprender comandos crudos; dile al agente qué quieres construir o resolver (ej. *"Implementemos el repositorio JsonHabitRepository con tests unitarios"*). El arnés clasificará la intención en la lane adecuada.
2. **Revisa antes de confirmar:** Respeta la regla de `antigravity.md`: el agente te propondrá qué hará y cómo antes de tocar código, y solo actuará cuando le confirmes.
3. **Auditoría continua:** Cada vez que se tome una decisión de arquitectura o de negocio, el agente la registrará en `02-DOCS/decisiones.md`.
4. **Pruebas automáticas frecuentes:** Ejecuta `mvn test` y `mvn clean compile` al concluir cada componente para asegurar que la evidencia sea sólida antes de dar la tarea por concluida.
