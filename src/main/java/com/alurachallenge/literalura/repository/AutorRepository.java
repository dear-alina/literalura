package com.alurachallenge.literalura.repository;

import com.alurachallenge.literalura.model.Autor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AutorRepository extends JpaRepository<Autor, Long> {

    // JOIN FETCH dinámico: carga libros en la misma query para el catálogo público
    @EntityGraph(attributePaths = {"libros"})
    @Query("SELECT a FROM Autor a WHERE a.anoNacimiento <= :fecha AND (a.anoFallecimiento IS NULL OR a.anoFallecimiento >= :fecha)")
    List<Autor> findAutoresVivosEnAno(@Param("fecha") Integer fecha);

    // JOIN FETCH dinámico: búsqueda por nombre con libros para la vista de detalle
    @EntityGraph(attributePaths = {"libros"})
    Optional<Autor> findByNombreContainsIgnoreCase(String nombre);

    // LAZY puro — solo se usa internamente en LibroService para validar/crear autores,
    // nunca se serializa; mantenerlo sin @EntityGraph evita joins innecesarios en escritura.
    Optional<Autor> findByNombre(String nombre);

    // JOIN FETCH dinámico: paginado del catálogo general de autores
    @EntityGraph(attributePaths = {"libros"})
    Page<Autor> findAll(Pageable pageable);

    // JOIN FETCH dinámico: búsqueda por ID para la vista de detalle individual
    @EntityGraph(attributePaths = {"libros"})
    Optional<Autor> findById(Long id);
}