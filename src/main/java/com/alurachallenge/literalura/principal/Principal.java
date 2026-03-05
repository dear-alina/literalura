package com.alurachallenge.literalura.principal;

import com.alurachallenge.literalura.model.*;
import com.alurachallenge.literalura.repositorio.AutorRepository;
import com.alurachallenge.literalura.repositorio.LibroRepository;
import com.alurachallenge.literalura.service.ConsumoAPI;
import com.alurachallenge.literalura.service.ConvierteDatos;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
@Component
public class Principal {
    private Scanner teclado = new Scanner(System.in);
    private ConsumoAPI consumoAPI = new ConsumoAPI();
    private static final String URL_BASE = "https://gutendex.com/books/";
    private ConvierteDatos conversor = new ConvierteDatos();
    private LibroRepository libroRepository;
    private AutorRepository autorRepository;

    public Principal(LibroRepository libroRepository, AutorRepository autorRepository) {
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
    }

    public void muestraElMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    ---------------------------------------------------
                    Elige alguna opción usando los siguientes números:
                    ---------------------------------------------------
                    1 - Buscar libro por título y registrarlo
                    2 - Buscar libro en la BD por título
                    3 - Listar todos los libros registrados
                    4 - Listar todos los autores registrados
                    5 - Listar autores vivos en un ano determinado
                    6 - Listar libros por idioma
                    0 - Salir
                    """;
            System.out.println(menu);
            try {
                opcion = Integer.parseInt(teclado.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Opción inválida, ingrese un número.");
                continue;
            }

            switch (opcion) {
                case 1:
                    buscarYRegistrarLibro();
                    break;
                case 2:
                    buscarLibroPorTituloEnBD();
                    break;
                case 3:
                    listarLibrosRegistrados();
                    break;
                case 4:
                    listarAutoresRegistrados();
                    break;
                case 5:
                    listarAutoresVivosEnAno();
                    break;
                case 6:
                    listarLibrosPorIdioma();
                    break;
                case 0:
                    System.out.println("Cerrando la aplicación...");
                    break;
                default:
                    System.out.println("Opción inválida");
            }
        }
    }

    // 1. Buscar en API y guardar
    private void buscarYRegistrarLibro() {
        DatosLibro datosLibro = obtenerDatosLibroDeAPI();
        if (datosLibro == null) {
            System.out.println("No se encontró el libro en la API.");
            return;
        }

        // Verificar si ya existe en BD por título exacto
        Optional<Libro> libroExistente = libroRepository.findByTituloIgnoreCase(datosLibro.titulo());
        if (libroExistente.isPresent()) {
            System.out.println("El libro ya está registrado en la base de datos:");
            System.out.println(libroExistente.get());
            return;
        }

        // Crear libro temporal con los datos de la API
        Libro nuevoLibro = new Libro(datosLibro);

        // Manejar autor: buscar por nombre o crear nuevo
        Autor autorTemp = nuevoLibro.getAutor();
        if (autorTemp != null) {
            Optional<Autor> autorExistente = autorRepository.findByNombreContainsIgnoreCase(autorTemp.getNombre());
            if (autorExistente.isPresent()) {
                nuevoLibro.setAutor(autorExistente.get());
            } else {
                // Guardar autor nuevo
                autorRepository.save(autorTemp);
                // autorTemp ahora tiene ID
                nuevoLibro.setAutor(autorTemp);
            }
        }

        // Guardar libro (el autor ya está gestionado)
        libroRepository.save(nuevoLibro);
        System.out.println("Libro registrado con éxito:");
        System.out.println(nuevoLibro);
    }

    // Metodo auxiliar para consultar API y extraer el primer libro que coincida
    private DatosLibro obtenerDatosLibroDeAPI() {
        System.out.println("Ingrese el título del libro a buscar:");
        String titulo = teclado.nextLine();
        String url = URL_BASE + "?search=" + titulo.replace(" ", "%20");
        String json = consumoAPI.obtenerDatos(url);
        Resultados resultados = conversor.obtenerDatos(json, Resultados.class);
        return resultados.listaLibros().stream()
                .filter(l -> l.titulo().toLowerCase().contains(titulo.toLowerCase()))
                .findFirst()
                .orElse(null);
    }

    // 2. Buscar libro en BD por título
    private void buscarLibroPorTituloEnBD() {
        System.out.println("Ingrese el título del libro a buscar en la base de datos:");
        String titulo = teclado.nextLine();
        Optional<Libro> libro = libroRepository.findByTituloIgnoreCase(titulo);
        if (libro.isPresent()) {
            System.out.println("Libro encontrado:");
            System.out.println(libro.get());
        } else {
            System.out.println("Libro no encontrado en la base de datos.");
        }
    }

    // 3. Listar todos los libros registrados
    private void listarLibrosRegistrados() {
        List<Libro> libros = libroRepository.findAll();
        if (libros.isEmpty()) {
            System.out.println("No hay libros registrados.");
            return;
        }
        System.out.println("Lista de libros registrados:");
        libros.stream()
                .sorted(Comparator.comparing(Libro::getTitulo))
                .forEach(System.out::println);
    }

    // 4. Listar todos los autores registrados
    private void listarAutoresRegistrados() {
        List<Autor> autores = autorRepository.findAll();
        if (autores.isEmpty()) {
            System.out.println("No hay autores registrados.");
            return;
        }
        System.out.println("Lista de autores registrados:");
        autores.stream()
                .sorted(Comparator.comparing(Autor::getNombre))
                .forEach(a -> {
                    System.out.println("Nombre: " + a.getNombre());
                    System.out.println("Nacimiento: " + a.getAnoNacimiento());
                    System.out.println("Fallecimiento: " + (a.getAnoFallecimiento() != null ? a.getAnoFallecimiento() : "Vive"));
                    System.out.println("Libros: " + a.getLibros().stream()
                            .map(Libro::getTitulo)
                            .collect(Collectors.joining(", ")));
                    System.out.println("------");
                });
    }

    // 5. Listar autores vivos en un año determinado
    private void listarAutoresVivosEnAno() {
        System.out.println("Ingrese el año (ej. 1900):");
        int ano;
        try {
            ano = Integer.parseInt(teclado.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Año inválido.");
            return;
        }
        List<Autor> autoresVivos = autorRepository.findAutoresVivosEnAno(ano);
        if (autoresVivos.isEmpty()) {
            System.out.println("No hay autores vivos en el año " + ano);
            return;
        }
        System.out.println("Autores vivos en " + ano + ":");
        autoresVivos.stream()
                .sorted(Comparator.comparing(Autor::getNombre))
                .forEach(a -> System.out.println(a.getNombre()));
    }

    // 6. Listar libros por idioma
    private void listarLibrosPorIdioma() {
        System.out.println("""
                Idiomas disponibles:
                - Inglés
                - Español
                - Portugués
                - Ruso
                Ingrese el idioma (en español, ej. Inglés):""");
        String idiomaInput = teclado.nextLine();
        Idioma idioma;
        try {
            idioma = Idioma.fromEspanol(idiomaInput);
        } catch (IllegalArgumentException e) {
            System.out.println("Idioma no válido.");
            return;
        }
        List<Libro> libros = libroRepository.findByIdioma(idioma);
        if (libros.isEmpty()) {
            System.out.println("No hay libros en ese idioma.");
            return;
        }
        System.out.println("Libros en " + idiomaInput + " (" + libros.size() + " encontrados):");
        libros.stream()
                .sorted(Comparator.comparing(Libro::getTitulo))
                .forEach(System.out::println);
    }
}

