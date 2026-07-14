package com.alurachallenge.literalura.controller;

import com.alurachallenge.literalura.dto.ActualizarLibroDTO;
import com.alurachallenge.literalura.dto.ActualizarNotaDTO;
import com.alurachallenge.literalura.dto.BusquedaLibroDTO;
import com.alurachallenge.literalura.dto.LibroActualizadoResponseDTO;
import com.alurachallenge.literalura.dto.LibroDetalleResponseDTO;
import com.alurachallenge.literalura.dto.LibroResponseDTO;
import com.alurachallenge.literalura.exception.GlobalExceptionHandler;
import com.alurachallenge.literalura.exception.ResourceNotFoundException;
import com.alurachallenge.literalura.model.Idioma;
import com.alurachallenge.literalura.service.LibroService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LibroController.class)
@Import(GlobalExceptionHandler.class)
class LibroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LibroService libroService;

    @Test
    void buscarYRegistrar_ok_deberiaRetornar201ConBody() throws Exception {
        when(libroService.buscarYRegistrarLibro(any(BusquedaLibroDTO.class)))
                .thenReturn(new LibroResponseDTO(
                        1L, "Don Quijote", 1342, "Miguel de Cervantes", Idioma.ESPANOL, "Libro registrado exitosamente"
                ));

        mockMvc.perform(post("/api/libros/buscar-y-registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Don Quijote\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titulo").value("Don Quijote"))
                .andExpect(jsonPath("$.gutendexId").value(1342))
                .andExpect(jsonPath("$.autor").value("Miguel de Cervantes"));
    }

    @Test
    void buscarYRegistrar_runtimeException_deberiaRetornar404ConMensaje() throws Exception {
        when(libroService.buscarYRegistrarLibro(any(BusquedaLibroDTO.class)))
                .thenThrow(new RuntimeException("No encontrado en Gutendex"));

        mockMvc.perform(post("/api/libros/buscar-y-registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Fantasma\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No encontrado en Gutendex"));
    }

    @Test
    void buscarYRegistrar_errorInterno_deberiaRetornar500ConMensajeGenerico() throws Exception {
        when(libroService.buscarYRegistrarLibro(any(BusquedaLibroDTO.class)))
                .thenThrow(new IllegalStateException("Fallo interno"));

        mockMvc.perform(post("/api/libros/buscar-y-registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"X\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscarPorTitulo_ok_deberiaRetornar200ConDetalle() throws Exception {
        LibroDetalleResponseDTO.AutorDetalleDTO autor = new LibroDetalleResponseDTO.AutorDetalleDTO(
                5L, "Jane Austen", 1775, 1817
        );
        when(libroService.buscarLibroPorTitulo("Pride and Prejudice"))
                .thenReturn(new LibroDetalleResponseDTO(10L, "Pride and Prejudice", 1342, autor, Idioma.INGLES, null));

        mockMvc.perform(get("/api/libros/buscar")
                        .param("titulo", "Pride and Prejudice")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.titulo").value("Pride and Prejudice"))
                .andExpect(jsonPath("$.autor.nombre").value("Jane Austen"));
    }

    @Test
    void buscarPorTitulo_noEncontrado_deberiaRetornar404() throws Exception {
        when(libroService.buscarLibroPorTitulo("NoExiste"))
                .thenThrow(new ResourceNotFoundException("Libro 'NoExiste' no encontrado en la base de datos"));

        mockMvc.perform(get("/api/libros/buscar")
                        .param("titulo", "NoExiste")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Libro 'NoExiste' no encontrado en la base de datos"));
    }

    @Test
    void actualizarNota_noEncontrado_deberiaRetornar404() throws Exception {
        when(libroService.actualizarNotaLibro(any(Long.class), any(ActualizarNotaDTO.class)))
                .thenThrow(new ResourceNotFoundException("Libro con ID 999 no encontrado"));

        mockMvc.perform(patch("/api/libros/{id}/nota", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":\"texto\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void actualizarLibro_ok_deberiaRetornar200() throws Exception {
        when(libroService.actualizarLibro(any(Long.class), any(ActualizarLibroDTO.class)))
                .thenReturn(new LibroActualizadoResponseDTO(
                        1L, "Titulo Nuevo", "Autor X", Idioma.ESPANOL, "nota", "Libro actualizado exitosamente"
                ));

        mockMvc.perform(put("/api/libros/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Titulo Nuevo\",\"autorNombre\":\"Autor X\",\"idioma\":\"es\",\"nota\":\"nota\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Libro actualizado exitosamente"))
                .andExpect(jsonPath("$.titulo").value("Titulo Nuevo"));
    }

    @Test
    void eliminarLibro_ok_deberiaRetornar204() throws Exception {
        mockMvc.perform(delete("/api/libros/{id}", 1))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminarLibro_noEncontrado_deberiaRetornar404() throws Exception {
        doThrow(new ResourceNotFoundException("Libro con ID 404 no encontrado"))
                .when(libroService).eliminarLibro(404L);

        mockMvc.perform(delete("/api/libros/{id}", 404))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Libro con ID 404 no encontrado"));
    }
}

