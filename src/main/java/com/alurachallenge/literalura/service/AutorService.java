package com.alurachallenge.literalura.service;

import com.alurachallenge.literalura.dto.AutorResponseDTO;
import com.alurachallenge.literalura.dto.AutorDetalleResponseDTO;
import com.alurachallenge.literalura.dto.LibroListResponseDTO;
import com.alurachallenge.literalura.model.Autor;
import com.alurachallenge.literalura.model.Libro;
import com.alurachallenge.literalura.repository.AutorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AutorService {
    
    @Autowired
    private AutorRepository autorRepository;
    
    public Page<AutorResponseDTO> listarTodosLosAutores(Pageable pageable) {
        return autorRepository.findAll(pageable)
            .map(this::convertirAResponseDTO);
    }
    
    public AutorDetalleResponseDTO obtenerAutorDetalle(Long id) {
        Autor autor = autorRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Autor no encontrado"));
        return convertirADetalleDTO(autor);
    }
    
    public List<AutorResponseDTO> obtenerAutoresVivosEnAno(Integer ano) {
        if (ano == null || ano < 0) {
            throw new IllegalArgumentException("El año debe ser un número válido positivo");
        }
        
        return autorRepository.findAutoresVivosEnAno(ano)
            .stream()
            .map(this::convertirAResponseDTO)
            .collect(Collectors.toList());
    }
    
    private AutorResponseDTO convertirAResponseDTO(Autor autor) {
        int totalLibros = autor.getLibros() != null ? autor.getLibros().size() : 0;
        
        return new AutorResponseDTO(
            autor.getId(),
            autor.getNombre(),
            autor.getAnoNacimiento(),
            autor.getAnoFallecimiento(),
            totalLibros
        );
    }
    
    private AutorDetalleResponseDTO convertirADetalleDTO(Autor autor) {
        List<LibroListResponseDTO> librosDTO = autor.getLibros() != null ? 
            autor.getLibros().stream()
                .map(this::convertirLibroADTO)
                .collect(Collectors.toList()) :
            List.of();
        
        return new AutorDetalleResponseDTO(
            autor.getId(),
            autor.getNombre(),
            autor.getAnoNacimiento(),
            autor.getAnoFallecimiento(),
            librosDTO
        );
    }
    
    private LibroListResponseDTO convertirLibroADTO(Libro libro) {
        return new LibroListResponseDTO(
            libro.getId(),
            libro.getTitulo(),
            libro.getGutendexId(),
            libro.getAutor() != null ? libro.getAutor().getNombre() : "Desconocido",
            libro.getIdioma()
        );
    }
}
