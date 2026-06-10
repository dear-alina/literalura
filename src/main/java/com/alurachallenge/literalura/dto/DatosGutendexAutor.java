package com.alurachallenge.literalura.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record DatosGutendexAutor(
    @JsonAlias("name")
    String nombre,
    
    @JsonAlias("birth_year")
    Integer anoNacimiento,
    
    @JsonAlias("death_year")
    Integer anoFallecimiento
) {}
