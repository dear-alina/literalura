package com.alurachallenge.literalura.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ConsumoAPI {

    private static final Logger log = LoggerFactory.getLogger(ConsumoAPI.class);

    public String obtenerDatos(String url) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            log.error("Error al obtener datos de '{}': {}", url, e.getMessage(), e);
            throw new RuntimeException("Error al consumir la API: " + e.getMessage(), e);
        }
    }
}
