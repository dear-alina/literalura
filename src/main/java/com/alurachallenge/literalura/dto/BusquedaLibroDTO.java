package com.alurachallenge.literalura.dto;

import jakarta.validation.constraints.NotBlank;

public record BusquedaLibroDTO(
    @NotBlank(message = "El título es obligatorio")
    String titulo
) {}
