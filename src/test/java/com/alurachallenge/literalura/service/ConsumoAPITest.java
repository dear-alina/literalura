package com.alurachallenge.literalura.service;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class ConsumoAPITest {

    @Test
    void obtenerDatos_cuandoRespuestaOk_deberiaRetornarBody() throws Exception {
        ConsumoAPI consumoAPI = new ConsumoAPI();
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        doReturn("body-ok").when(response).body();
        doReturn(response).when(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));

        try (MockedStatic<HttpClient> mocked = mockStatic(HttpClient.class)) {
            mocked.when(HttpClient::newHttpClient).thenReturn(client);

            String resultado = consumoAPI.obtenerDatos("https://api.test");

            assertThat(resultado).isEqualTo("body-ok");
        }
    }

    @Test
    void obtenerDatos_cuandoFallaCliente_deberiaLanzarRuntimeException() throws Exception {
        ConsumoAPI consumoAPI = new ConsumoAPI();
        HttpClient client = mock(HttpClient.class);
        doThrow(new RuntimeException("fallo-red"))
                .when(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));

        try (MockedStatic<HttpClient> mocked = mockStatic(HttpClient.class)) {
            mocked.when(HttpClient::newHttpClient).thenReturn(client);

            assertThatThrownBy(() -> consumoAPI.obtenerDatos("https://api.error"))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}

