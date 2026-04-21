# Plan API: Endpoints requeridos para la migracion del Desktop

> **Agent:** Agent 1 (API + Data)
> **Dominio:** `/api/`
> **Origen:** Requisitos del plan de migracion del desktop (`/desktop/plan.md`)
> **Prioridad:** Bloqueante para las Fases 3-5 del desktop (excepto API-4)
> **Fecha:** 2026-04-05

---

## Contexto

El desktop SGE esta migrando de conexion JDBC directa a consumo de la API REST central.
Durante el analisis se identificaron 4 funcionalidades que el desktop necesita y que la API
aun no expone. Sin estos endpoints, los DAOs del desktop no podran funcionar.

**Endpoints existentes que SI cubren la migracion:**
- Auth: POST /api/auth/login, POST /api/auth/refresh
- Pacientes: GET/POST /api/pacientes, GET/PUT/DELETE /api/pacientes/{dni}
- Sanitarios: GET/POST /api/sanitarios, GET/PUT/DELETE /api/sanitarios/{dni}
- Citas: GET /api/citas?fecha=, GET /api/citas/sanitario/{dniSan}, POST, DELETE
- Catalogo: GET /api/catalogo/discapacidades, tratamientos, niveles-progresion
- Asignaciones: GET/POST /api/pacientes/{dniPac}/discapacidades, PUT nivel, GET/PUT tratamientos visibilidad

**Endpoints que FALTAN (este plan):**

| Tarea | Endpoint | Bloqueante |
|-------|----------|------------|
| API-1 | POST/GET /api/pacientes/{dni}/foto | SI |
| API-2 | GET /api/pacientes/buscar?texto=X | SI |
| API-2b | GET /api/sanitarios/buscar?texto=X | SI |
| API-3 | Enriquecer CitaResponse con nombres | SI |
| API-4 | FK id_nivel en tratamiento + migracion | NO |

---

## API-1: Endpoint de foto de paciente

### Descripcion

La tabla `paciente` tiene una columna `foto BYTEA` usada por el desktop para almacenar
la foto del paciente. La API necesita exponer esta funcionalidad via endpoints REST.

### Endpoints a crear

```
POST   /api/pacientes/{dni}/foto    Content-Type: multipart/form-data
GET    /api/pacientes/{dni}/foto    Accept: image/*
DELETE /api/pacientes/{dni}/foto    (opcional, baja prioridad)
```

### Especificacion

**POST /api/pacientes/{dni}/foto**
- Recibe: `multipart/form-data` con un campo `foto` de tipo archivo (image/png, image/jpeg).
- Valida que el paciente exista y este activo.
- Persiste los bytes en la columna `foto` (BYTEA) de la tabla `paciente`.
- Responde: `200 OK` sin body, o `404` si el paciente no existe.
- Auth: `@PreAuthorize("isAuthenticated()")` (cualquier rol puede subir foto).

**GET /api/pacientes/{dni}/foto**
- Devuelve los bytes de la foto con `Content-Type: image/png` (o detectar formato).
- Si no hay foto, responde `204 No Content`.
- Si el paciente no existe, responde `404`.
- Auth: `@PreAuthorize("isAuthenticated()")`.

### Archivos a crear/modificar

| Archivo | Cambio |
|---------|--------|
| `PacienteController.java` | Agregar metodos `subirFoto()` y `obtenerFoto()` |
| `PacienteService.java` (app layer) | Agregar metodos `guardarFoto(dni, bytes)` y `obtenerFoto(dni)` |
| `PacienteRepository.java` | Query custom `@Query("UPDATE Paciente SET foto = ?2 WHERE dniPac = ?1")` o usar el entity directamente |
| `Paciente.java` (entity) | Verificar que el campo `foto` (byte[] con @Lob) existe en la entidad JPA |

### Codigo orientativo del controller

```java
@PostMapping("/{dni}/foto")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<Void> subirFoto(
        @PathVariable String dni,
        @RequestParam("foto") MultipartFile foto) throws IOException {
    pacienteService.guardarFoto(dni, foto.getBytes());
    return ResponseEntity.ok().build();
}

@GetMapping("/{dni}/foto")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<byte[]> obtenerFoto(@PathVariable String dni) {
    byte[] foto = pacienteService.obtenerFoto(dni);
    if (foto == null || foto.length == 0) {
        return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .body(foto);
}
```

---

## API-2: Endpoint de busqueda full-text de pacientes y sanitarios

### Descripcion

El desktop tiene busqueda por texto libre en sus tablas de pacientes y sanitarios
(LIKE en DNI, nombre, apellidos, email, NSS). La API necesita endpoints equivalentes.

### Endpoints a crear

```
GET /api/pacientes/buscar?texto=X&page=0&size=50&sort=nombrePac,asc
GET /api/sanitarios/buscar?texto=X&page=0&size=50&sort=nombreSan,asc
```

### Especificacion — Pacientes

**GET /api/pacientes/buscar**
- Query params:
  - `texto` (String, obligatorio): termino de busqueda.
  - `page`, `size`, `sort` (Pageable de Spring Data).
- Busca con ILIKE (case-insensitive) en: `dni_pac`, `nombre_pac`, `apellido1_pac`,
  `apellido2_pac`, `email_pac`, `num_ss`.
- Solo pacientes activos (`activo = true`).
- Responde: `PageResponse<PacienteResponse>`.
- Auth: `@PreAuthorize("isAuthenticated()")`.

### Especificacion — Sanitarios

**GET /api/sanitarios/buscar**
- Misma logica pero sobre campos de sanitario: `dni_san`, `nombre_san`, `apellido1_san`,
  `apellido2_san`, `email_san`.
- Solo sanitarios activos.
- Responde: `PageResponse<SanitarioResponse>`.

### Archivos a crear/modificar

| Archivo | Cambio |
|---------|--------|
| `PacienteController.java` | Agregar metodo `buscar(@RequestParam String texto, Pageable pageable)` |
| `PacienteService.java` (app layer) | Agregar metodo `buscar(String texto, Pageable pageable)` |
| `PacienteRepository.java` | Agregar query JPQL con LOWER + LIKE en multiples campos |
| `SanitarioController.java` | Agregar metodo `buscar(...)` analogo |
| `SanitarioService.java` (app layer) | Agregar metodo `buscar(...)` |
| `SanitarioRepository.java` | Agregar query JPQL |

### Query JPQL orientativa (PacienteRepository)

```java
@Query("""
    SELECT p FROM Paciente p
    WHERE p.activo = true
    AND (LOWER(p.dniPac) LIKE LOWER(CONCAT('%', :texto, '%'))
      OR LOWER(p.nombrePac) LIKE LOWER(CONCAT('%', :texto, '%'))
      OR LOWER(p.apellido1Pac) LIKE LOWER(CONCAT('%', :texto, '%'))
      OR LOWER(p.apellido2Pac) LIKE LOWER(CONCAT('%', :texto, '%'))
      OR LOWER(p.emailPac) LIKE LOWER(CONCAT('%', :texto, '%'))
      OR LOWER(p.numSs) LIKE LOWER(CONCAT('%', :texto, '%')))
    """)
Page<Paciente> buscarPorTexto(@Param("texto") String texto, Pageable pageable);
```

### Codigo orientativo del controller

```java
@GetMapping("/buscar")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<PageResponse<PacienteResponse>> buscar(
        @RequestParam String texto,
        Pageable pageable) {
    return ResponseEntity.ok(pacienteService.buscar(texto, pageable));
}
```

---

## API-3: Enriquecer CitaResponse con nombres

### Descripcion

`CitaResponse` actualmente solo devuelve los DNIs del paciente y sanitario.
El desktop necesita los nombres completos para mostrarlos en las tablas y agenda.

### Cambio requerido

Modificar el record `CitaResponse` para incluir nombres:

```java
// ANTES:
public record CitaResponse(
    String dniPac,
    String dniSan,
    LocalDate fechaCita,
    LocalTime horaCita
) {}

// DESPUES:
public record CitaResponse(
    String dniPac,
    String dniSan,
    LocalDate fechaCita,
    LocalTime horaCita,
    String nombrePaciente,
    String nombreSanitario
) {}
```

### Archivos a modificar

| Archivo | Cambio |
|---------|--------|
| `CitaResponse.java` | Agregar campos `nombrePaciente` y `nombreSanitario` |
| `CitaMapper.java` (o donde se mapee) | Resolver nombres desde las entidades Paciente y Sanitario asociadas a la Cita |

### Logica de resolucion de nombres

La entidad `Cita` tiene relaciones `@ManyToOne` con `Paciente` y `Sanitario`.
El mapper debe construir los nombres completos:

```java
// En el mapper (MapStruct o manual):
String nombrePaciente = cita.getPaciente().getNombrePac() + " "
    + cita.getPaciente().getApellido1Pac() + " "
    + (cita.getPaciente().getApellido2Pac() != null ? cita.getPaciente().getApellido2Pac() : "");

String nombreSanitario = cita.getSanitario().getNombreSan() + " "
    + cita.getSanitario().getApellido1San() + " "
    + (cita.getSanitario().getApellido2San() != null ? cita.getSanitario().getApellido2San() : "");
```

### Impacto en otros consumidores

El mobile BFF (`/mobile/backend`) tambien consume estos endpoints.
Agregar campos al record es **retrocompatible** — los clientes existentes
simplemente ignoran los campos nuevos si no los necesitan.

---

## API-4: FK id_nivel en tratamiento (no bloqueante)

### Descripcion

Agregar columna `id_nivel` a la tabla `tratamiento` para vincular cada tratamiento
a un nivel de progresion clinica (agudo, subagudo, fortalecimiento, funcional).
Esto permite filtrar tratamientos por nivel de progresion del paciente.

### Prioridad

**BAJA** — No bloquea la migracion del desktop. Se puede implementar despues.

### Migracion Flyway

Crear `V4__tratamiento_nivel_progresion.sql`:

```sql
-- Vincular tratamientos a niveles de progresion clinica
ALTER TABLE tratamiento
    ADD COLUMN id_nivel INTEGER REFERENCES nivel_progresion(id_nivel);

-- Indice para filtrado por nivel
CREATE INDEX idx_tratamiento_nivel ON tratamiento(id_nivel);
```

### Archivos a modificar

| Archivo | Cambio |
|---------|--------|
| `V4__tratamiento_nivel_progresion.sql` | Nueva migracion Flyway |
| `Tratamiento.java` (entity) | Agregar campo `@ManyToOne NivelProgresion nivel` |
| `TratamientoResponse.java` | Agregar campos `Integer idNivel` y `String nombreNivel` |
| `CatalogoService.java` | Actualizar mapper para incluir nivel en la respuesta |

---

## Orden de ejecucion recomendado

```
API-3 (CitaResponse — cambio mas sencillo, solo DTO + mapper)
  |
  v
API-2 (Busqueda — query JPQL + controller + service)
  |
  v
API-1 (Foto — multipart upload, mas complejo)
  |
  v
API-4 (id_nivel — no bloqueante, puede hacerse al final o en otra iteracion)
```

---

## Verificacion

Tras implementar API-1, API-2 y API-3, ejecutar:

```bash
./mvnw test
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Y verificar con curl:

```bash
# API-1: Foto
curl -X POST http://localhost:8080/api/pacientes/12345678A/foto \
  -H "Authorization: Bearer {token}" \
  -F "foto=@foto.png"

curl http://localhost:8080/api/pacientes/12345678A/foto \
  -H "Authorization: Bearer {token}" --output foto.png

# API-2: Busqueda
curl "http://localhost:8080/api/pacientes/buscar?texto=garcia&page=0&size=10" \
  -H "Authorization: Bearer {token}"

curl "http://localhost:8080/api/sanitarios/buscar?texto=lopez&page=0&size=10" \
  -H "Authorization: Bearer {token}"

# API-3: Citas con nombres
curl "http://localhost:8080/api/citas?fecha=2026-04-05&page=0&size=10" \
  -H "Authorization: Bearer {token}"
# Verificar que la respuesta incluye nombrePaciente y nombreSanitario
```
