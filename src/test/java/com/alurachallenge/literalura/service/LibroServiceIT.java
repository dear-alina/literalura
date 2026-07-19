package com.alurachallenge.literalura.service;

import com.alurachallenge.literalura.dto.*;
import com.alurachallenge.literalura.exception.LibroNoEncontradoException;
import com.alurachallenge.literalura.model.Autor;
import com.alurachallenge.literalura.model.Idioma;
import com.alurachallenge.literalura.model.Libro;
import com.alurachallenge.literalura.repository.AutorRepository;
import com.alurachallenge.literalura.repository.LibroRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(properties = "app.interactive=false")
@Testcontainers
@Transactional
class LibroServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private LibroService libroService;

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private AutorRepository autorRepository;

    // ClienteGutendex llama API externa: se mockea para aislar la lógica de BD
    @MockitoBean
    private ClienteGutendex clienteGutendex;

    // gutendexId es NOT NULL + UNIQUE en la entidad Libro: generamos valores únicos por test
    private static final AtomicInteger GUTENDEX_ID_SEQ = new AtomicInteger(300000);

    // --- Helpers de setup ---

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

    private RespuestaGutendex mockRespuestaGutendex(String titulo, String autorNombre,
                                                     int anoNac, int anoFal, String idioma) {
        DatosGutendexAutor datosAutor = new DatosGutendexAutor(autorNombre, anoNac, anoFal);
        DatosGutendexLibro datosLibro = new DatosGutendexLibro(1L, titulo, List.of(datosAutor),
                List.of(idioma), 1000L);
        return new RespuestaGutendex(List.of(datosLibro));
    }

    // --- Tests de registrarLibro (el navegador ya obtuvo el libro de Gutendex) ---

    private RegistrarLibroDTO datosRegistro(long gutendexId, String titulo, String autorNombre,
                                            int anoNac, int anoFal, String idioma) {
        return new RegistrarLibroDTO(gutendexId, titulo,
                List.of(new DatosGutendexAutor(autorNombre, anoNac, anoFal)),
                List.of(idioma), 1000L);
    }

    @Test
    void registrarLibro_libroNuevo_deberiaGuardarEnBDYRetornarMensajeExitoso() {
        LibroResponseDTO resultado = libroService.registrarLibro(
                datosRegistro(1342L, "Don Quijote", "Miguel de Cervantes", 1547, 1616, "es"));

        assertThat(resultado.titulo()).isEqualTo("Don Quijote");
        assertThat(resultado.autor()).isEqualTo("Miguel de Cervantes");
        assertThat(resultado.idioma()).isEqualTo(Idioma.ESPANOL);
        assertThat(resultado.mensaje()).isEqualTo("Libro registrado exitosamente");
        assertThat(libroRepository.findByGutendexId(1342)).isPresent();
        assertThat(autorRepository.findByNombre("Miguel de Cervantes")).isPresent();
    }

    @Test
    void registrarLibro_libroYaExistenteEnBD_deberiaRetornarTrasDeduplicarPorGutendexId() {
        Autor autor = guardarAutor("Cervantes", 1547, 1616);
        Libro libroExistente = guardarLibro("Don Quijote", autor, Idioma.ESPANOL);
        libroExistente.setGutendexId(1342);
        libroRepository.save(libroExistente);

        LibroResponseDTO resultado = libroService.registrarLibro(
                datosRegistro(1342L, "Don Quijote", "Cervantes", 1547, 1616, "es"));

        assertThat(resultado.mensaje()).isEqualTo("Libro ya existe en la base de datos");
        // No se crea un segundo libro con el mismo gutendexId
        assertThat(libroRepository.findByGutendexId(1342)).contains(libroExistente);
    }

    @Test
    void registrarLibro_conIdiomaNoReconocido_deberiaGuardarIdiomaNull() {
        LibroResponseDTO resultado = libroService.registrarLibro(
                new RegistrarLibroDTO(7777L, "Libro idioma raro",
                        List.of(new DatosGutendexAutor("Autor Desconocido", 1900, 1980)),
                        List.of("xx"), 5L));

        assertThat(resultado.idioma()).isNull();
        assertThat(libroRepository.findByGutendexId(7777)).isPresent();
    }

    // --- Tests de buscarLibroPorTitulo ---

    @Test
    void buscarLibroPorTitulo_libroExistente_deberiaRetornarDetalleConAutor() {
        Autor autor = guardarAutor("Jane Austen", 1775, 1817);
        guardarLibro("Pride and Prejudice", autor, Idioma.INGLES);

        LibroDetalleResponseDTO resultado = libroService.buscarLibroPorTitulo("Pride and Prejudice");

        assertThat(resultado.titulo()).isEqualTo("Pride and Prejudice");
        assertThat(resultado.autor().nombre()).isEqualTo("Jane Austen");
        assertThat(resultado.autor().anoNacimiento()).isEqualTo(1775);
        assertThat(resultado.idioma()).isEqualTo(Idioma.INGLES);
    }

    @Test
    void buscarLibroPorTitulo_libroInexistente_deberiaLanzarLibroNoEncontradoException() {
        // Arrange: sin datos en BD y el mock de ClienteGutendex responde null (miss completo)
        when(clienteGutendex.buscarLibrosPorTitulo("Titulo No Existe")).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> libroService.buscarLibroPorTitulo("Titulo No Existe"))
                .isInstanceOf(LibroNoEncontradoException.class)
                .hasMessageContaining("no encontrado ni en la base de datos local ni en Gutendex");
    }

    // --- Tests de listarTodosLosLibros ---

    @Test
    void listarTodosLosLibros_conVariosLibros_deberiaRetornarPaginaConTodos() {
        Autor autor = guardarAutor("Autor Paginacion", 1800, 1880);
        guardarLibro("Libro A", autor, Idioma.ESPANOL);
        guardarLibro("Libro B", autor, Idioma.INGLES);
        guardarLibro("Libro C", autor, Idioma.PORTUGUES);

        Page<LibroListResponseDTO> pagina = libroService.listarTodosLosLibros(PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(pagina.getContent())
                .extracting(LibroListResponseDTO::titulo)
                .contains("Libro A", "Libro B", "Libro C");
    }

    // --- Tests de obtenerLibrosPorIdioma ---

    @Test
    void obtenerLibrosPorIdioma_deberiaRetornarSoloLibrosDelIdiomaIndicado() {
        Autor autor = guardarAutor("Autor Idioma", 1800, null);
        guardarLibro("Libro en Español", autor, Idioma.ESPANOL);
        guardarLibro("Book in English", autor, Idioma.INGLES);

        LibrosPorIdiomaResponseDTO resultado = libroService.obtenerLibrosPorIdioma("es");

        assertThat(resultado.idioma()).isEqualTo("ESPANOL");
        assertThat(resultado.libros())
                .extracting(LibroListResponseDTO::titulo)
                .contains("Libro en Español")
                .doesNotContain("Book in English");
        assertThat(resultado.totalLibros()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void obtenerLibrosPorIdioma_codigoInvalido_deberiaLanzarIllegalArgumentException() {
        assertThatThrownBy(() -> libroService.obtenerLibrosPorIdioma("xx"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Código de idioma inválido");
    }

    // --- Tests de actualizarLibro ---

    @Test
    void actualizarLibro_conNuevoTitulo_deberiaModificarTituloEnBD() {
        Autor autor = guardarAutor("Autor Para Actualizar", 1800, null);
        Libro libro = guardarLibro("Titulo Original", autor, Idioma.ESPANOL);

        LibroActualizadoResponseDTO resultado = libroService.actualizarLibro(
                libro.getId(), new ActualizarLibroDTO("Titulo Nuevo", null, null, null));

        assertThat(resultado.titulo()).isEqualTo("Titulo Nuevo");
        assertThat(resultado.mensaje()).isEqualTo("Libro actualizado exitosamente");
        assertThat(libroRepository.findById(libro.getId()))
                .isPresent()
                .get().extracting(Libro::getTitulo).isEqualTo("Titulo Nuevo");
    }

    // --- Tests de eliminarLibro ---

    @Test
    void eliminarLibro_libroExistente_deberiaRemoverDeBD() {
        Autor autor = guardarAutor("Autor Para Eliminar", 1800, null);
        Libro libro = guardarLibro("Libro Para Eliminar", autor, Idioma.ESPANOL);
        Long id = libro.getId();

        libroService.eliminarLibro(id);

        assertThat(libroRepository.findById(id)).isEmpty();
    }

    // --- Tests de buscarFlexible ---

    @Test
    void buscarFlexible_coincideParcialPorTitulo_deberiaRetornarLibro() {
        Autor autor = guardarAutor("Jane Austen Flexible", 1775, 1817);
        guardarLibro("Pride and Prejudice Flexible", autor, Idioma.INGLES);

        List<LibroListResponseDTO> resultado = libroService.buscarFlexible("pride");

        assertThat(resultado)
                .extracting(LibroListResponseDTO::titulo)
                .contains("Pride and Prejudice Flexible");
    }

    @Test
    void buscarFlexible_coincideParcialPorAutor_deberiaRetornarLibro() {
        Autor autor = guardarAutor("Fiódor Dostoyevski Flexible", 1821, 1881);
        guardarLibro("Crimen y Castigo Flexible", autor, Idioma.ESPANOL);

        List<LibroListResponseDTO> resultado = libroService.buscarFlexible("dostoyevski");

        assertThat(resultado)
                .extracting(LibroListResponseDTO::titulo)
                .contains("Crimen y Castigo Flexible");
    }

    @Test
    void buscarFlexible_sinTermino_deberiaRetornarCatalogoCompleto() {
        Autor autor = guardarAutor("Autor Catalogo Completo", 1800, null);
        guardarLibro("Libro Catalogo Uno", autor, Idioma.ESPANOL);

        List<LibroListResponseDTO> resultado = libroService.buscarFlexible(null);

        assertThat(resultado)
                .extracting(LibroListResponseDTO::titulo)
                .contains("Libro Catalogo Uno");
    }

    @Test
    void buscarFlexible_sinCoincidencias_deberiaRetornarListaVacia() {
        List<LibroListResponseDTO> resultado = libroService.buscarFlexible("termino_inexistente_xyz_123");

        assertThat(resultado).isEmpty();
    }

    // --- Tests de buscarLibroPorId ---

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

    // --- Tests de actualizarNotaLibro (mutación atómica) ---

    @Test
    void actualizarNotaLibro_conNotaValida_deberiaActualizarSoloElCampoNota() {
        Autor autor = guardarAutor("Autor Nota Atomica", 1800, null);
        Libro libro = guardarLibro("Libro Nota Atomica", autor, Idioma.ESPANOL);

        LibroActualizadoResponseDTO resultado = libroService.actualizarNotaLibro(
                libro.getId(), new ActualizarNotaDTO("Una reseña personal de prueba."));

        assertThat(resultado.nota()).isEqualTo("Una reseña personal de prueba.");
        assertThat(resultado.titulo()).isEqualTo("Libro Nota Atomica");
        assertThat(resultado.mensaje()).isEqualTo("Nota actualizada exitosamente");
        assertThat(libroRepository.findById(libro.getId()))
                .isPresent()
                .get().extracting(Libro::getNota).isEqualTo("Una reseña personal de prueba.");
    }

    @Test
    void actualizarNotaLibro_conNotaNull_deberiaBorrarNotaExistente() {
        Autor autor = guardarAutor("Autor Borrar Nota", 1800, null);
        Libro libro = guardarLibro("Libro Borrar Nota", autor, Idioma.ESPANOL);
        libro.setNota("Nota previa a borrar");
        libroRepository.save(libro);

        libroService.actualizarNotaLibro(libro.getId(), new ActualizarNotaDTO(null));

        assertThat(libroRepository.findById(libro.getId()))
                .isPresent()
                .get().extracting(Libro::getNota).isNull();
    }

    @Test
    void actualizarNotaLibro_libroInexistente_deberiaLanzarResourceNotFoundException() {
        assertThatThrownBy(() ->
                libroService.actualizarNotaLibro(999999L, new ActualizarNotaDTO("Nota huérfana")))
                .hasMessageContaining("no encontrado");
    }

    // --- Test de deduplicación por gutendexId (reemplaza deduplicación por título) ---

    @Test
    void registrarLibro_gutendexIdYaRegistrado_noDeberiaCrearDuplicado() {
        Autor autor = guardarAutor("Miguel de Cervantes Dedup", 1547, 1616);
        Libro libroExistente = guardarLibro("Don Quijote Dedup", autor, Idioma.ESPANOL);
        libroExistente.setGutendexId(5000);
        libroRepository.save(libroExistente);

        LibroResponseDTO resultado = libroService.registrarLibro(
                datosRegistro(5000L, "Don Quijote Dedup", "Miguel de Cervantes Dedup", 1547, 1616, "es"));

        assertThat(resultado.mensaje()).isEqualTo("Libro ya existe en la base de datos");
        assertThat(libroRepository.findByGutendexId(5000)).isPresent();
        // Confirma que no se creó un segundo registro con el mismo gutendexId
        assertThat(libroRepository.findByTitulo("Don Quijote Dedup")).isPresent();
    }
}
