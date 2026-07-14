package com.alurachallenge.literalura.controller;

import com.alurachallenge.literalura.dto.BusquedaLibroDTO;
import com.alurachallenge.literalura.dto.LibroResponseDTO;
import com.alurachallenge.literalura.dto.LibroDetalleResponseDTO;
import com.alurachallenge.literalura.dto.LibroListResponseDTO;
import com.alurachallenge.literalura.dto.LibrosPorIdiomaResponseDTO;
import com.alurachallenge.literalura.dto.ActualizarLibroDTO;
import com.alurachallenge.literalura.dto.ActualizarNotaDTO;
import com.alurachallenge.literalura.dto.LibroActualizadoResponseDTO;
import com.alurachallenge.literalura.service.LibroService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/libros")
public class LibroController {
    
    @Autowired
    private LibroService libroService;
    
    @PostMapping("/buscar-y-registrar")
    public ResponseEntity<LibroResponseDTO> buscarYRegistrar(
            @Valid @RequestBody BusquedaLibroDTO busqueda) {
        try {
            LibroResponseDTO respuesta = libroService.buscarYRegistrarLibro(busqueda);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new LibroResponseDTO(null, null, null, null, null, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new LibroResponseDTO(null, null, null, null, null, "Error interno del servidor"));
        }
    }
    
    @GetMapping("/busqueda-flexible")
    public ResponseEntity<List<LibroListResponseDTO>> busquedaFlexible(
            @RequestParam(required = false) String q) {
        List<LibroListResponseDTO> resultado = libroService.buscarFlexible(q);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/buscar")
    public ResponseEntity<LibroDetalleResponseDTO> buscarPorTitulo(
            @RequestParam String titulo) {
        LibroDetalleResponseDTO respuesta = libroService.buscarLibroPorTitulo(titulo);
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/idioma")

    public ResponseEntity<LibrosPorIdiomaResponseDTO> obtenerLibrosPorIdioma(
            @RequestParam String idioma) {
        LibrosPorIdiomaResponseDTO respuesta = libroService.obtenerLibrosPorIdioma(idioma);
        return ResponseEntity.ok(respuesta);

    }
    
    @GetMapping("/{id}")
    public ResponseEntity<LibroDetalleResponseDTO> obtenerLibroPorId(@PathVariable Long id) {
        LibroDetalleResponseDTO respuesta = libroService.buscarLibroPorId(id);
        return ResponseEntity.ok(respuesta);
    }

    @PatchMapping("/{id}/nota")
    public ResponseEntity<LibroActualizadoResponseDTO> actualizarNota(
            @PathVariable Long id,
            @RequestBody ActualizarNotaDTO actualizacion) {
        LibroActualizadoResponseDTO respuesta = libroService.actualizarNotaLibro(id, actualizacion);
        return ResponseEntity.ok(respuesta);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LibroActualizadoResponseDTO> actualizarLibro(
            @PathVariable Long id,
            @RequestBody ActualizarLibroDTO actualizacion) {
        LibroActualizadoResponseDTO respuesta = libroService.actualizarLibro(id, actualizacion);
        return ResponseEntity.ok(respuesta);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarLibro(@PathVariable Long id) {
        libroService.eliminarLibro(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping
    public ResponseEntity<Page<LibroListResponseDTO>> listarLibros(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "titulo") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        
        Sort.Direction sortDir = direction.equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sort));
        Page<LibroListResponseDTO> libros = libroService.listarTodosLosLibros(pageable);
        
        return ResponseEntity.ok(libros);
    }
}
