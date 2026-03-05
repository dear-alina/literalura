package com.alurachallenge.literalura.model;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "autors")
public class Autor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;
    @Column(unique = true)
    private String nombre;
    private Integer anoNacimiento;
    private Integer anoFallecimiento;

    @OneToMany(mappedBy = "autor", cascade = CascadeType.ALL,fetch = FetchType.EAGER)
    private List<Libro> libros;

    public Autor(){}
    public Autor(DatosAutor datosAutor){
        this.nombre = datosAutor.nombre();
        this.anoNacimiento = datosAutor.anoNacimiento();
        this.anoFallecimiento = datosAutor.anoFallecimiento();
    }

    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getAnoNacimiento() {
        return anoNacimiento;
    }

    public void setAnoNacimiento(Integer anoNacimiento) {
        this.anoNacimiento = anoNacimiento;
    }

    public Integer getAnoFallecimiento() {
        return anoFallecimiento;
    }

    public void setAnoFallecimiento(Integer anoFallecimiento) {
        this.anoFallecimiento = anoFallecimiento;
    }

    public List<Libro> getLibros() {
        return libros;
    }

    public void setLibros(List<Libro> libros) {
        libros.forEach(l -> l.setAutor(this));
        this.libros = libros;
    }

    @Override
    public String toString() {
        return "----------Autor---------- " +
                "\nNombre = " + nombre + '\'' +
                "\nAño de nacimiento = " + anoNacimiento +
                "\nAño de fallecimiento = " + anoFallecimiento;
    }
}
