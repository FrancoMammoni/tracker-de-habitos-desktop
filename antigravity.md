# Instrucciones del Agente
Antes de Tocar codigo:
- Lee la especificación funcional y las reglas en `02 DOCS/reglasdenegocio.md`.
- Consulta el stack y paquetes acordados en `02 DOCS/arquitectura.md`.
- Tienes que decirme que realizarás y como, y cuando te confirme el cambio aplicalo.
Luego:
- Al finalizar cada  (si aplica), actualiza el estado de las decisiones importantes o contexto importante en `02 DOCS/decisiones.md`.
- Si tomamos una decision sobre las reglas de negocio, arquitectura o tomamos una decision nueva, debes modificar y actualizar estos archivos.

# Stack Tecnologico

* **Lenguaje:** Java 21 (utilizar características modernas como `record` para modelos inmutables).

* **Framework UI:** JavaFX 21 + tema **AtlantaFX** (Primer Dark / Primer Light).

* **Persistencia:** Jackson Databind para lectura/escritura en `./data/habits_data.json`.

* **Build Tool:** Maven.
  
  * Compilar: `mvn clean compile`
  
  * Ejecutar pruebas: `mvn test`
  
  * Lanzar aplicación: `mvn javafx:run`
