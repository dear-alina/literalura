package com.alurachallenge.literalura.dto;

import com.alurachallenge.literalura.model.Idioma;

public record LibroDetalleResponseDTO(
    Long id,
    String titulo,
    AutorDetalleDTO autor,
    Idioma idioma
) {
    public record AutorDetalleDTO(
        Long id,
        String nombre,
        Integer anoNacimiento,
        Integer anoFallecimiento
    ) {}
}
