package com.alurachallenge.literalura.dto;

import com.alurachallenge.literalura.model.Idioma;

public record LibroDetalleResponseDTO(
    Long id,
    String titulo,
    Integer gutendexId,
    AutorDetalleDTO autor,
    Idioma idioma,
    String nota
) {
    public record AutorDetalleDTO(
        Long id,
        String nombre,
        Integer anoNacimiento,
        Integer anoFallecimiento
    ) {}
}
