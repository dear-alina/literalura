package com.alurachallenge.literalura.dto;

import com.alurachallenge.literalura.model.Idioma;

public record LibroResponseDTO(
    Long id,
    String titulo,
    String autor,
    Idioma idioma,
    String mensaje
) {}
