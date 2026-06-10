package com.alurachallenge.literalura.dto;

import com.alurachallenge.literalura.model.Idioma;

public record LibroListResponseDTO(
    Long id,
    String titulo,
    String autor,
    Idioma idioma
) {}
