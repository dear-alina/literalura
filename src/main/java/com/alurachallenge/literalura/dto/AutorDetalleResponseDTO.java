package com.alurachallenge.literalura.dto;

import java.util.List;

public record AutorDetalleResponseDTO(
    Long id,
    String nombre,
    Integer anoNacimiento,
    Integer anoFallecimiento,
    List<LibroListResponseDTO> libros
) {}
