package com.alurachallenge.literalura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Datos de un libro ya obtenido de Gutendex por el cliente (navegador) y enviados
 * al backend únicamente para su deduplicación y persistencia.
 *
 * <p>La consulta a Gutendex se realiza desde el navegador porque Cloudflare bloquea
 * (HTTP 403, "managed challenge") las peticiones provenientes de la IP de datacenter
 * de Render; el navegador, con IP residencial, sí supera ese control.</p>
 *
 * <p>Los componentes {@code autores} y sus campos aceptan tanto los nombres en español
 * (nombre, anoNacimiento, anoFallecimiento) como los alias nativos de Gutendex
 * (name, birth_year, death_year) gracias a {@link DatosGutendexAutor}.</p>
 */
public record RegistrarLibroDTO(
    @NotNull(message = "El gutendexId es obligatorio")
    Long gutendexId,

    @NotBlank(message = "El título es obligatorio")
    String titulo,

    List<DatosGutendexAutor> autores,

    List<String> idiomas,

    Long descargas
) {}
