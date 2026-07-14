package com.alurachallenge.literalura.service;

import com.alurachallenge.literalura.dto.ActualizarLibroDTO;
import com.alurachallenge.literalura.dto.ActualizarNotaDTO;
import com.alurachallenge.literalura.dto.BusquedaLibroDTO;
import com.alurachallenge.literalura.dto.DatosGutendexAutor;
import com.alurachallenge.literalura.dto.DatosGutendexLibro;
import com.alurachallenge.literalura.dto.LibroActualizadoResponseDTO;
import com.alurachallenge.literalura.dto.LibroDetalleResponseDTO;
import com.alurachallenge.literalura.dto.LibroListResponseDTO;
import com.alurachallenge.literalura.dto.LibroResponseDTO;
import com.alurachallenge.literalura.dto.RespuestaGutendex;
import com.alurachallenge.literalura.exception.ResourceNotFoundException;
import com.alurachallenge.literalura.model.Autor;
import com.alurachallenge.literalura.model.Idioma;
import com.alurachallenge.literalura.model.Libro;
import com.alurachallenge.literalura.repository.AutorRepository;
import com.alurachallenge.literalura.repository.LibroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibroServiceTest {

    @Mock
    private LibroRepository libroRepository;

    @Mock
    private AutorRepository autorRepository;

    @Mock
    private ClienteGutendex clienteGutendex;

    @InjectMocks
    private LibroService libroService;

    @Test
    void buscarYRegistrarLibro_conRespuestaVacia_deberiaLanzarRuntimeException() {
        when(clienteGutendex.buscarLibrosPorTitulo("X")).thenReturn(new RespuestaGutendex(List.of()));

        assertThatThrownBy(() -> libroService.buscarYRegistrarLibro(new BusquedaLibroDTO("X")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado en Gutendex");
    }

    @Test
    void buscarYRegistrarLibro_siExistePorGutendexId_deberiaRetornarExistente() {
        Libro existente = new Libro();
        existente.setId(1L);
        existente.setTitulo("Don Quijote");
        existente.setGutendexId(1342);
        existente.setIdioma(Idioma.ESPANOL);
        existente.setAutor(null);

        DatosGutendexLibro datos = new DatosGutendexLibro(
                1342L, "Don Quijote", List.of(), List.of("es"), 1000L);

        when(clienteGutendex.buscarLibrosPorTitulo("Don Quijote"))
                .thenReturn(new RespuestaGutendex(List.of(datos)));
        when(libroRepository.findByGutendexId(1342)).thenReturn(Optional.of(existente));

        LibroResponseDTO resultado = libroService.buscarYRegistrarLibro(new BusquedaLibroDTO("Don Quijote"));

        assertThat(resultado.mensaje()).isEqualTo("Libro ya existe en la base de datos");
        assertThat(resultado.autor()).isEqualTo("Desconocido");
        verify(libroRepository, never()).save(any(Libro.class));
    }

    @Test
    void buscarYRegistrarLibro_conAutorEIdiomaInvalidos_deberiaGuardarConNulos() {
        DatosGutendexLibro datos = new DatosGutendexLibro(
                null, "Libro sin datos", null, List.of("xx"), 10L);
        when(clienteGutendex.buscarLibrosPorTitulo("Libro sin datos"))
                .thenReturn(new RespuestaGutendex(List.of(datos)));
        when(libroRepository.save(any(Libro.class))).thenAnswer(invocation -> {
            Libro l = invocation.getArgument(0);
            l.setId(50L);
            return l;
        });

        LibroResponseDTO resultado = libroService.buscarYRegistrarLibro(new BusquedaLibroDTO("Libro sin datos"));

        assertThat(resultado.id()).isEqualTo(50L);
        assertThat(resultado.idioma()).isNull();
        assertThat(resultado.autor()).isEqualTo("Desconocido");
    }

    @Test
    void buscarFlexible_conQNull_deberiaBuscarConCadenaVacia() {
        when(libroRepository.findByTituloContainingIgnoreCaseOrAutorNombreContainingIgnoreCase("", ""))
                .thenReturn(List.of());

        List<LibroListResponseDTO> resultado = libroService.buscarFlexible(null);

        assertThat(resultado).isEmpty();
        verify(libroRepository).findByTituloContainingIgnoreCaseOrAutorNombreContainingIgnoreCase("", "");
    }

    @Test
    void buscarLibroPorId_cuandoNoExiste_deberiaLanzarResourceNotFoundException() {
        when(libroRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libroService.buscarLibroPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void actualizarNotaLibro_cuandoNoExiste_deberiaLanzarResourceNotFoundException() {
        when(libroRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libroService.actualizarNotaLibro(123L, new ActualizarNotaDTO("nota")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("123");
    }

    @Test
    void actualizarLibro_conIdiomaInvalido_deberiaLanzarIllegalArgumentException() {
        Libro libro = new Libro();
        libro.setId(7L);
        libro.setTitulo("Titulo");
        when(libroRepository.findById(7L)).thenReturn(Optional.of(libro));

        assertThatThrownBy(() -> libroService.actualizarLibro(7L,
                new ActualizarLibroDTO("nuevo", "autor", "xx", "nota")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Código de idioma inválido");
    }

    @Test
    void actualizarLibro_conNotaEnBlanco_deberiaPersistirNotaNull() {
        Libro libro = new Libro();
        libro.setId(8L);
        libro.setTitulo("Titulo");
        libro.setGutendexId(888);
        libro.setNota("anterior");
        when(libroRepository.findById(8L)).thenReturn(Optional.of(libro));
        when(libroRepository.save(any(Libro.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LibroActualizadoResponseDTO resultado = libroService.actualizarLibro(8L,
                new ActualizarLibroDTO(null, null, null, "   "));

        ArgumentCaptor<Libro> captor = ArgumentCaptor.forClass(Libro.class);
        verify(libroRepository).save(captor.capture());
        assertThat(captor.getValue().getNota()).isNull();
        assertThat(resultado.nota()).isNull();
    }

    @Test
    void buscarLibroPorTitulo_cuandoNoExiste_deberiaLanzarResourceNotFoundException() {
        when(libroRepository.findByTitulo("No existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libroService.buscarLibroPorTitulo("No existe"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No existe");
    }

    @Test
    void eliminarLibro_cuandoNoExiste_deberiaLanzarResourceNotFoundException() {
        when(libroRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libroService.eliminarLibro(1000L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(libroRepository, never()).deleteById(anyLong());
    }

    @Test
    void obtenerLibrosPorIdioma_conCodigoValido_deberiaMapearResultado() {
        Autor autor = new Autor();
        autor.setNombre("Autor");

        Libro libro = new Libro();
        libro.setId(10L);
        libro.setTitulo("Libro ES");
        libro.setGutendexId(2020);
        libro.setIdioma(Idioma.ESPANOL);
        libro.setAutor(autor);
        when(libroRepository.findByIdioma(Idioma.ESPANOL)).thenReturn(List.of(libro));

        var resultado = libroService.obtenerLibrosPorIdioma("es");

        assertThat(resultado.idioma()).isEqualTo("ESPANOL");
        assertThat(resultado.totalLibros()).isEqualTo(1);
        assertThat(resultado.libros()).hasSize(1);
        assertThat(resultado.libros().getFirst().titulo()).isEqualTo("Libro ES");
    }

    @Test
    void actualizarLibro_conAutorExistente_deberiaReusarAutor() {
        Libro libro = new Libro();
        libro.setId(9L);
        libro.setTitulo("Titulo");
        when(libroRepository.findById(9L)).thenReturn(Optional.of(libro));

        Autor autorExistente = new Autor();
        autorExistente.setNombre("Autor Existente");
        when(autorRepository.findByNombre("Autor Existente")).thenReturn(Optional.of(autorExistente));
        when(libroRepository.save(any(Libro.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LibroActualizadoResponseDTO resultado = libroService.actualizarLibro(
                9L, new ActualizarLibroDTO(null, "Autor Existente", null, null));

        assertThat(resultado.autor()).isEqualTo("Autor Existente");
        verify(autorRepository, never()).save(any(Autor.class));
    }

    @Test
    void actualizarLibro_conAutorNuevo_deberiaCrearAutor() {
        Libro libro = new Libro();
        libro.setId(11L);
        libro.setTitulo("Titulo");
        when(libroRepository.findById(11L)).thenReturn(Optional.of(libro));
        when(autorRepository.findByNombre("Autor Nuevo")).thenReturn(Optional.empty());
        when(autorRepository.save(any(Autor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(libroRepository.save(any(Libro.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LibroActualizadoResponseDTO resultado = libroService.actualizarLibro(
                11L, new ActualizarLibroDTO(null, "Autor Nuevo", null, null));

        assertThat(resultado.autor()).isEqualTo("Autor Nuevo");
        verify(autorRepository).save(any(Autor.class));
    }
}
