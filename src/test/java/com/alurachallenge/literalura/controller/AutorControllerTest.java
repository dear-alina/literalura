package com.alurachallenge.literalura.controller;

import com.alurachallenge.literalura.dto.AutorDetalleResponseDTO;
import com.alurachallenge.literalura.dto.AutorResponseDTO;
import com.alurachallenge.literalura.exception.GlobalExceptionHandler;
import com.alurachallenge.literalura.exception.ResourceNotFoundException;
import com.alurachallenge.literalura.service.AutorService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AutorController.class)
@Import(GlobalExceptionHandler.class)
class AutorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AutorService autorService;

    @Test
    void listarAutores_conDirectionDesc_deberiaConstruirPageableDesc() throws Exception {
        Page<AutorResponseDTO> page = new PageImpl<>(List.of(
                new AutorResponseDTO(1L, "Zola", 1840, 1902, 1)
        ));
        when(autorService.listarTodosLosAutores(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/autores")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "nombre")
                        .param("direction", "desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Zola"));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(autorService).listarTodosLosAutores(captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("nombre")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("nombre").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void obtenerAutoresVivos_conListaVacia_deberiaRetornar200ConArrayVacio() throws Exception {
        when(autorService.obtenerAutoresVivosEnAno(2026)).thenReturn(List.of());

        mockMvc.perform(get("/api/autores/vivos")
                        .param("ano", "2026")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void obtenerAutoresVivos_conAnoInvalido_deberiaRetornar400() throws Exception {
        when(autorService.obtenerAutoresVivosEnAno(-1))
                .thenThrow(new IllegalArgumentException("El año debe ser un número válido positivo"));

        mockMvc.perform(get("/api/autores/vivos")
                        .param("ano", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("El año debe ser un número válido positivo"));
    }

    @Test
    void obtenerDetalleAutor_ok_deberiaRetornar200() throws Exception {
        when(autorService.obtenerAutorDetalle(7L))
                .thenReturn(new AutorDetalleResponseDTO(7L, "Autor Detalle", 1900, 1980, List.of()));

        mockMvc.perform(get("/api/autores/{id}", 7)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.nombre").value("Autor Detalle"));
    }

    @Test
    void obtenerDetalleAutor_noEncontrado_deberiaRetornar404() throws Exception {
        when(autorService.obtenerAutorDetalle(999L))
                .thenThrow(new ResourceNotFoundException("Autor no encontrado"));

        mockMvc.perform(get("/api/autores/{id}", 999)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Autor no encontrado"));
    }
}

