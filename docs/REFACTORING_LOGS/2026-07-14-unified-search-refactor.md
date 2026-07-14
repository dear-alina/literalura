# Log de Refactorización: Endpoint de Búsqueda Unificada

**Fecha:** 2026-07-14  
**Autor del cambio:** Refactorización asistida por GitHub Copilot

---

## Archivos Modificados

| Archivo | Tipo de cambio |
|---|---|
| `src/main/java/com/alurachallenge/literalura/service/LibroService.java` | Modificado — lógica unificada + import añadido |
| `src/main/java/com/alurachallenge/literalura/controller/LibroController.java` | Modificado — eliminación de try-catch manual |

---

## Detalle Técnico

### Endpoint afectado

```
GET /api/libros/buscar?titulo={titulo}
```

Expuesto en `LibroController.buscarPorTitulo()`, que delega a `LibroService.buscarLibroPorTitulo()`.

### Flujo implementado: BD Local → API Externa → Guardado en BD → Retorno

```
Cliente HTTP
    │
    ▼
GET /api/libros/buscar?titulo=...
    │
    ▼
LibroController.buscarPorTitulo(titulo)
    │
    ▼
LibroService.buscarLibroPorTitulo(titulo)  [@Transactional]
    │
    ├─ [Paso 1] libroRepository.findByTituloIgnoreCase(titulo)
    │
    ├─ [Paso 2] HIT LOCAL → return convertirADetalleDTO(libro)  ──► HTTP 200
    │
    └─ [Paso 3] MISS LOCAL → clienteGutendex.buscarLibrosPorTitulo(titulo)
         │
         ├─ [Paso 4] HIT EXTERNO:
         │       ├─ Deduplicar: findByGutendexId(id) → si existe, devolver sin duplicar
         │       ├─ obtenerOCrearAutor(datosGutendex.autores())
         │       ├─ new Libro() ← mapear datos del DTO Gutendex
         │       ├─ libroRepository.save(nuevoLibro)  ← persistencia en Supabase
         │       └─ return convertirADetalleDTO(libroGuardado)  ──► HTTP 200
         │
         └─ [Paso 5] MISS COMPLETO → throw LibroNoEncontradoException  ──► HTTP 404
```

### Cambios clave en `LibroService`

**Antes:**
```java
public LibroDetalleResponseDTO buscarLibroPorTitulo(String titulo) {
    Libro libro = libroRepository.findByTitulo(titulo)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Libro '" + titulo + "' no encontrado en la base de datos"
        ));
    return convertirADetalleDTO(libro);
}
```

**Después:**
```java
@Transactional
public LibroDetalleResponseDTO buscarLibroPorTitulo(String titulo) {
    // Paso 1 & 2: BD local (case-insensitive)
    Optional<Libro> libroLocal = libroRepository.findByTituloIgnoreCase(titulo);
    if (libroLocal.isPresent()) {
        return convertirADetalleDTO(libroLocal.get());
    }

    // Paso 3: Consulta Gutendex
    RespuestaGutendex respuesta = clienteGutendex.buscarLibrosPorTitulo(titulo);

    if (respuesta == null || respuesta.resultados() == null || respuesta.resultados().isEmpty()) {
        throw new LibroNoEncontradoException(
            "Libro '" + titulo + "' no encontrado ni en la base de datos local ni en Gutendex"
        );
    }

    // Paso 4: Persistir con deduplicación por gutendexId
    DatosGutendexLibro datosGutendex = respuesta.resultados().get(0);

    if (datosGutendex.id() != null) {
        Optional<Libro> porGutendexId = libroRepository.findByGutendexId(datosGutendex.id().intValue());
        if (porGutendexId.isPresent()) {
            return convertirADetalleDTO(porGutendexId.get());
        }
    }

    Autor autor = obtenerOCrearAutor(datosGutendex.autores());
    Libro nuevoLibro = new Libro();
    nuevoLibro.setTitulo(datosGutendex.titulo());
    nuevoLibro.setAutor(autor);
    nuevoLibro.setIdioma(mapearIdioma(datosGutendex.idiomas()));
    if (datosGutendex.id() != null) {
        nuevoLibro.setGutendexId(datosGutendex.id().intValue());
    }

    Libro libroGuardado = libroRepository.save(nuevoLibro);
    return convertirADetalleDTO(libroGuardado);
}
```

### Cambios en `LibroController`

- Eliminado el bloque `try-catch` manual en `buscarYRegistrar` que enmascaraba errores con respuestas genéricas.  
- Las excepciones ahora se propagan al `GlobalExceptionHandler` de forma consistente.
- `buscarYRegistrarLibro` en el servicio también fue actualizado: lanza `LibroNoEncontradoException` (HTTP 404) en lugar de `RuntimeException` genérico (que antes producía HTTP 500).

### Manejo de excepciones (cadena completa)

| Excepción | Capturada por | Código HTTP |
|---|---|---|
| `LibroNoEncontradoException` | `GlobalExceptionHandler.handleLibroNoEncontrado` | 404 Not Found |
| `ResourceNotFoundException` | `GlobalExceptionHandler.handleResourceNotFound` | 404 Not Found |
| `IllegalArgumentException` | `GlobalExceptionHandler.handleIllegalArgument` | 400 Bad Request |
| `RuntimeException` (genérica) | `GlobalExceptionHandler.handleRuntimeException` | 500 Internal Server Error |

### Deduplicación anti-duplicados

La estrategia de deduplicación se aplica en **dos niveles** dentro de `buscarLibroPorTitulo`:

1. **Por título** — `findByTituloIgnoreCase` (búsqueda local, Paso 1)
2. **Por `gutendexId`** — `findByGutendexId` (antes de persistir, Paso 4)

Esto garantiza que un libro obtenido de Gutendex nunca se inserte dos veces aunque el usuario use variaciones de capitalización en el título.

---

## Checklist de Regresión

- [x] El flujo no genera registros duplicados en la base de datos local (deduplicación doble: por título y por `gutendexId`).
- [x] El frontend sigue recibiendo la misma estructura JSON esperada (`LibroDetalleResponseDTO`: `id`, `titulo`, `gutendexId`, `autor`, `idioma`, `nota`).
- [x] Los endpoints no afectados (`/idioma`, `/{id}`, `PUT`, `PATCH`, `DELETE`, `GET` paginado) no fueron modificados.
- [x] El `GlobalExceptionHandler` centraliza todos los errores; no hay try-catch manuales en el controller que enmascaren la respuesta de error.
