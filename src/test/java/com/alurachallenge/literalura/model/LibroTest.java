package com.alurachallenge.literalura.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LibroTest {

    @Test
    void constructorConDatosLibro_sinAutorNiIdioma_deberiaAsignarNulos() {
        DatosLibro datosLibro = new DatosLibro(1, "Sin Datos", List.of(), List.of(), 0.0);

        Libro libro = new Libro(datosLibro);

        assertThat(libro.getTitulo()).isEqualTo("Sin Datos");
        assertThat(libro.getAutor()).isNull();
        assertThat(libro.getIdioma()).isNull();
    }

    @Test
    void constructorConDatosLibro_conAutorEIdioma_deberiaMapearCampos() {
        DatosAutor datosAutor = new DatosAutor("Autor Uno", 1900, 1980);
        DatosLibro datosLibro = new DatosLibro(2, "Titulo Dos", List.of(datosAutor), List.of("es"), 5.0);

        Libro libro = new Libro(datosLibro);

        assertThat(libro.getTitulo()).isEqualTo("Titulo Dos");
        assertThat(libro.getAutor()).isNotNull();
        assertThat(libro.getAutor().getNombre()).isEqualTo("Autor Uno");
        assertThat(libro.getIdioma()).isEqualTo(Idioma.ESPANOL);
    }

    @Test
    void toString_conAutorNulo_deberiaUsarDesconocido() {
        Libro libro = new Libro();
        libro.setTitulo("Titulo Tres");
        libro.setAutor(null);
        libro.setIdioma(Idioma.INGLES);

        String texto = libro.toString();

        assertThat(texto).contains("Titulo Tres");
        assertThat(texto).contains("Desconocido");
    }
}

