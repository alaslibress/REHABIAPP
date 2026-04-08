# Plan — Transacciones atómicas en `PacienteService` y `SanitarioService`

> **Thinker:** Agent 3 (Opus)
> **Doer objetivo:** Sonnet
> **Fecha:** 2026-04-08
> **Items del checklist `desktop/CLAUDE.md §7 — CRUD operations`:**
> - [ ] Implement atomic transactions (commit/rollback) in PacienteService for compound operations (patient + phones + address + photo).
> - [ ] Implement atomic transactions in SanitarioService for compound operations (practitioner + role + phones).
> - [ ] Integrate photo upload within the same transaction as patient INSERT.

---

## 1. Contexto y diagnóstico

El checklist habla de "atomic transactions in `PacienteService`/`SanitarioService`". Los nombres son los mismos pero el contexto cambió radicalmente tras la migración JDBC → REST API:

- En el mundo JDBC, atomicidad significaba `BEGIN/COMMIT/ROLLBACK` en una `Connection` local del desktop.
- En el mundo REST, **el desktop NO tiene transacciones**. La atomicidad real ocurre en la API (Spring `@Transactional`).
- Por tanto, "atomic transaction in PacienteService" hoy se traduce a: "el desktop debe hacer **una sola llamada HTTP** que dispare **una sola transacción** en la API que cubra todas las entidades del agregado".

### 1.1 Estado real auditado (recorrido por todo el stack)

| Operación compuesta | Estado actual | ¿Atómico hoy? |
|---------------------|---------------|---------------|
| `paciente + telefonos` | `POST /api/pacientes` ya recibe `telefonos: List<String>` y los persiste vía cascade `OneToMany` con `@Transactional`. | ✅ **SÍ** (server-side cascade) |
| `paciente + direccion` | El controlador desktop **valida** `txtCalle/txtNumero/txtPiso/txtCodigoPostal/txtLocalidad/txtProvincia` pero **NUNCA** los copia al objeto `Paciente`. `PacienteRequest` (API y desktop) no tiene campo `direccion`. `Paciente.java` (entidad) sí tiene `@ManyToOne Direccion` pero `PacienteService.crear()` no la setea. **Los campos se pierden silenciosamente.** | ❌ **NO** (no fluyen) |
| `paciente + foto` | `desktop/PacienteService.insertar(p, t1, t2, foto)` hace **dos llamadas HTTP** secuenciales: `POST /api/pacientes` y luego `POST /api/pacientes/{dni}/foto`. Si la segunda falla, el paciente queda creado **sin foto** (estado roto). El comentario JavaDoc en `controladorAgregarPaciente.java:434` afirma "operacion atomica" — **es mentira**. | ❌ **NO** (split state real) |
| `sanitario + rol + telefonos` | `POST /api/sanitarios` acepta `cargo` + `telefonos`, y `SanitarioService.crear()` (API) construye `Sanitario` + `SanitarioRol` + `TelefonoSanitario` en cascada bajo `@Transactional`. Una sola llamada HTTP. | ✅ **SÍ** (ya correcto, falta verificar y documentar) |

### 1.2 Qué hay que hacer realmente

1. **Wire address through the API.** Añadir campos `direccion` a `PacienteRequest`/`PacienteResponse`, persistir en la misma `@Transactional` que crea el paciente con un patrón "find-or-create" sobre `localidad`/`cp`/`direccion`. Wire en el desktop (Paciente.java + controladores) para que los campos no se pierdan.
2. **Atomic photo + paciente.** Crear un endpoint `POST /api/pacientes` (y `PUT /api/pacientes/{dni}`) que acepte `multipart/form-data` con dos partes: `paciente` (JSON con todo el agregado) y `foto` (archivo opcional). Toda la operación dentro de una sola `@Transactional`.
3. **Refactor desktop.** `PacienteService.insertar(p, t1, t2, foto)` y `actualizar(p, dni, t1, t2, foto)` deben pasar de "dos calls" a "un call multipart". Eliminar `PacienteDAO.insertarFoto()` del flujo de creación/edición (mantenerlo solo para reemplazo aislado de la foto desde otra pantalla, si lo necesita alguien).
4. **Verificar Sanitario.** Confirmar que la cascada funciona (test integración), eliminar el comentario "TODO: Mover operaciones de foto al servicio" del controlador, dejar JavaDoc claro de que la operación es atómica server-side.
5. **Tests.** JUnit 5 + Mockito sobre los wrappers desktop verificando que `insertar(...)` hace **una sola llamada** al `ApiClient` y que un fallo HTTP propaga la excepción sin dejar estado parcial.
6. **Marcar [x]** los 3 ítems del checklist.

### 1.3 Limitaciones contextuales

- **`api.cp` y `api.localidad` están casi vacías** (2 filas seed). Cuando un sanitario inserta una dirección con un CP nuevo, la API debe crear el `CodigoPostal` + `Localidad` automáticamente en la misma transacción. Si no, todo flujo de alta falla con `RecursoNoEncontradoException`.
- **No existen `DireccionRepository`, `CodigoPostalRepository`, `LocalidadRepository`** en la API. Hay que crearlos.
- **`ApiClient.java` (desktop) no soporta multipart**. Hay que añadirle un método `postMultipart(path, jsonBody, fileBytes, fileName, contentType, responseClass)`. `PacienteDAO.insertarFoto()` ya construye multipart manualmente — reutilizar esa lógica.
- **Flyway no está activo en dev** (igual que en el plan anterior). Los cambios de schema se aplican con SQL directo; la migración V9 se crea para entornos frescos pero no se aplica a dev.

---

## 2. Decisiones de diseño

| Decisión | Elección | Justificación |
|----------|----------|---------------|
| ¿Multipart vs JSON+segundo POST con compensación? | **Multipart en un único endpoint.** | El patrón saga (POST + DELETE en caso de fallo de foto) es frágil — el DELETE puede fallar también, dejando huérfano el paciente. Una sola petición HTTP con `@Transactional` es la única solución verdaderamente atómica. |
| ¿Mantener `POST /api/pacientes/{dni}/foto`? | **Sí**, como endpoint independiente para "reemplazar foto en paciente existente" desde otras pantallas. Solo se elimina su uso desde el flujo de creación/edición. | El endpoint sigue siendo válido para una pantalla "Editar foto" aislada. No romper API pública. |
| ¿`paciente + direccion` en JSON anidado o flat? | **Anidado**: `direccion: { calle, numero, piso, cp, nombreLocalidad, provincia }` dentro del JSON `paciente`. | Más limpio, refleja el modelo, fácil de hacer opcional (`null` = paciente sin dirección registrada). |
| Manejo de `cp` / `localidad` no existentes | **Find-or-create idempotente.** Si el CP existe, se reusa. Si no, se inserta `localidad` (find-or-create por nombre+provincia) + `cp`. Todo dentro del mismo `@Transactional`. | Evita exigir un catálogo precargado completo (España tiene >55k CPs). El sanitario teclea y la API resuelve. |
| ¿`PUT /api/pacientes/{dni}` también multipart? | **Sí**, para que la edición tenga la misma semántica atómica. Si el sanitario sube una foto nueva al editar, va en la misma transacción que el resto del UPDATE. | Consistencia con `POST`. Sin esto, la edición sigue rota. |
| Tamaño máximo de foto | **5 MB** (configurable en `application.yml` con `spring.servlet.multipart.max-file-size=5MB`). Si la foto es mayor, la API responde `413 Payload Too Large` antes de tocar BD. | Pacientes con fotos >5MB son extremadamente raros y rompen rendimiento. |
| Tests | **JUnit 5 + Mockito** sobre `desktop/PacienteService` y `desktop/SanitarioService` con `ApiClient` mockeado. **No** integración real con la API (no hay infra de IT en este repo). | Cubre el contrato del wrapper. Los tests de integración server-side son competencia del Agent 1. |

---

## 3. Plan de ejecución por fases

Las fases se ejecutan **secuencialmente**. Cada fase deja el sistema en un estado compilable. Si una fase falla, no avanzar a la siguiente — diagnosticar y corregir.

---

### FASE 0 — Preparación (lectura obligatoria + arranque del stack)

**Objetivo:** garantizar que el Doer tiene el contexto correcto y el stack arranca antes de tocar nada.

**Acciones:**

1. Leer (en orden):
   - `/home/alaslibres/DAM/RehabiAPP/CLAUDE.md` (orquestador global)
   - `/home/alaslibres/DAM/RehabiAPP/desktop/CLAUDE.md` (este dominio)
   - `/home/alaslibres/DAM/RehabiAPP/desktop/.claude/skills/javafx-java24/SKILL.md` (skill obligatorio para Java 24/JavaFX)
2. Arrancar el stack siguiendo el RUNBOOK del `desktop/CLAUDE.md` (PostgreSQL → API → Desktop). Verificar:
   ```bash
   curl http://localhost:8080/actuator/health
   docker exec rehabiapp-db psql -U admin -d rehabiapp -c "SELECT COUNT(*) FROM cp; SELECT COUNT(*) FROM localidad; SELECT COUNT(*) FROM direccion;"
   ```
3. Login con `ADMIN0000 / admin` desde la UI para confirmar que el flujo base funciona.

**Criterio de éxito:** la API responde 200, el desktop puede listar pacientes existentes.

---

### FASE 1 — API: repositorios para `Direccion`, `CodigoPostal`, `Localidad`

**Objetivo:** habilitar consultas de catálogo de direcciones desde la capa de servicio.

**Archivos a crear** (todos en `/home/alaslibres/DAM/RehabiAPP/api/src/main/java/com/rehabiapp/api/domain/repository/`):

1. **`DireccionRepository.java`**
   ```java
   package com.rehabiapp.api.domain.repository;

   import com.rehabiapp.api.domain.entity.Direccion;
   import org.springframework.data.jpa.repository.JpaRepository;
   import org.springframework.stereotype.Repository;

   import java.util.Optional;

   @Repository
   public interface DireccionRepository extends JpaRepository<Direccion, Long> {

       /**
        * Busca una direccion existente que coincida exactamente con calle/numero/piso/cp.
        * Util para reutilizar registros en lugar de duplicar.
        */
       Optional<Direccion> findByCalleAndNumeroAndPisoAndCodigoPostalCp(
               String calle, String numero, String piso, String cp);
   }
   ```

2. **`CodigoPostalRepository.java`**
   ```java
   package com.rehabiapp.api.domain.repository;

   import com.rehabiapp.api.domain.entity.CodigoPostal;
   import org.springframework.data.jpa.repository.JpaRepository;
   import org.springframework.stereotype.Repository;

   @Repository
   public interface CodigoPostalRepository extends JpaRepository<CodigoPostal, String> {
   }
   ```

3. **`LocalidadRepository.java`**
   ```java
   package com.rehabiapp.api.domain.repository;

   import com.rehabiapp.api.domain.entity.Localidad;
   import org.springframework.data.jpa.repository.JpaRepository;
   import org.springframework.stereotype.Repository;

   import java.util.Optional;

   @Repository
   public interface LocalidadRepository extends JpaRepository<Localidad, String> {

       /**
        * Busca una localidad por su nombre exacto.
        * Util para el patron find-or-create al insertar direcciones.
        */
       Optional<Localidad> findByNombreLocalidad(String nombreLocalidad);
   }
   ```

**Verificación:** `./mvnw clean compile` debe terminar sin errores.

---

### FASE 2 — API: extender DTOs (`PacienteRequest`, `PacienteResponse`) con `DireccionDto`

**Objetivo:** transportar dirección por el contrato JSON.

**Archivos a crear / modificar** en `/home/alaslibres/DAM/RehabiAPP/api/src/main/java/com/rehabiapp/api/application/dto/`:

1. **Crear `DireccionDto.java`** (record nuevo):
   ```java
   package com.rehabiapp.api.application.dto;

   import jakarta.validation.constraints.NotBlank;
   import jakarta.validation.constraints.Pattern;
   import jakarta.validation.constraints.Size;

   /**
    * DTO de direccion postal usado tanto en request como en response del paciente.
    * Todos los campos obligatorios excepto piso. El cp se valida con regex de 5 digitos.
    */
   public record DireccionDto(
           @NotBlank @Size(max = 200) String calle,
           @Size(max = 20) String numero,
           @Size(max = 20) String piso,
           @NotBlank @Pattern(regexp = "^[0-9]{5}$") String cp,
           @NotBlank @Size(max = 100) String nombreLocalidad,
           @Size(max = 100) String provincia
   ) {}
   ```

2. **Editar `PacienteRequest.java`** — añadir campo `DireccionDto direccion` al final del record (antes de `telefonos`). Importar `jakarta.validation.Valid` y anotar el campo con `@Valid` para que la validación recursiva funcione:
   ```java
   public record PacienteRequest(
           @NotBlank @Size(max = 20) String dniPac,
           @NotBlank String dniSan,
           // ... resto de campos ...
           Boolean consentimientoRgpd,
           @Valid DireccionDto direccion,   // ← NUEVO (opcional, puede ser null)
           List<String> telefonos
   ) {}
   ```

3. **Editar `PacienteResponse.java`** — añadir `DireccionDto direccion` al final del record:
   ```java
   public record PacienteResponse(
           // ... campos existentes ...
           List<String> telefonos,
           DireccionDto direccion   // ← NUEVO
   ) {}
   ```

**Verificación:** `./mvnw clean compile`. Aparecerán errores en `PacienteService` y `PacienteMapper` que se resolverán en la Fase 3.

---

### FASE 3 — API: `PacienteService` con find-or-create de `Direccion` y `PacienteMapper` actualizado

**Objetivo:** persistir `Direccion` (creando `Localidad`/`CodigoPostal` si no existen) en la misma transacción que `Paciente`. El método sigue siendo `@Transactional` (heredado de la clase) — toda la operación es un único rollback unit.

**Archivo:** `/home/alaslibres/DAM/RehabiAPP/api/src/main/java/com/rehabiapp/api/application/service/PacienteService.java`

1. **Inyectar los nuevos repos:** añadir tres campos finales y modificar el constructor:
   ```java
   private final DireccionRepository direccionRepository;
   private final CodigoPostalRepository codigoPostalRepository;
   private final LocalidadRepository localidadRepository;

   public PacienteService(
           PacienteRepository pacienteRepository,
           SanitarioRepository sanitarioRepository,
           PacienteMapper pacienteMapper,
           AuditService auditService,
           DireccionRepository direccionRepository,
           CodigoPostalRepository codigoPostalRepository,
           LocalidadRepository localidadRepository
   ) { ... }
   ```

2. **Crear método privado `resolverDireccion(DireccionDto dto)`** que devuelve la entidad `Direccion` lista para asociar:
   ```java
   /**
    * Find-or-create idempotente de Direccion + CodigoPostal + Localidad.
    * Ejecuta dentro de la misma @Transactional del metodo llamante.
    *
    * @param dto Datos de direccion del request, o null.
    * @return Entidad Direccion gestionada por JPA, o null si dto es null.
    */
   private Direccion resolverDireccion(DireccionDto dto) {
       if (dto == null) return null;

       // 1. Find-or-create Localidad por nombre
       Localidad localidad = localidadRepository.findByNombreLocalidad(dto.nombreLocalidad())
               .orElseGet(() -> {
                   Localidad nueva = new Localidad();
                   nueva.setNombreLocalidad(dto.nombreLocalidad());
                   nueva.setProvincia(dto.provincia());
                   return localidadRepository.save(nueva);
               });

       // 2. Find-or-create CodigoPostal por cp
       CodigoPostal cp = codigoPostalRepository.findById(dto.cp())
               .orElseGet(() -> {
                   CodigoPostal nuevo = new CodigoPostal();
                   nuevo.setCp(dto.cp());
                   nuevo.setLocalidad(localidad);
                   return codigoPostalRepository.save(nuevo);
               });

       // 3. Find-or-create Direccion por (calle, numero, piso, cp) — reutiliza si ya existe
       return direccionRepository
               .findByCalleAndNumeroAndPisoAndCodigoPostalCp(
                       dto.calle(), dto.numero(), dto.piso(), dto.cp())
               .orElseGet(() -> {
                   Direccion d = new Direccion();
                   d.setCalle(dto.calle());
                   d.setNumero(dto.numero());
                   d.setPiso(dto.piso());
                   d.setCodigoPostal(cp);
                   return direccionRepository.save(d);
               });
   }
   ```

3. **En `crear(PacienteRequest)`**, después de `paciente.setActivo(true);` y antes del bucle de teléfonos, añadir:
   ```java
   paciente.setDireccion(resolverDireccion(request.direccion()));
   ```

4. **En `actualizar(String dni, PacienteRequest)`**, antes de `auditService.registrar(...)`, añadir:
   ```java
   if (request.direccion() != null) {
       paciente.setDireccion(resolverDireccion(request.direccion()));
   }
   // Si request.direccion() es null, mantener la direccion existente intacta.
   ```

5. **Imports nuevos** al inicio del archivo:
   ```java
   import com.rehabiapp.api.application.dto.DireccionDto;
   import com.rehabiapp.api.domain.entity.CodigoPostal;
   import com.rehabiapp.api.domain.entity.Direccion;
   import com.rehabiapp.api.domain.entity.Localidad;
   import com.rehabiapp.api.domain.repository.CodigoPostalRepository;
   import com.rehabiapp.api.domain.repository.DireccionRepository;
   import com.rehabiapp.api.domain.repository.LocalidadRepository;
   ```

**Archivo:** `/home/alaslibres/DAM/RehabiAPP/api/src/main/java/com/rehabiapp/api/application/mapper/PacienteMapper.java`

6. **Mapear la dirección al `PacienteResponse`.** Localizar el método `toResponse(Paciente)` y añadir un mapeo manual o `@Mapping` MapStruct dependiendo de cómo esté estructurado el mapper actual. Patrón sugerido (revisar primero el archivo y adaptar):
   ```java
   @Mapping(target = "direccion", expression =
       "java(paciente.getDireccion() == null ? null : new DireccionDto(" +
       "paciente.getDireccion().getCalle(), " +
       "paciente.getDireccion().getNumero(), " +
       "paciente.getDireccion().getPiso(), " +
       "paciente.getDireccion().getCodigoPostal().getCp(), " +
       "paciente.getDireccion().getCodigoPostal().getLocalidad().getNombreLocalidad(), " +
       "paciente.getDireccion().getCodigoPostal().getLocalidad().getProvincia()))")
   PacienteResponse toResponse(Paciente paciente);
   ```
   **Nota:** la entidad `Direccion` y su `CodigoPostal`/`Localidad` están en `LAZY`. Para que el mapper no lance `LazyInitializationException`, asegurar que el método del service que llama al mapper esté dentro de una transacción abierta — ya lo está porque `PacienteService` es `@Transactional` a nivel clase.

**Verificación:**
```bash
cd /home/alaslibres/DAM/RehabiAPP/api
./mvnw clean compile
```
Debe compilar sin errores.

---

### FASE 4 — API: endpoint multipart `POST /api/pacientes` y `PUT /api/pacientes/{dni}`

**Objetivo:** una sola petición HTTP que acepta el JSON del paciente + los bytes de la foto, todo dentro de la misma `@Transactional`.

**Archivo:** `/home/alaslibres/DAM/RehabiAPP/api/src/main/java/com/rehabiapp/api/presentation/controller/PacienteController.java`

1. **Modificar el endpoint `crear`** — cambiar la firma para aceptar multipart con dos partes (`paciente` JSON y `foto` archivo opcional):
   ```java
   /**
    * Crea un nuevo paciente y, opcionalmente, su fotografia, en una unica transaccion atomica.
    *
    * <p>Recibe multipart/form-data con dos partes:</p>
    * <ul>
    *   <li><b>paciente</b>: JSON serializado de PacienteRequest (Content-Type application/json).</li>
    *   <li><b>foto</b>: archivo de imagen opcional (image/png o image/jpeg).</li>
    * </ul>
    *
    * <p>POST /api/pacientes (multipart/form-data)</p>
    *
    * @param request DTO validado con los datos del nuevo paciente (incluyendo telefonos y direccion).
    * @param foto    Archivo de imagen opcional.
    * @return 201 Created con los datos del paciente creado.
    */
   @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
   @PreAuthorize("isAuthenticated()")
   public ResponseEntity<PacienteResponse> crear(
           @Valid @RequestPart("paciente") PacienteRequest request,
           @RequestPart(value = "foto", required = false) MultipartFile foto) throws IOException {
       byte[] fotoBytes = (foto != null && !foto.isEmpty()) ? foto.getBytes() : null;
       PacienteResponse response = pacienteService.crearConFoto(request, fotoBytes);
       return ResponseEntity.status(HttpStatus.CREATED).body(response);
   }
   ```

2. **Modificar el endpoint `actualizar`** análogamente:
   ```java
   @PutMapping(value = "/{dni}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
   @PreAuthorize("isAuthenticated()")
   public ResponseEntity<PacienteResponse> actualizar(
           @PathVariable String dni,
           @Valid @RequestPart("paciente") PacienteRequest request,
           @RequestPart(value = "foto", required = false) MultipartFile foto) throws IOException {
       byte[] fotoBytes = (foto != null && !foto.isEmpty()) ? foto.getBytes() : null;
       PacienteResponse response = pacienteService.actualizarConFoto(dni, request, fotoBytes);
       return ResponseEntity.ok(response);
   }
   ```

3. **Imports nuevos:** `org.springframework.web.bind.annotation.RequestPart`. Eliminar `@RequestBody` de los métodos modificados.

4. **Mantener intacto** `POST /api/pacientes/{dni}/foto` y `GET /api/pacientes/{dni}/foto` (siguen siendo válidos para edición aislada de foto). **No** mantener una variante JSON-only de `crear`/`actualizar` — el desktop migrará al multipart en una sola pasada y mantener dos variantes complica el contrato.

**Archivo:** `/home/alaslibres/DAM/RehabiAPP/api/src/main/java/com/rehabiapp/api/application/service/PacienteService.java`

5. **Añadir dos nuevos métodos públicos** que envuelven los existentes y persisten la foto en la misma transacción:
   ```java
   /**
    * Crea un paciente con direccion, telefonos y opcionalmente foto, todo en una unica transaccion.
    * Si la persistencia de la foto falla, se hace rollback completo del paciente.
    *
    * @param request   DTO con los datos del paciente.
    * @param fotoBytes Bytes de la foto, o null si no hay foto.
    * @return DTO de respuesta con el paciente creado.
    */
   public PacienteResponse crearConFoto(PacienteRequest request, byte[] fotoBytes) {
       // 1. Resolver sanitario y construir entidad (logica reutilizada de crear())
       Sanitario sanitario = sanitarioRepository.findByDniSanAndActivoTrue(request.dniSan())
               .orElseThrow(() -> new RecursoNoEncontradoException(
                       "Sanitario no encontrado: " + request.dniSan()));

       Paciente paciente = new Paciente();
       paciente.setDniPac(request.dniPac());
       paciente.setSanitario(sanitario);
       // ... (replicar todos los setters de crear() — extraer a metodo privado si crece) ...
       paciente.setActivo(true);
       paciente.setDireccion(resolverDireccion(request.direccion()));

       if (request.telefonos() != null) {
           request.telefonos().forEach(tel -> {
               TelefonoPaciente t = new TelefonoPaciente();
               t.setPaciente(paciente);
               t.setTelefono(tel);
               paciente.getTelefonos().add(t);
           });
       }

       // 2. Persistir foto en la MISMA entidad antes del save -> mismo flush, misma transaccion
       if (fotoBytes != null && fotoBytes.length > 0) {
           paciente.setFoto(fotoBytes);
       }

       Paciente guardado = pacienteRepository.save(paciente);
       auditService.registrar(AccionAuditoria.CREATE, "paciente", request.dniPac(),
               "Paciente creado" + (fotoBytes != null ? " (con foto)" : ""));
       return pacienteMapper.toResponse(guardado);
   }

   /**
    * Actualiza un paciente y opcionalmente su foto en una unica transaccion.
    * Si fotoBytes es null, la foto existente se mantiene intacta.
    */
   public PacienteResponse actualizarConFoto(String dni, PacienteRequest request, byte[] fotoBytes) {
       Paciente paciente = pacienteRepository.findByDniPacAndActivoTrue(dni)
               .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado: " + dni));

       // ... (replicar los setters de actualizar()) ...

       if (request.direccion() != null) {
           paciente.setDireccion(resolverDireccion(request.direccion()));
       }

       if (fotoBytes != null && fotoBytes.length > 0) {
           paciente.setFoto(fotoBytes);
       }

       auditService.registrar(AccionAuditoria.UPDATE, "paciente", dni,
               "Paciente actualizado" + (fotoBytes != null ? " (con foto)" : ""));
       return pacienteMapper.toResponse(pacienteRepository.save(paciente));
   }
   ```

6. **REFACTOR sugerido (no obligatorio):** los métodos antiguos `crear(PacienteRequest)` y `actualizar(String, PacienteRequest)` quedan **sin uso** desde el controlador. Eliminarlos o marcarlos `@Deprecated`. Recomendación del Thinker: **eliminarlos** para evitar duplicación. La firma con foto es la nueva canónica.

**Archivo:** `/home/alaslibres/DAM/RehabiAPP/api/src/main/resources/application.yml` (o `application-local.yml`)

7. **Configurar el límite de tamaño multipart:**
   ```yaml
   spring:
     servlet:
       multipart:
         max-file-size: 5MB
         max-request-size: 6MB
   ```

**Verificación:**
```bash
cd /home/alaslibres/DAM/RehabiAPP/api
./mvnw clean spring-boot:run
```
Debe arrancar sin errores. Probar con `curl`:
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"dni":"ADMIN0000","contrasena":"admin"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# Crear paciente sin foto, con direccion
curl -X POST http://localhost:8080/api/pacientes \
  -H "Authorization: Bearer $TOKEN" \
  -F 'paciente={"dniPac":"99999999X","dniSan":"ADMIN0000","nombrePac":"Test","apellido1Pac":"Atomico","edadPac":30,"direccion":{"calle":"Calle Mayor","numero":"1","cp":"28013","nombreLocalidad":"Madrid","provincia":"Madrid"},"telefonos":["600111222"]};type=application/json'

# Crear paciente con foto
curl -X POST http://localhost:8080/api/pacientes \
  -H "Authorization: Bearer $TOKEN" \
  -F 'paciente={"dniPac":"99999998Y","dniSan":"ADMIN0000","nombrePac":"Test2","apellido1Pac":"ConFoto","edadPac":25,"direccion":{"calle":"Gran Via","numero":"50","cp":"28013","nombreLocalidad":"Madrid","provincia":"Madrid"},"telefonos":["600222333"]};type=application/json' \
  -F 'foto=@/tmp/test.png;type=image/png'
```
Ambas deben devolver `201 Created` con el JSON del paciente. La segunda debe haber persistido la foto (verificar con `GET /api/pacientes/99999998Y/foto`).

---

### FASE 5 — Desktop: extender `Paciente.java` con campos de dirección y `toPacienteRequest()`

**Objetivo:** que el modelo del desktop transporte la dirección de extremo a extremo.

**Archivos a modificar** en `/home/alaslibres/DAM/RehabiAPP/desktop/src/main/java/com/javafx/`:

1. **Crear `dto/DireccionDto.java`** (record nuevo en el package `dto`):
   ```java
   package com.javafx.dto;

   /**
    * DTO de direccion postal usado en PacienteRequest/PacienteResponse.
    * Mismo shape que el record DireccionDto de la API — Jackson lo mapea por nombres.
    */
   public record DireccionDto(
           String calle,
           String numero,
           String piso,
           String cp,
           String nombreLocalidad,
           String provincia
   ) {}
   ```

2. **Editar `dto/PacienteRequest.java`** (record desktop) — añadir el campo `DireccionDto direccion` antes de `telefonos`:
   ```java
   public record PacienteRequest(
           String dniPac,
           String dniSan,
           // ... resto ...
           Boolean consentimientoRgpd,
           DireccionDto direccion,   // ← NUEVO
           List<String> telefonos
   ) {}
   ```

3. **Editar `dto/PacienteResponse.java`** análogamente — añadir `DireccionDto direccion`.

4. **Editar `Clases/Paciente.java`** — añadir 6 propiedades JavaFX (`StringProperty calle, numero, piso, codigoPostal, localidad, provincia`), getters/setters/properties al estilo del resto, y modificar:

   a. **El constructor `desdePacienteResponse`** para copiar la dirección del response al objeto:
      ```java
      if (response.direccion() != null) {
          p.setCalle(response.direccion().calle());
          p.setNumero(response.direccion().numero());
          p.setPiso(response.direccion().piso());
          p.setCodigoPostal(response.direccion().cp());
          p.setLocalidad(response.direccion().nombreLocalidad());
          p.setProvincia(response.direccion().provincia());
      }
      ```

   b. **El método `toPacienteRequest()`** para construir el `DireccionDto` si los campos están informados:
      ```java
      DireccionDto direccion = null;
      if (getCalle() != null && !getCalle().isEmpty()
              && getCodigoPostal() != null && !getCodigoPostal().isEmpty()
              && getLocalidad() != null && !getLocalidad().isEmpty()) {
          direccion = new DireccionDto(
                  getCalle(),
                  getNumero(),
                  getPiso(),
                  getCodigoPostal(),
                  getLocalidad(),
                  getProvincia()
          );
      }

      return new PacienteRequest(
              getDni(), getDniSanitario(), getNombre(),
              // ...
              isConsentimientoRgpd(),
              direccion,           // ← NUEVO (puede ser null si el formulario no la captura)
              telefonos
      );
      ```

**Verificación:** `./gradlew compileJava` debe pasar.

---

### FASE 6 — Desktop: `ApiClient.postMultipart()` + refactor `PacienteDAO` y `PacienteService`

**Objetivo:** una sola llamada HTTP atómica desde el desktop al endpoint multipart.

**Archivo:** `/home/alaslibres/DAM/RehabiAPP/desktop/src/main/java/com/javafx/Clases/ApiClient.java`

1. **Añadir un método público nuevo `postMultipart`** (y `putMultipart`) al `ApiClient`. Aprovechar la lógica existente de `PacienteDAO.insertarFoto()` (líneas 146–177) y generalizarla:
   ```java
   /**
    * Envia una peticion multipart/form-data con una parte JSON y, opcionalmente, una parte de archivo.
    * Util para endpoints atomicos de creacion/edicion que combinan datos estructurados y un blob.
    *
    * @param path         Ruta absoluta sin baseUrl (ej. "/api/pacientes").
    * @param method       "POST" o "PUT".
    * @param jsonPartName Nombre de la parte JSON (ej. "paciente").
    * @param jsonBody     Objeto a serializar como JSON.
    * @param filePartName Nombre de la parte archivo (ej. "foto"). Puede ser null si no hay archivo.
    * @param fileBytes    Bytes del archivo. Puede ser null.
    * @param fileName     Nombre del archivo. Puede ser null.
    * @param fileMime     Content-Type del archivo (ej. "image/png"). Puede ser null.
    * @param responseType Clase de la respuesta a deserializar.
    */
   public <T> T sendMultipart(String path, String method,
                              String jsonPartName, Object jsonBody,
                              String filePartName, byte[] fileBytes,
                              String fileName, String fileMime,
                              Class<T> responseType) {
       return ejecutarConReintento(() -> ejecutarMultipart(path, method,
               jsonPartName, jsonBody, filePartName, fileBytes, fileName, fileMime, responseType));
   }

   private <T> T ejecutarMultipart(String path, String method,
                                   String jsonPartName, Object jsonBody,
                                   String filePartName, byte[] fileBytes,
                                   String fileName, String fileMime,
                                   Class<T> responseType) {
       try {
           String boundary = "----RehabiAppBoundary" + System.currentTimeMillis();
           String json = objectMapper.writeValueAsString(jsonBody);

           // Construir cuerpo multipart con dos partes: json + archivo opcional
           java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();

           // Parte JSON
           String jsonHeader = "--" + boundary + "\r\n"
                   + "Content-Disposition: form-data; name=\"" + jsonPartName + "\"\r\n"
                   + "Content-Type: application/json\r\n\r\n";
           out.write(jsonHeader.getBytes(StandardCharsets.UTF_8));
           out.write(json.getBytes(StandardCharsets.UTF_8));
           out.write("\r\n".getBytes(StandardCharsets.UTF_8));

           // Parte archivo (opcional)
           if (fileBytes != null && fileBytes.length > 0) {
               String fileHeader = "--" + boundary + "\r\n"
                       + "Content-Disposition: form-data; name=\"" + filePartName
                       + "\"; filename=\"" + fileName + "\"\r\n"
                       + "Content-Type: " + fileMime + "\r\n\r\n";
               out.write(fileHeader.getBytes(StandardCharsets.UTF_8));
               out.write(fileBytes);
               out.write("\r\n".getBytes(StandardCharsets.UTF_8));
           }

           // Cierre del boundary
           out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
           byte[] cuerpo = out.toByteArray();

           HttpRequest.Builder builder = HttpRequest.newBuilder()
                   .uri(URI.create(baseUrl + path))
                   .timeout(timeout)
                   .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                   .header("Authorization", "Bearer " + accessToken);

           if ("PUT".equalsIgnoreCase(method)) {
               builder.PUT(HttpRequest.BodyPublishers.ofByteArray(cuerpo));
           } else {
               builder.POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo));
           }

           HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
           LOG.debug("{} multipart {}{} -> {}", method, baseUrl, path, response.statusCode());

           if (response.statusCode() == 200 || response.statusCode() == 201) {
               if (responseType == Void.class || response.body() == null || response.body().isEmpty()) {
                   return null;
               }
               return objectMapper.readValue(response.body(), responseType);
           }
           manejarErrorHttp(response.statusCode(), response.body());
           return null;
       } catch (ConexionException | AutenticacionException | PermisoException
                | ValidacionException | DuplicadoException e) {
           throw e;
       } catch (Exception e) {
           throw new ConexionException("Error al enviar multipart: " + e.getMessage(), e);
       }
   }
   ```

2. **Imports nuevos** en `ApiClient.java`: `java.io.ByteArrayOutputStream`, `java.nio.charset.StandardCharsets`.

**Archivo:** `/home/alaslibres/DAM/RehabiAPP/desktop/src/main/java/com/javafx/DAO/PacienteDAO.java`

3. **Modificar `insertar(Paciente paciente)`** para aceptar un `byte[] fotoBytes` opcional y usar el nuevo método multipart:
   ```java
   /**
    * Inserta un nuevo paciente con telefonos, direccion y foto opcional en UNA SOLA llamada HTTP atomica.
    * El servidor garantiza la atomicidad via @Transactional. Si cualquier parte falla, no queda nada persistido.
    *
    * @param paciente  Paciente a crear (con telefonos y direccion ya seteados).
    * @param fotoBytes Bytes de la foto, o null si no hay foto.
    */
   public void insertar(Paciente paciente, byte[] fotoBytes) {
       api.sendMultipart(
               "/api/pacientes", "POST",
               "paciente", paciente.toPacienteRequest(),
               "foto", fotoBytes,
               fotoBytes != null ? "foto.png" : null,
               fotoBytes != null ? "image/png" : null,
               PacienteResponse.class
       );
   }

   /** Sobrecarga sin foto, llama al multipart con fotoBytes=null. */
   public void insertar(Paciente paciente) {
       insertar(paciente, null);
   }
   ```

4. **Modificar `actualizar(Paciente paciente, String dniOriginal)`** análogamente:
   ```java
   public void actualizar(Paciente paciente, String dniOriginal, byte[] fotoBytes) {
       api.sendMultipart(
               "/api/pacientes/" + dniOriginal, "PUT",
               "paciente", paciente.toPacienteRequest(),
               "foto", fotoBytes,
               fotoBytes != null ? "foto.png" : null,
               fotoBytes != null ? "image/png" : null,
               PacienteResponse.class
       );
   }

   public void actualizar(Paciente paciente, String dniOriginal) {
       actualizar(paciente, dniOriginal, null);
   }
   ```

5. **Mantener** `insertarFoto(String dni, File archivo)` y `obtenerFoto(String dni)` intactos — siguen siendo válidos para edición aislada de foto desde otras pantallas.

**Archivo:** `/home/alaslibres/DAM/RehabiAPP/desktop/src/main/java/com/javafx/service/PacienteService.java`

6. **Reescribir el método `insertar(Paciente, String, String, File)`** para una sola llamada al DAO:
   ```java
   /**
    * Inserta un paciente con telefonos y foto en una unica transaccion atomica.
    * Si la foto falla, el paciente NO se crea (rollback total en el server).
    */
   public void insertar(Paciente paciente, String tel1, String tel2, File archivoFoto) {
       paciente.setTelefono1(tel1 != null ? tel1 : "");
       paciente.setTelefono2(tel2 != null ? tel2 : "");
       byte[] fotoBytes = leerBytesArchivo(archivoFoto);
       pacienteDAO.insertar(paciente, fotoBytes);
   }
   ```

7. **Reescribir `actualizar(Paciente, String, String, String, File)`** análogamente:
   ```java
   public void actualizar(Paciente paciente, String dniOriginal, String tel1, String tel2, File archivoFoto) {
       paciente.setTelefono1(tel1 != null ? tel1 : "");
       paciente.setTelefono2(tel2 != null ? tel2 : "");
       byte[] fotoBytes = leerBytesArchivo(archivoFoto);
       pacienteDAO.actualizar(paciente, dniOriginal, fotoBytes);
   }
   ```

8. **Añadir helper privado `leerBytesArchivo(File f)`:**
   ```java
   /**
    * Lee los bytes de un archivo, devolviendo null si el archivo es null.
    * @throws com.javafx.excepcion.ValidacionException si el archivo no se puede leer.
    */
   private byte[] leerBytesArchivo(File archivo) {
       if (archivo == null) return null;
       try {
           return java.nio.file.Files.readAllBytes(archivo.toPath());
       } catch (java.io.IOException e) {
           throw new com.javafx.excepcion.ValidacionException(
                   "No se pudo leer el archivo: " + archivo.getName(), "foto");
       }
   }
   ```

9. **Eliminar las sobrecargas obsoletas** `insertar(Paciente, String, String)` y `actualizar(Paciente, String, String, String)` SOLO si ningún controlador las usa. Verificar con un grep:
   ```bash
   grep -rn "pacienteService.insertar(" desktop/src/main/java/com/javafx/Interface/
   grep -rn "pacienteService.actualizar(" desktop/src/main/java/com/javafx/Interface/
   ```
   Si hay usos sin foto, mantener una sobrecarga de 3 parámetros que delegue al de 4 con `archivoFoto=null`. Si no hay usos, eliminar.

**Verificación:**
```bash
cd /home/alaslibres/DAM/RehabiAPP/desktop
./gradlew compileJava
```

---

### FASE 7 — Desktop: wire campos de dirección del controlador al `Paciente`

**Objetivo:** que los campos `txtCalle/txtNumero/txtPiso/txtCodigoPostal/txtLocalidad/txtProvincia` (que hoy se validan pero no se transmiten) acaben en el objeto `Paciente`.

**Archivo:** `/home/alaslibres/DAM/RehabiAPP/desktop/src/main/java/com/javafx/Interface/controladorAgregarPaciente.java`

1. **En el método `guardarPaciente()` (alrededor de la línea 382)**, después de los `paciente.setMedicacionActual(...)` y antes del bloque `try { if (modoEdicion) ...`, añadir:
   ```java
   // Configurar direccion (los validadores ya garantizan que calle/cp/localidad estan presentes)
   paciente.setCalle(txtCalle.getText().trim());
   paciente.setNumero(txtNumero.getText().trim());
   paciente.setPiso(txtPiso.getText().trim());
   paciente.setCodigoPostal(txtCodigoPostal.getText().trim());
   paciente.setLocalidad(txtLocalidad.getText().trim());
   paciente.setProvincia(txtProvincia.getText().trim());
   ```

2. **En el modo edición**, también poblar los campos al cargar el paciente. Buscar el método que rellena el formulario al editar (probablemente `cargarDatosPaciente(Paciente)` o similar) y añadir:
   ```java
   txtCalle.setText(paciente.getCalle() != null ? paciente.getCalle() : "");
   txtNumero.setText(paciente.getNumero() != null ? paciente.getNumero() : "");
   txtPiso.setText(paciente.getPiso() != null ? paciente.getPiso() : "");
   txtCodigoPostal.setText(paciente.getCodigoPostal() != null ? paciente.getCodigoPostal() : "");
   txtLocalidad.setText(paciente.getLocalidad() != null ? paciente.getLocalidad() : "");
   txtProvincia.setText(paciente.getProvincia() != null ? paciente.getProvincia() : "");
   ```

3. **Eliminar el comentario engañoso** en las líneas 433–435 y 452–454:
   - Cambiar `"La operacion es atomica: paciente + telefonos + foto en una sola transaccion."`
   - Por `"Atomico server-side: una sola peticion multipart al endpoint POST/PUT /api/pacientes."`
   - Eliminar también el comentario `// (Se mantiene PacienteDAO solo para operaciones de foto que aún no están en el servicio)` y el `// TODO: Mover operaciones de foto al servicio` (líneas 144-146) — ya no aplica.

**Verificación:** `./gradlew compileJava` y arrancar la aplicación: crear un paciente nuevo con dirección y foto, verificar que se persiste correctamente con `GET /api/pacientes/{dni}` (la respuesta debe incluir el bloque `direccion`).

---

### FASE 8 — Sanitario: verificación + documentación (no requiere código)

**Objetivo:** confirmar que `sanitario + rol + telefonos` ya es atómico y dejarlo documentado.

**Acciones:**

1. **Verificar la atomicidad actual** con un test manual via `curl`:
   ```bash
   TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"dni":"ADMIN0000","contrasena":"admin"}' \
     | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

   curl -X POST http://localhost:8080/api/sanitarios \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "dniSan":"99999998Z",
       "nombreSan":"Test",
       "apellido1San":"Atomico",
       "emailSan":"test.atomico@example.com",
       "contrasena":"test1234",
       "cargo":"NURSE",
       "telefonos":["600999888","600999889"]
     }'
   ```
   Debe devolver `201 Created`. Verificar en BD que se crearon 1 fila en `sanitario`, 1 en `sanitario_agrega_sanitario` (rol), 2 en `telefono_sanitario` — todas con el mismo `dni_san`.

2. **Verificar el rollback** simulando un fallo: enviar el mismo `dniSan` dos veces. La segunda debe fallar con `409 Conflict`/`DuplicadoException` y NO debe haber creado roles ni teléfonos huérfanos. Comprobar:
   ```sql
   SELECT COUNT(*) FROM telefono_sanitario WHERE dni_san = '99999998Z';
   -- Debe ser 2 (los del primer insert), no 4.
   ```

3. **Editar el JavaDoc de `desktop/SanitarioService.java`** (líneas 11-15) para reflejar el estado real:
   ```java
   /**
    * Capa de servicio para operaciones de Sanitario.
    *
    * <p><b>Atomicidad:</b> La creacion y actualizacion de un sanitario son
    * operaciones atomicas server-side. Una sola llamada HTTP a POST/PUT /api/sanitarios
    * dispara una unica transaccion @Transactional en la API que persiste sanitario,
    * rol (sanitario_agrega_sanitario) y telefonos (telefono_sanitario) via cascade.
    * Si cualquier parte falla, se hace rollback completo.</p>
    *
    * <p>Delega directamente al SanitarioDAO que consume la API REST.
    * El BCrypt y la auditoria son responsabilidad de la API.</p>
    */
   ```

4. **No tocar `SanitarioService.java` ni `SanitarioDAO.java` del desktop** — su lógica ya es correcta.

5. **No tocar `SanitarioService.java` ni `SanitarioController.java` de la API** — ya son atómicos.

---

### FASE 9 — Tests JUnit 5 + Mockito (REQUERIDO por `desktop/CLAUDE.md §2.4`)

**Objetivo:** garantizar que los wrappers desktop hacen una sola llamada y propagan errores correctamente. Cubre el contrato sin necesidad de stack real.

**Archivo a crear:** `/home/alaslibres/DAM/RehabiAPP/desktop/src/test/java/com/javafx/service/PacienteServiceTest.java`

```java
package com.javafx.service;

import com.javafx.Clases.ApiClient;
import com.javafx.Clases.Paciente;
import com.javafx.DAO.PacienteDAO;
import com.javafx.dto.PacienteResponse;
import com.javafx.excepcion.ConexionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests del wrapper desktop PacienteService.
 * Verifica que la insercion/actualizacion compuesta hace UNA SOLA llamada
 * al PacienteDAO (que internamente hace UNA SOLA llamada multipart al ApiClient).
 */
@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock private PacienteDAO pacienteDAO;
    private PacienteService service;

    @BeforeEach
    void setUp() {
        // Inyectar el mock por reflexion ya que PacienteService crea el DAO en el constructor.
        // Alternativa preferida: refactorizar PacienteService para aceptar el DAO por constructor.
        service = new PacienteService();
        // ... usar reflection para reemplazar el campo final pacienteDAO con el mock ...
    }

    @Test
    void insertar_conFoto_haceUnaSolaLlamadaAtomica() {
        Paciente paciente = nuevoPacienteValido();
        File foto = new File("src/test/resources/foto-test.png"); // colocar un PNG mock pequeno

        service.insertar(paciente, "600111222", "600222333", foto);

        verify(pacienteDAO, times(1)).insertar(eq(paciente), any(byte[].class));
        verifyNoMoreInteractions(pacienteDAO);
    }

    @Test
    void insertar_sinFoto_haceUnaSolaLlamadaConBytesNull() {
        Paciente paciente = nuevoPacienteValido();

        service.insertar(paciente, "600111222", "", null);

        verify(pacienteDAO, times(1)).insertar(eq(paciente), isNull());
    }

    @Test
    void insertar_propagaConexionExceptionSinDejarEstadoParcial() {
        Paciente paciente = nuevoPacienteValido();
        doThrow(new ConexionException("API caida")).when(pacienteDAO).insertar(any(), any());

        assertThrows(ConexionException.class,
                () -> service.insertar(paciente, "600111222", "", null));

        // Garantia: solo hubo UN intento, no hay compensacion (no hace falta — el server hace rollback)
        verify(pacienteDAO, times(1)).insertar(any(), any());
    }

    @Test
    void actualizar_conFoto_haceUnaSolaLlamadaAtomica() {
        Paciente paciente = nuevoPacienteValido();
        File foto = new File("src/test/resources/foto-test.png");

        service.actualizar(paciente, "12345678A", "600111222", "", foto);

        verify(pacienteDAO, times(1)).actualizar(eq(paciente), eq("12345678A"), any(byte[].class));
    }

    private Paciente nuevoPacienteValido() {
        Paciente p = new Paciente();
        p.setDni("99999999X");
        p.setNombre("Test");
        // ... setear campos minimos validos ...
        return p;
    }
}
```

**Nota del Thinker para el Doer:** el `PacienteService` actual crea su `PacienteDAO` con `new` en el constructor (línea 18), lo que dificulta el mocking. **Antes de escribir los tests, refactorizar `PacienteService` para aceptar el DAO por constructor** (constructor injection con un DAO opcional). Mantener un constructor sin parámetros que llame al constructor con `new PacienteDAO()` para no romper el resto del código:

```java
public class PacienteService {

    private final PacienteDAO pacienteDAO;

    public PacienteService() {
        this(new PacienteDAO());
    }

    /** Constructor para tests con DAO inyectado. */
    PacienteService(PacienteDAO pacienteDAO) {
        this.pacienteDAO = pacienteDAO;
    }
    // ...
}
```

Aplicar el mismo patrón a `SanitarioService`.

**Archivo a crear:** `/home/alaslibres/DAM/RehabiAPP/desktop/src/test/java/com/javafx/service/SanitarioServiceTest.java`

Estructura idéntica a `PacienteServiceTest`. Tests:
- `insertar_haceUnaSolaLlamadaAtomicaAlDAO()`
- `insertar_propagaConexionExceptionSinReintentar()`
- `actualizar_haceUnaSolaLlamadaAtomicaAlDAO()`
- `cambiarContrasena_haceUnaSolaLlamada()`

**Crear archivo PNG mock:** `/home/alaslibres/DAM/RehabiAPP/desktop/src/test/resources/foto-test.png` (cualquier PNG válido pequeño, p.ej. 100×100 px). Puede generarse con:
```bash
mkdir -p /home/alaslibres/DAM/RehabiAPP/desktop/src/test/resources
python3 -c "
import struct, zlib
def png(w,h):
    sig = b'\\x89PNG\\r\\n\\x1a\\n'
    ihdr = struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)
    ihdr_chunk = struct.pack('>I', len(ihdr)) + b'IHDR' + ihdr + struct.pack('>I', zlib.crc32(b'IHDR'+ihdr))
    raw = b''.join(b'\\x00' + b'\\xff\\xff\\xff\\xff'*w for _ in range(h))
    comp = zlib.compress(raw)
    idat_chunk = struct.pack('>I', len(comp)) + b'IDAT' + comp + struct.pack('>I', zlib.crc32(b'IDAT'+comp))
    iend_chunk = struct.pack('>I', 0) + b'IEND' + struct.pack('>I', zlib.crc32(b'IEND'))
    return sig + ihdr_chunk + idat_chunk + iend_chunk
open('/home/alaslibres/DAM/RehabiAPP/desktop/src/test/resources/foto-test.png','wb').write(png(10,10))
"
```

**Verificación:**
```bash
cd /home/alaslibres/DAM/RehabiAPP/desktop
./gradlew test
```
Debe pasar todos los tests existentes + los nuevos. **Si algún test antiguo se rompe por la refactorización del constructor, arreglarlo en la misma fase.**

---

### FASE 10 — Verificación end-to-end

Ejecutar en este orden tras completar las fases 1–9:

1. **Compilación limpia API + Desktop:**
   ```bash
   cd /home/alaslibres/DAM/RehabiAPP/api && ./mvnw clean compile
   cd /home/alaslibres/DAM/RehabiAPP/desktop && ./gradlew clean build
   ```

2. **Arranque del stack** (PostgreSQL + API + Desktop) según RUNBOOK.

3. **E2E manual desde la UI:**
   - Login con `ADMIN0000 / admin`.
   - Pestaña Pacientes → "Añadir paciente".
   - Rellenar TODOS los campos incluyendo dirección (Calle, Número, Piso, CP, Localidad, Provincia) y subir una foto.
   - Guardar. Verificar: mensaje de éxito, paciente aparece en la lista, al abrir su ficha se ven dirección y foto.
   - Editar el mismo paciente: cambiar Calle, dejar el resto, NO subir foto nueva. Guardar. Verificar que la foto previa se mantiene y la dirección se actualizó.
   - Editar de nuevo: subir una foto distinta. Verificar que la nueva foto reemplaza la antigua.

4. **Verificación de atomicidad real (rollback test):**
   - Intentar crear un paciente con un `dniPac` que ya existe (provoca error de duplicado en la API).
   - Verificar en BD que NO quedó ningún registro huérfano:
     ```sql
     SELECT COUNT(*) FROM telefono_paciente WHERE dni_pac = '<dni-duplicado>';
     SELECT COUNT(*) FROM direccion d
        WHERE d.id_direccion NOT IN (SELECT id_direccion FROM paciente WHERE id_direccion IS NOT NULL);
     ```
     La primera consulta debe seguir devolviendo el COUNT del paciente original (no se duplican teléfonos). La segunda no debe crecer entre intentos (no hay direcciones huérfanas — `find-or-create` reutiliza si los datos ya coinciden).

5. **Verificación de la atomicidad de la foto:**
   - Crear un paciente nuevo con un archivo de foto inválido (p.ej. `.txt` renombrado a `.png`). La API debe responder error y NO debe haber creado nada en `paciente`. Verificar:
     ```sql
     SELECT * FROM paciente WHERE dni_pac = '<dni-test>';
     ```
     Debe devolver 0 filas.

6. **Verificación de Sanitario:**
   - Crear un sanitario nuevo desde la UI con dos teléfonos.
   - Verificar en BD: 1 fila en `sanitario`, 1 en `sanitario_agrega_sanitario`, 2 en `telefono_sanitario`, todas con el mismo DNI.
   - Intentar crear el mismo sanitario otra vez (DNI duplicado). Debe fallar. Verificar que NO hay teléfonos ni rol huérfanos del segundo intento.

7. **Tests automatizados:**
   ```bash
   cd /home/alaslibres/DAM/RehabiAPP/desktop && ./gradlew test
   ```
   Todos verdes. Si hay fallo, **arreglar antes** de avanzar — no marcar el checklist con tests rotos.

---

### FASE 11 — Migración Flyway documental (V9) y checklist

**Objetivo:** dejar histórico de la nueva semántica multipart para entornos futuros. **No** aplica cambios de schema (no hay nuevas tablas — solo se empieza a usar `direccion` que ya existe).

**Acción única:** la migración V9 NO es necesaria. La FASE 8 del plan anterior (V8) cubre las correcciones de schema. Esta tarea solo modifica código de aplicación. **Saltar este paso.**

**Marcar checklist:** editar `/home/alaslibres/DAM/RehabiAPP/desktop/CLAUDE.md` sección `## 7. IMPLEMENTATION CHECKLIST → ### CRUD operations`. Cambiar las 3 líneas:

```
- [x] Implement atomic transactions (commit/rollback) in PacienteService for compound operations (patient + phones + address + photo).
- [x] Implement atomic transactions in SanitarioService for compound operations (practitioner + role + phones).
- [x] Integrate photo upload within the same transaction as patient INSERT.
```

No tocar el resto del checklist.

---

## 4. Orden de ejecución recomendado

1. **FASE 0** — leer contexto, arrancar stack.
2. **FASE 1** — repos. Compilar API.
3. **FASE 2** — DTOs API. Compilar API (errores esperables en service/mapper, se arreglan en F3).
4. **FASE 3** — service + mapper API. Compilar API limpio.
5. **FASE 4** — endpoint multipart API. Arrancar API. **Probar con `curl` antes de continuar.**
6. **FASE 5** — modelo desktop. Compilar desktop.
7. **FASE 6** — `ApiClient.sendMultipart` + DAO + service desktop. Compilar desktop.
8. **FASE 7** — wire campos del controlador. Compilar y arrancar desktop. **E2E manual mínimo: crear un paciente con dirección y foto.**
9. **FASE 8** — verificar Sanitario, actualizar JavaDoc. Sin código.
10. **FASE 9** — refactor de constructores para inyección + tests. `./gradlew test` verde.
11. **FASE 10** — verificación E2E completa con rollback tests.
12. **FASE 11** — marcar checklist `[x]`.

**Si algo falla en mitad de una fase, no avanzar.** Diagnosticar, corregir, re-verificar la fase actual antes de pasar a la siguiente.

---

## 5. Archivos críticos (referencia rápida)

### API (`/home/alaslibres/DAM/RehabiAPP/api`)

**Crear:**
- `src/main/java/com/rehabiapp/api/domain/repository/DireccionRepository.java`
- `src/main/java/com/rehabiapp/api/domain/repository/CodigoPostalRepository.java`
- `src/main/java/com/rehabiapp/api/domain/repository/LocalidadRepository.java`
- `src/main/java/com/rehabiapp/api/application/dto/DireccionDto.java`

**Editar:**
- `src/main/java/com/rehabiapp/api/application/dto/PacienteRequest.java` — añadir `DireccionDto direccion`
- `src/main/java/com/rehabiapp/api/application/dto/PacienteResponse.java` — añadir `DireccionDto direccion`
- `src/main/java/com/rehabiapp/api/application/service/PacienteService.java` — `crearConFoto`, `actualizarConFoto`, `resolverDireccion`, nuevos repos en constructor; eliminar `crear`/`actualizar` antiguos
- `src/main/java/com/rehabiapp/api/application/mapper/PacienteMapper.java` — mapping de `direccion`
- `src/main/java/com/rehabiapp/api/presentation/controller/PacienteController.java` — `@RequestPart` en `crear`/`actualizar`
- `src/main/resources/application.yml` (o `application-local.yml`) — `multipart.max-file-size: 5MB`

### Desktop (`/home/alaslibres/DAM/RehabiAPP/desktop`)

**Crear:**
- `src/main/java/com/javafx/dto/DireccionDto.java`
- `src/test/java/com/javafx/service/PacienteServiceTest.java`
- `src/test/java/com/javafx/service/SanitarioServiceTest.java`
- `src/test/resources/foto-test.png`

**Editar:**
- `src/main/java/com/javafx/dto/PacienteRequest.java` — añadir `DireccionDto direccion`
- `src/main/java/com/javafx/dto/PacienteResponse.java` — añadir `DireccionDto direccion`
- `src/main/java/com/javafx/Clases/Paciente.java` — 6 propiedades JavaFX, `desdePacienteResponse` y `toPacienteRequest` con dirección
- `src/main/java/com/javafx/Clases/ApiClient.java` — `sendMultipart` + `ejecutarMultipart`
- `src/main/java/com/javafx/DAO/PacienteDAO.java` — `insertar(p, fotoBytes)`, `actualizar(p, dni, fotoBytes)`
- `src/main/java/com/javafx/service/PacienteService.java` — refactor a constructor injection; `insertar/actualizar` con `byte[]`; helper `leerBytesArchivo`
- `src/main/java/com/javafx/service/SanitarioService.java` — refactor a constructor injection; JavaDoc atomicidad
- `src/main/java/com/javafx/Interface/controladorAgregarPaciente.java` — wire de los `txtCalle/txtNumero/txtPiso/txtCodigoPostal/txtLocalidad/txtProvincia` al objeto Paciente; eliminar comentarios engañosos
- `CLAUDE.md` — marcar las 3 líneas del checklist como `[x]`

---

## 6. Resumen ejecutivo para el Doer

| Bloque | Esfuerzo | Riesgo |
|--------|----------|--------|
| API: 3 repos nuevos + DireccionDto + extensión PacienteRequest/Response | Bajo | Bajo |
| API: PacienteService.resolverDireccion + crearConFoto/actualizarConFoto | Medio | Medio (LazyInitException si el mapper no está dentro de TX — ya garantizado) |
| API: PacienteController @RequestPart multipart | Bajo | Bajo |
| API: PacienteMapper @Mapping de direccion | Bajo | Bajo (verificar `LazyInitializationException`) |
| Desktop: DireccionDto + Paciente.java con 6 propiedades + toPacienteRequest | Bajo | Bajo |
| Desktop: ApiClient.sendMultipart (≈80 líneas) | Medio | Medio (parsing multipart manual) |
| Desktop: PacienteDAO.insertar/actualizar(p, byte[]) | Bajo | Bajo |
| Desktop: PacienteService refactor a constructor injection + helper bytes | Bajo | Bajo |
| Desktop: wire 6 campos en controladorAgregarPaciente | Trivial | Bajo |
| Tests JUnit 5 + Mockito (refactor inyección + 8 tests aprox.) | Medio | Medio (refactor de constructor puede romper otros tests existentes) |
| Verificación E2E manual (5 escenarios) | Bajo | Bajo |
| Marcar checklist | Trivial | — |

**Total estimado:** 11 fases secuenciales. La fase de mayor riesgo es la 6 (`sendMultipart`) — está parcialmente probada por la lógica reutilizada de `PacienteDAO.insertarFoto`, pero el formato multipart es propenso a bugs sutiles (boundaries, CRLF, content-types).

**Garantía clave a entregar:** después de la FASE 7, el flujo "alta de paciente con dirección + foto" debe ser **una sola petición HTTP** desde el desktop a la API, y el rollback server-side debe garantizar que cualquier fallo deja la BD exactamente como estaba antes del intento. Sin esto, los 3 ítems del checklist no se pueden marcar `[x]`.
