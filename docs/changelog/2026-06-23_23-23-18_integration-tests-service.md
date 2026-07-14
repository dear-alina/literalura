# Tests de Integración - Capa Service (LibroService & AutorService)

**Fecha:** 2026-06-23 23:23:18  
**Categoría:** Tests  
**Estado:** Completado  
**Impacto:** Medio

## 📋 Resumen
Se implementaron las suites de tests de integración para `LibroService` y `AutorService` usando `@SpringBootTest` y Testcontainers (PostgreSQL) con `@ServiceConnection`. Los tests ejercen la lógica de negocio completa contra una base de datos real en contenedor aislado, con `@Transactional` para rollback automático y `@MockitoBean` sobre `ClienteGutendex` para aislar la integración externa.

## 🔧 Archivos Modificados/Creados
- `src/test/java/com/alurachallenge/literalura/service/LibroServiceIT.java` → Nueva clase con 9 tests de integración que cubren `buscarYRegistrarLibro`, `buscarLibroPorTitulo`, `listarTodosLosLibros`, `obtenerLibrosPorIdioma`, `actualizarLibro` y `eliminarLibro`
- `src/test/java/com/alurachallenge/literalura/service/AutorServiceIT.java` → Nueva clase con 6 tests de integración que cubren `listarTodosLosAutores`, `obtenerAutorDetalle` y `obtenerAutoresVivosEnAno`

## 📊 Capas Afectadas
- [ ] AutorController / LibroController
- [x] AutorService / LibroService
- [ ] ConsumoAPI / ClienteGutendex (integración)
- [ ] ConvierteDatos / IConvierteDatos (transformación)
- [x] AutorRepository / LibroRepository
- [x] Model (Entidades JPA)
- [ ] Config (Security, Redis, Properties)
- [ ] Exception (manejo de errores)

## 🧪 Tests Asociados

**LibroServiceIT — 9 tests:**

| Test | Método | Qué verifica |
|---|---|---|
| `buscarYRegistrarLibro_libroNuevo_deberiaGuardarEnBDYRetornarMensajeExitoso` | `buscarYRegistrarLibro` | Libro + Autor persisten en BD real |
| `buscarYRegistrarLibro_libroYaExistenteEnBD_deberiaRetornarSinConsultarGutendex` | `buscarYRegistrarLibro` | No llama Gutendex si ya existe en BD |
| `buscarYRegistrarLibro_gutendexSinResultados_deberiaLanzarExcepcion` | `buscarYRegistrarLibro` | Lanza `RuntimeException` con mensaje correcto |
| `buscarLibroPorTitulo_libroExistente_deberiaRetornarDetalleConAutor` | `buscarLibroPorTitulo` | DTO mapea título, autor e idioma correctamente |
| `buscarLibroPorTitulo_libroInexistente_deberiaLanzarResourceNotFoundException` | `buscarLibroPorTitulo` | Lanza excepción con mensaje descriptivo |
| `listarTodosLosLibros_conVariosLibros_deberiaRetornarPaginaConTodos` | `listarTodosLosLibros` | Paginación con contenido real en BD |
| `obtenerLibrosPorIdioma_deberiaRetornarSoloLibrosDelIdiomaIndicado` | `obtenerLibrosPorIdioma` | Filtra por idioma, excluye otros idiomas |
| `actualizarLibro_conNuevoTitulo_deberiaModificarTituloEnBD` | `actualizarLibro` | Cambio persiste en BD tras la llamada |
| `eliminarLibro_libroExistente_deberiaRemoverDeBD` | `eliminarLibro` | `findById` retorna vacío tras eliminación |

**AutorServiceIT — 6 tests:**

| Test | Método | Qué verifica |
|---|---|---|
| `listarTodosLosAutores_conAutoresEnBD_deberiaRetornarPaginaConDatos` | `listarTodosLosAutores` | Paginación con autores reales en BD |
| `listarTodosLosAutores_deberiaMapearCamposCorrectosMenteAlDTO` | `listarTodosLosAutores` | Campos DTO: años nacimiento/fallecimiento y totalLibros |
| `obtenerAutorDetalle_autorConLibros_deberiaRetornarDetalleConListaDeLibros` | `obtenerAutorDetalle` | Devuelve lista de libros asociados al autor |
| `obtenerAutorDetalle_autorInexistente_deberiaLanzarExcepcion` | `obtenerAutorDetalle` | Lanza `RuntimeException` con "Autor no encontrado" |
| `obtenerAutoresVivosEnAno_deberiaRetornarAutoresQueVivenEnEseAno` | `obtenerAutoresVivosEnAno` | JPQL filtra correctamente vivos vs muertos |
| `obtenerAutoresVivosEnAno_anoNegativo_deberiaLanzarIllegalArgumentException` | `obtenerAutoresVivosEnAno` | Valida argumento inválido |

## ⚠️ Consideraciones de Arquitectura de Literalura
- ✅ Se respeta la separación de capas: los tests ejercen Service → Repository → PostgreSQL sin saltarse niveles
- ✅ `ClienteGutendex` (ConsumoAPI externa) se mockea con `@MockitoBean` — nunca toca la red en tests
- ✅ `ConvierteDatos` / `IConvierteDatos` no afectados — los tests construyen DTOs directamente
- ✅ Sin breaking changes en endpoints de `AutorController` / `LibroController`
- ✅ `@Transactional` en clase de test garantiza rollback automático; no se requiere `@AfterEach` de limpieza
- ⚠️ JWT/Security no está activo; si se añade `AuthConfig`, los services podrían requerir contexto de seguridad en tests
- ✅ Redis caché no aplica en este cambio
- ⚠️ La imagen Docker `postgres:16-alpine` debe estar disponible localmente (`docker pull postgres:16-alpine`)

## ✅ Verificación Pre-Entrega
- [x] Código compila sin errores (`mvn test-compile` → BUILD SUCCESS)
- [ ] Tests unitarios verdes (mvn test) — bloqueado por descarga de imagen Docker
- [ ] Tests integración verdes (Testcontainers) — bloqueado por descarga de imagen Docker
- [x] Sin dependencias circulares
- [x] Documentación actualizada

## 🔄 Actualización de Auditoría 2026-07-10
- **Estado de auditoría:** Actualizada con nuevos métodos de servicio y corrección por `gutendexId` no nulo/único.
- **Brechas cubiertas desde 2026-06-23 (LibroService):**
  - `buscarFlexible(String q)` (título, autor, sin término, sin coincidencias)
  - `buscarLibroPorId(Long id)` (existente / inexistente)
  - `actualizarNotaLibro(Long id, ActualizarNotaDTO)` (éxito, null, inexistente)
  - deduplicación por `gutendexId` en `buscarYRegistrarLibro(...)`
- **Brecha corregida (AutorService):**
  - helper de integración actualizado para asignar `gutendexId` único en libros de prueba.
- **Archivos actualizados:**
  - `src/test/java/com/alurachallenge/literalura/service/LibroServiceIT.java`
  - `src/test/java/com/alurachallenge/literalura/service/AutorServiceIT.java`
- **Cobertura actual service:**
  - `LibroServiceIT` pasó de **9** a **21** tests.
  - `AutorServiceIT` pasó de **6** a **8** tests.