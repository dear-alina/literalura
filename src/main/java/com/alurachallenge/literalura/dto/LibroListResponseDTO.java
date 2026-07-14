package com.alurachallenge.literalura.dto;

import com.alurachallenge.literalura.model.Idioma;

public record LibroListResponseDTO(
    Long id,
    String titulo,
    Integer gutendexId,
    String autor,
    Idioma idioma
) {}
