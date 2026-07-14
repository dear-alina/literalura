# API Integration — Literalura

Documentación completa de todos los endpoints REST disponibles en el backend de **Literalura**.

---

## Información General

| Campo            | Valor                              |
|------------------|------------------------------------|
| **URL Base**     | `http://localhost:8080`            |
| **Prefijo API**  | `/api`                             |
| **Formato**      | JSON (`application/json`)          |
| **Base de datos**| PostgreSQL                         |
| **Framework**    | Spring Boot                        |

> **Nota:** No se define ningún `server.port` ni `server.servlet.context-path` en `application.properties`, por lo que el puerto por defecto es el **8080**.

---

## Formato de Error Estándar

Todos los errores devuelven el siguiente cuerpo JSON:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "mensaje": "Descripción del error",
  "ruta": "/api/libros/buscar"
}
```

---

## Módulo: Libros

**Base path:** `/api/libros`  
**Controlador:** `LibroController`

---

### `GET /api/libros`

- **Descripción:** Lista todos los libros registrados en la base de datos con soporte de paginación y ordenamiento dinámico.
- **Controlador y Método:** `LibroController.listarLibros`
- **Parámetros de Entrada:**
  - **Query Params (`@RequestParam`):**

    | Nombre      | Tipo     | Obligatorio | Default    | Ejemplo        | Descripción                                    |
    |-------------|----------|-------------|------------|----------------|------------------------------------------------|
    | `page`      | `int`    | No          | `0`        | `page=0`       | Número de página (basado en 0)                 |
    | `size`      | `int`    | No          | `10`       | `size=5`       | Cantidad de elementos por página               |
    | `sort`      | `String` | No          | `titulo`   | `sort=titulo`  | Campo por el cual ordenar                      |
    | `direction` | `String` | No          | `asc`      | `direction=desc` | Dirección del ordenamiento: `asc` o `desc`   |

- **Cuerpo de Solicitud:** No aplica.
- **Cuerpo de Respuesta:**

```json
{
  "content": [
    {
      "id": 1,
      "titulo": "Oliver Twist",
      "autor": "Charles Dickens",
      "idioma": "INGLES"
    },
    {
      "id": 2,
      "titulo": "Don Quijote",
      "autor": "Miguel de Cervantes",
      "idioma": "ESPANOL"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": { "sorted": true, "unsorted": false }
  },
  "totalElements": 2,
  "totalPages": 1,
  "last": true,
  "first": true
}
```

- **Códigos de Estado HTTP:**
  - `200 OK` — Listado obtenido correctamente (puede estar vacío).

---

### `POST /api/libros/buscar-y-registrar`

- **Descripción:** Busca un libro por título en la API externa **Gutendex**. Si ya existe en la base de datos local lo retorna directamente; si no existe, lo registra junto con su autor y devuelve los datos persistidos.
- **Controlador y Método:** `LibroController.buscarYRegistrar`
- **Parámetros de Entrada:** Ninguno.
- **Cuerpo de Solicitud (Request Body):**

```json
{
  "titulo": "Oliver Twist"
}
```

> El campo `titulo` es **obligatorio** (`@NotBlank`).

- **Cuerpo de Respuesta — Éxito (libro registrado):**

```json
{
  "id": 1,
  "titulo": "Oliver Twist",
  "autor": "Charles Dickens",
  "idioma": "INGLES",
  "mensaje": "Libro registrado exitosamente"
}
```

- **Cuerpo de Respuesta — Libro ya existente:**

```json
{
  "id": 1,
  "titulo": "Oliver Twist",
  "autor": "Charles Dickens",
  "idioma": "INGLES",
  "mensaje": "Libro ya existe en la base de datos"
}
```

- **Cuerpo de Respuesta — Error (libro no encontrado en Gutendex):**

```json
{
  "id": null,
  "titulo": null,
  "autor": null,
  "idioma": null,
  "mensaje": "Libro no encontrado en Gutendex"
}
```

- **Códigos de Estado HTTP:**
  - `201 Created` — Libro buscado y/o registrado exitosamente.
  - `400 Bad Request` — El campo `titulo` está vacío o no se envió.
  - `404 Not Found` — El título no fue encontrado en Gutendex.
  - `500 Internal Server Error` — Error interno del servidor (p. ej. fallo de red con Gutendex).

---

### `GET /api/libros/busqueda-flexible`

- **Descripción:** ⭐ **Búsqueda flexible en tiempo real dentro del catálogo local.** Coincidencia parcial e insensible a mayúsculas/minúsculas en títulos de libros **o** nombres de autores. Ideal para inputs de búsqueda en el frontend. Si `q` se omite o está vacío, retorna todos los libros.
- **Controlador y Método:** `LibroController.busquedaFlexible`
- **Parámetros de Entrada:**
  - **Query Params (`@RequestParam`):**

    | Nombre | Tipo     | Obligatorio | Ejemplo       | Descripción                                                  |
    |--------|----------|-------------|---------------|--------------------------------------------------------------|
    | `q`    | `String` | No          | `q=oliver`    | Término libre. Coincide parcialmente con título o autor.     |

- **Cuerpo de Solicitud:** No aplica.
- **Cuerpo de Respuesta (200 OK):**

```json
[
  {
    "id": 1,
    "titulo": "Oliver Twist",
    "autor": "Charles Dickens",
    "idioma": "INGLES"
  },
  {
    "id": 3,
    "titulo": "A Tale of Two Cities",
    "autor": "Charles Dickens",
    "idioma": "INGLES"
  }
]
```

> **Ejemplo:** `q=dickens` retorna todos los libros de Charles Dickens.  
> **Ejemplo:** `q=oliver` retorna "Oliver Twist" y cualquier libro cuyo autor contenga "oliver".  
> **Ejemplo:** sin parámetro `q` retorna el catálogo completo.

- **Códigos de Estado HTTP:**
  - `200 OK` — Búsqueda exitosa (lista vacía `[]` si no hay coincidencias).

---

### `GET /api/libros/buscar`

- **Descripción:** ⭐ **Busca un libro por su título exacto** dentro de la base de datos local. Devuelve información detallada incluyendo los datos completos del autor.
- **Controlador y Método:** `LibroController.buscarPorTitulo`
- **Parámetros de Entrada:**
  - **Query Params (`@RequestParam`):**

    | Nombre   | Tipo     | Obligatorio | Ejemplo                      |
    |----------|----------|-------------|------------------------------|
    | `titulo` | `String` | **Sí**      | `titulo=Oliver Twist`        |

- **Cuerpo de Solicitud:** No aplica.
- **Cuerpo de Respuesta — Éxito:**

```json
{
  "id": 1,
  "titulo": "Oliver Twist",
  "autor": {
    "id": 10,
    "nombre": "Charles Dickens",
    "anoNacimiento": 1812,
    "anoFallecimiento": 1870
  },
  "idioma": "INGLES"
}
```

- **Cuerpo de Respuesta — Error (no encontrado):**

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "mensaje": "Libro 'Oliver Twist' no encontrado en la base de datos",
  "ruta": "/api/libros/buscar"
}
```

- **Códigos de Estado HTTP:**
  - `200 OK` — Libro encontrado.
  - `404 Not Found` — No existe un libro con ese título en la BD.

---

### `GET /api/libros/idioma`

- **Descripción:** ⭐ **Filtra todos los libros por idioma.** Retorna la lista completa de libros registrados en el idioma indicado junto con el conteo total.
- **Controlador y Método:** `LibroController.obtenerLibrosPorIdioma`
- **Parámetros de Entrada:**
  - **Query Params (`@RequestParam`):**

    | Nombre   | Tipo     | Obligatorio | Valores válidos              | Ejemplo         |
    |----------|----------|-------------|------------------------------|-----------------|
    | `idioma` | `String` | **Sí**      | `en`, `es`, `pt`, `ru`       | `idioma=en`     |

  > **Códigos de idioma disponibles:**
  > | Código | Idioma     |
  > |--------|------------|
  > | `en`   | Inglés     |
  > | `es`   | Español    |
  > | `pt`   | Portugués  |
  > | `ru`   | Ruso       |

- **Cuerpo de Solicitud:** No aplica.
- **Cuerpo de Respuesta — Éxito:**

```json
{
  "idioma": "INGLES",
  "totalLibros": 2,
  "libros": [
    {
      "id": 1,
      "titulo": "Oliver Twist",
      "autor": "Charles Dickens",
      "idioma": "INGLES"
    },
    {
      "id": 3,
      "titulo": "A Tale of Two Cities",
      "autor": "Charles Dickens",
      "idioma": "INGLES"
    }
  ]
}
```

- **Cuerpo de Respuesta — Error (código inválido):**

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "mensaje": "Código de idioma inválido: fr. Códigos válidos: en, es, pt, ru",
  "ruta": "/api/libros/idioma"
}
```

- **Códigos de Estado HTTP:**
  - `200 OK` — Consulta exitosa (puede retornar lista vacía si no hay libros en ese idioma).
  - `400 Bad Request` — Código de idioma no reconocido.

---

### `PUT /api/libros/{id}`

- **Descripción:** Actualiza parcialmente los datos de un libro existente (título, nombre del autor y/o idioma). Solo se modifican los campos que se envíen con valor no nulo y no vacío.
- **Controlador y Método:** `LibroController.actualizarLibro`
- **Parámetros de Entrada:**
  - **Path Variables (`@PathVariable`):**

    | Nombre | Tipo   | Ejemplo |
    |--------|--------|---------|
    | `id`   | `Long` | `1`     |

- **Cuerpo de Solicitud (Request Body):** Todos los campos son **opcionales**; envía solo los que deseas modificar.

```json
{
  "titulo": "Oliver Twist - Edición Revisada",
  "autorNombre": "Charles Dickens",
  "idioma": "en"
}
```

- **Cuerpo de Respuesta — Éxito:**

```json
{
  "id": 1,
  "titulo": "Oliver Twist - Edición Revisada",
  "autor": "Charles Dickens",
  "idioma": "INGLES",
  "mensaje": "Libro actualizado exitosamente"
}
```

- **Códigos de Estado HTTP:**
  - `200 OK` — Libro actualizado correctamente.
  - `400 Bad Request` — Código de idioma inválido.
  - `404 Not Found` — No existe un libro con ese ID.

---

### `DELETE /api/libros/{id}`

- **Descripción:** Elimina permanentemente un libro de la base de datos por su ID.
- **Controlador y Método:** `LibroController.eliminarLibro`
- **Parámetros de Entrada:**
  - **Path Variables (`@PathVariable`):**

    | Nombre | Tipo   | Ejemplo |
    |--------|--------|---------|
    | `id`   | `Long` | `1`     |

- **Cuerpo de Solicitud:** No aplica.
- **Cuerpo de Respuesta:** Sin cuerpo.
- **Códigos de Estado HTTP:**
  - `204 No Content` — Libro eliminado exitosamente.
  - `404 Not Found` — No existe un libro con ese ID.

---

### `PATCH /api/libros/{id}/nota`

- **Descripción:** ⭐ **Actualización atómica y exclusiva de la nota/reseña personal de un libro.** Opera únicamente sobre el campo `nota`, garantizando que ningún otro atributo estructural (título, autor, idioma) sea sobreescrito o anulado por modelos incompletos del frontend.
- **Controlador y Método:** `LibroController.actualizarNota`
- **Parámetros de Entrada:**
  - **Path Variables (`@PathVariable`):**

    | Nombre | Tipo   | Ejemplo |
    |--------|--------|---------|
    | `id`   | `Long` | `1`     |

- **Cuerpo de Solicitud (Request Body):**

```json
{
  "nota": "Una novela imprescindible que retrata la dureza de la sociedad victoriana."
}
```

> Enviar `"nota": null` borra la nota existente. Enviar `"nota": "texto"` la establece o reemplaza.

- **Cuerpo de Respuesta (200 OK):**

```json
{
  "id": 1,
  "titulo": "Oliver Twist",
  "autor": "Charles Dickens",
  "idioma": "INGLES",
  "nota": "Una novela imprescindible que retrata la dureza de la sociedad victoriana.",
  "mensaje": "Nota actualizada exitosamente"
}
```

- **Ventaja Técnica:** Mutación atómica. No requiere enviar título, autor ni idioma — elimina el riesgo de sobreescribir columnas nulas o perder metadatos con un `PUT` de modelo incompleto.
- **Códigos de Estado HTTP:**
  - `200 OK` — Nota actualizada correctamente.
  - `404 Not Found` — No existe un libro con ese ID.

---

## Módulo: Autores

**Base path:** `/api/autores`  
**Controlador:** `AutorController`

---

### `GET /api/autores`

- **Descripción:** Lista todos los autores registrados en la base de datos con soporte de paginación y ordenamiento dinámico. Incluye el conteo total de libros por autor.
- **Controlador y Método:** `AutorController.listarAutores`
- **Parámetros de Entrada:**
  - **Query Params (`@RequestParam`):**

    | Nombre      | Tipo     | Obligatorio | Default    | Ejemplo          | Descripción                                    |
    |-------------|----------|-------------|------------|------------------|------------------------------------------------|
    | `page`      | `int`    | No          | `0`        | `page=0`         | Número de página (basado en 0)                 |
    | `size`      | `int`    | No          | `10`       | `size=5`         | Cantidad de elementos por página               |
    | `sort`      | `String` | No          | `nombre`   | `sort=nombre`    | Campo por el cual ordenar                      |
    | `direction` | `String` | No          | `asc`      | `direction=desc` | Dirección del ordenamiento: `asc` o `desc`     |

- **Cuerpo de Solicitud:** No aplica.
- **Cuerpo de Respuesta:**

```json
{
  "content": [
    {
      "id": 10,
      "nombre": "Charles Dickens",
      "anoNacimiento": 1812,
      "anoFallecimiento": 1870,
      "totalLibros": 3
    },
    {
      "id": 11,
      "nombre": "Gabriel García Márquez",
      "anoNacimiento": 1927,
      "anoFallecimiento": 2014,
      "totalLibros": 1
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": { "sorted": true, "unsorted": false }
  },
  "totalElements": 2,
  "totalPages": 1,
  "last": true,
  "first": true
}
```

- **Códigos de Estado HTTP:**
  - `200 OK` — Listado obtenido correctamente (puede estar vacío).

---

### `GET /api/autores/vivos`

- **Descripción:** ⭐ **Filtra autores que estaban vivos en un año específico.** Un autor se considera vivo si su año de nacimiento es ≤ al año indicado Y su año de fallecimiento es ≥ al año indicado (o no tiene año de fallecimiento registrado).
- **Controlador y Método:** `AutorController.obtenerAutoresVivos`
- **Parámetros de Entrada:**
  - **Query Params (`@RequestParam`):**

    | Nombre | Tipo      | Obligatorio | Ejemplo   | Descripción                     |
    |--------|-----------|-------------|-----------|----------------------------------|
    | `ano`  | `Integer` | **Sí**      | `ano=1900`| Año de consulta (debe ser ≥ 0)  |

- **Cuerpo de Solicitud:** No aplica.
- **Cuerpo de Respuesta — Éxito:**

```json
[
  {
    "id": 10,
    "nombre": "Charles Dickens",
    "anoNacimiento": 1812,
    "anoFallecimiento": 1870,
    "totalLibros": 3
  },
  {
    "id": 12,
    "nombre": "Leo Tolstói",
    "anoNacimiento": 1828,
    "anoFallecimiento": 1910,
    "totalLibros": 2
  }
]
```

- **Cuerpo de Respuesta — Error (año negativo):**

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "mensaje": "El año debe ser un número válido positivo",
  "ruta": "/api/autores/vivos"
}
```

- **Códigos de Estado HTTP:**
  - `200 OK` — Consulta exitosa (puede retornar lista vacía).
  - `400 Bad Request` — El año proporcionado es negativo o nulo.

---

### `GET /api/autores/{id}`

- **Descripción:** Obtiene el detalle completo de un autor por su ID, incluyendo la lista de todos sus libros registrados en la base de datos.
- **Controlador y Método:** `AutorController.obtenerDetalleAutor`
- **Parámetros de Entrada:**
  - **Path Variables (`@PathVariable`):**

    | Nombre | Tipo   | Ejemplo |
    |--------|--------|---------|
    | `id`   | `Long` | `10`    |

- **Cuerpo de Solicitud:** No aplica.
- **Cuerpo de Respuesta — Éxito:**

```json
{
  "id": 10,
  "nombre": "Charles Dickens",
  "anoNacimiento": 1812,
  "anoFallecimiento": 1870,
  "libros": [
    {
      "id": 1,
      "titulo": "Oliver Twist",
      "autor": "Charles Dickens",
      "idioma": "INGLES"
    },
    {
      "id": 3,
      "titulo": "A Tale of Two Cities",
      "autor": "Charles Dickens",
      "idioma": "INGLES"
    }
  ]
}
```

- **Cuerpo de Respuesta — Error (no encontrado):**

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 500,
  "mensaje": "Autor no encontrado",
  "ruta": "/api/autores/999"
}
```

- **Códigos de Estado HTTP:**
  - `200 OK` — Autor encontrado con su detalle.
  - `500 Internal Server Error` — No existe un autor con ese ID (`RuntimeException` lanzada desde el servicio; considerar refactorizar a `ResourceNotFoundException` para obtener `404`).

---

## Resumen de Endpoints

| Método   | Ruta                                | Descripción                                              |
|----------|-------------------------------------|----------------------------------------------------------|
| `GET`    | `/api/libros`                       | Listar todos los libros (paginado)                       |
| `POST`   | `/api/libros/buscar-y-registrar`    | Buscar en Gutendex y registrar libro                     |
| `GET`    | `/api/libros/busqueda-flexible`     | ⭐ Búsqueda flexible por título o autor (parcial)        |
| `GET`    | `/api/libros/buscar`                | Buscar libro por título exacto en BD                     |
| `GET`    | `/api/libros/idioma`                | ⭐ Filtrar libros por idioma (`en`, `es`, `pt`, `ru`)    |
| `GET`    | `/api/libros/{id}`                  | Obtener detalle completo de un libro por ID              |
| `PUT`    | `/api/libros/{id}`                  | Actualizar datos de un libro                             |
| `PATCH`  | `/api/libros/{id}/nota`             | ⭐ Actualizar exclusivamente la nota de un libro          |
| `DELETE` | `/api/libros/{id}`                  | Eliminar un libro                                        |
| `GET`    | `/api/autores`                      | Listar todos los autores (paginado)                      |
| `GET`    | `/api/autores/vivos`                | ⭐ Filtrar autores vivos en un año específico             |
| `GET`    | `/api/autores/{id}`                 | Obtener detalle de un autor con sus libros               |

---

## Notas de Integración

1. **Búsqueda por título (`GET /api/libros/buscar`):** La búsqueda es **exacta** (`findByTitulo`). Para encontrar un libro, el parámetro `titulo` debe coincidir exactamente con el valor almacenado en la base de datos, incluyendo mayúsculas/minúsculas.

2. **Registro automático de autor:** Al usar `POST /api/libros/buscar-y-registrar`, si el autor del libro obtenido desde Gutendex no existe en la BD, se crea automáticamente con sus datos de nacimiento y fallecimiento.

3. **Actualización parcial (`PUT /api/libros/{id}`):** Si en el body se envía un `autorNombre` que no existe en la BD, se crea un autor nuevo solo con ese nombre (sin años de nacimiento/fallecimiento).

4. **Idiomas soportados:** Solo se admiten los códigos `en`, `es`, `pt` y `ru` en los endpoints que reciben idioma como parámetro.

5. **`GET /api/autores/{id}` retorna 500 en lugar de 404** cuando el autor no existe (el servicio lanza `RuntimeException` genérica). Se recomienda migrar a `ResourceNotFoundException` para consistencia.
