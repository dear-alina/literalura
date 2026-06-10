package com.alurachallenge.literalura.dto;

import com.alurachallenge.literalura.model.Idioma;

public record LibroActualizadoResponseDTO(
    Long id,
    String titulo,
    String autor,
    Idioma idioma,
    String mensaje
) {}
