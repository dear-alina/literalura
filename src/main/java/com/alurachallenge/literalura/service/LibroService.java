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
import com.alurachallenge.literalura.exception.ResourceNotFoundException;
import com.alurachallenge.literalura.model.Autor;
import com.alurachallenge.literalura.model.Idioma;
import com.alurachallenge.literalura.model.Libro;
import com.alurachallenge.literalura.repository.AutorRepository;
import com.alurachallenge.literalura.repository.LibroRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Autowired
    private LibroRepository libroRepository;
    
    @Autowired
    private AutorRepository autorRepository;
    
    @Autowired
    private ClienteGutendex clienteGutendex;
    
    public LibroResponseDTO buscarYRegistrarLibro(BusquedaLibroDTO busqueda) {
        // Buscar en Gutendex primero para obtener el gutendexId real
        RespuestaGutendex respuesta = clienteGutendex.buscarLibrosPorTitulo(busqueda.titulo());

        if (respuesta == null || respuesta.resultados() == null || respuesta.resultados().isEmpty()) {
            throw new RuntimeException("Libro no encontrado en Gutendex");
        }

        DatosGutendexLibro datosGutendex = respuesta.resultados().get(0);

        // Deduplicar por gutendexId (más fiable que por título)
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
        
        DatosGutendexAutor datosAutor = autores.get(0);
        
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
            return Idioma.fromString(idiomas.get(0));
        } catch (Exception e) {
            return null;
        }
    }
    
    public List<LibroListResponseDTO> buscarFlexible(String q) {
        String termino = (q == null) ? "" : q.trim();
        return libroRepository
                .findByTituloContainingIgnoreCaseOrAutorNombreContainingIgnoreCase(termino, termino)
                .stream()
                .map(this::convertirAListDTO)
                .collect(Collectors.toList());
    }

    public LibroDetalleResponseDTO buscarLibroPorId(Long id) {
        Libro libro = libroRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Libro con ID " + id + " no encontrado"
            ));
        return convertirADetalleDTO(libro);
    }

    public LibroDetalleResponseDTO buscarLibroPorTitulo(String titulo) {
        Libro libro = libroRepository.findByTitulo(titulo)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Libro '" + titulo + "' no encontrado en la base de datos"
            ));
        
        return convertirADetalleDTO(libro);
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
    
    @Transactional
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
        Libro libro = libroRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Libro con ID " + id + " no encontrado"
            ));
        
        libroRepository.deleteById(id);
    }
}
