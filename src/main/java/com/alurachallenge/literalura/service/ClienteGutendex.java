package com.alurachallenge.literalura.service;

import com.alurachallenge.literalura.dto.RespuestaGutendex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");
            headers.set("Accept", "application/json, text/plain, */*");
            headers.set("Accept-Language", "en-US,en;q=0.9");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<RespuestaGutendex> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    RespuestaGutendex.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("Error al consultar Gutendex para '{}': {}", titulo, e.getMessage(), e);
            throw new RuntimeException("Error al buscar en Gutendex: " + e.getMessage(), e);
        }
    }
}
