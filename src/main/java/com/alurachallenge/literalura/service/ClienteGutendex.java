package com.alurachallenge.literalura.service;

import com.alurachallenge.literalura.dto.RespuestaGutendex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class ClienteGutendex {
    private static final String URL_BASE = "https://gutendex.com/books?search=";
    
    @Autowired
    private RestTemplate restTemplate;
    
    public RespuestaGutendex buscarLibrosPorTitulo(String titulo) {
        try {
            String tituloEncoded = URLEncoder.encode(titulo, StandardCharsets.UTF_8);
            String url = URL_BASE + tituloEncoded;
            return restTemplate.getForObject(url, RespuestaGutendex.class);
        } catch (Exception e) {
            throw new RuntimeException("Error al buscar en Gutendex: " + e.getMessage());
        }
    }
}
