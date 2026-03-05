package com.alurachallenge.literalura.model;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "libros")
public class Libro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;
    @Column(unique = true)
    private String titulo;
    @ManyToOne(fetch = FetchType.EAGER)
    private Autor autor;
    @Enumerated(EnumType.STRING)
    private Idioma idioma;

    //Constructor
    public Libro() {}

    public Libro(DatosLibro datosLibro){
        this.titulo = datosLibro.titulo();
        List<DatosAutor> autores = datosLibro.autores();
        if (autores != null && !autores.isEmpty()) {
            this.autor = new Autor(autores.getFirst());
        } else {
            this.autor = null;
        }
        List<String> lenguajes = datosLibro.idioma();
        if (lenguajes != null && !lenguajes.isEmpty()) {
            this.idioma = Idioma.fromString(lenguajes.getFirst());
        } else {
            this.idioma = null;
        }
    }
    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Autor getAutor() {
        return autor;
    }

    public void setAutor(Autor autor) {
        this.autor = autor;
    }

    public Idioma getIdioma() {
        return idioma;
    }

    public void setIdioma(Idioma idioma) {
        this.idioma = idioma;
    }

    @Override
    public String toString() {
        return "------------Libro-------------" +
                "\nTitulo = " + titulo + '\'' +
                "\nAutor = " + (autor != null ? autor.getNombre() : "Desconocido") +
                "\nIdioma = " + idioma;
    }
}


