# 📚 Literalura - Catálogo de Libros

Literalura es una aplicación de consola desarrollada con **Spring Boot** que permite consultar libros desde la API pública [Gutendex](https://gutendex.com/), almacenarlos en una base de datos PostgreSQL y gestionar información sobre autores. 
El programa ofrece un menú interactivo con varias opciones para buscar, listar y filtrar libros y autores.

---

## ✨ Funcionalidades

1. **Buscar libro por título en la API y registrarlo**  
   - Consulta la API de Gutendex, toma el primer resultado que coincida y lo guarda en la base de datos junto con su autor (si el autor no existe previamente, se crea automáticamente).

2. **Buscar libro en la base de datos por título**  
   - Permite buscar un libro ya registrado utilizando su título exacto (sin distinguir mayúsculas).

3. **Listar todos los libros registrados**  
   - Muestra todos los libros almacenados, ordenados alfabéticamente por título.

4. **Listar todos los autores registrados**  
   - Muestra todos los autores, incluyendo sus años de nacimiento y fallecimiento (si corresponde) y los títulos de sus libros.

5. **Listar autores vivos en un año determinado**  
   - El usuario ingresa un año y la aplicación muestra los autores que estaban vivos en ese año (nacidos antes o en ese año y fallecidos después o sin fecha de fallecimiento).

6. **Listar libros por idioma**  
   - El usuario elige un idioma de una lista predefinida (español, inglés, portugués, ruso) y la aplicación muestra todos los libros en ese idioma, junto con la cantidad encontrada.

---

## 🛠️ Tecnologías utilizadas

- **Java 17** (o superior)
- **Spring Boot 3.x**
- **Spring Data JPA**
- **PostgreSQL** (base de datos relacional)
- **Jackson** (para procesamiento JSON)
- **Maven** (gestión de dependencias)

---

## 📋 Requisitos previos

- Tener instalado **Java 17+** y **Maven**.
- Tener una instancia de **PostgreSQL** en ejecución.
- Conocimientos básicos para modificar el archivo `application.properties` con tus credenciales de base de datos.

---

## ⚙️ Configuración de la base de datos

1. Crea una base de datos en PostgreSQL (por ejemplo, `literalura`).
2. En el archivo `src/main/resources/application.properties`, configura tus datos de conexión:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/literalura
spring.datasource.username=tu_usuario
spring.datasource.password=tu_contraseña
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```
---

## 📖 Uso de la aplicación
- Al ejecutar el programa, verás el menú principal:
---------------------------------------------------
Elige alguna opción usando los siguientes números:
---------------------------------------------------
1 - Buscar libro por título y registrarlo
2 - Buscar libro en la BD por título
3 - Listar todos los libros registrados
4 - Listar todos los autores registrados
5 - Listar autores vivos en un año determinado
6 - Listar libros por idioma
0 - Salir

- Elige una opción e ingresa los datos que te pide, el programa hará su trabajo y te devolverá una impresión con lo solicitado :D
ASÍ DE FÁCIL!!

---
