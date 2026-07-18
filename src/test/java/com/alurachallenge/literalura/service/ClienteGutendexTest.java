package com.alurachallenge.literalura.service;

import com.alurachallenge.literalura.dto.RespuestaGutendex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    void buscarLibrosPorTitulo_deberiaCodificarUrlYRetornarCuerpo() {
        // Arrange
        RespuestaGutendex esperado = new RespuestaGutendex(List.of());
        when(restTemplate.exchange(
                eq("https://gutendex.com/books?search=Don+Quijote"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(RespuestaGutendex.class)
        )).thenReturn(ResponseEntity.ok(esperado));

        // Act
        RespuestaGutendex resultado = clienteGutendex.buscarLibrosPorTitulo("Don Quijote");

        // Assert
        assertThat(resultado).isSameAs(esperado);
        verify(restTemplate).exchange(
                eq("https://gutendex.com/books?search=Don+Quijote"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(RespuestaGutendex.class)
        );
    }

    @Test
    void buscarLibrosPorTitulo_conCaracteresEspeciales_deberiaCodificarUtf8() {
        // Arrange
        RespuestaGutendex esperado = new RespuestaGutendex(List.of());
        when(restTemplate.exchange(
                eq("https://gutendex.com/books?search=Cien+a%C3%B1os"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(RespuestaGutendex.class)
        )).thenReturn(ResponseEntity.ok(esperado));

        // Act
        RespuestaGutendex resultado = clienteGutendex.buscarLibrosPorTitulo("Cien años");

        // Assert
        assertThat(resultado).isSameAs(esperado);
    }

    @Test
    void buscarLibrosPorTitulo_conRespuestaSinCuerpo_deberiaRetornarNull() {
        // Arrange
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(RespuestaGutendex.class)
        )).thenReturn(ResponseEntity.ok().build());

        // Act
        RespuestaGutendex resultado = clienteGutendex.buscarLibrosPorTitulo("sin cuerpo");

        // Assert
        assertThat(resultado).isNull();
    }

    @Test
    void buscarLibrosPorTitulo_cuandoFallaRestTemplate_deberiaLanzarRuntimeExceptionEnvuelta() {
        // Arrange
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(RespuestaGutendex.class)
        )).thenThrow(new RestClientException("timeout"));

        // Act & Assert
        assertThatThrownBy(() -> clienteGutendex.buscarLibrosPorTitulo("test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al buscar en Gutendex")
                .hasMessageContaining("timeout");
    }
}
