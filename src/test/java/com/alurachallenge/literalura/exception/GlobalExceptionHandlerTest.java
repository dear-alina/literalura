package com.alurachallenge.literalura.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFound_deberiaRetornar404YErrorResponse() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/libros/99");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(
                new ResourceNotFoundException("Libro no encontrado"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().mensaje()).isEqualTo("Libro no encontrado");
        assertThat(response.getBody().ruta()).isEqualTo("/api/libros/99");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void handleLibroNoEncontrado_deberiaRetornar404YErrorResponse() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/libros/buscar");

        ResponseEntity<ErrorResponse> response = handler.handleLibroNoEncontrado(
                new LibroNoEncontradoException("No existe"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().mensaje()).isEqualTo("No existe");
        assertThat(response.getBody().ruta()).isEqualTo("/api/libros/buscar");
    }

    @Test
    void handleValidationException_deberiaRetornar400ConMensajeValidacion() throws Exception {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/libros");

        MethodParameter methodParameter = new MethodParameter(
                DummyController.class.getDeclaredMethod("dummy", String.class), 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
        bindingResult.addError(new FieldError("dto", "titulo", "no debe estar vacio"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().mensaje()).isEqualTo("Validación fallida");
        assertThat(response.getBody().ruta()).isEqualTo("/api/libros");
    }

    @Test
    void handleIllegalArgument_deberiaRetornar400() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/autores/vivos");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Ano invalido"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().mensaje()).isEqualTo("Ano invalido");
        assertThat(response.getBody().ruta()).isEqualTo("/api/autores/vivos");
    }

    @Test
    void handleRuntimeException_deberiaRetornar500() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/libros");

        ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(
                new RuntimeException("Error interno"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().mensaje()).isEqualTo("Error interno");
        assertThat(response.getBody().ruta()).isEqualTo("/api/libros");
    }

    static class DummyController {
        public void dummy(String titulo) {
        }
    }
}
