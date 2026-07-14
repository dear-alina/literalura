package com.alurachallenge.literalura.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AutorTest {

    @Test
    void constructorConDatosAutor_deberiaMapearCampos() {
        DatosAutor datosAutor = new DatosAutor("Gabriel Garcia Marquez", 1927, 2014);

        Autor autor = new Autor(datosAutor);

        assertThat(autor.getNombre()).isEqualTo("Gabriel Garcia Marquez");
        assertThat(autor.getAnoNacimiento()).isEqualTo(1927);
        assertThat(autor.getAnoFallecimiento()).isEqualTo(2014);
    }

    @Test
    void setLibros_deberiaSincronizarAutorEnCadaLibro() {
        Autor autor = new Autor();
        autor.setNombre("Autor Principal");
        Libro libro1 = new Libro();
        libro1.setTitulo("L1");
        Libro libro2 = new Libro();
        libro2.setTitulo("L2");

        autor.setLibros(List.of(libro1, libro2));

        assertThat(autor.getLibros()).hasSize(2);
        assertThat(libro1.getAutor()).isSameAs(autor);
        assertThat(libro2.getAutor()).isSameAs(autor);
    }
}

