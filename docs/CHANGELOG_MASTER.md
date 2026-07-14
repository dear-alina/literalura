# Historial de Cambios - Literalura v2.0

**Última actualización:** 2026-07-10
**Total de cambios documentados:** 7

---

## 📊 Índice de Cambios

| Fecha | Descripción | Categoría | Archivo |
|-------|------------|-----------|---------|
| 2026-06-23 | Primera suite de tests de integración para LibroController y AutorController con Testcontainers (PostgreSQL) + @ServiceConnection | Tests | [Ver](./changelog/2026-06-23_23-08-31_integration-tests-controller.md) |
| 2026-06-23 | Tests de integración para LibroService y AutorService con Testcontainers (PostgreSQL), @Transactional rollback y mock de ClienteGutendex | Tests | [Ver](./changelog/2026-06-23_23-23-18_integration-tests-service.md) |
| 2026-06-23 | Tests de integración para LibroRepository y AutorRepository con @DataJpaTest, Testcontainers (PostgreSQL) y cobertura completa de derived queries y @Query JPQL | Tests | [Ver](./changelog/2026-06-23_23-36-10_integration-tests-repository.md) |
| 2026-07-08 | Corrección de bug en AutorServiceIT: sincronización del lado inverso de relación @OneToMany en test helpers para evitar caché de sesión Hibernate inconsistente | Bugfix | — |
| 2026-07-08 | Nuevo endpoint `PATCH /api/libros/{id}/nota` para mutación atómica del campo nota. Nuevo DTO `ActualizarNotaDTO`. Campo `nota` agregado a `Libro`, DTOs de respuesta y lógica de actualización en `LibroService` | Feature | — |
| 2026-07-09 | Resolución de proxies LAZY en `AutorRepository` con `@EntityGraph(attributePaths = {"libros"})` en métodos de lectura del catálogo. Migración de `Autor.libros` de `FetchType.EAGER` a `LAZY`. `findByNombre` conserva LAZY puro para escrituras internas | JPA Optimization | [Ver](./REFACTORING_LOGS/2026-07-10-backend-refactor-libros-autores.md) |
| 2026-07-10 | Implementación completa del campo `gutendexId` en entidad `Libro` (`@Column unique + not null`). Nuevos endpoints `GET /api/libros/{id}` y `GET /api/libros/busqueda-flexible`. Deduplicación migrada de título a `gutendexId`. Campo `gutendexId` expuesto en todos los DTOs de respuesta de libros | Feature | [Ver](./REFACTORING_LOGS/2026-07-10-backend-refactor-libros-autores.md) |

---

## 📝 Cambios Registrados

### 2026-07-10 — Integración gutendexId + Nuevos Endpoints de Libros

**Archivos afectados:**
- `model/Libro.java` — campos `gutendexId` y `nota` con getters/setters
- `model/DatosLibro.java` — campo `id` agregado para mapear ID de Gutendex
- `model/Autor.java` — `FetchType.LAZY` en relación `@OneToMany`
- `repository/LibroRepository.java` — `findByGutendexId`, `findByTituloContainingIgnoreCaseOrAutorNombreContainingIgnoreCase`
- `repository/AutorRepository.java` — `@EntityGraph` en métodos de lectura + `findById(Long)` override
- `service/LibroService.java` — métodos `buscarFlexible`, `buscarLibroPorId`, `actualizarNotaLibro`; deduplicación por gutendexId
- `service/AutorService.java` — actualización de `convertirLibroADTO` con `gutendexId`
- `controller/LibroController.java` — endpoints `GET /{id}`, `GET /busqueda-flexible`, `PATCH /{id}/nota`
- `dto/LibroListResponseDTO.java` — campo `gutendexId`
- `dto/LibroDetalleResponseDTO.java` — campos `gutendexId` y `nota`
- `dto/LibroResponseDTO.java` — campo `gutendexId`
- `dto/LibroActualizadoResponseDTO.java` — campo `nota`
- `dto/ActualizarLibroDTO.java` — campo `nota`
- `dto/ActualizarNotaDTO.java` — **nuevo record**

### 2026-07-08 — Campo nota + Endpoint PATCH atómico

**Archivos afectados:**
- `model/Libro.java` — campo `nota` (`VARCHAR(1000)`)
- `dto/ActualizarNotaDTO.java` — creado
- `dto/ActualizarLibroDTO.java` — campo `nota` añadido
- `dto/LibroActualizadoResponseDTO.java` — campo `nota` añadido
- `service/LibroService.java` — método `actualizarNotaLibro(Long, ActualizarNotaDTO)`
- `controller/LibroController.java` — `PATCH /api/libros/{id}/nota`

### 2026-07-08 — Bugfix AutorServiceIT

**Problema:** `obtenerAutorDetalle_autorConLibros_deberiaRetornarDetalleConListaDeLibros` fallaba porque el caché de primer nivel de Hibernate devolvía la entidad `Autor` con `libros = null` al llamar `findById` dentro de la misma transacción, aunque los libros habían sido persistidos con `flush`.

**Solución:** Sincronización del lado inverso de la relación en los helpers del test:
- `guardarAutor()` inicializa `libros` como `ArrayList` vacío tras el `save`
- `guardarLibroParaAutor()` añade el libro a `autor.getLibros()` en memoria tras persistirlo

---

## 📌 Notas Importantes

- Cada cambio debe generar un archivo `.md` en `docs/changelog/` o `docs/REFACTORING_LOGS/`
- Mantén este archivo actualizado con cada cambio importante
- Usa el formato: `YYYY-MM-DD_HH-MM-SS_[descripcion-breve].md`
- El campo `gutendexId` es `NOT NULL` en BD — vaciar tabla `libros` antes del primer arranque si hay datos previos sin este campo

