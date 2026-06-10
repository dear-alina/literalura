package com.alurachallenge.literalura;

import com.alurachallenge.literalura.service.ConsumoAPI;
import com.alurachallenge.literalura.service.ConvierteDatos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.MockedStatic;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.alurachallenge.literalura.repository.AutorRepository;
import com.alurachallenge.literalura.repository.LibroRepository;
import com.alurachallenge.literalura.model.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(properties = "app.interactive=false")
class LiteraluraApplicationTests {

	@MockitoBean
	private LibroRepository libroRepository;

	@MockitoBean
	private AutorRepository autorRepository;

	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

	@BeforeEach
	void setUp() {
		// Redirigimos la salida de consola para capturar lo que el programa imprime
		System.setOut(new PrintStream(outContent));
	}

	// --- 1. PRUEBAS DE SERVICIOS (ConsumoAPI y ConvierteDatos) ---

	@Test
	void consumoAPI_deberiaCubrirTodoElFlujoYExcepciones() throws Exception {
		ConsumoAPI consumo = new ConsumoAPI();
		HttpClient mockClient = mock(HttpClient.class);
		HttpResponse<String> mockResponse = mock(HttpResponse.class);

		when(mockResponse.body()).thenReturn("{\"results\": []}");

		doReturn(mockResponse).when(mockClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));

		try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
			mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockClient);
			String resultado = consumo.obtenerDatos("https://api.test");
			assertEquals("{\"results\": []}", resultado);
		}

		try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
			mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockClient);

			doThrow(new RuntimeException("Error de red simulado"))
					.when(mockClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));

			assertThrows(RuntimeException.class, () -> consumo.obtenerDatos("https://error.test"));
		}
	}

	@Test
	void convierteDatos_deberiaCubrirExitoYErrorDeParsing() {
		ConvierteDatos conversor = new ConvierteDatos();

		// Exito
		String jsonValido = "{\"title\":\"Libro Test\", \"download_count\": 10}";
		DatosLibro resultado = conversor.obtenerDatos(jsonValido, DatosLibro.class);
		assertNotNull(resultado);

		// Fallo (Cubre el CATCH de ConvierteDatos)
		String jsonInvalido = "esto_no_es_un_json";
		assertThrows(RuntimeException.class, () ->
				conversor.obtenerDatos(jsonInvalido, DatosLibro.class)
		);
	}

	// --- 2. PRUEBAS DE MODELO (Autor, Libro, Idioma) ---

	@Test
	void idioma_deberiaProbarTodosLosCaminosDeConversion() {
		assertEquals(Idioma.INGLES, Idioma.fromString("en"));
		assertEquals(Idioma.ESPANOL, Idioma.fromEspanol("Español"));
		assertThrows(IllegalArgumentException.class, () -> Idioma.fromString("invalid"));
	}

	@Test
	void libro_y_autor_deberianManejarListasYDatosNulos() {
		DatosLibro datos = new DatosLibro("Test", List.of(), List.of(), 0.0);
		Libro libro = new Libro(datos);
		assertNull(libro.getAutor());

		Autor autor = new Autor();
		autor.setLibros(new ArrayList<>());
		assertNotNull(autor.getLibros());
	}
}
