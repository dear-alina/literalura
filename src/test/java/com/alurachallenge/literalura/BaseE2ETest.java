package com.alurachallenge.literalura;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.restassured.builder.RequestSpecBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseE2ETest {

    // Patrón "singleton container": se inicia UNA sola vez en el bloque estático y se
    // comparte entre todas las clases E2E sin @Container/@Testcontainers, que lo detendrían
    // y reiniciarían entre clases (rompiendo el datasource cuando Spring cachea el contexto).
    // La JVM/Ryuk lo detiene al finalizar la ejecución.
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("literalura_e2e")
            .withUsername("literalura")
            .withPassword("literalura");

    static {
        POSTGRES.start();
    }

    @LocalServerPort
    protected int port;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void configureRestAssured() {
        // Evita que RestAssured herede configuración estática de otras suites
        RestAssured.reset();
    }

    protected RequestSpecification requestSpec() {
        return new RequestSpecBuilder()
                .setBaseUri("http://localhost")
                .setPort(port)
                .build();
    }
}
