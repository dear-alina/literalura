package com.alurachallenge.literalura.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvierteDatos implements IConvierteDatos {

    private static final Logger log = LoggerFactory.getLogger(ConvierteDatos.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public <T> T obtenerDatos(String json, Class<T> clase) {
        try {
            return objectMapper.readValue(json, clase);
        } catch (JsonProcessingException e) {
            log.error("Error al deserializar JSON a {}: {}", clase.getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("Error al procesar JSON: " + e.getMessage(), e);
        }
    }
}
