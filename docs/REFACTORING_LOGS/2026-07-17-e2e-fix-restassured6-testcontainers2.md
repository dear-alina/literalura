# Log de Mantenimiento: Fix E2E (REST Assured 6), Testcontainers 2 y reparación de tests

**Fecha:** 2026-07-17
**Autor del cambio:** Mantenimiento asistido por Claude Code
**Resultado final:** `mvn test` con todas las suites → **128/128 tests en verde** (unitarios + integración + E2E)

---

## 1. Problema original: las pruebas E2E no ejecutaban

Las 6 pruebas E2E (`AutorControllerE2ETest`, `LibroControllerE2ETest`) fallaban todas con:

```
java.lang.NullPointerException: Cannot invoke "Object.hashCode()" because "key" is null
    at io.restassured.internal.RequestSpecificationImpl.applyProxySettings(RequestSpecificationImpl.groovy:2047)
```

### Causa raíz (confirmada)

**Incompatibilidad binaria entre REST Assured 5.x y Groovy 5.** Spring Boot 4 gestiona Groovy 5.0.4, mientras que toda la serie REST Assured 5.x está compilada contra Groovy 4. El NPE ocurría dentro de la metaprogramación de Groovy **antes de enviar cualquier petición HTTP**, por lo que fallaba siempre, independientemente del endpoint o de la configuración de proxy.

- Issue upstream: [rest-assured#1846 — Migrate to Groovy 5](https://github.com/rest-assured/rest-assured/issues/1846)
- Spring llegó a retirar el soporte REST Assured de Boot 4 por este motivo: [spring-boot#47685](https://github.com/spring-projects/spring-boot/issues/47685)

### Solución

Actualización a **REST Assured 6.0.0** ([release notes](https://github.com/rest-assured/rest-assured/wiki/ReleaseNotes60)), publicado con soporte para Groovy 5, Spring 7 y Jackson 3. Cambio de una línea en `pom.xml` (`rest-assured.version` 5.5.2 → 6.0.0). Los tests E2E no requirieron ningún cambio.

Además se retiró de `BaseE2ETest` el bloque de limpieza de propiedades de proxy en `@BeforeEach`: era una mitigación sin efecto basada en el diagnóstico erróneo del log 2026-07-13 (ya corregido con nota).

---

## 2. Saneamiento de versiones en `pom.xml`

| Dependencia | Antes | Después | Motivo |
|---|---|---|---|
| `io.rest-assured:rest-assured` | 5.5.2 | **6.0.0** | Compatibilidad Groovy 5 / Spring Boot 4 |
| `com.fasterxml.jackson.core:jackson-databind` | 2.16.0 (pin manual) | **2.20.2** (gestionada por BOM) | El pin antiguo desalineaba jackson-databind de jackson-core/annotations 2.20.x |
| `org.mockito:mockito-core` | 5.23.0 (pin manual) | **5.20.0** (vía `spring-boot-starter-test`) | El pin desalineaba mockito-core de mockito-junit-jupiter 5.20.0 |
| `org.testcontainers:*` | 1.20.6 (pin manual) | **2.0.3** (gestionada por BOM) | Alineación con la versión que Spring Boot 4.0.3 soporta oficialmente |

### Migración a Testcontainers 2

Testcontainers 2 renombró los módulos y reubicó clases:

- Artefactos: `junit-jupiter` → `testcontainers-junit-jupiter`, `postgresql` → `testcontainers-postgresql`
- Import: `org.testcontainers.containers.PostgreSQLContainer` → `org.testcontainers.postgresql.PostgreSQLContainer`
- `PostgreSQLContainer` ya no es genérico: `new PostgreSQLContainer<>(...)` → `new PostgreSQLContainer(...)`

Archivos ajustados (7): `BaseE2ETest`, `AutorControllerIT`, `LibroControllerIT`, `AutorServiceIT`, `LibroServiceIT`, `AutorRepositoryIT`, `LibroRepositoryIT`.

> **Nota operativa:** Testcontainers 2 usa la imagen `testcontainers/ryuk:0.13.0` como reaper. Si no se puede descargar (p. ej. sin red), se puede ejecutar con `TESTCONTAINERS_RYUK_DISABLED=true`.

---

## 3. Reparación de tests con expectativas desactualizadas (patrón AAA)

Los siguientes tests quedaron obsoletos tras el refactor 2026-07-14 (que movió el manejo de errores al `GlobalExceptionHandler` e introdujo `LibroNoEncontradoException` → 404). Todos fueron reestructurados con patrón **AAA (Arrange–Act–Assert)** explícito.

### Unitarios corregidos (6 fallos)

| Test | Problema | Corrección |
|---|---|---|
| `LibroControllerTest.buscarYRegistrar_runtimeException_deberiaRetornar404ConMensaje` | `RuntimeException` genérica mapea a 500, no a 404 | Renombrado a `..._libroNoEncontrado_...`; usa `LibroNoEncontradoException` → 404 |
| `LibroControllerTest.buscarYRegistrar_errorInterno_deberiaRetornar500ConMensajeGenerico` | El nombre decía 500 pero el assert esperaba 404 | Espera 500 + `status`/`mensaje` del `ErrorResponse` |
| `ClienteGutendexTest` (2 tests) | Mockeaba `getForObject`, pero el cliente usa `exchange` con headers → el stub nunca aplicaba y `response` era null | Stubs sobre `exchange(url, GET, HttpEntity, Class)` con URL codificada |
| `AutorServiceTest.obtenerAutorDetalle_cuandoNoExiste_deberiaLanzarExcepcion` | Esperaba mensaje "Autor no encontrado" | Espera `ResourceNotFoundException` con "Autor con ID 99 no encontrado" |
| `LibroServiceTest.buscarLibroPorTitulo_cuandoNoExiste_deberiaLanzarResourceNotFoundException` | Stubeaba `findByTitulo` (el servicio usa `findByTituloIgnoreCase`) y esperaba la excepción antigua | Stub correcto + espera `LibroNoEncontradoException` (miss local + miss Gutendex) |

### Integración corregidos (2 fallos, mismo origen)

- `AutorServiceIT.obtenerAutorDetalle_autorInexistente_deberiaLanzarExcepcion` → espera `ResourceNotFoundException` con mensaje actual
- `LibroServiceIT.buscarLibroPorTitulo_libroInexistente_deberiaLanzarResourceNotFoundException` → renombrado; espera `LibroNoEncontradoException` con mensaje del miss completo

### Cobertura añadida (verificación de suficiencia)

| Test nuevo | Qué cubre |
|---|---|
| `LibroControllerTest.buscarYRegistrar_conTituloEnBlanco_deberiaRetornar400PorValidacion` | Contrato de validación `@NotBlank` → 400 "Validación fallida" sin tocar el servicio |
| `ClienteGutendexTest.buscarLibrosPorTitulo_conCaracteresEspeciales_deberiaCodificarUtf8` | Codificación UTF-8 de la URL (`Cien años` → `Cien+a%C3%B1os`) |
| `ClienteGutendexTest.buscarLibrosPorTitulo_conRespuestaSinCuerpo_deberiaRetornarNull` | Comportamiento documentado ante respuesta 200 sin body |
| `LibroServiceTest.buscarLibroPorTitulo_conHitLocal_deberiaRetornarDetalleSinConsultarGutendex` | Hit en BD local no debe consultar la API externa (`verifyNoInteractions`) |

---

## 4. Resultado de la verificación

```
Tests run: 128, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Desglose: 57 unitarios puros + 65 de integración (Testcontainers PostgreSQL) + 6 E2E (REST Assured 6 + Testcontainers 2).
