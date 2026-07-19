# 📚 Literalura - Catálogo de Libros

Literalura es el **backend REST** de una plataforma web de gestión bibliográfica: expone doce endpoints (Spring Boot 4, Java 21), persiste libros y autores en PostgreSQL y sirve al frontend [`stitch_literalura`](https://github.com/dear-alina/literalura-frontend). El catálogo proviene de la API pública [Gutendex](https://gutendex.com/) (Project Gutenberg).

> **Integración con Gutendex desde el navegador (importante):** la consulta a Gutendex la realiza el **cliente (navegador)**, no el backend. Cloudflare bloquea con HTTP 403 ("managed challenge") las peticiones desde la IP de datacenter de Render; el navegador, con IP residencial y CORS permitido por Gutendex (`access-control-allow-origin: *`), sí las resuelve. El endpoint `POST /api/libros/buscar-y-registrar` recibe el libro **ya obtenido** por el navegador y solo lo deduplica por `gutendexId` y lo persiste.

---

## ✨ Funcionalidades

1. **Registrar un libro obtenido de Gutendex**  
   - El navegador busca en Gutendex y envía el primer resultado; el backend deduplica por `gutendexId` y lo guarda junto con su autor (si el autor no existe previamente, se crea automáticamente).

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

- **Java 21**
- **Spring Boot 4.x**
- **Spring Data JPA**
- **PostgreSQL** (base de datos relacional)
- **Jackson** (para procesamiento JSON)
- **Maven** (gestión de dependencias)

### Stack de pruebas

- **JUnit 5 + Mockito + AssertJ** (tests unitarios, patrón AAA)
- **MockMvc / @WebMvcTest** (tests de contrato HTTP)
- **Testcontainers 2 (PostgreSQL)** (tests de integración con base de datos real)
- **REST Assured 6** (tests E2E sobre la aplicación completa)

---

## 📋 Requisitos previos

- Tener instalado **Java 21+** y **Maven** (o usar el wrapper `./mvnw`).
- Tener una instancia de **PostgreSQL** en ejecución.
- **Docker** en ejecución para los tests de integración y E2E (Testcontainers).
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
```
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

## 📖 Nuevo uso de la aplicación
- Al ejecutar el programa, ya no se verá el menú principal pues se ha estructurado de forma que se pueda conectar a un fronted :D

