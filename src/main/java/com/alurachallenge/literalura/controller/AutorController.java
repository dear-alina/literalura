package com.alurachallenge.literalura.controller;

import com.alurachallenge.literalura.dto.AutorResponseDTO;
import com.alurachallenge.literalura.dto.AutorDetalleResponseDTO;
import com.alurachallenge.literalura.service.AutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/autores")
public class AutorController {
    
    @Autowired
    private AutorService autorService;
    
    @GetMapping
    public ResponseEntity<Page<AutorResponseDTO>> listarAutores(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nombre") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        
        Sort.Direction sortDir = direction.equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sort));
        Page<AutorResponseDTO> autores = autorService.listarTodosLosAutores(pageable);
        
        return ResponseEntity.ok(autores);
    }
    
    @GetMapping("/vivos")
    public ResponseEntity<List<AutorResponseDTO>> obtenerAutoresVivos(
            @RequestParam Integer ano) {
        List<AutorResponseDTO> autores = autorService.obtenerAutoresVivosEnAno(ano);
        return ResponseEntity.ok(autores);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AutorDetalleResponseDTO> obtenerDetalleAutor(@PathVariable Long id) {
        AutorDetalleResponseDTO autor = autorService.obtenerAutorDetalle(id);
        return ResponseEntity.ok(autor);
    }
}
