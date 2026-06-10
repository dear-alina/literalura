package com.alurachallenge.literalura.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String mensaje,
    String ruta
) {}
