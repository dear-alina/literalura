# ‹ Checklist SDLC - Backend Literalura

**Proyecto:** Literalura Backend
**Stack:** Java 21 + Spring Boot + PostgreSQL
**Version:** 2.0
**Ultima Actualizacion:** 2026-07-10
**Objetivo:** Backend REST completo para catalogo de libros con integracion a Gutendex API

---

##  FASE 1: Configuracion del Proyecto

### 1.1 Estructura de Carpetas
- [x] Estructura base creada: `controller/`, `service/`, `repository/`, `model/`, `dto/`, `exception/`, `config/`
- [x] Paquetes organizados por dominio: `com.alurachallenge.literalura.*`
- [x] Carpeta `resources/` configurada con `application.properties`

### 1.2 Dependencias Maven
- [x] Spring Boot Starter Web
- [x] Spring Boot Starter Data JPA
- [x] PostgreSQL Driver
- [x] Jackson (para JSON mapping con `@JsonAlias`)
- [x] Spring Boot Starter Validation (`@NotBlank` en DTOs)
- [x] RestTemplate para consumir API Gutendex
- [x] Testcontainers (PostgreSQL) para tests de integracion

### 1.3 Configuracion Base
- [x] `application.properties`: datasource con variables de entorno (`${DB_HOST}`, `${DB_USER}`, `${DB_PASSWORD}`)
- [x] Hibernate: `spring.jpa.hibernate.ddl-auto=update`
- [x] SQL logging habilitado: `spring.jpa.show-sql=true`, `format_sql=true`

---

##  FASE 2: Modelado de Datos

### 2.1 Entidades JPA

- [x] Entidad `Libro` (`@Entity`, tabla `libros`)
  - `id` (Long, @Id, @GeneratedValue)
  - `gutendexId` (Integer, @Column unique + not null) ” ID de Gutendex para URL de portadas
  - `titulo` (String, @Column unique)
  - `autor` (@ManyToOne, FetchType.EAGER)
  - `idioma` (@Enumerated(STRING), enum `Idioma`)
  - `nota` (String, @Column length=1000) ” reseña personal del usuario

- [x] Entidad `Autor` (`@Entity`, tabla `autors`)
  - `id` (Long, @Id, @GeneratedValue)
  - `nombre` (String, @Column unique)
  - `anoNacimiento` (Integer)
  - `anoFallecimiento` (Integer, nullable)
  - `libros` (@OneToMany mappedBy="autor", CascadeType.ALL, **FetchType.LAZY**)

- [x] Enum `Idioma` con valores: `INGLES("en")`, `ESPANOL("es")`, `PORTUGUES("pt")`, `RUSO("ru")`

### 2.2 DTOs (Data Transfer Objects)

- [x] `LibroListResponseDTO` ” id, titulo, gutendexId, autor (String), idioma
- [x] `LibroDetalleResponseDTO` ” id, titulo, gutendexId, autor (AutorDetalleDTO), idioma, nota
- [x] `LibroResponseDTO` ” id, titulo, gutendexId, autor, idioma, mensaje
- [x] `LibroActualizadoResponseDTO` ” id, titulo, autor, idioma, nota, mensaje
- [x] `ActualizarLibroDTO` ” titulo, autorNombre, idioma, nota (todos opcionales)
- [x] `ActualizarNotaDTO` ” nota (mutacion atomica)
- [x] `BusquedaLibroDTO` ” titulo (@NotBlank)
- [x] `LibrosPorIdiomaResponseDTO` ” idioma, totalLibros, List<LibroListResponseDTO>
- [x] `AutorResponseDTO` ” id, nombre, anoNacimiento, anoFallecimiento, totalLibros
- [x] `AutorDetalleResponseDTO` ” id, nombre, anoNacimiento, anoFallecimiento, List<LibroListResponseDTO>
- [x] `DatosGutendexLibro` ” id (Long), titulo, autores, idiomas, descargas
- [x] `DatosGutendexAutor` ” nombre, anoNacimiento, anoFallecimiento
- [x] `DatosLibro` (legacy model) ” id (Integer), titulo, autores, idioma, descargas

---

## FASE 3: Capa de Acceso a Datos (Repository) 

### 3.1 LibroRepository
- [x] `findByGutendexId(Integer)` ” deduplicacion canonica al registrar desde Gutendex
- [x] `findByTitulo(String)` ” busqueda exacta por titulo
- [x] `findByTituloIgnoreCase(String)`
- [x] `findByIdioma(Idioma)` ” filtrado por idioma
- [x] `findAll(Pageable)` ” listado paginado
- [x] `findByTituloContainingIgnoreCaseOrAutorNombreContainingIgnoreCase(String, String)` ” busqueda flexible

### 3.2 AutorRepository
- [x] `findAutoresVivosEnAno(@Param Integer)` ” JPQL custom + `@EntityGraph`
- [x] `findByNombreContainsIgnoreCase(String)` + `@EntityGraph`
- [x] `findByNombre(String)` ” LAZY puro, solo para validaciones internas de escritura
- [x] `findAll(Pageable)` + `@EntityGraph`
- [x] `findById(Long)` override + `@EntityGraph` ” para vista de detalle individual

### 3.3 Estrategia de Fetch con @EntityGraph
- [x] `@EntityGraph(attributePaths = {"libros"})` aplicado en metodos de lectura del catalogo para forzar LEFT OUTER JOIN y evitar LazyInitializationException al serializar
- [x] `findByNombre` conserva LAZY puro para evitar sobrecarga en flujos de escritura

---

## FASE 4: Consumo de API Externa (Gutendex) 

### 4.1 Cliente HTTP
- [x] Clase `ClienteGutendex` con RestTemplate
  - Endpoint: `https://gutendex.com/books/?search={titulo}`
  - Mapeo a `RespuestaGutendex` →’ `List<DatosGutendexLibro>`

### 4.2 Parseo de Respuesta
- [x] Mapeo con `@JsonAlias` en records Java
- [x] Campos extraidos: id, titulo, autores (nombre, años), idiomas, descargas
- [x] `@JsonIgnoreProperties(ignoreUnknown = true)` aplicado

### 4.3 Integracion con BD
- [x] Deduplicacion por `gutendexId` (ID canonico de Gutendex) antes de guardar
- [x] Creacion automatica de autor si no existe (`findByNombre` →’ save si ausente)
- [x] `gutendexId` asignado al nuevo libro desde `DatosGutendexLibro.id()`

---

##  FASE 5: Capa de Logica de Negocio (Service) 

### LibroService
- [x] `buscarYRegistrarLibro(BusquedaLibroDTO)` ” busca en Gutendex, deduplica por gutendexId, persiste libro y autor
- [x] `buscarLibroPorTitulo(String)` ” busqueda exacta en BD, lanza ResourceNotFoundException si no existe
- [x] `buscarLibroPorId(Long)` ” por ID interno, lanza ResourceNotFoundException si no existe
- [x] `buscarFlexible(String q)` ” busqueda parcial case-insensitive por titulo o autor
- [x] `listarTodosLosLibros(Pageable)` ” paginado
- [x] `obtenerLibrosPorIdioma(String)` ” filtra por enum Idioma
- [x] `actualizarLibro(Long, ActualizarLibroDTO)` ” actualizacion parcial de todos los campos incluida nota
- [x] `actualizarNotaLibro(Long, ActualizarNotaDTO)` ” mutacion atomica exclusiva del campo nota
- [x] `eliminarLibro(Long)` ” elimina por ID, valida existencia previa

### AutorService
- [x] `listarTodosLosAutores(Pageable)` ” paginado con totalLibros
- [x] `obtenerAutorDetalle(Long)` ” detalle con lista de libros anidada
- [x] `obtenerAutoresVivosEnAno(Integer)` ” valida año positivo, filtra por rango de nacimiento/fallecimiento

---

##  FASE 6: Capa de Presentacion (REST Controllers) 

### LibroController (`/api/libros`)
- [x] `POST /buscar-y-registrar` →’ `buscarYRegistrarLibro`
- [x] `GET /buscar?titulo=` →’ `buscarPorTitulo` (exacto)
- [x] `GET /busqueda-flexible?q=` →’ `busquedaFlexible` (parcial)
- [x] `GET /{id}` →’ `obtenerLibroPorId`
- [x] `GET /idioma?idioma=` →’ `obtenerLibrosPorIdioma`
- [x] `GET` (paginado) →’ `listarLibros`
- [x] `PUT /{id}` →’ `actualizarLibro`
- [x] `PATCH /{id}/nota` →’ `actualizarNota`
- [x] `DELETE /{id}` →’ `eliminarLibro`

### AutorController (`/api/autores`)
- [x] `GET` (paginado) →’ `listarAutores`
- [x] `GET /{id}` →’ `obtenerDetalleAutor`
- [x] `GET /vivos?ano=` →’ `obtenerAutoresVivos`

### Validacion de Entrada
- [x] `@NotBlank` en `BusquedaLibroDTO.titulo`
- [x] Validacion de año positivo en `obtenerAutoresVivosEnAno`
- [x] Validacion de codigo de idioma en `obtenerLibrosPorIdioma`

---

## FASE 7: Manejo de Errores y Configuracion Global 

### Excepciones Personalizadas
- [x] `ResourceNotFoundException extends RuntimeException` →’ HTTP 404
- [x] `LibroNoEncontradoException extends RuntimeException` →’ HTTP 404

### GlobalExceptionHandler (@ControllerAdvice)
- [x] `ResourceNotFoundException` →’ 404
- [x] `LibroNoEncontradoException` →’ 404
- [x] `MethodArgumentNotValidException` →’ 400 (validacion de DTOs)
- [x] `IllegalArgumentException` →’ 400 (año invalido, idioma invalido)
- [x] `RuntimeException` generica →’ 500

### ErrorResponse DTO
- [x] Campos: `timestamp`, `status`, `mensaje`, `ruta`

### CORS
- [x] Configuracion CORS en `CorsConfiguration.java` para permitir peticiones del frontend

---

## FASE 8: Testing (Integracion) 

### Tests de Repository (`@DataJpaTest` + Testcontainers)
- [x] `LibroRepositoryIT` ” derived queries y JPQL
- [x] `AutorRepositoryIT` ” derived queries y JPQL

### Tests de Service (`@SpringBootTest` + Testcontainers)
- [x] `LibroServiceIT` ” flujos de negocio con mock de `ClienteGutendex`
- [x] `AutorServiceIT` ” flujos de negocio con @Transactional rollback
  - [x] Bugfix: helpers de test sincronizan lado inverso @OneToMany para evitar cache de sesion inconsistente

### Tests de Controller (`@SpringBootTest` + Testcontainers)
- [x] `LibroControllerIT` ” endpoints REST
- [x] `AutorControllerIT` ” endpoints REST

---

##  FASE 9: Configuracion de Despliegue 

### Variables de Entorno Requeridas
- [x] `DB_HOST` ” host de PostgreSQL
- [x] `DB_USER` ” usuario de BD
- [x] `DB_PASSWORD` ” contraseña de BD
- [ ] Crear `application-prod.properties` con configuracionn de produccion
- [ ] Ejecutar `mvn clean package` y verificar JAR ejecutable

---

## FASE 10: Documentacion 

### Documentacion de API
- [x] `docs/ENDPOINTS_DOCUMENTATION.md` ” 12 endpoints documentados con ejemplos
- [x] `docs/API_INTEGRATION.md` ” contrato completo para el frontend
- [x] `docs/CHANGELOG_MASTER.md` ” historial de cambios
- [x] `docs/REFACTORING_LOGS/` ” logs tecnicos de refactorizaciones

---

##  Esquema de Base de Datos (Estado Actual)

```
tabla: libros
  - id              BIGSERIAL PRIMARY KEY
  - gutendex_id     INTEGER UNIQUE NOT NULL
  - titulo          VARCHAR UNIQUE
  - autor_id        BIGINT REFERENCES autors(id)
  - idioma          VARCHAR (enum: INGLES, ESPANOL, PORTUGUES, RUSO)
  - nota            VARCHAR(1000)

tabla: autors
  - id              BIGSERIAL PRIMARY KEY
  - nombre          VARCHAR UNIQUE
  - ano_nacimiento  INTEGER
  - ano_fallecimiento INTEGER
```

---

##  URL de Portada (Frontend)

Con el campo `gutendexId` disponible en todas las respuestas de libros:

```
https://gutenberg.org/{gutendexId}/pg{gutendexId}.cover.medium.jpg
```

---

**Ultima Actualizacion:** 2026-07-10
**Estado:** Implementación completa” 12 endpoints activos

