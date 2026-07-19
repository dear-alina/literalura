package com.alurachallenge.literalura.controller;

import com.alurachallenge.literalura.BaseE2ETest;
import com.alurachallenge.literalura.model.Autor;
import com.alurachallenge.literalura.model.Idioma;
import com.alurachallenge.literalura.model.Libro;
import com.alurachallenge.literalura.repository.AutorRepository;
import com.alurachallenge.literalura.repository.LibroRepository;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class LibroControllerE2ETest extends BaseE2ETest {
    private static final String BUSCAR_REGISTRAR_ENDPOINT = "/api/libros/buscar-y-registrar";
    private static final String PATCH_NOTA_ENDPOINT = "/api/libros/{id}/nota";

    private RequestSpecification apiSpec;

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private AutorRepository autorRepository;

    @BeforeEach
    void setUpE2E() {
        apiSpec = requestSpec();
        libroRepository.deleteAll();
        autorRepository.deleteAll();
    }

    @Test
    void postBuscarYRegistrar_conLibroDeGutendex_deberiaPersistirEnPostgreSQLYRetornar201() {
        // El navegador ya obtuvo el libro de Gutendex y envía sus datos al backend
        given().spec(apiSpec)
                .contentType("application/json")
                .body("""
                        {"gutendexId":1342,"titulo":"Don Quijote",
                         "autores":[{"nombre":"Miguel de Cervantes","anoNacimiento":1547,"anoFallecimiento":1616}],
                         "idiomas":["es"],"descargas":1000}
                        """)
        .when()
                .post(BUSCAR_REGISTRAR_ENDPOINT)
        .then()
                .statusCode(201)
                .body("titulo", equalTo("Don Quijote"))
                .body("gutendexId", equalTo(1342))
                .body("autor", equalTo("Miguel de Cervantes"));

        Optional<Libro> guardado = libroRepository.findByGutendexId(1342);
        assertThat(guardado).isPresent();
        assertThat(guardado.get().getTitulo()).isEqualTo("Don Quijote");
        assertThat(autorRepository.findByNombre("Miguel de Cervantes")).isPresent();
    }

    @Test
    void patchNota_deberiaActualizarNotaYRetornar200() {
        Autor autor = new Autor();
        autor.setNombre("Autor E2E");
        autor = autorRepository.save(autor);

        Libro libro = new Libro();
        libro.setTitulo("Libro E2E");
        libro.setGutendexId(9001);
        libro.setIdioma(Idioma.ESPANOL);
        libro.setAutor(autor);
        libro = libroRepository.save(libro);

        given().spec(apiSpec)
                .contentType("application/json")
                .body("""
                        {"nota":"Reseña E2E actualizada"}
                        """)
        .when()
                .patch(PATCH_NOTA_ENDPOINT, libro.getId())
        .then()
                .statusCode(200)
                .body("id", equalTo(libro.getId().intValue()))
                .body("nota", equalTo("Reseña E2E actualizada"))
                .body("mensaje", equalTo("Nota actualizada exitosamente"));

        Libro recargado = libroRepository.findById(libro.getId()).orElseThrow();
        assertThat(recargado.getNota()).isEqualTo("Reseña E2E actualizada");
    }

    @Test
    void patchNota_cuandoLibroNoExiste_deberiaRetornar404ConContratoError() {
        given().spec(apiSpec)
                .contentType("application/json")
                .body("""
                        {"nota":"No aplica"}
                        """)
        .when()
                .patch(PATCH_NOTA_ENDPOINT, 999999)
        .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("mensaje", equalTo("Libro con ID 999999 no encontrado"))
                .body("ruta", equalTo("/api/libros/999999/nota"));
    }
}
