package com.alurachallenge.literalura.repository;

import com.alurachallenge.literalura.model.Idioma;
import com.alurachallenge.literalura.model.Libro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface LibroRepository extends JpaRepository<Libro, Long> {
    Optional<Libro> findByGutendexId(Integer gutendexId);
    Optional<Libro> findByTitulo(String titulo);
    Optional<Libro> findByTituloIgnoreCase(String titulo);
    List<Libro> findByIdioma(Idioma idioma);
    Page<Libro> findAll(Pageable pageable);
    List<Libro> findByTituloContainingIgnoreCaseOrAutorNombreContainingIgnoreCase(
            String titulo, String autorNombre);
}