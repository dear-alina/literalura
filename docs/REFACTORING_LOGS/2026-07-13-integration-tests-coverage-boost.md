# Log de Automatización QA: Cierre de Brechas en Pruebas de Integración
- **Fecha:** 2026-07-13
- **Tipo de Tarea:** Pruebas de Integración Web (MockMvc) y Contexto Spring Boot.
- **Objetivo:** Elevar métricas de JaCoCo en el paquete `controller` y en `LiteraluraApplication` para alcanzar el 100% global.

## 1. Clases de Prueba Generadas / Actualizadas
- `LibroControllerTest.java` -> Cobertura de rutas HTTP faltantes (`POST /buscar-y-registrar`, `GET /buscar`, `PATCH /{id}/nota`, `PUT /{id}`, `DELETE /{id}`) con aserciones de `200/201/204/404` y contrato JSON.
- `AutorControllerTest.java` -> Cobertura de endpoint faltante `GET /api/autores/{id}` y ramas de `GET /api/autores/vivos` (lista vacía y `400` por año inválido), además de rama `direction=desc` en paginación.
- `LiteraluraApplicationTests.java` -> Validación de carga de contexto (`contextLoads`) y delegación de `main` a `SpringApplication.run`.

## 2. Detalle de Estrategia de Integración (MockMvc & Contexto)
Se usó `@WebMvcTest` + `MockMvc` para la capa web con `@MockitoBean` en los servicios (`LibroService`, `AutorService`), evitando dependencia de base de datos para pruebas de controlador y enfocando la validación en contrato HTTP/JSON.
Se importó `GlobalExceptionHandler` para validar explícitamente respuestas de error (`404` / `400`) con estructura de `ErrorResponse`.
Para la raíz de aplicación se utilizó `@SpringBootTest` en `LiteraluraApplicationTests` para verificar arranque de contexto y una prueba adicional del método `main` con `mockStatic(SpringApplication.class)`.

## 3. Matriz de Endpoints y Ramas Cubiertas (Web Layer Checklist)
- [x] **Rutas HTTP en LibroController:** Verificado el estatus `200 OK` para lecturas y actualizaciones de notas (`PATCH`).
- [x] **Ramas de Error Web (404/400):** Evaluada la respuesta de MockMvc ante recursos inexistentes en controladores.
- [x] **Contexto de Aplicación:** Verificada la carga de Beans en `LiteraluraApplication`.
