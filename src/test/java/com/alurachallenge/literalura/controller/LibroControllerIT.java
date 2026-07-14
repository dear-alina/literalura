package com.alurachallenge.literalura.controller;

import com.alurachallenge.literalura.model.Autor;
import com.alurachallenge.literalura.model.Idioma;
import com.alurachallenge.literalura.model.Libro;
import com.alurachallenge.literalura.repository.AutorRepository;
import com.alurachallenge.literalura.repository.LibroRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @Transactional garantiza rollback tras cada test para aislar los datos
// insertados por los nuevos tests (nota, gutendexId, busqueda-flexible) y no
// afectar aserciones de "base de datos vacía" en otros tests de la clase.
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
    private LibroRepository libroRepository;

    @Autowired
    private AutorRepository autorRepository;

    // gutendexId es NOT NULL + UNIQUE en la entidad Libro: generamos valores únicos por test
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
    void listarLibros_conBaseDatosVacia_deberiaRetornar200ConPaginaVacia() throws Exception {
        mockMvc.perform(get("/api/libros")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listarLibros_conParametrosDePaginacion_deberiaRetornar200() throws Exception {
        mockMvc.perform(get("/api/libros")
                .param("page", "0")
                .param("size", "5")
                .param("sort", "titulo")
                .param("direction", "asc")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void obtenerLibrosPorIdioma_conIdiomaValido_deberiaRetornar200() throws Exception {
        mockMvc.perform(get("/api/libros/idioma")
                .param("idioma", "en")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idioma").value("INGLES"))
                .andExpect(jsonPath("$.totalLibros").value(0));
    }

    // --- Tests de GET /api/libros/{id} ---

    @Test
    void obtenerLibroPorId_libroExistente_deberiaRetornar200ConDetalleYGutendexId() throws Exception {
        Autor autor = guardarAutor("George Orwell MockMvc", 1903, 1950);
        Libro libro = guardarLibro("1984 MockMvc", autor, Idioma.INGLES);

        mockMvc.perform(get("/api/libros/{id}", libro.getId())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(libro.getId()))
                .andExpect(jsonPath("$.titulo").value("1984 MockMvc"))
                .andExpect(jsonPath("$.gutendexId").value(libro.getGutendexId()))
                .andExpect(jsonPath("$.autor.nombre").value("George Orwell MockMvc"))
                .andExpect(jsonPath("$.idioma").value("INGLES"));
    }

    @Test
    void obtenerLibroPorId_libroInexistente_deberiaRetornar404() throws Exception {
        mockMvc.perform(get("/api/libros/{id}", 999999)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // --- Tests de GET /api/libros/busqueda-flexible ---

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
        mockMvc.perform(get("/api/libros/busqueda-flexible")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // --- Tests de PATCH /api/libros/{id}/nota ---

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
