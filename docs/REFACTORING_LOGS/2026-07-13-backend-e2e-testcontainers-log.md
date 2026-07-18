# Log de Infraestructura QA: Implementación de Pruebas E2E con Testcontainers
- **Fecha de Creación:** 2026-07-13
- **Tecnologías:** Testcontainers (PostgreSQL), RestAssured, Spring Boot Test.
- **Ubicación de Infraestructura:** `src/test/java/com/alurachallenge/literalura/BaseE2ETest.java`

## 1. Arquitectura de Contenedores Sostenible (Singleton Pattern)
Se definió una clase base abstracta compartida (`BaseE2ETest`) con un contenedor PostgreSQL `static` para que todas las clases E2E reutilicen la misma instancia y no levanten contenedores por archivo de prueba.  
Con `@DynamicPropertySource` se inyectan en runtime `spring.datasource.url`, `spring.datasource.username` y `spring.datasource.password` a partir del contenedor activo, resolviendo de forma transparente puertos efímeros y credenciales dinámicas.

## 2. Archivos e Infraestructura Creados
- `src/test/resources/application-test.properties` -> Propiedades ddl-auto de aislamiento.
- `src/test/java/com/alurachallenge/literalura/BaseE2ETest.java` -> Clase core de infraestructura de Docker.
- `src/test/java/com/alurachallenge/literalura/controller/AutorControllerE2ETest.java` -> Pruebas de flujo de red con RestAssured.
- `src/test/java/com/alurachallenge/literalura/controller/LibroControllerE2ETest.java` -> Pruebas de flujo de red con RestAssured.

## 3. Matriz de Flujos de Integridad E2E (Checklist)
- [x] **Aislamiento de Puerto:** Servidor web levantado de forma segura en `RANDOM_PORT`.
- [x] **Persistencia Real:** Operaciones de escritura impactan un motor PostgreSQL real (no H2/en memoria).
- [x] **Validación de Contrato HTTP:** RestAssured valida códigos de estado y la estructura exacta de los payloads JSON devueltos.

## 4. Bloques de Código de la Infraestructura Implementada
```java
// BaseE2ETest.java (núcleo)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseE2ETest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("literalura_e2e")
            .withUsername("literalura")
            .withPassword("literalura");

    @LocalServerPort
    protected int port;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void configureRestAssured() {
        RestAssured.reset();
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }
}
```

```java
// Caso representativo E2E con RestAssured (LibroControllerE2ETest)
@Test
void patchNota_deberiaActualizarNotaYRetornar200() {
    given()
            .contentType("application/json")
            .body("""
                    {"nota":"Reseña E2E actualizada"}
                    """)
    .when()
            .patch("/api/libros/{id}/nota", libroId)
    .then()
            .statusCode(200)
            .body("nota", equalTo("Reseña E2E actualizada"))
            .body("mensaje", equalTo("Nota actualizada exitosamente"));
}
```

## 5. Ajuste correctivo aplicado durante ejecución
Se intentó mitigar un fallo de `NullPointerException` en `io.restassured.internal.RequestSpecificationImpl.applyProxySettings` con `RestAssured.reset()` y limpieza explícita de propiedades de proxy en cada `@BeforeEach`.

> **⚠️ Corrección (2026-07-17):** El diagnóstico anterior era incorrecto. La causa real del NPE no era estado de proxy residual, sino una **incompatibilidad binaria entre REST Assured 5.x (compilado contra Groovy 4) y Groovy 5**, que Spring Boot 4 gestiona por defecto. La limpieza de propiedades de proxy nunca tuvo efecto y fue retirada de `BaseE2ETest`. La solución definitiva fue actualizar a **REST Assured 6.0.0** (con soporte oficial de Groovy 5). Ver `2026-07-17-e2e-fix-restassured6-testcontainers2.md`.

## 6. Refactor de aislamiento por clase (sin duplicaciones)
- Se eliminó la configuración global mutable de RestAssured (`baseURI`/`port`) como estado compartido.
- `BaseE2ETest` ahora expone `requestSpec()` para construir `RequestSpecification` por prueba/clase con el `RANDOM_PORT` actual.
- `AutorControllerE2ETest` y `LibroControllerE2ETest` usan `given().spec(apiSpec)` y constantes de endpoints para evitar repetición y rutas hardcodeadas duplicadas.
- Se centralizó la mitigación de proxy/estado en la clase base (no repetida en cada test).
