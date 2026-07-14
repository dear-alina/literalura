# Tests de Integración - Capa Controller (LibroController & AutorController)

**Fecha:** 2026-06-23 23:08:31  
**Categoría:** Tests  
**Estado:** Completado  
**Impacto:** Medio

## 📋 Resumen
Se implementaron las primeras suites de tests de integración para `LibroController` y `AutorController` usando `@SpringBootTest`, `@AutoConfigureMockMvc` y Testcontainers (PostgreSQL) con `@ServiceConnection`. Cada clase cubre los endpoints GET principales con respuesta HTTP 200 sobre una base de datos real en contenedor aislado.

## 🔧 Archivos Modificados/Creados
- `pom.xml` → Corregidos artifact IDs de Testcontainers (`junit-jupiter`, `postgresql` con versión `1.20.6`), eliminado artefacto inexistente `spring-boot-starter-data-jpa-test`, añadido `spring-boot-starter-webmvc-test` (requerido en Spring Boot 4 para `@AutoConfigureMockMvc`)
- `src/test/java/com/alurachallenge/literalura/controller/LibroControllerIT.java` → Nueva clase con 3 tests de integración para `GET /api/libros` y `GET /api/libros/idioma`
- `src/test/java/com/alurachallenge/literalura/controller/AutorControllerIT.java` → Nueva clase con 3 tests de integración para `GET /api/autores` y `GET /api/autores/vivos`

## 📊 Capas Afectadas
- [x] AutorController / LibroController
- [ ] AutorService / LibroService
- [ ] ConsumoAPI / ClienteGutendex (integración)
- [ ] ConvierteDatos / IConvierteDatos (transformación)
- [ ] AutorRepository / LibroRepository
- [ ] Model (Entidades JPA)
- [ ] Config (Security, Redis, Properties)
- [ ] Exception (manejo de errores)

## 🧪 Tests Asociados
- ✅ `src/test/java/com/alurachallenge/literalura/controller/LibroControllerIT.java`
- ✅ `src/test/java/com/alurachallenge/literalura/controller/AutorControllerIT.java`

**Cobertura por clase:**

| Clase | Test | Endpoint | Verifica |
|---|---|---|---|
| `LibroControllerIT` | `listarLibros_conBaseDatosVacia_deberiaRetornar200ConPaginaVacia` | `GET /api/libros` | HTTP 200, `content[]`, `totalElements=0` |
| `LibroControllerIT` | `listarLibros_conParametrosDePaginacion_deberiaRetornar200` | `GET /api/libros?page=0&size=5` | HTTP 200, paginación |
| `LibroControllerIT` | `obtenerLibrosPorIdioma_conIdiomaValido_deberiaRetornar200` | `GET /api/libros/idioma?idioma=en` | HTTP 200, idioma `INGLES`, `totalLibros=0` |
| `AutorControllerIT` | `listarAutores_conBaseDatosVacia_deberiaRetornar200ConPaginaVacia` | `GET /api/autores` | HTTP 200, `content[]`, `totalElements=0` |
| `AutorControllerIT` | `listarAutores_conParametrosDePaginacion_deberiaRetornar200` | `GET /api/autores?page=0&size=5` | HTTP 200, paginación |
| `AutorControllerIT` | `obtenerAutoresVivos_conAnoValido_deberiaRetornar200` | `GET /api/autores/vivos?ano=1850` | HTTP 200, lista JSON |

## ⚠️ Consideraciones de Arquitectura de Literalura
- ✅ Se respeta la separación de capas: los tests ejercen Controller → Service → Repository → PostgreSQL sin mockear ninguna capa intermedia
- ✅ Sin breaking changes en endpoints existentes
- ⚠️ JWT/Security no está activo en el contexto actual; si se añade `AuthConfig`, los tests requerirán cabecera `Authorization`
- ✅ Se mantienen las interfaces (`IConvierteDatos`) — no fueron modificadas
- ✅ `ClienteGutendex` no se invoca en estos tests (solo endpoints de lectura de BD)
- ✅ Redis caché no aplica en este cambio (no se registró `@EnableCaching` ni `@Cacheable` activos)
- ⚠️ La imagen Docker `postgres:16-alpine` debe estar disponible localmente para ejecutar los tests (`docker pull postgres:16-alpine`)

## ✅ Verificación Pre-Entrega
- [x] Código compila sin errores (`mvn test-compile` → BUILD SUCCESS)
- [ ] Tests unitarios verdes (mvn test) — bloqueado por descarga de imagen Docker
- [ ] Tests integración verdes (Testcontainers) — bloqueado por descarga de imagen Docker
- [x] Sin dependencias circulares
- [x] Documentación actualizada

## 🔄 Actualización de Auditoría 2026-07-10
- **Estado de auditoría:** Actualizada con nuevos endpoints y casos de error.
- **Brechas cubiertas desde 2026-06-23:**
  - `GET /api/libros/{id}` (200/404)
  - `GET /api/libros/busqueda-flexible` (con y sin `q`)
  - `PATCH /api/libros/{id}/nota` (200/404)
- **Archivo actualizado:** `src/test/java/com/alurachallenge/literalura/controller/LibroControllerIT.java`
- **Cobertura actual controller:** `LibroControllerIT` pasó de **3** a **10** tests.
- **Nota técnica:** se agregó `@Transactional` para rollback por test y aislamiento de datos.