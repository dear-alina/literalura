# 📚 Literalura API - Documentación Oficial de Endpoints

**Versión:** 1.0  
**Última Actualización:** Junio 2026  
**Base URL:** `http://localhost:8080/api`

---

## 📋 Tabla de Contenidos

1. [Endpoints de Libros](#endpoints-de-libros)
   - [Crear Libro (Buscar y Registrar)](#crear-libro-buscar-y-registrar)
   - [Buscar Libro por Título](#buscar-libro-por-título)
   - [Listar Libros](#listar-libros)
   - [Filtrar Libros por Idioma](#filtrar-libros-por-idioma)
   - [Actualizar Libro](#actualizar-libro)
   - [Eliminar Libro](#eliminar-libro)

2. [Endpoints de Autores](#endpoints-de-autores)
   - [Listar Autores](#listar-autores)
   - [Obtener Detalle de Autor](#obtener-detalle-de-autor)
   - [Listar Autores Vivos](#listar-autores-vivos)

3. [Guía de Parámetros](#guía-de-parámetros)
4. [Códigos de Estado HTTP](#códigos-de-estado-http)
5. [Ejemplos de Uso](#ejemplos-de-uso)

---

# 🔖 Endpoints de Libros

## Crear Libro (Buscar y Registrar)

### `POST /api/libros/buscar-y-registrar`

**Descripción:**  
Busca un libro en la API de Gutendex por su título y lo registra automáticamente en la base de datos. Si el libro ya existe en la BD, retorna sus datos con un mensaje indicando que ya estaba registrado.

**Parámetros:**

| Parámetro | Ubicación | Tipo | Obligatorio | Descripción |
|-----------|-----------|------|----------|-------------|
| `titulo` | Body (JSON) | String | Sí | Título del libro a buscar |

**Body (Ejemplo):**
```json
{
  "titulo": "1984"
}
```

**Respuestas:**

| Status | Descripción | Ejemplo |
|--------|-------------|---------|
| **201 Created** | Libro registrado exitosamente | Ver ejemplo abajo |
| **201 Created** | Libro ya existe en BD | Ver ejemplo abajo |
| **404 Not Found** | Libro no encontrado en Gutendex | `{"id": null, "titulo": null, "autor": null, "idioma": null, "mensaje": "Libro no encontrado en Gutendex"}` |
| **400 Bad Request** | Validación fallida (título vacío) | Error de validación |
| **500 Internal Server Error** | Error en servidor | `{"mensaje": "Error interno del servidor"}` |

**Respuesta (Nuevo Libro - 201):**
```json
{
  "id": 1,
  "titulo": "1984",
  "autor": "George Orwell",
  "idioma": "en",
  "mensaje": "Libro registrado exitosamente"
}
```

**Respuesta (Libro Duplicado - 201):**
```json
{
  "id": 1,
  "titulo": "1984",
  "autor": "George Orwell",
  "idioma": "en",
  "mensaje": "Libro ya existe en la base de datos"
}
```

---

## Buscar Libro por Título

### `GET /api/libros/buscar`

**Descripción:**  
Busca un libro específico en la base de datos local por su título exacto. Retorna información detallada del libro incluyendo datos completos del autor.

**Parámetros:**

| Parámetro | Ubicación | Tipo | Obligatorio | Descripción |
|-----------|-----------|------|----------|-------------|
| `titulo` | Query String | String | Sí | Título del libro a buscar (búsqueda exacta) |

**Ejemplo:**
```
GET /api/libros/buscar?titulo=1984
```

**Respuestas:**

| Status | Descripción |
|--------|-------------|
| **200 OK** | Libro encontrado |
| **404 Not Found** | Libro no encontrado |
| **400 Bad Request** | Parámetro faltante |

**Respuesta (200 OK):**
```json
{
  "id": 1,
  "titulo": "1984",
  "autor": {
    "id": 1,
    "nombre": "George Orwell",
    "anoNacimiento": 1903,
    "anoFallecimiento": 1950
  },
  "idioma": "en"
}
```

**Respuesta (404 Not Found):**
```json
{
  "timestamp": "2026-06-09T00:35:00",
  "status": 404,
  "mensaje": "Libro 'NoExiste' no encontrado en la base de datos",
  "ruta": "/api/libros/buscar"
}
```

---

## Listar Libros

### `GET /api/libros`

**Descripción:**  
Retorna una lista paginada de todos los libros registrados. Soporta paginación, ordenamiento y dirección de orden.

**Parámetros:**

| Parámetro | Ubicación | Tipo | Obligatorio | Predeterminado | Descripción |
|-----------|-----------|------|----------|---------|-------------|
| `page` | Query String | Integer | No | 0 | Número de página (base 0) |
| `size` | Query String | Integer | No | 10 | Cantidad de elementos por página |
| `sort` | Query String | String | No | titulo | Campo para ordenar |
| `direction` | Query String | String | No | asc | Dirección: `asc` o `desc` |

**Ejemplos:**
```
GET /api/libros
GET /api/libros?page=0&size=10&sort=titulo&direction=asc
GET /api/libros?page=1&size=5&sort=id&direction=desc
```

**Respuestas:**

| Status | Descripción |
|--------|-------------|
| **200 OK** | Libros encontrados |
| **200 OK** | Lista vacía (sin libros) |

**Respuesta (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "titulo": "1984",
      "autor": "George Orwell",
      "idioma": "en"
    },
    {
      "id": 2,
      "titulo": "Cien años de soledad",
      "autor": "Gabriel García Márquez",
      "idioma": "es"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 2,
  "totalElements": 15,
  "last": false,
  "size": 10,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "numberOfElements": 10,
  "first": true,
  "empty": false
}
```

---

## Filtrar Libros por Idioma

### `GET /api/libros/idioma`

**Descripción:**  
Filtra y retorna todos los libros que han sido registrados en un idioma específico. Retorna información agregada con total de libros y lista completa.

**Parámetros:**

| Parámetro | Ubicación | Tipo | Obligatorio | Valores Válidos | Descripción |
|-----------|-----------|------|----------|---------|-------------|
| `idioma` | Query String | String | Sí | en, es, pt, ru | Código ISO del idioma |

**Ejemplos:**
```
GET /api/libros/idioma?idioma=en
GET /api/libros/idioma?idioma=es
GET /api/libros/idioma?idioma=pt
GET /api/libros/idioma?idioma=ru
```

**Respuestas:**

| Status | Descripción |
|--------|-------------|
| **200 OK** | Libros encontrados |
| **200 OK** | Sin libros en ese idioma (lista vacía) |
| **400 Bad Request** | Código de idioma inválido |

**Respuesta (200 OK - Con Libros):**
```json
{
  "idioma": "INGLES",
  "totalLibros": 3,
  "libros": [
    {
      "id": 1,
      "titulo": "1984",
      "autor": "George Orwell",
      "idioma": "en"
    },
    {
      "id": 3,
      "titulo": "The Great Gatsby",
      "autor": "F. Scott Fitzgerald",
      "idioma": "en"
    }
  ]
}
```

**Respuesta (400 Bad Request):**
```json
{
  "timestamp": "2026-06-09T00:35:00",
  "status": 400,
  "mensaje": "Código de idioma inválido: xx. Códigos válidos: en, es, pt, ru",
  "ruta": "/api/libros/idioma"
}
```

---

## Actualizar Libro

### `PUT /api/libros/{id}`

**Descripción:**  
Actualiza la información de un libro existente. Permite actualizar título, autor e idioma de forma independiente. Solo actualiza los campos que se envíen en el body.

**Parámetros:**

| Parámetro | Ubicación | Tipo | Obligatorio | Descripción |
|-----------|-----------|------|----------|-------------|
| `id` | Path | Long | Sí | ID único del libro |
| `titulo` | Body (JSON) | String | No | Nuevo título del libro |
| `autorNombre` | Body (JSON) | String | No | Nombre del autor (crea si no existe) |
| `idioma` | Body (JSON) | String | No | Código ISO del idioma |

**Body (Ejemplo - Actualizar Todo):**
```json
{
  "titulo": "1984 - Edición Especial",
  "autorNombre": "George Orwell",
  "idioma": "es"
}
```

**Body (Ejemplo - Actualizar Solo Título):**
```json
{
  "titulo": "1984 Revisado"
}
```

**Ejemplos:**
```
PUT /api/libros/1
PUT /api/libros/5
PUT /api/libros/10
```

**Respuestas:**

| Status | Descripción |
|--------|-------------|
| **200 OK** | Libro actualizado exitosamente |
| **404 Not Found** | Libro no encontrado |
| **400 Bad Request** | Idioma inválido |

**Respuesta (200 OK):**
```json
{
  "id": 1,
  "titulo": "1984 - Edición Especial",
  "autor": "George Orwell",
  "idioma": "es",
  "mensaje": "Libro actualizado exitosamente"
}
```

**Respuesta (404 Not Found):**
```json
{
  "timestamp": "2026-06-09T00:35:00",
  "status": 404,
  "mensaje": "Libro con ID 999 no encontrado",
  "ruta": "/api/libros/999"
}
```

---

## Eliminar Libro

### `DELETE /api/libros/{id}`

**Descripción:**  
Elimina un libro de la base de datos de forma permanente. No retorna cuerpo de respuesta en caso de éxito.

**Parámetros:**

| Parámetro | Ubicación | Tipo | Obligatorio | Descripción |
|-----------|-----------|------|----------|-------------|
| `id` | Path | Long | Sí | ID único del libro a eliminar |

**Ejemplos:**
```
DELETE /api/libros/1
DELETE /api/libros/5
DELETE /api/libros/10
```

**Respuestas:**

| Status | Descripción |
|--------|-------------|
| **204 No Content** | Libro eliminado exitosamente |
| **404 Not Found** | Libro no encontrado |

**Respuesta (204 No Content):**
```
(Sin cuerpo de respuesta)
```

**Respuesta (404 Not Found):**
```json
{
  "timestamp": "2026-06-09T00:35:00",
  "status": 404,
  "mensaje": "Libro con ID 999 no encontrado",
  "ruta": "/api/libros/999"
}
```

---

# 👥 Endpoints de Autores

## Listar Autores

### `GET /api/autores`

**Descripción:**  
Retorna una lista paginada de todos los autores registrados. Soporta paginación, ordenamiento y dirección de orden. Incluye conteo de libros de cada autor.

**Parámetros:**

| Parámetro | Ubicación | Tipo | Obligatorio | Predeterminado | Descripción |
|-----------|-----------|------|----------|---------|-------------|
| `page` | Query String | Integer | No | 0 | Número de página (base 0) |
| `size` | Query String | Integer | No | 10 | Cantidad de elementos por página |
| `sort` | Query String | String | No | nombre | Campo para ordenar |
| `direction` | Query String | String | No | asc | Dirección: `asc` o `desc` |

**Ejemplos:**
```
GET /api/autores
GET /api/autores?page=0&size=10&sort=nombre&direction=asc
GET /api/autores?page=1&size=5&sort=anoNacimiento&direction=desc
```

**Respuestas:**

| Status | Descripción |
|--------|-------------|
| **200 OK** | Autores encontrados |
| **200 OK** | Lista vacía (sin autores) |

**Respuesta (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "nombre": "George Orwell",
      "anoNacimiento": 1903,
      "anoFallecimiento": 1950,
      "totalLibros": 3
    },
    {
      "id": 2,
      "nombre": "Gabriel García Márquez",
      "anoNacimiento": 1927,
      "anoFallecimiento": 2014,
      "totalLibros": 5
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 1,
  "totalElements": 2,
  "last": true,
  "size": 10,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "numberOfElements": 2,
  "first": true,
  "empty": false
}
```

---

## Obtener Detalle de Autor

### `GET /api/autores/{id}`

**Descripción:**  
Retorna información detallada de un autor específico, incluyendo la lista completa de libros que ha escrito.

**Parámetros:**

| Parámetro | Ubicación | Tipo | Obligatorio | Descripción |
|-----------|-----------|------|----------|-------------|
| `id` | Path | Long | Sí | ID único del autor |

**Ejemplos:**
```
GET /api/autores/1
GET /api/autores/5
GET /api/autores/10
```

**Respuestas:**

| Status | Descripción |
|--------|-------------|
| **200 OK** | Autor encontrado |
| **404 Not Found** | Autor no encontrado |

**Respuesta (200 OK):**
```json
{
  "id": 1,
  "nombre": "George Orwell",
  "anoNacimiento": 1903,
  "anoFallecimiento": 1950,
  "libros": [
    {
      "id": 1,
      "titulo": "1984",
      "autor": "George Orwell",
      "idioma": "en"
    },
    {
      "id": 2,
      "titulo": "Rebelión en la granja",
      "autor": "George Orwell",
      "idioma": "en"
    }
  ]
}
```

**Respuesta (404 Not Found):**
```json
{
  "timestamp": "2026-06-09T00:35:00",
  "status": 404,
  "mensaje": "Autor no encontrado",
  "ruta": "/api/autores/999"
}
```

---

## Listar Autores Vivos

### `GET /api/autores/vivos`

**Descripción:**  
Filtra y retorna todos los autores que estaban vivos durante un año específico. Un autor se considera vivo en un año si nació en o antes de ese año y falleció después de ese año (o no tiene fecha de fallecimiento).

**Parámetros:**

| Parámetro | Ubicación | Tipo | Obligatorio | Descripción |
|-----------|-----------|------|----------|-------------|
| `ano` | Query String | Integer | Sí | Año para filtrar autores vivos |

**Ejemplos:**
```
GET /api/autores/vivos?ano=1900
GET /api/autores/vivos?ano=1950
GET /api/autores/vivos?ano=2023
```

**Lógica de Filtro:**
Un autor está vivo en un año X si:
- `anoNacimiento <= X` (nació en o antes del año)
- `anoFallecimiento IS NULL OR anoFallecimiento >= X` (no ha fallecido o falleció después)

**Respuestas:**

| Status | Descripción |
|--------|-------------|
| **200 OK** | Autores encontrados |
| **200 OK** | Sin autores vivos (lista vacía) |
| **400 Bad Request** | Año inválido (negativo o no numérico) |

**Respuesta (200 OK):**
```json
[
  {
    "id": 1,
    "nombre": "George Orwell",
    "anoNacimiento": 1903,
    "anoFallecimiento": 1950,
    "totalLibros": 3
  },
  {
    "id": 3,
    "nombre": "J.R.R. Tolkien",
    "anoNacimiento": 1892,
    "anoFallecimiento": 1973,
    "totalLibros": 7
  }
]
```

**Respuesta (200 OK - Lista Vacía):**
```json
[]
```

**Respuesta (400 Bad Request):**
```json
{
  "timestamp": "2026-06-09T00:35:00",
  "status": 400,
  "mensaje": "El año debe ser un número válido positivo",
  "ruta": "/api/autores/vivos"
}
```

---

# 📖 Guía de Parámetros

## Tipos de Parámetros

### Path Variables
Forman parte de la URL:
```
DELETE /api/libros/{id}     ← {id} es path variable
GET /api/autores/{id}       ← {id} es path variable
```

### Query Parameters
Se envían en la query string:
```
GET /api/libros?titulo=1984                 ← titulo es query parameter
GET /api/libros?page=1&size=5               ← page y size son query parameters
GET /api/autores/vivos?ano=1950             ← ano es query parameter
```

### Body (Request Body)
Se envían en el cuerpo JSON:
```
POST /api/libros/buscar-y-registrar
{
  "titulo": "1984"                          ← En el body
}
```

## Campos Ordenables por Endpoint

### Libros
- `id` - ID del libro
- `titulo` - Título del libro
- `autor` - Nombre del autor (si se soporta)
- `idioma` - Idioma del libro

### Autores
- `id` - ID del autor
- `nombre` - Nombre del autor
- `anoNacimiento` - Año de nacimiento
- `anoFallecimiento` - Año de fallecimiento

## Códigos ISO de Idiomas

| Código | Idioma | Ejemplo |
|--------|--------|---------|
| `en` | Inglés | English |
| `es` | Español | Español |
| `pt` | Portugués | Português |
| `ru` | Ruso | Русский |

---

# 🔴 Códigos de Estado HTTP

| Status | Descripción | Cuándo se usa |
|--------|-------------|-----------|
| **200 OK** | Solicitud exitosa con respuesta | GET, PUT cuando retorna datos |
| **201 Created** | Recurso creado exitosamente | POST cuando crea nuevo recurso |
| **204 No Content** | Solicitud exitosa sin respuesta | DELETE cuando se elimina |
| **400 Bad Request** | Parámetros inválidos | Validación fallida, parámetros incorrectos |
| **404 Not Found** | Recurso no encontrado | ID inexistente, búsqueda sin resultados |
| **500 Internal Server Error** | Error en el servidor | Fallo inesperado |

---

# 💡 Ejemplos de Uso

## 1. Crear un Nuevo Libro

```bash
curl -X POST "http://localhost:8080/api/libros/buscar-y-registrar" \
  -H "Content-Type: application/json" \
  -d '{"titulo": "1984"}'
```

**Respuesta:**
```json
{
  "id": 1,
  "titulo": "1984",
  "autor": "George Orwell",
  "idioma": "en",
  "mensaje": "Libro registrado exitosamente"
}
```

---

## 2. Buscar un Libro por Título

```bash
curl -X GET "http://localhost:8080/api/libros/buscar?titulo=1984"
```

**Respuesta:**
```json
{
  "id": 1,
  "titulo": "1984",
  "autor": {
    "id": 1,
    "nombre": "George Orwell",
    "anoNacimiento": 1903,
    "anoFallecimiento": 1950
  },
  "idioma": "en"
}
```

---

## 3. Listar Libros con Paginación

```bash
curl -X GET "http://localhost:8080/api/libros?page=0&size=5&sort=titulo&direction=asc"
```

---

## 4. Filtrar Libros por Idioma

```bash
curl -X GET "http://localhost:8080/api/libros/idioma?idioma=es"
```

**Respuesta:**
```json
{
  "idioma": "ESPANOL",
  "totalLibros": 2,
  "libros": [...]
}
```

---

## 5. Actualizar un Libro

```bash
curl -X PUT "http://localhost:8080/api/libros/1" \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "1984 - Edición Especial",
    "idioma": "es"
  }'
```

---

## 6. Eliminar un Libro

```bash
curl -X DELETE "http://localhost:8080/api/libros/1"
```

**Respuesta:** 204 No Content (sin body)

---

## 7. Listar Autores

```bash
curl -X GET "http://localhost:8080/api/autores?page=0&size=10"
```

---

## 8. Obtener Detalle de Autor

```bash
curl -X GET "http://localhost:8080/api/autores/1"
```

---

## 9. Listar Autores Vivos en Año Específico

```bash
curl -X GET "http://localhost:8080/api/autores/vivos?ano=1950"
```

---

# 📊 Resumen de Endpoints

| # | Método | Ruta | Descripción | Status |
|---|--------|------|-------------|--------|
| 1 | POST | `/api/libros/buscar-y-registrar` | Crear libro desde Gutendex | 201 |
| 2 | GET | `/api/libros/buscar` | Buscar libro por título | 200 |
| 3 | GET | `/api/libros/idioma` | Filtrar por idioma | 200 |
| 4 | GET | `/api/libros` | Listar libros paginado | 200 |
| 5 | PUT | `/api/libros/{id}` | Actualizar libro | 200 |
| 6 | DELETE | `/api/libros/{id}` | Eliminar libro | 204 |
| 7 | GET | `/api/autores` | Listar autores paginado | 200 |
| 8 | GET | `/api/autores/{id}` | Obtener detalle de autor | 200 |
| 9 | GET | `/api/autores/vivos` | Autores vivos en año | 200 |

**Total: 9 Endpoints**

---

# 🔐 Notas de Seguridad

- Todos los endpoints validan entrada del usuario
- Se implementó manejo global de excepciones
- Las búsquedas no distinguen mayúsculas/minúsculas en algunos casos
- IDs deben ser números positivos enteros
- Años deben ser números válidos positivos

---

**Versión:** 1.0  
**Fecha:** Junio 2026  
**Mantenedor:** Equipo Literalura
