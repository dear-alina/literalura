package com.alurachallenge.literalura.service;

import com.alurachallenge.literalura.dto.AutorDetalleResponseDTO;
import com.alurachallenge.literalura.dto.AutorResponseDTO;
import com.alurachallenge.literalura.dto.LibroListResponseDTO;
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
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "app.interactive=false")
@Testcontainers
@Transactional
class AutorServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private AutorService autorService;

    @Autowired
    private AutorRepository autorRepository;

    @Autowired
    private LibroRepository libroRepository;

    // gutendexId es NOT NULL + UNIQUE en la entidad Libro: generamos valores únicos por test
    private static final AtomicInteger GUTENDEX_ID_SEQ = new AtomicInteger(500000);

    // --- Helpers de setup ---

    private Autor guardarAutor(String nombre, Integer anoNac, Integer anoFal) {
        Autor autor = new Autor();
        autor.setNombre(nombre);
        autor.setAnoNacimiento(anoNac);
        autor.setAnoFallecimiento(anoFal);
        Autor saved = autorRepository.save(autor);
        // Inicializar la lista para mantener el lado inverso sincronizado
        if (saved.getLibros() == null) {
            saved.setLibros(new ArrayList<>());
        }
        return saved;
    }

    private void guardarLibroParaAutor(String titulo, Autor autor, Idioma idioma) {
        Libro libro = new Libro();
        libro.setTitulo(titulo);
        libro.setAutor(autor);
        libro.setIdioma(idioma);
        libro.setGutendexId(GUTENDEX_ID_SEQ.incrementAndGet());
        libroRepository.save(libro);
        // Sincronizar el lado inverso para que el caché de sesión de Hibernate sea consistente
        if (autor.getLibros() == null) {
            autor.setLibros(new ArrayList<>());
        }
        autor.getLibros().add(libro);
    }

    // --- Tests de listarTodosLosAutores ---

    @Test
    void listarTodosLosAutores_conAutoresEnBD_deberiaRetornarPaginaConDatos() {
        guardarAutor("Gabriel García Márquez", 1927, 2014);
        guardarAutor("Julio Cortázar", 1914, 1984);

        Page<AutorResponseDTO> pagina = autorService.listarTodosLosAutores(
                PageRequest.of(0, 10, Sort.by("nombre")));

        assertThat(pagina.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(pagina.getContent())
                .extracting(AutorResponseDTO::nombre)
                .contains("Gabriel García Márquez", "Julio Cortázar");
    }

    @Test
    void listarTodosLosAutores_deberiaMapearCamposCorrectosMenteAlDTO() {
        guardarAutor("Leo Tolstói", 1828, 1910);

        Page<AutorResponseDTO> pagina = autorService.listarTodosLosAutores(PageRequest.of(0, 10));

        AutorResponseDTO dto = pagina.getContent().stream()
                .filter(a -> a.nombre().equals("Leo Tolstói"))
                .findFirst()
                .orElseThrow();

        assertThat(dto.anoNacimiento()).isEqualTo(1828);
        assertThat(dto.anoFallecimiento()).isEqualTo(1910);
        assertThat(dto.totalLibros()).isEqualTo(0);
    }

    // --- Tests de obtenerAutorDetalle ---

    @Test
    void obtenerAutorDetalle_autorConLibros_deberiaRetornarDetalleConListaDeLibros() {
        Autor autor = guardarAutor("Charles Dickens", 1812, 1870);
        guardarLibroParaAutor("Oliver Twist", autor, Idioma.INGLES);
        guardarLibroParaAutor("A Tale of Two Cities", autor, Idioma.INGLES);

        // Flush para asegurar visibilidad en la misma transacción
        autorRepository.flush();
        libroRepository.flush();

        AutorDetalleResponseDTO resultado = autorService.obtenerAutorDetalle(autor.getId());

        assertThat(resultado.nombre()).isEqualTo("Charles Dickens");
        assertThat(resultado.anoNacimiento()).isEqualTo(1812);
        assertThat(resultado.libros())
                .extracting(LibroListResponseDTO::titulo)
                .contains("Oliver Twist", "A Tale of Two Cities");
    }

    @Test
    void obtenerAutorDetalle_autorInexistente_deberiaLanzarExcepcion() {
        assertThatThrownBy(() -> autorService.obtenerAutorDetalle(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Autor no encontrado");
    }

    // --- Tests de obtenerAutoresVivosEnAno ---

    @Test
    void obtenerAutoresVivosEnAno_deberiaRetornarAutoresQueVivenEnEseAno() {
        // Vivo en 1900: nació antes y murió después (o no ha muerto)
        guardarAutor("Autor Vivo en 1900", 1850, 1950);
        // Nació en 1900, murió 1901: también estaba vivo en 1900
        guardarAutor("Autor Nació en 1900", 1900, 1901);
        // Murió antes de 1900: no debe aparecer
        guardarAutor("Autor Muerto en 1899", 1800, 1899);
        // Nació después de 1900: no debe aparecer
        guardarAutor("Autor Nació en 1901", 1901, 1980);

        List<AutorResponseDTO> resultado = autorService.obtenerAutoresVivosEnAno(1900);

        assertThat(resultado)
                .extracting(AutorResponseDTO::nombre)
                .contains("Autor Vivo en 1900", "Autor Nació en 1900")
                .doesNotContain("Autor Muerto en 1899", "Autor Nació en 1901");
    }

    @Test
    void obtenerAutoresVivosEnAno_autorSinAnoFallecimiento_deberiaConsiderarseComoVivo() {
        guardarAutor("Autor Sin Año Fallecimiento", 1800, null);

        List<AutorResponseDTO> resultado = autorService.obtenerAutoresVivosEnAno(1900);

        assertThat(resultado)
                .extracting(AutorResponseDTO::nombre)
                .contains("Autor Sin Año Fallecimiento");
    }

    @Test
    void obtenerAutoresVivosEnAno_anoNegativo_deberiaLanzarIllegalArgumentException() {
        assertThatThrownBy(() -> autorService.obtenerAutoresVivosEnAno(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("año debe ser un número válido positivo");
    }
}
