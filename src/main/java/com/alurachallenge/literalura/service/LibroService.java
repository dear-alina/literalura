package com.alurachallenge.literalura.service;

import com.alurachallenge.literalura.dto.BusquedaLibroDTO;
import com.alurachallenge.literalura.dto.DatosGutendexAutor;
import com.alurachallenge.literalura.dto.DatosGutendexLibro;
import com.alurachallenge.literalura.dto.LibroResponseDTO;
import com.alurachallenge.literalura.dto.LibroDetalleResponseDTO;
import com.alurachallenge.literalura.dto.LibroListResponseDTO;
import com.alurachallenge.literalura.dto.LibrosPorIdiomaResponseDTO;
import com.alurachallenge.literalura.dto.ActualizarLibroDTO;
import com.alurachallenge.literalura.dto.ActualizarNotaDTO;
import com.alurachallenge.literalura.dto.LibroActualizadoResponseDTO;
import com.alurachallenge.literalura.dto.RespuestaGutendex;
import com.alurachallenge.literalura.exception.LibroNoEncontradoException;
import com.alurachallenge.literalura.exception.ResourceNotFoundException;
import com.alurachallenge.literalura.model.Autor;
import com.alurachallenge.literalura.model.Idioma;
import com.alurachallenge.literalura.model.Libro;
import com.alurachallenge.literalura.repository.AutorRepository;
import com.alurachallenge.literalura.repository.LibroRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class LibroService {

    private static final Logger log = LoggerFactory.getLogger(LibroService.class);

    private final LibroRepository libroRepository;
    private final AutorRepository autorRepository;
    private final ClienteGutendex clienteGutendex;

    public LibroService(LibroRepository libroRepository,
                        AutorRepository autorRepository,
                        ClienteGutendex clienteGutendex) {
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
        this.clienteGutendex = clienteGutendex;
    }
    
    public LibroResponseDTO buscarYRegistrarLibro(BusquedaLibroDTO busqueda) {
        // Buscar en Gutendex primero para obtener el gutendexId real
        RespuestaGutendex respuesta = clienteGutendex.buscarLibrosPorTitulo(busqueda.titulo());

        if (respuesta == null || respuesta.resultados() == null || respuesta.resultados().isEmpty()) {
            throw new LibroNoEncontradoException("Libro no encontrado en Gutendex: " + busqueda.titulo());
        }

        DatosGutendexLibro datosGutendex = respuesta.resultados().getFirst();
        if (datosGutendex.id() != null) {
            Optional<Libro> libroExistente = libroRepository.findByGutendexId(datosGutendex.id().intValue());
            if (libroExistente.isPresent()) {
                Libro libro = libroExistente.get();
                return new LibroResponseDTO(
                    libro.getId(),
                    libro.getTitulo(),
                    libro.getGutendexId(),
                    libro.getAutor() != null ? libro.getAutor().getNombre() : "Desconocido",
                    libro.getIdioma(),
                    "Libro ya existe en la base de datos"
                );
            }
        }

        // Crear o obtener autor
        Autor autor = obtenerOCrearAutor(datosGutendex.autores());

        // Crear y guardar libro
        Libro nuevoLibro = new Libro();
        nuevoLibro.setTitulo(datosGutendex.titulo());
        nuevoLibro.setAutor(autor);
        nuevoLibro.setIdioma(mapearIdioma(datosGutendex.idiomas()));
        if (datosGutendex.id() != null) {
            nuevoLibro.setGutendexId(datosGutendex.id().intValue());
        }

        Libro libroGuardado = libroRepository.save(nuevoLibro);

        return new LibroResponseDTO(
            libroGuardado.getId(),
            libroGuardado.getTitulo(),
            libroGuardado.getGutendexId(),
            libroGuardado.getAutor() != null ? libroGuardado.getAutor().getNombre() : "Desconocido",
            libroGuardado.getIdioma(),
            "Libro registrado exitosamente"
        );
    }
    
    private Autor obtenerOCrearAutor(List<DatosGutendexAutor> autores) {
        if (autores == null || autores.isEmpty()) {
            return null;
        }
        
        DatosGutendexAutor datosAutor = autores.getFirst();
        
        // Buscar autor por nombre, si no existe crearlo
        Optional<Autor> autorExistente = autorRepository.findByNombre(datosAutor.nombre());
        if (autorExistente.isPresent()) {
            return autorExistente.get();
        }
        
        Autor nuevoAutor = new Autor();
        nuevoAutor.setNombre(datosAutor.nombre());
        nuevoAutor.setAnoNacimiento(datosAutor.anoNacimiento());
        nuevoAutor.setAnoFallecimiento(datosAutor.anoFallecimiento());
        
        return autorRepository.save(nuevoAutor);
    }
    
    private Idioma mapearIdioma(List<String> idiomas) {
        if (idiomas == null || idiomas.isEmpty()) {
            return null;
        }
        
        try {
            return Idioma.fromString(idiomas.getFirst());
        } catch (Exception e) {
            log.warn("Código de idioma no reconocido '{}', se guardará como null", idiomas.getFirst());
            return null;
        }
    }
    
    public List<LibroListResponseDTO> buscarFlexible(String q) {
        String termino = (q == null) ? "" : q.trim();

        List<Libro> resultadosLocales = libroRepository
                .findByTituloContainingIgnoreCaseOrAutorNombreContainingIgnoreCase(termino, termino);

        if (!resultadosLocales.isEmpty() || termino.isEmpty()) {
            return resultadosLocales.stream()
                    .map(this::convertirAListDTO)
                    .collect(Collectors.toList());
        }

        // BD vacía para ese término y q no está vacío → consultar Gutendex
        try {
            RespuestaGutendex respuesta = clienteGutendex.buscarLibrosPorTitulo(termino);

            if (respuesta == null || respuesta.resultados() == null || respuesta.resultados().isEmpty()) {
                return List.of();
            }

            DatosGutendexLibro datos = respuesta.resultados().getFirst();

            // Deduplicar por gutendexId antes de persistir
            if (datos.id() != null) {
                Optional<Libro> existente = libroRepository.findByGutendexId(datos.id().intValue());
                if (existente.isPresent()) {
                    return List.of(convertirAListDTO(existente.get()));
                }
            }

            Autor autor = obtenerOCrearAutor(datos.autores());

            Libro nuevoLibro = new Libro();
            nuevoLibro.setTitulo(datos.titulo());
            nuevoLibro.setAutor(autor);
            nuevoLibro.setIdioma(mapearIdioma(datos.idiomas()));
            if (datos.id() != null) {
                nuevoLibro.setGutendexId(datos.id().intValue());
            }

            Libro guardado = libroRepository.save(nuevoLibro);
            return List.of(convertirAListDTO(guardado));

        } catch (Exception e) {
            log.warn("Fallo al consultar Gutendex para '{}': {}", termino, e.getMessage());
            return List.of();
        }
    }

    public LibroDetalleResponseDTO buscarLibroPorId(Long id) {
        Libro libro = libroRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Libro con ID " + id + " no encontrado"
            ));
        return convertirADetalleDTO(libro);
    }

    public LibroDetalleResponseDTO buscarLibroPorTitulo(String titulo) {
        // Paso 1 & 2: Buscar en BD local primero (case-insensitive)
        Optional<Libro> libroLocal = libroRepository.findByTituloIgnoreCase(titulo);
        if (libroLocal.isPresent()) {
            return convertirADetalleDTO(libroLocal.get());
        }

        // Paso 3: Miss local → consultar API externa Gutendex
        RespuestaGutendex respuesta = clienteGutendex.buscarLibrosPorTitulo(titulo);

        if (respuesta == null || respuesta.resultados() == null || respuesta.resultados().isEmpty()) {
            // Paso 5: Miss completo
            throw new LibroNoEncontradoException(
                "Libro '" + titulo + "' no encontrado ni en la base de datos local ni en Gutendex"
            );
        }

        // Paso 4: Hit externo → deduplicar por gutendexId y persistir
        DatosGutendexLibro datosGutendex = respuesta.resultados().getFirst();

        if (datosGutendex.id() != null) {
            Optional<Libro> porGutendexId = libroRepository.findByGutendexId(datosGutendex.id().intValue());
            if (porGutendexId.isPresent()) {
                return convertirADetalleDTO(porGutendexId.get());
            }
        }

        Autor autor = obtenerOCrearAutor(datosGutendex.autores());

        Libro nuevoLibro = new Libro();
        nuevoLibro.setTitulo(datosGutendex.titulo());
        nuevoLibro.setAutor(autor);
        nuevoLibro.setIdioma(mapearIdioma(datosGutendex.idiomas()));
        if (datosGutendex.id() != null) {
            nuevoLibro.setGutendexId(datosGutendex.id().intValue());
        }

        Libro libroGuardado = libroRepository.save(nuevoLibro);
        return convertirADetalleDTO(libroGuardado);
    }
    
    public Page<LibroListResponseDTO> listarTodosLosLibros(Pageable pageable) {
        return libroRepository.findAll(pageable)
            .map(this::convertirAListDTO);
    }
    
    private LibroListResponseDTO convertirAListDTO(Libro libro) {
        return new LibroListResponseDTO(
            libro.getId(),
            libro.getTitulo(),
            libro.getGutendexId(),
            libro.getAutor() != null ? libro.getAutor().getNombre() : "Desconocido",
            libro.getIdioma()
        );
    }

    private LibroDetalleResponseDTO convertirADetalleDTO(Libro libro) {
        LibroDetalleResponseDTO.AutorDetalleDTO autorDTO = null;

        if (libro.getAutor() != null) {
            Autor autor = libro.getAutor();
            autorDTO = new LibroDetalleResponseDTO.AutorDetalleDTO(
                autor.getId(),
                autor.getNombre(),
                autor.getAnoNacimiento(),
                autor.getAnoFallecimiento()
            );
        }

        return new LibroDetalleResponseDTO(
            libro.getId(),
            libro.getTitulo(),
            libro.getGutendexId(),
            autorDTO,
            libro.getIdioma(),
            libro.getNota()
        );
    }
    
    public LibrosPorIdiomaResponseDTO obtenerLibrosPorIdioma(String codigoIdioma) {
        try {
            Idioma idioma = Idioma.fromString(codigoIdioma);
            List<Libro> libros = libroRepository.findByIdioma(idioma);
            
            List<LibroListResponseDTO> librosDTO = libros.stream()
                .map(this::convertirAListDTO)
                .collect(Collectors.toList());
            
            return new LibrosPorIdiomaResponseDTO(
                idioma.name(),
                libros.size(),
                librosDTO
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Código de idioma inválido: " + codigoIdioma + 
                ". Códigos válidos: en, es, pt, ru");
        }
    }
    
    public LibroActualizadoResponseDTO actualizarNotaLibro(Long id, ActualizarNotaDTO actualizacion) {
        Libro libro = libroRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Libro con ID " + id + " no encontrado"
            ));

        libro.setNota(actualizacion.nota());
        Libro libroActualizado = libroRepository.save(libro);

        return new LibroActualizadoResponseDTO(
            libroActualizado.getId(),
            libroActualizado.getTitulo(),
            libroActualizado.getAutor() != null ? libroActualizado.getAutor().getNombre() : "Desconocido",
            libroActualizado.getIdioma(),
            libroActualizado.getNota(),
            "Nota actualizada exitosamente"
        );
    }

    public LibroActualizadoResponseDTO actualizarLibro(Long id, ActualizarLibroDTO actualizacion) {
        Libro libro = libroRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Libro con ID " + id + " no encontrado"
            ));
        
        // Actualizar titulo si se proporciona
        if (actualizacion.titulo() != null && !actualizacion.titulo().isBlank()) {
            libro.setTitulo(actualizacion.titulo());
        }
        
        // Actualizar autor si se proporciona
        if (actualizacion.autorNombre() != null && !actualizacion.autorNombre().isBlank()) {
            Autor autor = obtenerOCrearAutor(actualizacion.autorNombre());
            libro.setAutor(autor);
        }
        
        // Actualizar idioma si se proporciona
        if (actualizacion.idioma() != null && !actualizacion.idioma().isBlank()) {
            try {
                Idioma idioma = Idioma.fromString(actualizacion.idioma());
                libro.setIdioma(idioma);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Código de idioma inválido: " + actualizacion.idioma());
            }
        }

        // Actualizar nota si se proporciona (se permite vaciar enviando cadena vacía)
        if (actualizacion.nota() != null) {
            libro.setNota(actualizacion.nota().isBlank() ? null : actualizacion.nota());
        }
        
        Libro libroActualizado = libroRepository.save(libro);
        
        return new LibroActualizadoResponseDTO(
            libroActualizado.getId(),
            libroActualizado.getTitulo(),
            libroActualizado.getAutor() != null ? libroActualizado.getAutor().getNombre() : "Desconocido",
            libroActualizado.getIdioma(),
            libroActualizado.getNota(),
            "Libro actualizado exitosamente"
        );
    }
    
    private Autor obtenerOCrearAutor(String nombreAutor) {
        Optional<Autor> autorExistente = autorRepository.findByNombre(nombreAutor);
        if (autorExistente.isPresent()) {
            return autorExistente.get();
        }
        
        Autor nuevoAutor = new Autor();
        nuevoAutor.setNombre(nombreAutor);
        return autorRepository.save(nuevoAutor);
    }
    
    public void eliminarLibro(Long id) {
        libroRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Libro con ID " + id + " no encontrado"
            ));

        libroRepository.deleteById(id);
    }
}
