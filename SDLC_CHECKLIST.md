# 📋 Checklist SDLC - Backend Literalura

**Proyecto:** Literalura Backend  
**Stack:** Java + Spring Boot + PostgreSQL  
**Versión:** 1.0  
**Objetivo:** Implementar 6 funcionalidades de búsqueda y gestión de libros

---

## 🏗️ FASE 1: Configuración del Proyecto

### 1.1 Estructura de Carpetas
- [ ] Crear estructura base: `controller/`, `service/`, `repository/`, `model/`, `dto/`, `exception/`, `config/`
- [ ] Organizar paquetes por dominio (ej: `com.literalura.books.*`)
- [ ] Crear carpeta `resources/` para configuración (application.properties, application-dev.properties)

### 1.2 Dependencias Maven
- [ ] Agregar Spring Boot Starter Web
- [ ] Agregar Spring Boot Starter Data JPA
- [ ] Agregar PostgreSQL Driver
- [ ] Agregar Jackson (para JSON mapping)
- [ ] Agregar Lombok (opcional, para reducir boilerplate)
- [ ] Agregar Spring Boot Starter Validation
- [ ] Agregar RestTemplate o WebClient para consumir API Gutendex

### 1.3 Configuración Base
- [ ] Configurar `application.properties`: datasource URL, username, password, JPA properties
- [ ] Configurar Hibernate: `spring.jpa.hibernate.ddl-auto=update` (desarrollo) o `validate` (producción)
- [ ] Configurar logging: nivel DEBUG para desarrollo, INFO para producción

---

## 🗄️ FASE 2: Modelado de Datos

### 2.1 Modelo de Dominio
- [ ] Crear clase `Book` (entidad JPA)
  - `id` (Long, @Id, @GeneratedValue)
  - `title` (String, no nulo)
  - `author` (String, no nulo)
  - `language` (String, ej: "es", "en")
  - `downloadCount` (Long, default 0)
  - `externalId` (Long, ID de Gutendex)
  - `createdAt` (LocalDateTime, @CreationTimestamp)
  
- [ ] Crear clase `Author` (entidad JPA)
  - `id` (Long, @Id, @GeneratedValue)
  - `name` (String, no nulo)
  - `birthYear` (Integer, nullable)
  - `deathYear` (Integer, nullable)
  - `books` (Set<Book>, relación @OneToMany)

- [ ] Crear relación entre `Book` y `Author`
  - Mapear `@ManyToOne` en Book → Author
  - Mapear `@OneToMany` en Author → Books

### 2.2 DTOs (Data Transfer Objects)
- [ ] Crear `BookDTO` (para respuestas REST)
  - Campos principales: id, title, author, language, downloadCount
  
- [ ] Crear `AuthorDTO` (para respuestas de autores)
  - Campos principales: id, name, birthYear, deathYear, bookCount
  
- [ ] Crear `GutendexResponseDTO` (para mapear respuesta de API)
  - Desestructurar JSON de Gutendex a estructura Java

---

## 🔌 FASE 3: Capa de Acceso a Datos (Repository)

### 3.1 Configuración de JPA
- [ ] Crear interface `BookRepository extends JpaRepository<Book, Long>`
  - `findByTitle(String title)`
  - `findByLanguage(String language)`
  - `findAllByOrderByTitleAsc()`
  - `findByExternalId(Long externalId)` (para evitar duplicados)

- [ ] Crear interface `AuthorRepository extends JpaRepository<Author, Long>`
  - `findAll()` con ordenamiento
  - `findByBirthYearLessThanAndDeathYearGreaterThan(Integer year)` (autores vivos)

### 3.2 Validación de Base de Datos
- [ ] Verificar que Spring JPA cree las tablas automáticamente
- [ ] Revisar logs: "CREATE TABLE" y "ALTER TABLE" confirmados
- [ ] Testear conexión básica a PostgreSQL

---

## ⚙️ FASE 4: Consumo de API Externa (Gutendex)

### 4.1 Cliente HTTP
- [ ] Crear clase `GutendexClient` o usar `RestTemplate`
  - Endpoint base: `https://gutendex.com/books`
  - Método: `searchBookByTitle(String title)` → Retorna lista de libros

### 4.2 Parseo de Respuesta
- [ ] Mapear JSON de Gutendex a `GutendexResponseDTO`
- [ ] Extraer campos relevantes: título, autores, idiomas, descargas
- [ ] Manejar casos: API no responde, no hay resultados, timeout

### 4.3 Integración con BD
- [ ] Crear método `saveBookFromGutendex(String title)` en service
- [ ] Validar no duplicar: verificar `externalId` antes de guardar
- [ ] Guardar `Book` y `Author` sincronizados

---

## 🧠 FASE 5: Capa de Lógica de Negocio (Service)

### 5.1 Funcionalidad 1: Buscar y Guardar Libro desde Gutendex
- [ ] Crear método `searchAndSaveBook(String title)` en `BookService`
  - Llamar `GutendexClient.searchBookByTitle(title)`
  - Validar que el libro no exista ya en BD
  - Guardar `Book` y asociar `Author`
  - Retornar `BookDTO` con resultado

### 5.2 Funcionalidad 2: Buscar Libro en BD por Título
- [ ] Crear método `findBookByTitle(String title)` en `BookService`
  - Consultar `BookRepository.findByTitle(title)`
  - Manejar caso: libro no encontrado
  - Retornar `BookDTO`

### 5.3 Funcionalidad 3: Listar Todos los Libros
- [ ] Crear método `getAllBooksOrderedByTitle()` en `BookService`
  - Consultar `BookRepository.findAllByOrderByTitleAsc()`
  - Convertir a `List<BookDTO>`
  - Retornar ordenados alfabéticamente

### 5.4 Funcionalidad 4: Mostrar Todos los Autores
- [ ] Crear método `getAllAuthors()` en `AuthorService`
  - Consultar `AuthorRepository.findAll()`
  - Convertir a `List<AuthorDTO>`
  - Incluir datos: nombre, años de vida, cantidad de libros

### 5.5 Funcionalidad 5: Autores Vivos en Año Específico
- [ ] Crear método `getAuthorsAliveInYear(Integer year)` en `AuthorService`
  - Query custom: `birthYear <= year AND (deathYear IS NULL OR deathYear >= year)`
  - Retornar `List<AuthorDTO>` con autores vivos en ese año

### 5.6 Funcionalidad 6: Filtrar Libros por Idioma
- [ ] Crear método `getBooksByLanguage(String language)` en `BookService`
  - Consultar `BookRepository.findByLanguage(language)`
  - Validar códigos de idioma válidos (ej: "es", "en", "fr")
  - Retornar `List<BookDTO>`

### 5.7 Mapeo DTO ↔ Entity
- [ ] Crear métodos auxiliares de conversión
  - `toDTO(Book)` → `BookDTO`
  - `toDTO(Author)` → `AuthorDTO`
  - `toEntity(GutendexResponseDTO)` → `Book` + `Author`

---

## 🌐 FASE 6: Capa de Presentación (REST Controllers)

### 6.1 BookController
- [ ] Crear clase `BookController` con `@RestController`
  - `POST /api/books/search?title={title}` → Func 1
  - `GET /api/books/find?title={title}` → Func 2
  - `GET /api/books` → Func 3
  - `GET /api/books/language?language={language}` → Func 6

### 6.2 AuthorController
- [ ] Crear clase `AuthorController` con `@RestController`
  - `GET /api/authors` → Func 4
  - `GET /api/authors/alive-in-year?year={year}` → Func 5

### 6.3 Validación de Entrada
- [ ] Validar parámetros no nulos
- [ ] Validar formato de año (4 dígitos)
- [ ] Validar códigos de idioma válidos

### 6.4 Respuestas HTTP
- [ ] Status 200 OK para búsquedas exitosas
- [ ] Status 201 CREATED para guardado de libros
- [ ] Status 204 NO CONTENT si no hay resultados
- [ ] Status 400 BAD REQUEST para parámetros inválidos

---

## 🚨 FASE 7: Manejo de Errores y Configuración Global

### 7.1 Excepciones Personalizadas
- [ ] Crear `ResourceNotFoundException extends RuntimeException`
- [ ] Crear `InvalidRequestException extends RuntimeException`
- [ ] Crear `ExternalApiException extends RuntimeException` (para Gutendex)

### 7.2 GlobalExceptionHandler
- [ ] Crear clase `GlobalExceptionHandler` con `@ControllerAdvice`
- [ ] Manejar `ResourceNotFoundException` → HTTP 404
- [ ] Manejar `InvalidRequestException` → HTTP 400
- [ ] Manejar `ExternalApiException` → HTTP 503
- [ ] Manejar `Exception` genérica → HTTP 500
- [ ] Crear `ErrorResponse` DTO con: `timestamp`, `status`, `message`, `details`

### 7.3 Validación con Jakarta/Javax
- [ ] Usar `@NotNull`, `@NotBlank` en DTOs
- [ ] Usar `@Positive` para años y conteos
- [ ] Usar `@Valid` en parámetros de controller

### 7.4 Logging
- [ ] Agregar logs INFO en inicio de métodos críticos
- [ ] Agregar logs ERROR en excepciones
- [ ] Agregar logs DEBUG para tracing de ejecución

---

## ✅ FASE 8: Testing (Unitario e Integración)

### 8.1 Tests de Repository
- [ ] Test: `findByTitle()` retorna libro correcto
- [ ] Test: `findByLanguage()` filtra por idioma
- [ ] Test: `findAllByOrderByTitleAsc()` ordena alfabéticamente

### 8.2 Tests de Service
- [ ] Test: `searchAndSaveBook()` guarda en BD correctamente
- [ ] Test: `searchAndSaveBook()` evita duplicados
- [ ] Test: `getAllBooksOrderedByTitle()` retorna lista ordenada
- [ ] Test: `getAuthorsAliveInYear()` calcula correctamente

### 8.3 Tests de Controller
- [ ] Test: endpoint `POST /api/books/search` retorna 201
- [ ] Test: endpoint `GET /api/books` retorna 200 con lista
- [ ] Test: endpoint con parámetro inválido retorna 400

### 8.4 Tests de Excepción
- [ ] Test: `ResourceNotFoundException` lanza HTTP 404
- [ ] Test: `InvalidRequestException` lanza HTTP 400

---

## 🔧 FASE 9: Configuración de Despliegue

### 9.1 Perfiles de Configuración
- [ ] Crear `application-dev.properties` (localhost)
- [ ] Crear `application-prod.properties` (servidor remoto)
- [ ] Configurar `application.yml` con perfiles

### 9.2 Variables de Entorno
- [ ] Documentar variables requeridas: DB_URL, DB_USER, DB_PASSWORD
- [ ] Usar `${variable}` en properties para externalizar configuración

### 9.3 Construcción y Packaging
- [ ] Ejecutar `mvn clean package`
- [ ] Verificar JAR generado sin errores
- [ ] Testear JAR ejecutable: `java -jar app.jar`

---

## 📊 FASE 10: Documentación y Revisión Final

### 10.1 Documentación de API
- [ ] Documentar cada endpoint: método HTTP, ruta, parámetros, respuestas
- [ ] Incluir ejemplos de curl/Postman
- [ ] Documentar códigos de error HTTP

### 10.2 Readme del Proyecto
- [ ] Instrucciones de instalación y configuración
- [ ] Requisitos previos (Java, Maven, PostgreSQL)
- [ ] Pasos para ejecutar: build, run, test

### 10.3 Revisión de Código
- [ ] Code review: verificar nombres de variables consistentes
- [ ] Revisar que no hay hardcoding de valores
- [ ] Verificar imports no utilizados

### 10.4 Validación Final
- [ ] Todas las 6 funcionalidades implementadas y testeadas
- [ ] Sin errores en logs
- [ ] Base de datos sincronizada correctamente
- [ ] API responde correctamente a todas las rutas

---

## 📝 NOTAS TÉCNICAS

### Estructura de Ejemplo - Árbol de Carpetas
```
literalura/
├── src/main/java/com/literalura/
│   ├── controller/
│   │   ├── BookController.java
│   │   └── AuthorController.java
│   ├── service/
│   │   ├── BookService.java
│   │   ├── AuthorService.java
│   │   └── GutendexClient.java
│   ├── repository/
│   │   ├── BookRepository.java
│   │   └── AuthorRepository.java
│   ├── model/
│   │   ├── Book.java
│   │   └── Author.java
│   ├── dto/
│   │   ├── BookDTO.java
│   │   ├── AuthorDTO.java
│   │   └── GutendexResponseDTO.java
│   ├── exception/
│   │   ├── ResourceNotFoundException.java
│   │   ├── InvalidRequestException.java
│   │   └── GlobalExceptionHandler.java
│   ├── config/
│   │   └── ApplicationConfig.java
│   └── LiteraluraApplication.java
├── src/main/resources/
│   ├── application.properties
│   ├── application-dev.properties
│   └── application-prod.properties
├── src/test/java/
│   └── (tests por cada clase)
└── pom.xml
```

### Snippet: GlobalExceptionHandler
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(), 404, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(404).body(error);
    }
    
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(
            InvalidRequestException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(), 400, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(400).body(error);
    }
}
```

### Snippet: BookService - Funcionalidad 1
```java
@Service
@Transactional
public class BookService {
    
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GutendexClient gutendexClient;
    
    public BookDTO searchAndSaveBook(String title) {
        // Buscar en Gutendex
        GutendexResponseDTO gutendexBook = gutendexClient.searchBookByTitle(title);
        
        // Evitar duplicados
        if (bookRepository.findByExternalId(gutendexBook.getId()).isPresent()) {
            throw new InvalidRequestException("Libro ya registrado");
        }
        
        // Guardar author
        Author author = new Author();
        author.setName(gutendexBook.getAuthorName());
        author.setBirthYear(gutendexBook.getAuthorBirthYear());
        author = authorRepository.save(author);
        
        // Guardar book
        Book book = new Book();
        book.setTitle(gutendexBook.getTitle());
        book.setAuthor(author);
        book.setLanguage(gutendexBook.getLanguage());
        book.setExternalId(gutendexBook.getId());
        book = bookRepository.save(book);
        
        return toDTO(book);
    }
}
```

---

**Última Actualización:** Junio 2026  
**Estado:** Template Listo para Implementación
