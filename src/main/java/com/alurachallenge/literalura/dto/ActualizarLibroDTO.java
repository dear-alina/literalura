package com.alurachallenge.literalura.dto;

import com.alurachallenge.literalura.model.Idioma;

public record ActualizarLibroDTO(
    String titulo,
    String autorNombre,
    String idioma
) {}
