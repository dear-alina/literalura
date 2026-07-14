package com.alurachallenge.literalura.service;

import com.alurachallenge.literalura.dto.RespuestaGutendex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteGutendexTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ClienteGutendex clienteGutendex;

    @Test
    void buscarLibrosPorTitulo_deberiaCodificarUrlCorrectamente() {
        RespuestaGutendex esperado = new RespuestaGutendex(null);
        when(restTemplate.getForObject(
                "https://gutendex.com/books?search=Don+Quijote",
                RespuestaGutendex.class
        )).thenReturn(esperado);

        RespuestaGutendex resultado = clienteGutendex.buscarLibrosPorTitulo("Don Quijote");

        assertThat(resultado).isSameAs(esperado);
        verify(restTemplate).getForObject(
                eq("https://gutendex.com/books?search=Don+Quijote"),
                eq(RespuestaGutendex.class)
        );
    }

    @Test
    void buscarLibrosPorTitulo_cuandoFallaRestTemplate_deberiaLanzarRuntimeExceptionEnvuelta() {
        when(restTemplate.getForObject(
                "https://gutendex.com/books?search=test",
                RespuestaGutendex.class
        )).thenThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> clienteGutendex.buscarLibrosPorTitulo("test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al buscar en Gutendex")
                .hasMessageContaining("timeout");
    }
}

