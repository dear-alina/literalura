package com.alurachallenge.literalura.dto;

import java.util.List;

public record LibrosPorIdiomaResponseDTO(
    String idioma,
    Integer totalLibros,
    List<LibroListResponseDTO> libros
) {
}
