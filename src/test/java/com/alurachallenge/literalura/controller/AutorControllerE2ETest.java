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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

class AutorControllerE2ETest extends BaseE2ETest {
    private static final String AUTORES_ENDPOINT = "/api/autores";
    private static final String AUTORES_VIVOS_ENDPOINT = "/api/autores/vivos";

    private RequestSpecification apiSpec;

    @Autowired
    private AutorRepository autorRepository;

    @Autowired
    private LibroRepository libroRepository;

    @BeforeEach
    void setUpE2E() {
        apiSpec = requestSpec();
        libroRepository.deleteAll();
        autorRepository.deleteAll();
    }

    @Test
    void getAutores_deberiaRetornarListadoPaginadoConContratoJson() {
        Autor autor = new Autor();
        autor.setNombre("Autor Listado E2E");
        autor.setAnoNacimiento(1900);
        autor.setAnoFallecimiento(1980);
        autor = autorRepository.save(autor);

        Libro libro = new Libro();
        libro.setTitulo("Libro Autor Listado");
        libro.setGutendexId(8100);
        libro.setIdioma(Idioma.ESPANOL);
        libro.setAutor(autor);
        libroRepository.save(libro);

        given().spec(apiSpec)
        .when()
                .get(AUTORES_ENDPOINT + "?page=0&size=10&sort=nombre&direction=asc")
        .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].nombre", equalTo("Autor Listado E2E"))
                .body("content[0].totalLibros", equalTo(1));
    }

    @Test
    void getAutoresVivos_deberiaRetornarAutoresEnAno() {
        Autor autorVivo = new Autor();
        autorVivo.setNombre("Autor Vivo E2E");
        autorVivo.setAnoNacimiento(1850);
        autorVivo.setAnoFallecimiento(1920);
        autorRepository.save(autorVivo);

        given().spec(apiSpec)
                .queryParam("ano", 1900)
        .when()
                .get(AUTORES_VIVOS_ENDPOINT)
        .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].nombre", equalTo("Autor Vivo E2E"));
    }

    @Test
    void getAutoresVivos_conAnoInvalido_deberiaRetornar400() {
        given().spec(apiSpec)
                .queryParam("ano", -1)
        .when()
                .get(AUTORES_VIVOS_ENDPOINT)
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("mensaje", equalTo("El año debe ser un número válido positivo"));
    }
}
