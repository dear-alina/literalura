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
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AutorRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private AutorRepository autorRepository;

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    // gutendexId es NOT NULL + UNIQUE en la entidad Libro: generamos valores únicos por test
    private static final AtomicInteger GUTENDEX_ID_SEQ = new AtomicInteger(200000);

    // --- Helper ---

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
        // Sincronizar lado inverso para evitar estado stale en el caché de sesión
        if (autor.getLibros() == null) {
            autor.setLibros(new ArrayList<>());
        }
        autor.getLibros().add(libro);
    }

    // --- Test de guardado ---

    @Test
    void save_autorNuevo_deberiaAsignarIdYPersistirEnBD() {
        Autor autor = new Autor();
        autor.setNombre("Leo Tolstói");
        autor.setAnoNacimiento(1828);
        autor.setAnoFallecimiento(1910);

        Autor guardado = autorRepository.save(autor);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getId()).isGreaterThan(0L);
        assertThat(autorRepository.findById(guardado.getId()))
                .isPresent()
                .get().extracting(Autor::getNombre).isEqualTo("Leo Tolstói");
    }

    // --- Tests de findByNombre (derived query exacta) ---

    @Test
    void findByNombre_nombreExistente_deberiaRetornarAutor() {
        guardarAutor("Jane Austen", 1775, 1817);

        Optional<Autor> resultado = autorRepository.findByNombre("Jane Austen");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getAnoNacimiento()).isEqualTo(1775);
        assertThat(resultado.get().getAnoFallecimiento()).isEqualTo(1817);
    }

    @Test
    void findByNombre_nombreInexistente_deberiaRetornarEmpty() {
        Optional<Autor> resultado = autorRepository.findByNombre("Autor Fantasma XYZ");

        assertThat(resultado).isEmpty();
    }

    // --- Tests de findByNombreContainsIgnoreCase (derived query parcial + case-insensitive) ---

    @Test
    void findByNombreContainsIgnoreCase_deberiaEncontrarConSubcadenaSinImportarMayusculas() {
        guardarAutor("Charles Dickens", 1812, 1870);

        assertThat(autorRepository.findByNombreContainsIgnoreCase("dickens")).isPresent();
        assertThat(autorRepository.findByNombreContainsIgnoreCase("CHARLES")).isPresent();
        assertThat(autorRepository.findByNombreContainsIgnoreCase("charLES dICKens")).isPresent();
    }

    @Test
    void findByNombreContainsIgnoreCase_subcadenaInexistente_deberiaRetornarEmpty() {
        assertThat(autorRepository.findByNombreContainsIgnoreCase("xyz_nombre_inexistente")).isEmpty();
    }

    // --- Test de findAutoresVivosEnAno (@Query JPQL personalizada) ---

    @Test
    void findAutoresVivosEnAno_deberiaAplicarFiltroJPQLCorrectamente() {
        // Caso 1: nació antes de 1900, murió después → VIVO en 1900
        guardarAutor("Vivo Completo", 1850, 1950);

        // Caso 2: nació exactamente en 1900, murió después → VIVO en 1900
        guardarAutor("Nació Exacto 1900", 1900, 1980);

        // Caso 3: anoFallecimiento null (aún vive) → VIVO en cualquier año posterior al nacimiento
        guardarAutor("Sin Año Fallecimiento", 1870, null);

        // Caso 4: murió antes de 1900 → NO debe aparecer
        guardarAutor("Muerto en 1899", 1800, 1899);

        // Caso 5: nació después de 1900 → NO debe aparecer
        guardarAutor("Nació en 1901", 1901, 1980);

        List<Autor> vivosEn1900 = autorRepository.findAutoresVivosEnAno(1900);

        assertThat(vivosEn1900)
                .extracting(Autor::getNombre)
                .contains("Vivo Completo", "Nació Exacto 1900", "Sin Año Fallecimiento")
                .doesNotContain("Muerto en 1899", "Nació en 1901");
    }

    @Test
    void findAutoresVivosEnAno_sinAutoresVivosEnEseAnio_deberiaRetornarListaVacia() {
        guardarAutor("Autor Antiguo", 1700, 1750);

        List<Autor> vivosEn2000 = autorRepository.findAutoresVivosEnAno(2000);

        assertThat(vivosEn2000)
                .extracting(Autor::getNombre)
                .doesNotContain("Autor Antiguo");
    }

    @Test
    void findAutoresVivosEnAno_anoFallecimientoIgualAlAnoBuscado_deberiaIncluirse() {
        // Murió exactamente en 1900 → aún contaba como vivo en 1900
        guardarAutor("Murió en 1900", 1850, 1900);

        List<Autor> vivosEn1900 = autorRepository.findAutoresVivosEnAno(1900);

        assertThat(vivosEn1900)
                .extracting(Autor::getNombre)
                .contains("Murió en 1900");
    }

    // --- Test de findAll paginado (Spring Data derived) ---

    @Test
    void findAll_conPaginacionYOrden_deberiaRespetarPageSizeYOrdenarPorNombre() {
        guardarAutor("Zola, Emile",       1840, 1902);
        guardarAutor("Austen, Jane",      1775, 1817);
        guardarAutor("Borges, Jorge Luis",1899, 1986);
        guardarAutor("Dickens, Charles",  1812, 1870);

        Page<Autor> pagina = autorRepository.findAll(PageRequest.of(0, 2, Sort.by("nombre")));

        assertThat(pagina.getTotalElements()).isGreaterThanOrEqualTo(4);
        assertThat(pagina.getContent()).hasSize(2);
        assertThat(pagina.getContent().get(0).getNombre()).isEqualTo("Austen, Jane");
        assertThat(pagina.getContent().get(1).getNombre()).isEqualTo("Borges, Jorge Luis");
    }

    // --- Tests de @EntityGraph(attributePaths = {"libros"}) ---
    // Verifican que la colección LAZY se cargue en la misma query (LEFT JOIN FETCH)
    // y siga accesible incluso tras vaciar el contexto de persistencia (simulando
    // acceso fuera de la sesión original, como ocurriría al serializar con Jackson).

    @Test
    void findAll_conEntityGraph_deberiaInicializarColeccionLibrosSinLazyException() {
        Autor autor = guardarAutor("Autor Con Libros EntityGraph", 1850, 1920);
        guardarLibroParaAutor("Libro EntityGraph Uno", autor);
        guardarLibroParaAutor("Libro EntityGraph Dos", autor);

        entityManager.flush();
        entityManager.clear();

        Page<Autor> pagina = autorRepository.findAll(PageRequest.of(0, 10));
        Autor autorEncontrado = pagina.getContent().stream()
                .filter(a -> a.getNombre().equals("Autor Con Libros EntityGraph"))
                .findFirst()
                .orElseThrow();

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
        // findByNombre NO tiene @EntityGraph: se usa solo para validar existencia
        // en flujos de escritura (LibroService.obtenerOCrearAutor), no para serializar.
        guardarAutor("Autor Solo Validacion", 1700, 1780);

        Optional<Autor> resultado = autorRepository.findByNombre("Autor Solo Validacion");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNombre()).isEqualTo("Autor Solo Validacion");
    }
}
