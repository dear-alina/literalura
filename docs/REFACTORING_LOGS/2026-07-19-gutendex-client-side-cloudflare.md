# Log de Refactorización: Integración con Gutendex movida al navegador (bloqueo Cloudflare)

**Fecha:** 2026-07-19
**Tipo:** Corrección de arquitectura + refactorización de contrato de API
**Resultado:** Suite backend 128/128 en verde; registro de libros operativo en producción (Render)

---

## 1. Problema

En producción (Render), el registro de libros fallaba con HTTP 500. Los logs mostraban que Gutendex respondía **403 Forbidden** con el interstitial de Cloudflare (`<title>Just a moment...</title>`, "managed challenge"):

```
c.a.literalura.service.ClienteGutendex : Error al consultar Gutendex para 'dracula':
403 Forbidden on GET request for "https://gutendex.com/books": "<!DOCTYPE html>...Just a moment..."
```

### Causa raíz (verificada)

Cloudflare **desafía por reputación de IP**, no por cabeceras. El código ya enviaba un `User-Agent` de navegador (`ClienteGutendex` línea 34) y aun así recibía 403. Comprobaciones:

- Desde una IP residencial, Gutendex responde **200 con y sin `User-Agent`** — la cabecera es irrelevante.
- Desde la IP de datacenter de Render, Cloudflare emite un challenge de JavaScript que **ninguna cabecera HTTP puede resolver**.

Por tanto, la solución de "añadir User-Agent" no aplica: ya estaba y no funciona en Render.

### Dato habilitante

Gutendex responde con `access-control-allow-origin: *`, por lo que **el navegador del usuario puede llamar a Gutendex directamente** (IP residencial + resuelve el challenge de Cloudflare de forma transparente).

## 2. Solución: la integración con Gutendex pasa al cliente

El flujo de registro deja de consultar Gutendex desde el servidor. Ahora:

```
Navegador → Gutendex (búsqueda)  →  POST /api/libros/buscar-y-registrar (libro ya resuelto)  →  backend deduplica y persiste
```

## 3. Cambios en el backend

| Archivo | Cambio |
|---|---|
| `dto/RegistrarLibroDTO.java` | **Nuevo record**: `gutendexId` (@NotNull), `titulo` (@NotBlank), `autores`, `idiomas`, `descargas`. Es el cuerpo que envía el navegador. |
| `dto/BusquedaLibroDTO.java` | **Eliminado** (ya no se busca por título en el servidor). |
| `service/LibroService.java` | `buscarYRegistrarLibro(BusquedaLibroDTO)` → **`registrarLibro(RegistrarLibroDTO)`**: sin llamada a Gutendex; deduplica por `gutendexId` y persiste reutilizando `obtenerOCrearAutor` y `mapearIdioma`. |
| `controller/LibroController.java` | `POST /api/libros/buscar-y-registrar` ahora recibe `RegistrarLibroDTO` (ruta y código 201 sin cambios). |
| `config/CorsConfiguration.java` | Añadidos orígenes locales `http://localhost:3000` y `http://127.0.0.1:3000`. |

Nota: `ClienteGutendex`/`ConsumoAPI`/`ConvierteDatos` se conservan; siguen siendo utilizados por los endpoints de búsqueda local con respaldo externo (`GET /buscar`, `GET /busqueda-flexible`), que degradan de forma segura si Gutendex no responde. El flujo crítico de registro ya no depende de ellos.

## 4. Contrato del endpoint (nuevo cuerpo)

`POST /api/libros/buscar-y-registrar`

```json
{
  "gutendexId": 1342,
  "titulo": "Don Quijote",
  "autores": [{ "nombre": "Miguel de Cervantes", "anoNacimiento": 1547, "anoFallecimiento": 1616 }],
  "idiomas": ["es"],
  "descargas": 1000
}
```

Respuestas: `201` (registrado o "ya existe" tras deduplicar), `400` (validación: falta `gutendexId` o `titulo`), `500` (error interno).

## 5. Tests actualizados

- `LibroServiceTest`, `LibroServiceIT`: los tests de `buscarYRegistrarLibro` pasan a `registrarLibro` (sin mock de Gutendex; datos entregados en el DTO).
- `LibroControllerTest`: `registrar_ok` (201), `registrar_libroDuplicado` (201 "ya existe"), `registrar_errorInterno` (500), `registrar_sinGutendexId` (400).
- `LibroControllerE2ETest`: el POST envía el payload real del navegador y verifica la persistencia en PostgreSQL; se retiró el `@MockitoBean ClienteGutendex`.
- `BaseE2ETest`: migrado al **patrón singleton container** (contenedor estático iniciado una vez en bloque estático, sin `@Container`/`@Testcontainers`). Necesario porque, al igualarse los contextos de las clases E2E, Spring los cachea y el reinicio del contenedor entre clases dejaba el datasource apuntando a un puerto obsoleto (Hikari: "Connection is not available").

## 6. Verificación

```
Backend:  Tests run: 128, Failures: 0, Errors: 0   (unit + integración Testcontainers + E2E REST Assured)
```

(Frontend: 29/29 Jest y 15/15 Cypress; ver repositorio `stitch_literalura`.)
