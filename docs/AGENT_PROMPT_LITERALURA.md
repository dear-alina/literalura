# PROMPT PARA AGENTE DE DESARROLLO - LITERALURA

---

## 🎯 CONTEXTO GENERAL DE LA APLICACIÓN

**Proyecto:** Literalura v2.0  
**Stack:** Spring Boot + PostgreSQL + Gutendex API  
**Características:** JWT/Spring Security + Redis Caching  

**Flujo Crítico Completo:**
```
Gutendex API 
   ↓
ClienteGutendex / ConsumoAPI  [Integración Externa]
   ↓
ConvierteDatos / IConvierteDatos  [Transformación DTO]
   ↓
AutorService / LibroService  [Lógica de Negocio]
   ↓
AutorRepository / LibroRepository  [Persistencia JPA]
   ↓
PostgreSQL  [Base de Datos]
   ↓
AutorController / LibroController  [REST API + Redis Cache]
```

**Arquitectura Real:**
```
COMPONENTES CLAVE:

Controllers:
  ├── AutorController      → GET/POST/DELETE /api/autores
  └── LibroController      → GET/POST/DELETE /api/libros

Services:
  ├── AutorService         → Lógica de autores + validaciones
  ├── LibroService         → Lógica de libros + búsquedas
  ├── ConsumoAPI           → Llama Gutendex (HttpClient)
  └── ConvierteDatos       → Parsea JSON → DTO/Model

Repositories:
  ├── AutorRepository      → JPA queries autores
  └── LibroRepository      → JPA queries libros (con búsquedas avanzadas)

Data Layer:
  ├── DTOs                 → Conversión de datos externos
  ├── Models               → Entidades JPA (Autor, Libro)
  └── PostgreSQL           → Persistencia

Servicios Transversales:
  ├── config/              → Security JWT, Redis, Properties
  ├── exception/           → Manejo de errores
  └── ConvierteDatos/IConvierteDatos  → Abstracción para parseadores
```

**Patrones Observados:**
- ✅ Interfaces para servicios (IConvierteDatos)
- ✅ Separación clara: integración externa vs lógica
- ✅ Servicios especializados (uno por entidad + integración)

---

## ⚙️ CAMBIO A REALIZAR

1. Implementar la suite final de pruebas de integración utilizando **Testcontainers (PostgreSQL)** y `@ServiceConnection`, enfocándote *exclusivamente* en la capa de persistencia: `LibroRepository.java` y `AutorRepository.java`.
2. **REGLA DE PARADA OBLIGATORIA:** Una vez que hayas generado el código de los tests para la capa Repository, **DETENTE**. Has completado el ciclo de las 3 capas del backend. No modifiques ninguna otra carpeta.
3. Al final de tu respuesta, hazme obligatoriamente la siguiente pregunta para cerrar el hito:
   > *"He finalizado con la capa Repository y con el 100% de la suite de integración del backend. ¿Deseas que te entregue ahora mismo los dos bloques Raw (el archivo `.md` y la fila del `CHANGELOG_MASTER.md`) para que los guardes manualmente?"*

```
---

## 📋 OBLIGACIONES AL FINALIZAR

### 1️⃣ GENERAR ARTEFACTO .MD DETALLADO

**Ubicación:** `docs/changelog/YYYY-MM-DD_HH-MM-SS_[nombre-cambio-breve].md`

**Formato Exacto:**

```markdown
# [Título Descriptivo del Cambio]

**Fecha:** YYYY-MM-DD HH:MM:SS  
**Categoría:** [Repository|Service|Controller|Config|Utilities|Tests]  
**Estado:** [Completado|Requiere Revisión]  
**Impacto:** [Alto|Medio|Bajo]

## 📋 Resumen
[2-3 líneas: QUÉ cambió y POR QUÉ]

## 🔧 Archivos Modificados/Creados
- `ruta/al/archivo1.java` → Descripción breve del cambio
- `ruta/al/archivo2.java` → Descripción breve del cambio

## 📊 Capas Afectadas
[Lista las capas específicas del flujo que toca - marca las que aplican]
- [ ] AutorController / LibroController
- [ ] AutorService / LibroService
- [ ] ConsumoAPI / ClienteGutendex (integración)
- [ ] ConvierteDatos / IConvierteDatos (transformación)
- [ ] AutorRepository / LibroRepository
- [ ] Model (Entidades JPA)
- [ ] Config (Security, Redis, Properties)
- [ ] Exception (manejo de errores)

## 🧪 Tests Asociados
[Menciona los tests que validan el cambio - ejemplo según qué modificaste]
- ✅ `src/test/java/.../controller/AutorControllerTest.java`
- ✅ `src/test/java/.../controller/LibroControllerTest.java`
- ✅ `src/test/java/.../service/AutorServiceIntegrationTest.java`
- ✅ `src/test/java/.../service/LibroServiceIntegrationTest.java`
- ✅ `src/test/java/.../repository/AutorRepositoryIntegrationTest.java`
- ✅ `src/test/java/.../repository/LibroRepositoryIntegrationTest.java`
- ✅ `src/test/java/.../util/ConvierteDatosTest.java`

## ⚠️ Consideraciones de Arquitectura de Literalura
**Valida estas preguntas según qué modificaste:**
- ¿Se respeta la separación ConsumoAPI → ConvierteDatos → Service?
- ¿Hay breaking changes en endpoints (AutorController/LibroController)?
- ¿Impacta JWT/Security en AuthConfig o endpoints?
- ¿Se mantienen las interfaces (ej: IConvierteDatos)?
- ¿Las queries nuevas en Repository están optimizadas?
- ¿Se maneja excepciones de Gutendex sin romper la app?
- ¿Escalará con múltiples búsquedas simultáneas en Gutendex?
- ¿El Redis caché se invalida correctamente si corresponde?

## ✅ Verificación Pre-Entrega
- [ ] Código compila sin errores
- [ ] Tests unitarios verdes (mvn test)
- [ ] Tests integración verdes (TestContainers)
- [ ] Sin dependencias circulares
- [ ] Documentación actualizada
```

---

### 2️⃣ ACTUALIZAR ÍNDICE CENTRAL

**Archivo:** `docs/CHANGELOG_MASTER.md`

**Agregar fila en tabla resumen:**
```markdown
| YYYY-MM-DD | [Descripción breve] | [Categoría] | [Ver](./changelog/YYYY-MM-DD_HH-MM-SS_nombre.md) |
```

**Agregar sección en "Últimos Cambios":**
```markdown
### [YYYY-MM-DD] [Título del Cambio]
**Archivos:** archivo1.java, archivo2.java  
**Capas Afectadas:** controller, service, repository  
**Tests:** ✅ AllTests  
[Link al .md detallado](./changelog/YYYY-MM-DD_HH-MM-SS_nombre.md)
```

---

## 🔍 VALIDACIÓN DE COHERENCIA CONTEXTUAL

**Antes de finalizar, valida estos puntos específicos de Literalura:**

1. **¿Se respetan las responsabilidades de capas?**
   - ConsumoAPI/ClienteGutendex SOLO llama API externa
   - ConvierteDatos SOLO transforma JSON → DTO/Model
   - AutorService/LibroService hacen lógica (NO queries directas)
   - Repository SOLO accede a BD con JPA
   - Controller SOLO expone endpoints REST

2. **¿El flujo Gutendex → BD es coherente?**
   - Datos externos SIEMPRE pasan por ConvierteDatos
   - No hay SQL queries en Service (va a Repository)
   - No hay lógica en Controller (va a Service)
   - ¿Cómo se cachea en Redis? (si aplica)

3. **¿Hay interfaz o patrón inconsistente?**
   - Si modificas ConvierteDatos: ¿respeta IConvierteDatos?
   - ¿Los servicios inyectan dependencias correctamente?
   - ¿Las excepciones se lanzan desde el lugar correcto?

4. **¿Impacta el flujo global?**
   - ¿Afecta JWT/Security en Controllers?
   - ¿Puede fallar Gutendex sin romper la app?
   - ¿Los tests que tocas son los correctos (integración vs unitarios)?

---

## 📌 CHECKLIST FINAL

- [ ] Artefacto .md creado en `docs/changelog/`
- [ ] CHANGELOG_MASTER.md actualizado
- [ ] Descripción del cambio menciona capas afectadas
- [ ] Contexto de arquitectura global preservado
- [ ] Tests verdes (unitarios + integración)
- [ ] Sin breaking changes documentados
- [ ] Próximos pasos mencionados (si aplica)

**Mensaje al finalizar:**
```
✅ Cambio completado
📄 Documentación: docs/changelog/YYYY-MM-DD_HH-MM-SS_[nombre].md
📊 Índice actualizado: docs/CHANGELOG_MASTER.md
🏗️ Flujo validado: Gutendex → ConsumoAPI → ConvierteDatos → Service → Repository → Controller
🧪 Tests ejecutados: [menciona cuáles pasaron]
```
