package com.alurachallenge.literalura.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultadosTest {

    @Test
    void record_deberiaConservarListaDeLibros() {
        DatosLibro datosLibro = new DatosLibro(1, "Titulo", List.of(), List.of("en"), 10.0);
        Resultados resultados = new Resultados(List.of(datosLibro));

        assertThat(resultados.listaLibros()).hasSize(1);
        assertThat(resultados.listaLibros().getFirst().titulo()).isEqualTo("Titulo");
    }
}

