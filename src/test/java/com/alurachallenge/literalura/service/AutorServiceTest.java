package com.alurachallenge.literalura.service;

import com.alurachallenge.literalura.dto.AutorDetalleResponseDTO;
import com.alurachallenge.literalura.dto.AutorResponseDTO;
import com.alurachallenge.literalura.exception.ResourceNotFoundException;
import com.alurachallenge.literalura.model.Autor;
import com.alurachallenge.literalura.model.Idioma;
import com.alurachallenge.literalura.model.Libro;
import com.alurachallenge.literalura.repository.AutorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutorServiceTest {

    @Mock
    private AutorRepository autorRepository;

    @InjectMocks
    private AutorService autorService;

    @Test
    void listarTodosLosAutores_deberiaMapearTotalLibrosConListaNula() {
        // Arrange
        Autor autor = new Autor();
        autor.setId(1L);
        autor.setNombre("Autor Uno");
        Page<Autor> page = new PageImpl<>(List.of(autor));
        when(autorRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);

        // Act
        Page<AutorResponseDTO> resultado = autorService.listarTodosLosAutores(PageRequest.of(0, 10));

        // Assert
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().getFirst().totalLibros()).isEqualTo(0);
    }

    @Test
    void obtenerAutorDetalle_conLibrosNulos_deberiaRetornarListaVacia() {
        // Arrange
        Autor autor = new Autor();
        autor.setId(2L);
        autor.setNombre("Autor Dos");
        when(autorRepository.findById(2L)).thenReturn(Optional.of(autor));

        // Act
        AutorDetalleResponseDTO resultado = autorService.obtenerAutorDetalle(2L);

        // Assert
        assertThat(resultado.libros()).isEmpty();
    }

    @Test
    void obtenerAutorDetalle_cuandoNoExiste_deberiaLanzarResourceNotFoundException() {
        // Arrange
        when(autorRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> autorService.obtenerAutorDetalle(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Autor con ID 99 no encontrado");
    }

    @Test
    void obtenerAutoresVivosEnAno_conAnoInvalido_deberiaLanzarIllegalArgumentException() {
        // Act & Assert (no requiere arrange: la validación ocurre antes de tocar el repositorio)
        assertThatThrownBy(() -> autorService.obtenerAutoresVivosEnAno(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("año");
    }

    @Test
    void obtenerAutoresVivosEnAno_conAnoValido_deberiaMapearResultados() {
        // Arrange
        Autor autor = new Autor();
        autor.setId(3L);
        autor.setNombre("Autor Vivo");
        autor.setAnoNacimiento(1900);
        autor.setAnoFallecimiento(null);

        Libro libro = new Libro();
        libro.setId(10L);
        libro.setTitulo("Libro Vivo");
        libro.setGutendexId(1234);
        libro.setIdioma(Idioma.ESPANOL);
        libro.setAutor(autor);
        autor.setLibros(List.of(libro));

        when(autorRepository.findAutoresVivosEnAno(1950)).thenReturn(List.of(autor));

        // Act
        List<AutorResponseDTO> resultado = autorService.obtenerAutoresVivosEnAno(1950);

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().nombre()).isEqualTo("Autor Vivo");
        assertThat(resultado.getFirst().totalLibros()).isEqualTo(1);
    }
}
