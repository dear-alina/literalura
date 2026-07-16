package com.alurachallenge.literalura.service;

import com.alurachallenge.literalura.dto.RespuestaGutendex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class ClienteGutendex {

    private static final Logger log = LoggerFactory.getLogger(ClienteGutendex.class);
    private static final String URL_BASE = "https://gutendex.com/books?search=";

    private final RestTemplate restTemplate;

    public ClienteGutendex(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public RespuestaGutendex buscarLibrosPorTitulo(String titulo) {
        try {
            String tituloEncoded = URLEncoder.encode(titulo, StandardCharsets.UTF_8);
            String url = URL_BASE + tituloEncoded;
            return restTemplate.getForObject(url, RespuestaGutendex.class);
        } catch (Exception e) {
            log.error("Error al consultar Gutendex para '{}': {}", titulo, e.getMessage(), e);
            throw new RuntimeException("Error al buscar en Gutendex: " + e.getMessage(), e);
        }
    }
}
