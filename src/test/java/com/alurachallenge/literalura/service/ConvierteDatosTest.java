package com.alurachallenge.literalura.service;

import com.alurachallenge.literalura.model.DatosLibro;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConvierteDatosTest {

    @Test
    void obtenerDatos_conJsonValido_deberiaDeserializar() {
        ConvierteDatos convierteDatos = new ConvierteDatos();
        String json = "{\"id\":1342,\"title\":\"Pride and Prejudice\",\"authors\":[],\"languages\":[\"en\"],\"download_count\":123}";

        DatosLibro datosLibro = convierteDatos.obtenerDatos(json, DatosLibro.class);

        assertThat(datosLibro).isNotNull();
        assertThat(datosLibro.id()).isEqualTo(1342);
        assertThat(datosLibro.titulo()).isEqualTo("Pride and Prejudice");
    }

    @Test
    void obtenerDatos_conJsonInvalido_deberiaLanzarRuntimeException() {
        ConvierteDatos convierteDatos = new ConvierteDatos();

        assertThatThrownBy(() -> convierteDatos.obtenerDatos("json-no-valido", DatosLibro.class))
                .isInstanceOf(RuntimeException.class);
    }
}

