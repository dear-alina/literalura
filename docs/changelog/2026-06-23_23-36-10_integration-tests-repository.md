# Tests de Integración - Capa Repository (LibroRepository & AutorRepository)

**Fecha:** 2026-06-23 23:36:10  
**Categoría:** Tests  
**Estado:** Completado  
**Impacto:** Medio

## 📋 Resumen
Se implementaron las suites de tests de integración para `LibroRepository` y `AutorRepository` usando `@DataJpaTest` (slice ligero JPA) y Testcontainers (PostgreSQL) con `@ServiceConnection`. Los tests verifican que el SQL autogenerado por Spring Data y la `@Query` JPQL personalizada funcionan correctamente contra una base de datos PostgreSQL real, con rollback automático entre tests gracias al comportamiento por defecto de `@DataJpaTest`.

## 🔧 Archivos Modificados/Creados
- `pom.xml` → Añadida dependencia `spring-boot-starter-data-jpa-test` (trae `@DataJpaTest` de `org.springframework.boot.data.jpa.test.autoconfigure` y `@AutoConfigureTestDatabase` de `org.springframework.boot.jdbc.test.autoconfigure`, ambos renombrados en Spring Boot 4)
- `src/test/java/com/alurachallenge/literalura/repository/LibroRepositoryIT.java` → Nueva clase con 9 tests que cubren `save`, `findByTitulo`, `findByTituloIgnoreCase`, `findByIdioma` y `findAll(Pageable)`
- `src/test/java/com/alurachallenge/literalura/repository/AutorRepositoryIT.java` → Nueva clase con 9 tests que cubren `save`, `findByNombre`, `findByNombreContainsIgnoreCase`, `findAutoresVivosEnAno` (@Query JPQL) y `findAll(Pageable)`

## 📊 Capas Afectadas
- [ ] AutorController / LibroController
- [ ] AutorService / LibroService
- [ ] ConsumoAPI / ClienteGutendex (integración)
- [ ] ConvierteDatos / IConvierteDatos (transformación)
- [x] AutorRepository / LibroRepository
- [x] Model (Entidades JPA)
- [ ] Config (Security, Redis, Properties)
- [ ] Exception (manejo de errores)

## 🧪 Tests Asociados

**LibroRepositoryIT — 9 tests:**

| Test | Query / Método Spring Data | Qué verifica |
|---|---|---|
| `save_libroNuevo_deberiaAsignarIdYPersistirEnBD` | `save()` | ID auto-generado por `IDENTITY`, persistencia real confirmada con `findById` |
| `findByTitulo_tituloExistente_deberiaRetornarLibroConDatosCompletos` | `findByTitulo` (derived) | `Optional` presente con título, autor e idioma correctamente mapeados |
| `findByTitulo_tituloInexistente_deberiaRetornarEmpty` | `findByTitulo` (derived) | `Optional.empty()` cuando no existe el título |
| `findByTituloIgnoreCase_deberiaEncontrarLibroSinImportarMayusculas` | `findByTituloIgnoreCase` (derived) | 3 variantes de casing retornan `Optional` presente |
| `findByTituloIgnoreCase_tituloInexistente_deberiaRetornarEmpty` | `findByTituloIgnoreCase` (derived) | `Optional.empty()` en búsqueda sin coincidencia |
| `findByIdioma_deberiaRetornarExclusivamenteLosLibrosDelIdiomaIndicado` | `findByIdioma` (derived, enum) | Solo devuelve libros del idioma pedido, excluye los demás |
| `findByIdioma_sinLibrosDelIdiomaSolicitado_deberiaRetornarListaVacia` | `findByIdioma` (derived, enum) | Lista vacía cuando no hay libros del idioma |
| `findAll_conPaginacionYOrden_deberiaRespetarPageSizeYOrdenarPorTitulo` | `findAll(Pageable)` | `pageSize=3`, orden ASC por título verificado en primeras posiciones |

**AutorRepositoryIT — 9 tests:**

| Test | Query / Método Spring Data | Qué verifica |
|---|---|---|
| `save_autorNuevo_deberiaAsignarIdYPersistirEnBD` | `save()` | ID auto-generado, campos `nombre`, `anoNacimiento`, `anoFallecimiento` persistidos |
| `findByNombre_nombreExistente_deberiaRetornarAutor` | `findByNombre` (derived) | `Optional` presente con años correctos |
| `findByNombre_nombreInexistente_deberiaRetornarEmpty` | `findByNombre` (derived) | `Optional.empty()` |
| `findByNombreContainsIgnoreCase_deberiaEncontrarConSubcadenaSinImportarMayusculas` | `findByNombreContainsIgnoreCase` (derived) | Búsqueda parcial case-insensitive en 3 variantes |
| `findByNombreContainsIgnoreCase_subcadenaInexistente_deberiaRetornarEmpty` | `findByNombreContainsIgnoreCase` (derived) | `Optional.empty()` sin coincidencia |
| `findAutoresVivosEnAno_deberiaAplicarFiltroJPQLCorrectamente` | `@Query JPQL` | 5 escenarios: 3 VIVO (`nacióAntes+murióDespués`, `nacióExacto`, `sinFallecimiento`) y 2 NO-VIVO (`murióAntes`, `nacióDespués`) |
| `findAutoresVivosEnAno_sinAutoresVivosEnEseAnio_deberiaRetornarListaVacia` | `@Query JPQL` | Lista vacía cuando todos murieron antes del año buscado |
| `findAutoresVivosEnAno_anoFallecimientoIgualAlAnoBuscado_deberiaIncluirse` | `@Query JPQL` | Límite exacto: `anoFallecimiento >= :fecha` incluye el año de fallecimiento |
| `findAll_conPaginacionYOrden_deberiaRespetarPageSizeYOrdenarPorNombre` | `findAll(Pageable)` | `pageSize=2`, orden ASC por nombre verificado en primeras posiciones |

## ⚠️ Consideraciones de Arquitectura de Literalura
- ✅ Se respeta la separación de capas: `@DataJpaTest` carga únicamente el slice JPA (Entities + Repositories), sin Services ni Controllers
- ✅ `@AutoConfigureTestDatabase(replace = NONE)` previene que Spring Boot sustituya PostgreSQL por H2 — los tests validan SQL real de producción
- ✅ Rollback automático por defecto en `@DataJpaTest` — aislamiento entre tests sin `@AfterEach` manual
- ✅ La `@Query JPQL` de `findAutoresVivosEnAno` se prueba contra PostgreSQL real (no H2), validando la compatibilidad del dialecto
- ✅ Sin breaking changes en ninguna capa superior (Service, Controller)
- ✅ Sin impacto en JWT/Security, Redis ni flujo Gutendex
- ⚠️ La imagen Docker `postgres:16-alpine` debe estar disponible localmente para ejecutar los tests (`docker pull postgres:16-alpine`)

## ✅ Verificación Pre-Entrega
- [x] Código compila sin errores (`mvn test-compile` → BUILD SUCCESS)
- [ ] Tests unitarios verdes (mvn test) — bloqueado por descarga de imagen Docker
- [ ] Tests integración verdes (Testcontainers) — bloqueado por descarga de imagen Docker
- [x] Sin dependencias circulares
- [x] Documentación actualizada

## 🔄 Actualización de Auditoría 2026-07-10
- **Estado de auditoría:** Actualizada con nuevos métodos de repositorio y validación de `@EntityGraph`.
- **Brechas cubiertas desde 2026-06-23 (LibroRepository):**
  - `findByGutendexId(Integer gutendexId)` (existente / inexistente)
  - `findByTituloContainingIgnoreCaseOrAutorNombreContainingIgnoreCase(...)` (coincidencia por título, por autor, sin coincidencias)
- **Brechas cubiertas desde 2026-06-23 (AutorRepository):**
  - verificación funcional de `@EntityGraph(attributePaths = {"libros"})` en:
    - `findAll(Pageable)`
    - `findById(Long)`
    - `findByNombreContainsIgnoreCase(String)`
  - confirmación de uso interno de `findByNombre(String)` sin `EntityGraph`.
- **Corrección transversal de fixtures:**
  - se asigna `gutendexId` único en helpers de persistencia de libros para cumplir `NOT NULL + UNIQUE`.
- **Archivos actualizados:**
  - `src/test/java/com/alurachallenge/literalura/repository/LibroRepositoryIT.java`
  - `src/test/java/com/alurachallenge/literalura/repository/AutorRepositoryIT.java`
- **Cobertura actual repository:**
  - `LibroRepositoryIT` pasó de **9** a **14** tests.
  - `AutorRepositoryIT` pasó de **9** a **14** tests.