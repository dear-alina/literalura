# Reporte de Evolución: Cobertura de Pruebas de Integración
- **Fecha de Actualización:** 2026-07-10
- **Archivos Base Auditados:** `docs/changelog/2026-06-23_23-08-31_integration-tests-controller.md`, `docs/changelog/2026-06-23_23-23-18_integration-tests-service.md`, `docs/changelog/2026-06-23_23-36-10_integration-tests-repository.md`.
- **Estado de la Suite:** Nuevos Tests Agregados

## 1. Brechas Detectadas (Cambios en el código vs Logs del 2026-06-23)
- Endpoints sin cobertura en controller:
  - `GET /api/libros/{id}`
  - `GET /api/libros/busqueda-flexible`
  - `PATCH /api/libros/{id}/nota`
- Métodos de servicio sin cobertura:
  - `buscarFlexible(String q)`
  - `buscarLibroPorId(Long id)`
  - `actualizarNotaLibro(Long id, ActualizarNotaDTO actualizacion)`
  - deduplicación por `gutendexId` en `buscarYRegistrarLibro(...)`
- Métodos de repositorio sin cobertura:
  - `findByGutendexId(Integer gutendexId)`
  - `findByTituloContainingIgnoreCaseOrAutorNombreContainingIgnoreCase(...)`
  - comportamiento de `@EntityGraph(attributePaths = {"libros"})` en consultas de `AutorRepository`
- Brecha técnica crítica:
  - fixtures de tests no asignaban `gutendexId` pese a restricción `NOT NULL + UNIQUE` en `Libro`, generando riesgo de fallo de persistencia.

## 2. Código de las Nuevas Pruebas de Integración Implementadas
```java
// src/test/java/com/alurachallenge/literalura/repository/LibroRepositoryIT.java
package com.alurachallenge.literalura.repository;

import com.alurachallenge.literalura.model.Autor;
import com.alurachallenge.literalura.model.Idioma;
import com.alurachallenge.literalura.model.Libro;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class LibroRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private AutorRepository autorRepository;

    private static final AtomicInteger GUTENDEX_ID_SEQ = new AtomicInteger(100000);

    private Autor guardarAutor(String nombre) {
        Autor autor = new Autor();
        autor.setNombre(nombre);
        autor.setAnoNacimiento(1800);
        autor.setAnoFallecimiento(1870);
        return autorRepository.save(autor);
    }

    private Libro guardarLibro(String titulo, Autor autor, Idioma idioma) {
        Libro libro = new Libro();
        libro.setTitulo(titulo);
        libro.setAutor(autor);
        libro.setIdioma(idioma);
        libro.setGutendexId(GUTENDEX_ID_SEQ.incrementAndGet());
        return libroRepository.save(libro);
    }

    @Test
    void findByGutendexId_idExistente_deberiaRetornarLibroCorrespondiente() {
        Autor autor = guardarAutor("Jane Austen");
        Libro libro = guardarLibro("Pride and Prejudice", autor, Idioma.INGLES);
        libro.setGutendexId(1342);
        libroRepository.save(libro);

        Optional<Libro> resultado = libroRepository.findByGutendexId(1342);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getTitulo()).isEqualTo("Pride and Prejudice");
    }

    @Test
    void findByGutendexId_idInexistente_deberiaRetornarEmpty() {
        Optional<Libro> resultado = libroRepository.findByGutendexId(999999);
        assertThat(resultado).isEmpty();
    }

    @Test
    void findByTituloContainingIgnoreCaseOrAutorNombreContainingIgnoreCase_coincideParcialPorTitulo() {
        Autor autor = guardarAutor("Herman Melville");
        guardarLibro("Moby Dick", autor, Idioma.INGLES);

        List<Libro> resultado = libroRepository
                .findByTituloContainingIgnoreCaseOrAutorNombreContainingIgnoreCase("moby", "moby");

        assertThat(resultado)
                .extracting(Libro::getTitulo)
                .contains("Moby Dick");
    }

    @Test
    void findByTituloContainingIgnoreCaseOrAutorNombreContainingIgnoreCase_coincideParcialPorAutor() {
        Autor autor = guardarAutor("Fiódor Dostoyevski");
        guardarLibro("Crimen y Castigo", autor, Idioma.ESPANOL);

        List<Libro> resultado = libroRepository
                .findByTituloContainingIgnoreCaseOrAutorNombreContainingIgnoreCase("dostoyevski", "dostoyevski");

        assertThat(resultado)
                .extracting(Libro::getTitulo)
                .contains("Crimen y Castigo");
    }

    @Test
    void findByTituloContainingIgnoreCaseOrAutorNombreContainingIgnoreCase_sinCoincidencias_deberiaRetornarListaVacia() {
        List<Libro> resultado = libroRepository
                .findByTituloContainingIgnoreCaseOrAutorNombreContainingIgnoreCase(
                        "termino_inexistente_xyz", "termino_inexistente_xyz");

        assertThat(resultado).isEmpty();
    }
}
```

```java
// src/test/java/com/alurachallenge/literalura/repository/AutorRepositoryIT.java
package com.alurachallenge.literalura.repository;

import com.alurachallenge.literalura.model.Autor;
import com.alurachallenge.literalura.model.Idioma;
import com.alurachallenge.literalura.model.Libro;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AutorRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private AutorRepository autorRepository;

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private static final AtomicInteger GUTENDEX_ID_SEQ = new AtomicInteger(200000);

    private Autor guardarAutor(String nombre, Integer anoNac, Integer anoFal) {
        Autor autor = new Autor();
        autor.setNombre(nombre);
        autor.setAnoNacimiento(anoNac);
        autor.setAnoFallecimiento(anoFal);
        return autorRepository.save(autor);
    }

    private void guardarLibroParaAutor(String titulo, Autor autor) {
        Libro libro = new Libro();
        libro.setTitulo(titulo);
        libro.setAutor(autor);
        libro.setIdioma(Idioma.ESPANOL);
        libro.setGutendexId(GUTENDEX_ID_SEQ.incrementAndGet());
        libroRepository.save(libro);
    }

    @Test
    void findAll_conEntityGraph_deberiaInicializarColeccionLibrosSinLazyException() {
        Autor autor = guardarAutor("Autor Con Libros EntityGraph", 1850, 1920);
        guardarLibroParaAutor("Libro EntityGraph Uno", autor);
        guardarLibroParaAutor("Libro EntityGraph Dos", autor);

        Page<Autor> pagina = autorRepository.findAll(PageRequest.of(0, 10));
        Autor autorEncontrado = pagina.getContent().stream()
                .filter(a -> a.getNombre().equals("Autor Con Libros EntityGraph"))
                .findFirst()
                .orElseThrow();

        entityManager.clear();

        assertThat(autorEncontrado.getLibros())
                .extracting(Libro::getTitulo)
                .contains("Libro EntityGraph Uno", "Libro EntityGraph Dos");
    }

    @Test
    void findById_conEntityGraph_deberiaInicializarColeccionLibrosParaVistaDeDetalle() {
        Autor autor = guardarAutor("Autor Detalle EntityGraph", 1900, 1980);
        guardarLibroParaAutor("Libro Detalle Uno", autor);
        Long id = autor.getId();

        entityManager.flush();
        entityManager.clear();

        Autor recargado = autorRepository.findById(id).orElseThrow();
        entityManager.clear();

        assertThat(recargado.getLibros())
                .extracting(Libro::getTitulo)
                .contains("Libro Detalle Uno");
    }

    @Test
    void findByNombreContainsIgnoreCase_conEntityGraph_deberiaInicializarColeccionLibros() {
        Autor autor = guardarAutor("Herman Melville EntityGraph", 1819, 1891);
        guardarLibroParaAutor("Moby Dick EntityGraph", autor);

        entityManager.flush();
        entityManager.clear();

        Autor recargado = autorRepository.findByNombreContainsIgnoreCase("melville entitygraph")
                .orElseThrow();
        entityManager.clear();

        assertThat(recargado.getLibros())
                .extracting(Libro::getTitulo)
                .contains("Moby Dick EntityGraph");
    }

    @Test
    void findByNombre_sinEntityGraph_esUsoInternoParaValidacionesDeEscritura() {
        guardarAutor("Autor Solo Validacion", 1700, 1780);

        Optional<Autor> resultado = autorRepository.findByNombre("Autor Solo Validacion");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNombre()).isEqualTo("Autor Solo Validacion");
    }
}
```

```java
// src/test/java/com/alurachallenge/literalura/service/LibroServiceIT.java
package com.alurachallenge.literalura.service;

import com.alurachallenge.literalura.dto.*;
import com.alurachallenge.literalura.model.Autor;
import com.alurachallenge.literalura.model.Idioma;
import com.alurachallenge.literalura.model.Libro;
import com.alurachallenge.literalura.repository.AutorRepository;
import com.alurachallenge.literalura.repository.LibroRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "app.interactive=false")
@Testcontainers
@Transactional
class LibroServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private LibroService libroService;

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private AutorRepository autorRepository;

    @MockitoBean
    private ClienteGutendex clienteGutendex;

    private static final AtomicInteger GUTENDEX_ID_SEQ = new AtomicInteger(300000);

    private Autor guardarAutor(String nombre, int anoNac, Integer anoFal) {
        Autor autor = new Autor();
        autor.setNombre(nombre);
        autor.setAnoNacimiento(anoNac);
        autor.setAnoFallecimiento(anoFal);
        return autorRepository.save(autor);
    }

    private Libro guardarLibro(String titulo, Autor autor, Idioma idioma) {
        Libro libro = new Libro();
        libro.setTitulo(titulo);
        libro.setAutor(autor);
        libro.setIdioma(idioma);
        libro.setGutendexId(GUTENDEX_ID_SEQ.incrementAndGet());
        return libroRepository.save(libro);
    }

    @Test
    void buscarFlexible_coincideParcialPorTitulo_deberiaRetornarLibro() {
        Autor autor = guardarAutor("Jane Austen Flexible", 1775, 1817);
        guardarLibro("Pride and Prejudice Flexible", autor, Idioma.INGLES);

        List<LibroListResponseDTO> resultado = libroService.buscarFlexible("pride");

        assertThat(resultado).extracting(LibroListResponseDTO::titulo)
                .contains("Pride and Prejudice Flexible");
    }

    @Test
    void buscarFlexible_coincideParcialPorAutor_deberiaRetornarLibro() {
        Autor autor = guardarAutor("Fiódor Dostoyevski Flexible", 1821, 1881);
        guardarLibro("Crimen y Castigo Flexible", autor, Idioma.ESPANOL);

        List<LibroListResponseDTO> resultado = libroService.buscarFlexible("dostoyevski");

        assertThat(resultado).extracting(LibroListResponseDTO::titulo)
                .contains("Crimen y Castigo Flexible");
    }

    @Test
    void buscarFlexible_sinTermino_deberiaRetornarCatalogoCompleto() {
        Autor autor = guardarAutor("Autor Catalogo Completo", 1800, null);
        guardarLibro("Libro Catalogo Uno", autor, Idioma.ESPANOL);

        List<LibroListResponseDTO> resultado = libroService.buscarFlexible(null);

        assertThat(resultado).extracting(LibroListResponseDTO::titulo)
                .contains("Libro Catalogo Uno");
    }

    @Test
    void buscarFlexible_sinCoincidencias_deberiaRetornarListaVacia() {
        List<LibroListResponseDTO> resultado = libroService.buscarFlexible("termino_inexistente_xyz_123");
        assertThat(resultado).isEmpty();
    }

    @Test
    void buscarLibroPorId_libroExistente_deberiaRetornarDetalleCompleto() {
        Autor autor = guardarAutor("Herman Melville PorId", 1819, 1891);
        Libro libro = guardarLibro("Moby Dick PorId", autor, Idioma.INGLES);

        LibroDetalleResponseDTO resultado = libroService.buscarLibroPorId(libro.getId());

        assertThat(resultado.id()).isEqualTo(libro.getId());
        assertThat(resultado.titulo()).isEqualTo("Moby Dick PorId");
        assertThat(resultado.gutendexId()).isEqualTo(libro.getGutendexId());
        assertThat(resultado.autor().nombre()).isEqualTo("Herman Melville PorId");
    }

    @Test
    void buscarLibroPorId_libroInexistente_deberiaLanzarResourceNotFoundException() {
        assertThatThrownBy(() -> libroService.buscarLibroPorId(999999L))
                .hasMessageContaining("no encontrado");
    }

    @Test
    void actualizarNotaLibro_conNotaValida_deberiaActualizarSoloElCampoNota() {
        Autor autor = guardarAutor("Autor Nota Atomica", 1800, null);
        Libro libro = guardarLibro("Libro Nota Atomica", autor, Idioma.ESPANOL);

        LibroActualizadoResponseDTO resultado = libroService.actualizarNotaLibro(
                libro.getId(), new ActualizarNotaDTO("Una reseña personal de prueba."));

        assertThat(resultado.nota()).isEqualTo("Una reseña personal de prueba.");
        assertThat(resultado.titulo()).isEqualTo("Libro Nota Atomica");
        assertThat(resultado.mensaje()).isEqualTo("Nota actualizada exitosamente");
    }

    @Test
    void actualizarNotaLibro_libroInexistente_deberiaLanzarResourceNotFoundException() {
        assertThatThrownBy(() -> libroService.actualizarNotaLibro(999999L, new ActualizarNotaDTO("Nota huérfana")))
                .hasMessageContaining("no encontrado");
    }

    @Test
    void buscarYRegistrarLibro_gutendexIdYaRegistrado_noDeberiaCrearDuplicado() {
        Autor autor = guardarAutor("Miguel de Cervantes Dedup", 1547, 1616);
        Libro libroExistente = guardarLibro("Don Quijote Dedup", autor, Idioma.ESPANOL);
        libroExistente.setGutendexId(5000);
        libroRepository.save(libroExistente);

        when(clienteGutendex.buscarLibrosPorTitulo("Don Quijote Dedup"))
                .thenReturn(new RespuestaGutendex(List.of(
                        new DatosGutendexLibro(5000L, "Don Quijote Dedup",
                                List.of(new DatosGutendexAutor("Miguel de Cervantes Dedup", 1547, 1616)),
                                List.of("es"), 1000L))));

        LibroResponseDTO resultado = libroService.buscarYRegistrarLibro(new BusquedaLibroDTO("Don Quijote Dedup"));

        assertThat(resultado.mensaje()).isEqualTo("Libro ya existe en la base de datos");
        assertThat(libroRepository.findByGutendexId(5000)).isPresent();
    }
}
```

```java
// src/test/java/com/alurachallenge/literalura/controller/LibroControllerIT.java
package com.alurachallenge.literalura.controller;

import com.alurachallenge.literalura.model.Autor;
import com.alurachallenge.literalura.model.Idioma;
import com.alurachallenge.literalura.model.Libro;
import com.alurachallenge.literalura.repository.AutorRepository;
import com.alurachallenge.literalura.repository.LibroRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.interactive=false")
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class LibroControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AutorRepository autorRepository;

    @Autowired
    private LibroRepository libroRepository;

    private static final AtomicInteger GUTENDEX_ID_SEQ = new AtomicInteger(400000);

    private Autor guardarAutor(String nombre, Integer anoNac, Integer anoFal) {
        Autor autor = new Autor();
        autor.setNombre(nombre);
        autor.setAnoNacimiento(anoNac);
        autor.setAnoFallecimiento(anoFal);
        return autorRepository.save(autor);
    }

    private Libro guardarLibro(String titulo, Autor autor, Idioma idioma) {
        Libro libro = new Libro();
        libro.setTitulo(titulo);
        libro.setAutor(autor);
        libro.setIdioma(idioma);
        libro.setGutendexId(GUTENDEX_ID_SEQ.incrementAndGet());
        return libroRepository.save(libro);
    }

    @Test
    void obtenerLibroPorId_libroExistente_deberiaRetornar200ConDetalleYGutendexId() throws Exception {
        Autor autor = guardarAutor("George Orwell MockMvc", 1903, 1950);
        Libro libro = guardarLibro("1984 MockMvc", autor, Idioma.INGLES);

        mockMvc.perform(get("/api/libros/{id}", libro.getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(libro.getId()))
                .andExpect(jsonPath("$.titulo").value("1984 MockMvc"))
                .andExpect(jsonPath("$.gutendexId").value(libro.getGutendexId()))
                .andExpect(jsonPath("$.autor.nombre").value("George Orwell MockMvc"))
                .andExpect(jsonPath("$.idioma").value("INGLES"));
    }

    @Test
    void obtenerLibroPorId_libroInexistente_deberiaRetornar404() throws Exception {
        mockMvc.perform(get("/api/libros/{id}", 999999).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void busquedaFlexible_conTerminoQueCoincideConTitulo_deberiaRetornar200ConResultados() throws Exception {
        Autor autor = guardarAutor("Jane Austen MockMvc", 1775, 1817);
        guardarLibro("Pride and Prejudice MockMvc", autor, Idioma.INGLES);

        mockMvc.perform(get("/api/libros/busqueda-flexible")
                        .param("q", "pride and prejudice mockmvc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Pride and Prejudice MockMvc"));
    }

    @Test
    void busquedaFlexible_sinParametroQ_deberiaRetornar200ConCatalogoCompleto() throws Exception {
        mockMvc.perform(get("/api/libros/busqueda-flexible").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void actualizarNota_conBodyValido_deberiaRetornar200ConNotaActualizada() throws Exception {
        Autor autor = guardarAutor("Autor Nota MockMvc", 1800, null);
        Libro libro = guardarLibro("Libro Nota MockMvc", autor, Idioma.ESPANOL);

        mockMvc.perform(patch("/api/libros/{id}/nota", libro.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\": \"Una reseña vía MockMvc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nota").value("Una reseña vía MockMvc"))
                .andExpect(jsonPath("$.mensaje").value("Nota actualizada exitosamente"));
    }

    @Test
    void actualizarNota_libroInexistente_deberiaRetornar404() throws Exception {
        mockMvc.perform(patch("/api/libros/{id}/nota", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\": \"Nota huérfana\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
```

## 3. Validación de Compilación y Ejecución
- `mvn test-compile`: **OK** (sin errores de compilación).
- `mvn -Dtest=LibroRepositoryIT,AutorRepositoryIT,LibroServiceIT,AutorServiceIT,LibroControllerIT test`: falló por entorno local sin Docker/Testcontainers disponible (`postgres:16-alpine` no descargable en esta ejecución).
