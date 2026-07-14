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

    // gutendexId es NOT NULL + UNIQUE en la entidad Libro: generamos valores únicos por test
    private static final AtomicInteger GUTENDEX_ID_SEQ = new AtomicInteger(100000);

    // --- Helper ---

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

    // --- Test de guardado ---

    @Test
    void save_libroNuevo_deberiaAsignarIdYPersistirEnBD() {
        Autor autor = guardarAutor("Autor Guardado");

        Libro libro = new Libro();
        libro.setTitulo("Libro Para Guardar");
        libro.setAutor(autor);
        libro.setIdioma(Idioma.ESPANOL);
        libro.setGutendexId(GUTENDEX_ID_SEQ.incrementAndGet());

        Libro guardado = libroRepository.save(libro);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getId()).isGreaterThan(0L);
        assertThat(libroRepository.findById(guardado.getId())).isPresent();
    }

    // --- Tests de findByTitulo (derived query) ---

    @Test
    void findByTitulo_tituloExistente_deberiaRetornarLibroConDatosCompletos() {
        Autor autor = guardarAutor("Gabriel García Márquez");
        guardarLibro("Cien Años de Soledad", autor, Idioma.ESPANOL);

        Optional<Libro> resultado = libroRepository.findByTitulo("Cien Años de Soledad");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getTitulo()).isEqualTo("Cien Años de Soledad");
        assertThat(resultado.get().getAutor().getNombre()).isEqualTo("Gabriel García Márquez");
        assertThat(resultado.get().getIdioma()).isEqualTo(Idioma.ESPANOL);
    }

    @Test
    void findByTitulo_tituloInexistente_deberiaRetornarEmpty() {
        Optional<Libro> resultado = libroRepository.findByTitulo("Titulo Que No Existe En BD");

        assertThat(resultado).isEmpty();
    }

    // --- Test de findByTituloIgnoreCase (derived query con IgnoreCase) ---

    @Test
    void findByTituloIgnoreCase_deberiaEncontrarLibroSinImportarMayusculas() {
        Autor autor = guardarAutor("Autor Case Test");
        guardarLibro("Don Quijote", autor, Idioma.ESPANOL);

        assertThat(libroRepository.findByTituloIgnoreCase("DON QUIJOTE")).isPresent();
        assertThat(libroRepository.findByTituloIgnoreCase("don quijote")).isPresent();
        assertThat(libroRepository.findByTituloIgnoreCase("dOn QuIjOtE")).isPresent();
    }

    @Test
    void findByTituloIgnoreCase_tituloInexistente_deberiaRetornarEmpty() {
        assertThat(libroRepository.findByTituloIgnoreCase("Titulo Inexistente XYZ")).isEmpty();
    }

    // --- Test de findByIdioma (derived query con enum) ---

    @Test
    void findByIdioma_deberiaRetornarExclusivamenteLosLibrosDelIdiomaIndicado() {
        Autor autor = guardarAutor("Autor Multi-Idioma");
        guardarLibro("Libro en Español",   autor, Idioma.ESPANOL);
        guardarLibro("Book in English",    autor, Idioma.INGLES);
        guardarLibro("Livro em Portugues", autor, Idioma.PORTUGUES);

        List<Libro> librosEspanol = libroRepository.findByIdioma(Idioma.ESPANOL);

        assertThat(librosEspanol).isNotEmpty();
        assertThat(librosEspanol)
                .extracting(Libro::getTitulo)
                .contains("Libro en Español")
                .doesNotContain("Book in English", "Livro em Portugues");
        assertThat(librosEspanol)
                .allMatch(l -> l.getIdioma() == Idioma.ESPANOL);
    }

    @Test
    void findByIdioma_sinLibrosDelIdiomaSolicitado_deberiaRetornarListaVacia() {
        Autor autor = guardarAutor("Autor Solo Español");
        guardarLibro("Libro Español Unico", autor, Idioma.ESPANOL);

        List<Libro> librosRuso = libroRepository.findByIdioma(Idioma.RUSO);

        assertThat(librosRuso).isEmpty();
    }

    // --- Test de findAll paginado (Spring Data derived) ---

    @Test
    void findAll_conPaginacionYOrden_deberiaRespetarPageSizeYOrdenarPorTitulo() {
        Autor autor = guardarAutor("Autor Paginacion");
        guardarLibro("Libro Zeta",  autor, Idioma.ESPANOL);
        guardarLibro("Libro Alpha", autor, Idioma.ESPANOL);
        guardarLibro("Libro Beta",  autor, Idioma.ESPANOL);
        guardarLibro("Libro Delta", autor, Idioma.ESPANOL);
        guardarLibro("Libro Gamma", autor, Idioma.ESPANOL);

        Page<Libro> pagina = libroRepository.findAll(PageRequest.of(0, 3, Sort.by("titulo")));

        assertThat(pagina.getTotalElements()).isGreaterThanOrEqualTo(5);
        assertThat(pagina.getContent()).hasSize(3);
        assertThat(pagina.getContent().get(0).getTitulo()).isEqualTo("Libro Alpha");
        assertThat(pagina.getContent().get(1).getTitulo()).isEqualTo("Libro Beta");
    }

    // --- Tests de findByGutendexId (derived query — deduplicación canónica) ---

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

    // --- Tests de findByTituloContainingIgnoreCaseOrAutorNombreContainingIgnoreCase (búsqueda flexible) ---

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
