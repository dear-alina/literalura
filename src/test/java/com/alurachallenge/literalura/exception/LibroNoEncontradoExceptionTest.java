package com.alurachallenge.literalura.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LibroNoEncontradoExceptionTest {

    @Test
    void constructor_deberiaConservarMensaje() {
        LibroNoEncontradoException ex = new LibroNoEncontradoException("Libro no existe");

        assertThat(ex).hasMessage("Libro no existe");
    }
}

