package com.alurachallenge.literalura.dto;

public record AutorResponseDTO(
    Long id,
    String nombre,
    Integer anoNacimiento,
    Integer anoFallecimiento,
    Integer totalLibros
) {}
