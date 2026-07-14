package com.alurachallenge.literalura.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatosAutorTest {

    @Test
    void record_deberiaConservarValores() {
        DatosAutor datosAutor = new DatosAutor("Jane Austen", 1775, 1817);

        assertThat(datosAutor.nombre()).isEqualTo("Jane Austen");
        assertThat(datosAutor.anoNacimiento()).isEqualTo(1775);
        assertThat(datosAutor.anoFallecimiento()).isEqualTo(1817);
    }
}

