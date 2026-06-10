package com.alurachallenge.literalura.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record RespuestaGutendex(
    @JsonAlias("results")
    List<DatosGutendexLibro> resultados
) {}
