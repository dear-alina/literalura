package com.alurachallenge.literalura.repositorio;

import com.alurachallenge.literalura.model.Autor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AutorRepository extends JpaRepository<Autor, Long> {
    @Query("SELECT a FROM Autor a WHERE a.anoNacimiento <= :fecha AND (a.anoFallecimiento IS NULL OR a.anoFallecimiento >= :fecha)")
    List<Autor> findAutoresVivosEnAno(@Param("fecha") Integer fecha);
    Optional<Autor> findByNombreContainsIgnoreCase(String nombre);
}