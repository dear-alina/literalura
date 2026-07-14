# Log de Refactorización Backend: Módulo Libros y Autores — Evolución Completa de API y Persistencia

- **Fecha:** 2026-07-10
- **Tipo de Ajuste:** Refactorización de Capas Múltiples (Entidad · Repositorio · Servicio · Controlador · DTOs)
- **Origen del Cambio:** Modificación directa en el espacio de trabajo.

---

## 🔍 1. Contexto y Objetivo Técnico

El backend presentaba cuatro problemáticas simultáneas que degradaban la experiencia del frontend y la integridad de los datos:

1. **`LazyInitializationException` / colecciones vacías en autores:** La relación `@OneToMany` entre `Autor` y `Libro` estaba configurada con `FetchType.EAGER`, lo que causaba cascadas innecesarias y problemas en borrados atómicos de libros individuales. Al migrarla a `LAZY`, las colecciones llegaban sin inicializar al serializador Jackson fuera de la sesión transaccional, devolviendo listas vacías o `null` al frontend.

2. **`HttpRequestMethodNotSupportedException` en `GET /api/libros/{id}`:** El controlador no exponía un handler para `GET` individual por ID, solo para `PUT` y `DELETE` en esa ruta.

3. **Ausencia de búsqueda flexible:** El único endpoint de búsqueda (`GET /api/libros/buscar`) realizaba coincidencia exacta por título. No existía búsqueda parcial ni por autor.

4. **`gutendexId` no persistido ni expuesto:** La entidad `Libro` no almacenaba el ID numérico de la API de Gutendex Project, impidiendo que el frontend construyera URLs de portadas (`https://gutenberg.org/{id}/pg{id}.cover.medium.jpg`). La deduplicación de libros se realizaba por título (frágil), en lugar de por ID canónico.

---

## 💻 2. Impacto por Capas y Archivos Modificados

### Capa de Entidades (`@Entity`)

**`src/main/java/com/alurachallenge/literalura/model/Libro.java`**
- Añadido campo `@Column(name = "gutendex_id", unique = true, nullable = false) private Integer gutendexId` con getter y setter.
- Añadido campo `@Column(length = 1000) private String nota` con getter y setter.
- Hibernate aplicará `ALTER TABLE libros ADD COLUMN gutendex_id INTEGER UNIQUE NOT NULL` y `ADD COLUMN nota VARCHAR(1000)` en el próximo arranque (`ddl-auto=update`).

**`src/main/java/com/alurachallenge/literalura/model/Autor.java`**
- Migrada la relación `@OneToMany` de `FetchType.EAGER` a `FetchType.LAZY`.
- Justificación: LAZY por defecto previene cargas masivas en operaciones de escritura; los reads que requieren la colección se gestionan mediante `@EntityGraph` en el repositorio.

**`src/main/java/com/alurachallenge/literalura/model/DatosLibro.java`**
- Añadido campo `@JsonAlias("id") Integer id` para capturar el ID de Gutendex desde la respuesta JSON de la API externa.

---

### Capa de Persistencia (`@Repository`)

**`src/main/java/com/alurachallenge/literalura/repository/AutorRepository.java`**

Aplicado `@EntityGraph(attributePaths = {"libros"})` de forma selectiva para forzar `LEFT OUTER JOIN` dinámico únicamente en los métodos de lectura que el frontend serializa:

| Método | `@EntityGraph` | Justificación |
|---|:---:|---|
| `findAll(Pageable)` | ✅ | Catálogo paginado — necesita `totalLibros` |
| `findAutoresVivosEnAno` | ✅ | Vista de filtrado por año — serializa colección |
| `findByNombreContainsIgnoreCase` | ✅ | Búsqueda de detalle |
| `findById(Long)` | ✅ | Vista modal de detalle individual |
| `findByNombre(String)` | ❌ | Uso interno en escritura — LAZY puro para no comprometer rendimiento |

**`src/main/java/com/alurachallenge/literalura/repository/LibroRepository.java`**
- Añadido `Optional<Libro> findByGutendexId(Integer gutendexId)` para deduplicación canónica por ID de Gutendex.
- Añadido `List<Libro> findByTituloContainingIgnoreCaseOrAutorNombreContainingIgnoreCase(String titulo, String autorNombre)` para búsqueda flexible parcial e insensible a mayúsculas.

---

### Capa de Negocio (`@Service`)

**`src/main/java/com/alurachallenge/literalura/service/LibroService.java`**

| Método | Cambio |
|---|---|
| `buscarYRegistrarLibro` | Reordenado: consulta Gutendex primero → deduplica por `gutendexId` (antes era por título) → asigna `gutendexId` al nuevo libro |
| `buscarFlexible(String q)` | **Nuevo.** Búsqueda parcial case-insensitive en título y nombre de autor |
| `buscarLibroPorId(Long id)` | **Nuevo.** Recupera libro por ID interno con `ResourceNotFoundException` |
| `actualizarNotaLibro(Long, ActualizarNotaDTO)` | **Nuevo.** Mutación atómica exclusiva del campo `nota` |
| `actualizarLibro` | Añadida actualización del campo `nota` con soporte para borrado (`isBlank → null`) |
| `convertirAListDTO` | Añadido `libro.getGutendexId()` al constructor de `LibroListResponseDTO` |
| `convertirADetalleDTO` | Añadidos `libro.getGutendexId()` y `libro.getNota()` al constructor de `LibroDetalleResponseDTO` |

**`src/main/java/com/alurachallenge/literalura/service/AutorService.java`**
- Actualizado `convertirLibroADTO` para incluir `libro.getGutendexId()` en el constructor de `LibroListResponseDTO` (corrección de error de compilación por arity mismatch).

---

### Capa de Transferencia (`DTO` / `Record`)

| Archivo | Campos añadidos | Usado en |
|---|---|---|
| `LibroListResponseDTO` | `Integer gutendexId` | `GET /api/libros`, búsqueda flexible, filtro por idioma |
| `LibroDetalleResponseDTO` | `Integer gutendexId`, `String nota` | `GET /api/libros/{id}`, `GET /api/libros/buscar` |
| `LibroResponseDTO` | `Integer gutendexId`, `String nota` *(vía `LibroActualizadoResponseDTO`)* | `POST /api/libros/buscar-y-registrar` |
| `LibroActualizadoResponseDTO` | `String nota` | `PUT /api/libros/{id}`, `PATCH /api/libros/{id}/nota` |
| `ActualizarLibroDTO` | `String nota` | `PUT /api/libros/{id}` |
| `ActualizarNotaDTO` | **Nuevo record** — `String nota` | `PATCH /api/libros/{id}/nota` |

---

### Capa de Controladores (`@RestController`)

**`src/main/java/com/alurachallenge/literalura/controller/LibroController.java`**

Endpoints añadidos:

| Método | Ruta | Handler |
|---|---|---|
| `GET` | `/api/libros/{id}` | `obtenerLibroPorId` |
| `GET` | `/api/libros/busqueda-flexible` | `busquedaFlexible` |
| `PATCH` | `/api/libros/{id}/nota` | `actualizarNota` |

Corrección: constructores de `LibroResponseDTO` en bloques `catch` actualizados a la nueva firma (6 argumentos).

---

## 🛠️ 3. Contrato de API — Nuevos Endpoints

### `GET /api/libros/{id}`
- **Parámetros de Ruta:** `id` (Long)
- **Respuesta (200 OK):**
```json
{
  "id": 1,
  "titulo": "Pride and Prejudice",
  "gutendexId": 1342,
  "autor": { "id": 5, "nombre": "Jane Austen", "anoNacimiento": 1775, "anoFallecimiento": 1817 },
  "idioma": "INGLES",
  "nota": "Mi reseña personal"
}
```
- **Errores:** `404 Not Found` si el libro no existe.

---

### `GET /api/libros/busqueda-flexible`
- **Query Params:** `q` (String, opcional)
- **Comportamiento:** Coincidencia parcial e insensible a mayúsculas en título O nombre de autor. Sin `q` retorna el catálogo completo.
- **Respuesta (200 OK):**
```json
[
  {
    "id": 1,
    "titulo": "Pride and Prejudice",
    "gutendexId": 1342,
    "autor": "Jane Austen",
    "idioma": "INGLES"
  }
]
```
- **Ejemplos:** `?q=pride` → libros con "pride" en el título; `?q=austen` → libros de Jane Austen.

---

### `PATCH /api/libros/{id}/nota`
- **Parámetros de Ruta:** `id` (Long)
- **Cuerpo de Solicitud:**
```json
{ "nota": "Una novela imprescindible del siglo XIX." }
```
> Enviar `"nota": null` borra la nota existente. Omitir el campo no modifica el valor.
- **Respuesta (200 OK):**
```json
{
  "id": 1,
  "titulo": "Pride and Prejudice",
  "autor": "Jane Austen",
  "idioma": "INGLES",
  "nota": "Una novela imprescindible del siglo XIX.",
  "mensaje": "Nota actualizada exitosamente"
}
```
- **Ventaja:** Mutación atómica — no requiere enviar título, autor ni idioma. Elimina el riesgo de sobreescritura de datos estructurales con modelos incompletos del frontend.
- **Errores:** `404 Not Found` si el libro no existe.

---

## 🗄️ 4. Cambios en el Esquema de Base de Datos

Hibernate aplicará automáticamente las siguientes alteraciones al arrancar con `ddl-auto=update`:

```sql
ALTER TABLE libros ADD COLUMN gutendex_id INTEGER UNIQUE NOT NULL;
ALTER TABLE libros ADD COLUMN nota VARCHAR(1000);
```

> ⚠️ **Precaución de migración:** Si existen filas en la tabla `libros` sin `gutendex_id`, la restricción `NOT NULL` causará un error en el arranque. Opciones:
> - Vaciar la tabla `libros` antes del primer arranque con el nuevo schema.
> - Cambiar temporalmente a `nullable = true`, ejecutar una migración manual de datos y luego restaurar la restricción.

---

## 🔐 5. Aislamiento de Operaciones Críticas

| Operación | Estrategia | Razón |
|---|---|---|
| `DELETE /api/libros/{id}` | LAZY en `Autor.libros` | Borrado atómico sin cargar colección; evita cascadas no deseadas |
| `findByNombre` en `AutorRepository` | Sin `@EntityGraph` | Solo usado en validaciones internas de escritura; nunca serializado |
| `actualizarNotaLibro` | `@Transactional` explícito | Garantiza que la mutación es atómica aunque el método esté en clase con `@Transactional` de clase |

---

## 🔗 6. URL de Portada — Contrato con el Frontend

Con `gutendexId` disponible en todas las respuestas de libros, el frontend puede construir la URL de portada directamente:

```javascript
const coverUrl = `https://gutenberg.org/${libro.gutendexId}/pg${libro.gutendexId}.cover.medium.jpg`;
```
