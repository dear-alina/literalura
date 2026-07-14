# Log de Infraestructura QA: Setup E2E (Testcontainers + RestAssured)
- **Fecha:** 2026-07-13
- **Tipo de Tarea:** Configuración de infraestructura de pruebas End-to-End.
- **Objetivo:** Habilitar base abstracta E2E con PostgreSQL aislado, puerto aleatorio y configuración dinámica de datasource.

## 1. Cambios implementados
- `pom.xml`
  - Se agregó `io.rest-assured:rest-assured` (scope test).
  - Se agregó `org.testcontainers:testcontainers` (scope test).
  - Se agregó `org.testcontainers:postgresql` (scope test).
  - Se mantuvo `spring-boot-starter-test`.
- `src/test/resources/application-test.properties`
  - `spring.jpa.hibernate.ddl-auto=create-drop`
  - `app.interactive=false`
- `src/test/java/com/alurachallenge/literalura/BaseE2ETest.java`
  - `@SpringBootTest(webEnvironment = RANDOM_PORT)`
  - `@Testcontainers`
  - `@ActiveProfiles("test")`
  - Contenedor singleton `postgres:15-alpine` en campo `static`.
  - `@DynamicPropertySource` para inyectar `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`.
  - Configuración de `RestAssured.baseURI` y `RestAssured.port` en `@BeforeEach`.

## 2. Nota técnica sobre credenciales
Aunque la aplicación local pueda funcionar sin credenciales explícitas en algunos entornos, el contenedor PostgreSQL de Testcontainers siempre expone un usuario/clave efectivos.  
Por eso se inyectan dinámicamente username/password para garantizar que el contexto E2E sea portable y determinístico.

## 3. Estado
- Compilación de pruebas (`mvn test-compile`): **OK**
