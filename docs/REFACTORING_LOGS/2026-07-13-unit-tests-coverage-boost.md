# Log de Automatización QA: Cierre de Brechas en Pruebas Unitarias
- **Fecha:** 2026-07-13
- **Tipo de Tarea:** Generación de Pruebas Unitarias Puras (Mockito / JUnit 5).
- **Objetivo:** Mejorar métricas de JaCoCo en capas `exception`, `service` y `model`.

## 1. Clases de Prueba Generadas / Actualizadas
- `GlobalExceptionHandlerTest.java` -> Validación unitaria de mapeo de excepciones a respuestas HTTP (`404`, `400`, `500`) y estructura de `ErrorResponse`.
- `LibroNoEncontradoExceptionTest.java` -> Verificación del mensaje propagado por la excepción personalizada.
- `LibroServiceTest.java` -> Cobertura de ramas de error y edge cases: `Optional.empty()`, deduplicación por `gutendexId`, idioma inválido, nota en blanco, búsqueda flexible con `q` nulo.
- `AutorServiceTest.java` -> Cobertura de ramas en validación de año, autor inexistente y mapeo de listas nulas de libros.
- `ClienteGutendexTest.java` -> Pruebas unitarias de construcción/codificación de URL y envoltura de errores del cliente HTTP.
- `ConsumoAPITest.java` -> Cobertura de consumo HTTP encapsulado: respuesta exitosa y excepción del cliente (`HttpClient.send`) envuelta en `RuntimeException`.
- `ConvierteDatosTest.java` -> Cobertura de deserialización JSON exitosa y rama de error (`JsonProcessingException`) en `ConvierteDatos`.
- `DatosAutorTest.java` -> Verificación de instanciación y acceso de campos del record.
- `ResultadosTest.java` -> Verificación de mapeo/retención de lista en el record.
- `LibroTest.java` -> Cobertura de constructor con/sin datos y `toString()` con autor nulo.
- `AutorTest.java` -> Cobertura de constructor desde `DatosAutor` y sincronización bidireccional en `setLibros`.

## 2. Detalle de Estrategia de Mocks (Lógica de Negocio)
Se implementaron pruebas unitarias aisladas con `@ExtendWith(MockitoExtension.class)`, `@Mock` y `@InjectMocks`, sin levantar contexto Spring ni usar base de datos.  
En `LibroServiceTest` y `AutorServiceTest` se mockearon repositorios y dependencias externas para forzar ramas específicas (por ejemplo, `Optional.empty()`, entradas inválidas, autores existentes/nuevos y respuestas alternas).  
En `GlobalExceptionHandlerTest` se instanció directamente el handler y se simuló `WebRequest`; para validación (`MethodArgumentNotValidException`) se construyó un `BindingResult` real para recorrer el flujo de errores.
En `ConsumoAPITest` se mockeó estáticamente `HttpClient.newHttpClient()` para aislar la llamada de red sin tocar infraestructura externa.

## 3. Matriz de Casos Edge y Ramas Cubiertas (Branch Coverage)
- [x] **Excepciones No Encontradas:** Verificado el lanzamiento y captura de `LibroNoEncontradoException`.
- [x] **Ramas Alternativas en Servicios:** Evaluados los flujos condicionales de error en `LibroService` y `AutorService`.
- [x] **Servicios de Integración HTTP/JSON:** Cubiertos caminos feliz y de error en `ConsumoAPI` y `ConvierteDatos`.
- [x] **Modelos y DTOs:** Verificada instanciación y métodos propios en `DatosAutor` y `Resultados`.

## 4. Código Java de las Pruebas Implementadas
```java
// GlobalExceptionHandlerTest.java (fragmento clave)
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
}
```

```java
// LibroServiceTest.java (fragmentos de ramas críticas)
@Test
void buscarLibroPorId_cuandoNoExiste_deberiaLanzarResourceNotFoundException() {
    when(libroRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> libroService.buscarLibroPorId(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
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
```

```java
// ConsumoAPITest.java (fragmento)
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
```
