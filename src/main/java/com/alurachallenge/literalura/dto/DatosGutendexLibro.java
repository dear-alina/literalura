package com.alurachallenge.literalura.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record DatosGutendexLibro(
    @JsonAlias("id")
    Long id,
    
    @JsonAlias("title")
    String titulo,
    
    @JsonAlias("authors")
    List<DatosGutendexAutor> autores,
    
    @JsonAlias("languages")
    List<String> idiomas,
    
    @JsonAlias("download_count")
    Long descargas
) {}
