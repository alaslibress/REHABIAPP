# PLAN.md — Desktop iteracion 2026-05-07 (stats-implementation)

> **Branch:** stats-implementation
> **Autor:** Agente 3 Thinker (Opus) — PRESCRIPTIVO. Doer (Sonnet) implementa paso a paso sin reinterpretar.
> **Idioma:** Codigo y comentarios en castellano (root `/CLAUDE.md` §4.5).
> **Scope:** Iteracion final. Tres tareas nuevas (G, H, I) + test (J) + hotfix (K).
> **Estado API:** TODOS los endpoints ya estan operativos en `/api`. Doer NO toca `/api`.
>
> Las fases 0/A/B/C/D/E del plan anterior estan IMPLEMENTADAS y se conservan como referencia en la seccion final (§REFERENCIA — fases ya completadas). NO retocar nada de esas fases salvo que la fase G/H/I/K lo exija explicitamente.

---

## PHASE K — HOTFIX: PermisoException "Error desconocido" al abrir ficha paciente

> **Sintoma reportado por el usuario:**
> ```
> Caused by: java.lang.reflect.InvocationTargetException
> Caused by: com.javafx.excepcion.PermisoException: Error desconocido
> ```
> Ocurre al hacer doble-click sobre un paciente para abrir su ficha (VentanaListarPaciente).
>
> **Diagnostico del Thinker:**
>
> 1. `controladorVentanaPacienteListar.initialize()` (linea 132) llama a `cargarMapaNiveles()` (linea 146), que llama a `catalogoService.listarNiveles()`. El catch SOLO captura `ConexionException`. Si la API devuelve 403 (Spring Security, body vacio), `PermisoException` propaga fuera de `initialize()`. JavaFX FXMLLoader envuelve la excepcion en `InvocationTargetException`, y la captura externa (`abrirFichaPaciente` linea 295) la traduce a un modal generico.
>
> 2. El mensaje "Error desconocido" viene de `ApiClient.extraerMensajeError()` (linea 549), que devuelve esa cadena cuando el body del response es null/vacio. Spring Security cuando rechaza por `@PreAuthorize` o por token caducado/invalido NO pasa por `GlobalExceptionHandler` y retorna 403 con body vacio.
>
> 3. El reintento de `ejecutarConReintento()` solo cubre 401 (renovar token). 403 NO se reintenta. Si el accessToken caduca y por algun fallo la API responde 403 en lugar de 401 (caso raro pero observado en algunas configs de Spring Security 7), el flujo termina en `PermisoException`.
>
> **Tres causas raiz posibles (NO asumir cual es real hasta tener URL del 403):**
> - **K-A**: Token caducado/invalido y la API devuelve 403 con body vacio.
> - **K-B**: Algun endpoint del flujo de apertura de ficha tiene `@PreAuthorize` que rechaza al usuario actual (NURSE intentando acceso clinico, p.ej.).
> - **K-C**: `controladorVentanaPacienteListar.initialize()` deja escapar `PermisoException` por catch demasiado estrecho (solo ConexionException).
>
> Independientemente de A o B, el caso C es un bug seguro: cualquier `PermisoException` lanzada desde `initialize()` rompe el FXML loader.

### K.0 — Reproducir y capturar la URL exacta del 403

Doer ejecuta antes de tocar nada:

1. Asegurar que el stack este levantado (postgres + api + data + desktop). Ver `/desktop/CLAUDE.md` §9.

2. En `desktop/src/main/java/com/javafx/Clases/ApiClient.java`, localizar `manejarErrorHttp` (linea 531) y, **temporalmente para diagnostico**, cambiar el `LOG.warn` para incluir la URL:

   ```java
   private void manejarErrorHttp(int statusCode, String responseBody) {
       String mensaje = extraerMensajeError(responseBody);
       String bodyRecortado = responseBody != null && responseBody.length() > 500
               ? responseBody.substring(0, 500) : responseBody;
       LOG.warn("Error HTTP {}: body={}", statusCode, bodyRecortado);
       ...
   }
   ```

   Este metodo no recibe la URL. Hay que extender el contrato. Cambiar la firma a:

   ```java
   private void manejarErrorHttp(int statusCode, String responseBody, String url) {
       String mensaje = extraerMensajeError(responseBody);
       String bodyRecortado = responseBody != null && responseBody.length() > 500
               ? responseBody.substring(0, 500) : responseBody;
       LOG.warn("Error HTTP {} en {}: body={}", statusCode, url, bodyRecortado);
       switch (statusCode) {
           case 400 -> throw new ValidacionException(mensaje, "peticion");
           case 401 -> throw new AutenticacionException(mensaje);
           case 403 -> throw new PermisoException(
               "Acceso denegado a " + url + (mensaje.equals("Error desconocido") ? "" : ": " + mensaje));
           case 404 -> throw new ValidacionException(mensaje, "entidad");
           case 409 -> throw new DuplicadoException(mensaje, "registro");
           default -> throw new ConexionException("Error de servidor (" + statusCode + ") en " + url + ": " + mensaje);
       }
   }
   ```

   Y en TODAS las llamadas a `manejarErrorHttp(...)` en este mismo fichero (busca con `grep -n "manejarErrorHttp" desktop/src/main/java/com/javafx/Clases/ApiClient.java`), pasa el `path` o `baseUrl + path` como tercer argumento. Para cada llamada, la URL real esta en el `HttpRequest` del scope local; se pasa directamente.

3. Reproducir el bug: login con `ADMIN0000/admin`, doble-click sobre un paciente. Anotar en consola la URL exacta del primer 403.

4. Anotar el resultado en el PR description bajo `## URL del 403 capturada`.

### K.1 — Fix obligatorio: capturar PermisoException donde corresponda

Independientemente de la URL detectada en K.0, el caso K-C SIEMPRE debe corregirse. `initialize()` no puede dejar escapar excepciones del dominio.

#### K.1.1 — Endurecer `cargarMapaNiveles()` en `controladorVentanaPacienteListar.java`

Localizar el metodo `cargarMapaNiveles()` (linea 146 aprox.). Sustituir el catch:

```java
private void cargarMapaNiveles() {
    mapaNiveles = new HashMap<>();
    try {
        List<NivelProgresion> niveles = catalogoService.listarNiveles();
        for (NivelProgresion nivel : niveles) {
            mapaNiveles.put(nivel.getNombreCorto(), nivel);
        }
    } catch (com.javafx.excepcion.RehabiAppException e) {
        // Cualquier ConexionException, PermisoException, AutenticacionException u otra
        // excepcion del dominio NO debe romper initialize() — los tooltips son opcionales
        System.err.println("No se pudieron cargar los niveles para tooltips ("
                + e.getClass().getSimpleName() + "): " + e.getMessage());
    } catch (Exception e) {
        System.err.println("Error inesperado cargando niveles: " + e.getMessage());
        e.printStackTrace();
    }
}
```

> Nota: el import `com.javafx.excepcion.RehabiAppException` ya existe en el archivo via `import com.javafx.excepcion.ConexionException;`. Anadirlo si falta.

#### K.1.2 — Endurecer `cargarDatosPaciente(String dni)` en `controladorVentanaPacienteListar.java`

Localizar el metodo `cargarDatosPaciente` (linea 315 aprox.). El catalogo de tratamientos solo cachea `ConexionException`; igual que en K.1.1, ampliar a `RehabiAppException`. Y envolver TODAS las llamadas API restantes en try/catch defensivo:

```java
public void cargarDatosPaciente(String dni) {
    this.dniPacienteActual = dni;

    // Cargar catalogo de tratamientos (opcional — solo se usa para filtrado por nivel)
    try {
        List<Tratamiento> catalogo = catalogoService.listarTratamientos();
        mapaTratamientos.clear();
        for (Tratamiento t : catalogo) {
            mapaTratamientos.put(t.getCodTrat(), t);
        }
    } catch (RehabiAppException e) {
        System.err.println("No se pudo cargar el catalogo de tratamientos ("
                + e.getClass().getSimpleName() + "): " + e.getMessage());
    } catch (Exception e) {
        System.err.println("Error inesperado al cargar catalogo: " + e.getMessage());
    }

    // Obtener paciente — bloqueante, sin paciente no hay ficha
    try {
        pacienteActual = pacienteDAO.obtenerPorDNI(dni);
    } catch (com.javafx.excepcion.PermisoException e) {
        VentanaUtil.mostrarVentanaInformativa(
            "No tienes permisos para ver este paciente.\n"
                + "Detalle: " + e.getMessage(),
            TipoMensaje.ERROR);
        pacienteActual = null;
    } catch (com.javafx.excepcion.ConexionException e) {
        VentanaUtil.mostrarVentanaInformativa(
            "Sin conexion con la API: " + e.getMessage(), TipoMensaje.ERROR);
        pacienteActual = null;
    } catch (RehabiAppException e) {
        VentanaUtil.mostrarVentanaInformativa(
            "Error al cargar el paciente: " + e.getMessage(), TipoMensaje.ERROR);
        pacienteActual = null;
    } catch (Exception e) {
        e.printStackTrace();
        VentanaUtil.mostrarVentanaInformativa(
            "Error inesperado al cargar el paciente.", TipoMensaje.ERROR);
        pacienteActual = null;
    }

    if (pacienteActual != null) {
        mostrarDatosEnLabels();
        cargarFotoPaciente();
        cargarDiscapacidadesPaciente();
    }
    // Si pacienteActual es null, ya se mostro un modal arriba — no anadir otro
}
```

> Importar `com.javafx.excepcion.RehabiAppException` si todavia no esta importado.

#### K.1.3 — Auditoria preventiva: revisar OTROS controladores con el mismo patron

Buscar en TODOS los controladores con `initialize()` que llamen a un servicio del API:

```bash
grep -rn "public void initialize" desktop/src/main/java/com/javafx/Interface/ | head -20
```

Para cada controlador identificado, verificar que su `initialize()` NO deje escapar ninguna excepcion del dominio. Aplicar el mismo patron de K.1.1: catch `RehabiAppException` ANCHO ademas del catch que ya tenga.

Lista esperada (no exhaustiva — Doer verifica caso por caso):

| Controlador | Llamada al API en initialize() | Catch suficiente? |
|---|---|---|
| `controladorVentanaPacienteListar` | `cargarMapaNiveles()` → `listarNiveles()` | NO (solo ConexionException) → fix en K.1.1 |
| `controladorAgregarTratamiento` | `cargarComboBoxes()` → `listarDiscapacidades()`, `listarNivelesProgresion()` | Verificar que tenga catch ancho |
| `controladorAgregarPaciente` | si tiene cargas API en initialize | Verificar |
| `controladorVentanaTratamientos` | `cargarTratamientos()` (NO esta en initialize, esta tras configurarTabla — verificar) | Verificar |
| `controladorVentanaPacientes` | `cargarPacientes()` ya cubierto por Phase G | OK |

Para cualquier controlador donde el initialize() ejecute IO de API SIN catch ancho de `RehabiAppException`, anadir el catch defensivo. Si el dato es opcional (tooltips, listas de filtro), loguear y seguir; si el dato es bloqueante (no se puede usar el formulario sin el), mostrar modal y dejar el formulario en estado inactivo (botones disabled), pero NUNCA propagar al FXMLLoader.

### K.2 — Mejorar el mensaje de error 403 con body vacio

Localizar `extraerMensajeError` en `ApiClient.java` (linea 549). Cambiarlo para que devuelva un mensaje mas util que "Error desconocido":

```java
private String extraerMensajeError(String responseBody) {
    if (responseBody == null || responseBody.isBlank()) {
        // Spring Security devuelve 403 con body vacio cuando @PreAuthorize falla
        // o cuando el token JWT esta caducado/invalido — el mensaje generico no
        // ayuda al diagnostico, pero al menos no dice "Error desconocido"
        return "Acceso denegado por el servidor (sin detalle)";
    }
    try {
        ErrorResponse error = objectMapper.readValue(responseBody, ErrorResponse.class);
        if (error.message() != null && !error.message().isBlank()) {
            return error.message();
        }
        if (error.detalle() != null && !error.detalle().isBlank()) {
            return error.detalle();
        }
        return responseBody;
    } catch (Exception e) {
        return responseBody;
    }
}
```

> Nota: verificar el record `ErrorResponse` interno del `ApiClient`. Si solo tiene campo `message`, anadir tambien `detalle` (la API devuelve `{"error":"...","detalle":"..."}` desde `GlobalExceptionHandler`).

### K.3 — Endurecer reintento contra 403 por token expirado (defensivo)

Si la URL capturada en K.0 indica que el 403 ocurre tras un periodo de inactividad, lo mas probable es que el accessToken caduco. La logica actual en `ejecutarConReintento` SOLO reintenta tras 401:

```java
private <T> T ejecutarConReintento(OperacionHttp<T> operacion) {
    try {
        return operacion.ejecutar();
    } catch (AutenticacionException e) {  // ← solo 401
        if (renovarToken()) {
            return operacion.ejecutar();
        }
        logout();
        throw e;
    }
}
```

**NO ampliar el catch a `PermisoException`** — eso ocultaria errores reales de RBAC. En su lugar, asegurar que la API SIEMPRE responde 401 (no 403) cuando el token esta caducado/invalido. Esto es responsabilidad del API (Agente 1), pero como Agente 3 documentamos el supuesto.

Si tras K.0 se confirma que el 403 viene de un token caducado, escalar al Thinker del Agente 1 (API) un ticket: "Spring Security debe devolver 401 en lugar de 403 cuando el JWT esta caducado/invalido". Mientras tanto, el fix de K.1 evita que la app se rompa.

### K.4 — Validacion manual

| # | Caso | Esperado |
|---|------|----------|
| K.4.1 | Login ADMIN0000 + doble-click sobre paciente | Ficha abre OK con todos los datos (nombre, foto, discapacidades, tratamientos) |
| K.4.2 | Login NURSE (00000002W) + doble-click sobre paciente | Ficha abre en modo solo lectura — sin excepcion |
| K.4.3 | Login ADMIN, esperar 16 minutos (token caducado), doble-click sobre paciente | Ficha abre OK (refresh transparente del token via 401 → renovarToken) |
| K.4.4 | Login ADMIN, apagar la API a proposito, doble-click sobre paciente | Modal `"Sin conexion con la API: ..."` — NO crash, NO InvocationTargetException |
| K.4.5 | Provocar 403 artificial (revocar permisos en BD a un paciente concreto si fuera posible, o simular con codigo temporal) | Modal `"No tienes permisos para ver este paciente. Detalle: Acceso denegado a ..."` — NO crash |
| K.4.6 | Repetir K.4.1..K.4.5 tras varios doble-clicks consecutivos en pacientes distintos | Sin regresiones, sin estados parciales en la ficha |

### K.5 — Tests

`desktop/src/test/java/com/javafx/Interface/ControladorVentanaPacienteListarTest.java`:
- `cargarMapaNiveles_conPermisoException_noPropaga`. Mockear `CatalogoService#listarNiveles` para que lance `PermisoException`. Invocar `cargarMapaNiveles()` por reflection y verificar que NO lanza, y que `mapaNiveles` queda vacio (HashMap inicializado pero vacio).
- `cargarDatosPaciente_conPermisoExceptionEnObtenerPorDNI_dejaPacienteActualNull_yMuestraModal`. Mockear `PacienteDAO#obtenerPorDNI` para que lance `PermisoException`. Verificar que `pacienteActual` queda null y el modal se muestra (mockear `VentanaUtil.mostrarVentanaInformativa` o capturar via `MockedStatic`).

Si la suite no soporta TestFx ni JavaFX toolkit en tests, mover la logica IO a un metodo package-private y testear ese metodo aislado.

### K.6 — Archivos modificados

Crear:
```
desktop/src/test/java/com/javafx/Interface/ControladorVentanaPacienteListarTest.java
```

Modificar:
```
desktop/src/main/java/com/javafx/Interface/controladorVentanaPacienteListar.java  (K.1.1, K.1.2)
desktop/src/main/java/com/javafx/Clases/ApiClient.java                            (K.0 logging URL + K.2 mensaje)
+ cualquier otro controlador identificado en K.1.3 con catch demasiado estrecho en initialize()
```

NO crear nuevos endpoints API. NO modificar `/api/**`.

### K.7 — Orden de ejecucion

```
K.0 (reproducir y capturar URL)
   ↓
K.1.1 (cargarMapaNiveles defensivo)        [obligatorio independiente de K.0]
   ↓
K.1.2 (cargarDatosPaciente defensivo)      [obligatorio independiente de K.0]
   ↓
K.1.3 (auditoria de otros initialize)      [obligatorio independiente de K.0]
   ↓
K.2  (mejorar mensaje de error)            [obligatorio]
   ↓
K.3  (NO ampliar catch a PermisoException — solo documentar) [no requiere codigo]
   ↓
K.4  (validacion manual)
   ↓
K.5  (tests)
```

K.0 puede dejar marcas de logging permanentes (mejor diagnostico) o quitarlas tras la captura — Doer decide segun ruido del log. Si la URL revela un caso K-A o K-B real, escalar al Thinker el descubrimiento ANTES de proseguir, NO inventar un fix de RBAC en el desktop.

---

---

## 0. CONTEXTO OBLIGATORIO (LEER ANTES DE TOCAR CODIGO)

1. `/CLAUDE.md` raiz — §4.5 estilo, §4.6 seguridad, §10 TestSprite.
2. `/desktop/CLAUDE.md` — §7 checklist Phase A-E (todas ya estan tachadas excepto los items que esta iteracion va a tachar).
3. `/desktop/.claude/skills/javafx-java24/SKILL.md` — todas las reglas (threading, MVC, CSS, no setStyle).
4. `desktop/src/main/resources/tema_claro.css` y `tema_oscuro.css` — completos.
5. `desktop/src/main/java/com/javafx/Clases/ApiClient.java` — helpers HTTP (`get`, `getBytes`, `post`, `delete`, `uploadFile`).

### 0BIS. CONTRATOS DE LA API (referencia rapida — NO inventar paths)

| Operacion | Metodo + Path | Auth | Response |
|-----------|---------------|------|----------|
| Listar progreso por tratamiento | `GET /api/pacientes/{dni}/progreso` | Bearer | `List<ProgresoTratamientoResponse>` |
| Check nuevos datos progreso | `GET /api/pacientes/{dni}/progreso/check?since=<Instant ISO>` | Bearer | `CheckProgresoResponse{hasNewData,lastSessionAt,count}` |
| Markdown progreso | `GET /api/pacientes/{dni}/progreso/markdown` | Bearer | `text/markdown;charset=UTF-8` |
| Forzar regeneracion MD | `POST /api/pacientes/{dni}/progreso/markdown/regenerar` | Bearer SPECIALIST | 202 Accepted |
| Listar videojuegos activos | `GET /api/videojuegos` | Bearer | `List<VideojuegoResponse>` |
| Videojuegos por discapacidad | `GET /api/videojuegos/discapacidad/{codDis}` | Bearer | `List<VideojuegoResponse>` |
| Juegos vinculados a tratamiento | `GET /api/catalogo/tratamientos/{cod}/videojuegos` | Bearer | `List<VideojuegoResponse>` |
| Vincular juego | `POST /api/catalogo/tratamientos/{cod}/videojuegos/{id}` | SPECIALIST | 200 OK |
| Desvincular juego | `DELETE /api/catalogo/tratamientos/{cod}/videojuegos/{id}` | SPECIALIST | 204 |
| Subir PDF tratamiento | `POST /api/tratamientos/{cod}/pdf` (multipart `file`) | SPECIALIST | 200 |
| Descargar PDF tratamiento | `GET /api/tratamientos/{cod}/pdf` | Bearer | `application/pdf` (binario) |
| Metadatos PDF | `GET /api/tratamientos/{cod}/pdf/metadatos` | Bearer | `PdfMetadatosResponse` |
| Eliminar PDF | `DELETE /api/tratamientos/{cod}/pdf` | SPECIALIST | 204 |

DTOs ya existentes en desktop (`com.javafx.Clases`):
`ProgresoEntrada(Instant fecha, Double valor)`,
`ProgresoTratamiento(...)`, `CheckProgreso(...)`, `PdfMetadato(...)`, `Videojuego(...)`.

NO crear nuevos records. NO renombrar campos.

---

## DECISIONES DEL THINKER PARA ESTA ITERACION

| Tema | Opciones evaluadas | Decision | Justificacion |
|------|--------------------|----------|---------------|
| Vista de tratamiento read-only | a) FXML nuevo `VentanaVerTratamiento.fxml` con controlador propio · b) Reutilizar `VentanaAgregarTratamiento.fxml` con un metodo `cargarDatosParaVer(...)` que deshabilita controles | **b) Reutilizar el FXML existente** | Cero duplicacion de UI. La FXML actual ya tiene los campos, tabla de juegos y boton "Descargar PDF". Solo hay que anadir un modo "ver" que: deshabilita inputs, oculta `btnImportarPdf`/`btnEliminarPdf`/`btnGuardar`, muestra `btnDescargarPdf`, cambia `btnCancelar` a "Cerrar" y el titulo a "Ver Tratamiento". Mantener un solo controlador con tres modos (`crear`, `editar`, `ver`) es mas barato que duplicar 200 lineas de FXML + un controlador nuevo. |
| Visualizacion de progreso del paciente | a) Previsualizacion JavaFX `LineChart` en modal · b) Generar y descargar PDF JasperReports con grafica | **a) Previsualizacion JavaFX `LineChart`** | Toda la infra ya existe (`VentanaProgresoPaciente.fxml`, `controladorVentanaProgresoPaciente`, `GraficoUtil.crearLineChart`, `ProgresoService`, `SyncProgresoService`, `ProgresoDAO`). Jasper exigiria: nuevo `.jrxml` con grafica de linea (configuracion compleja en JasperSoft Studio o XML manual), compilar `.jasper`, `JasperFillManager.fillReport`, `JasperExportManager.exportReportToPdfFile`, file-chooser, manejo de fuentes, y mantener un dataset paralelo. Coste de desarrollo > 10x. La vista interactiva ya cubre el caso de uso. La generacion en PDF queda en BACKLOG (Phase F del checklist) si el cliente lo pide. |
| Pestaña Pacientes rota | Diagnostico antes de fix | **Reproducir y aislar el stack trace** | El plan G impone que el Doer arranque la app, capture la traza en consola y la pegue en el commit. NO intentar fix a ciegas. Hay >5 hipotesis posibles (FXML, DAO, RBAC, NPE en cache de pestanias, error 500 del API). Sin la traza no se puede prescribir un fix. |

---

## PHASE G — FIX PESTAÑA PACIENTES ROTA

> **Sintoma reportado:** al pulsar el boton de la pestana "Pacientes" en `VentanaPrincipal`, la pestana no se abre (queda en blanco o se queda en la pestana anterior).
> **Hipotesis del Thinker (NO asumir cual es real hasta tener traza):**
> 1. `cargarPacientes()` en `controladorVentanaPacientes#initialize()` lanza excepcion (API caida, 401, deserializacion fallida, NPE en `paginacion`).
> 2. `cacheControladores` mantiene un controlador roto de una sesion anterior y al reusarlo `configurarPermisos()` revienta.
> 3. Mismatch FXML/controlador (algun `fx:id` declarado en FXML no existe en el controlador o viceversa).
> 4. El BorderPane principal recibe `null` porque `cargarPestania` lo absorbe en su `try/catch` general (`controladorVentanaPrincipal.java:279-282`).

### G.0 — Reproducir el bug

1. Levantar el stack completo (ver `/desktop/CLAUDE.md` §9):
   ```bash
   docker compose -f /home/alaslibres/DAM/RehabiAPP/infra/docker-compose.yml up -d postgres mongodb
   cd /home/alaslibres/DAM/RehabiAPP/api && set -a && source .env.local && set +a && ./mvnw spring-boot:run
   # nueva terminal
   cd /home/alaslibres/DAM/RehabiAPP/data && ./mvnw spring-boot:run
   # nueva terminal
   cd /home/alaslibres/DAM/RehabiAPP/desktop && ./gradlew run 2>&1 | tee /tmp/desktop-run.log
   ```
2. Login con `ADMIN0000 / admin`. La app entra en VentanaPrincipal (que segun `controladorVentanaPrincipal#initialize` ya carga "Pacientes" como primera pestana).
3. Anotar lo que se ve en pantalla:
   - Si la pestana esta en blanco → leer `/tmp/desktop-run.log` y copiar la PRIMERA traza que mencione `Error al cargar pestaña Pacientes:` o cualquier `Exception in thread`/`Caused by`.
   - Si la pestana inicial NO es Pacientes (otra pestana se carga primero), pulsar "Pacientes" y repetir.
4. Anadir la traza completa al PR description bajo `## Stack trace observado`.

### G.1 — Diagnostico segun traza

**Caso 1 — `ConexionException`/`AutenticacionException` en `pacienteDAO.listarTodos()` durante initialize:**

`controladorVentanaPacientes#cargarPacientes()` no captura excepciones. Cualquier fallo en `listarTodos()` aborta `initialize` y JavaFX devuelve `null` como contenido. Fix:

En `controladorVentanaPacientes.java`, refactorizar `cargarPacientes()` y enriquecer `initialize()`:

```java
private void cargarPacientes() {
    try {
        todosPacientes = pacienteDAO.listarTodos();
        if (todosPacientes == null) todosPacientes = new java.util.ArrayList<>();
        paginacion.setDatos(todosPacientes);
        System.out.println("Pacientes cargados: " + todosPacientes.size()
                + " (mostrando " + listaPacientes.size() + " por pagina)");
    } catch (com.javafx.excepcion.ConexionException e) {
        todosPacientes = new java.util.ArrayList<>();
        paginacion.setDatos(todosPacientes);
        VentanaUtil.mostrarVentanaInformativa(
            "No se pudo cargar la lista de pacientes (sin conexion con la API).",
            TipoMensaje.ERROR);
    } catch (Exception e) {
        todosPacientes = new java.util.ArrayList<>();
        paginacion.setDatos(todosPacientes);
        System.err.println("Error al cargar pacientes: " + e.getMessage());
        e.printStackTrace();
        VentanaUtil.mostrarVentanaInformativa(
            "Error al cargar pacientes: " + e.getMessage(), TipoMensaje.ERROR);
    }
}
```

Aplicar el mismo patron defensivo en `buscarPacientes(...)`, `realizarBusqueda()` y `aplicarFiltros(...)` — toda llamada a `pacienteDAO` debe ir envuelta en `try/catch` sin propagar a JavaFX.

**Caso 2 — Cache de controladores corrupto (`cacheControladores` reusa instancia tras login distinto):**

Si tras logout/login la pestana revienta porque el cache aun tiene un controlador de la sesion anterior:

En `controladorVentanaPrincipal#cargarPestania`, ya existe `limpiarCachePestania(...)` (linea 636) y `cacheControladores.clear()` (linea 650). Asegurar que se invoca al hacer logout:

Buscar el handler de logout existente. Si NO limpia el cache, anadir antes de mostrar la pantalla de inicio:
```java
cachePestanias.clear();
cacheControladores.clear();
```

**Caso 3 — `fx:id` declarado en FXML pero no en controlador (o al reves):**

Comparar `VentanaPacientes.fxml` (lineas 15-124) con `controladorVentanaPacientes.java` (lineas 40-94). Hoy ambos coinciden. Si la traza dice `IllegalArgumentException: ... not a valid type for FXML class` o `LoadException: ... fx:id`, identificar el fx:id y o bien anadir el campo al controlador o eliminarlo del FXML. NO renombrar.

**Caso 4 — `manejarDobleClicTabla` u otro handler nullable:**

Si la traza dice `NullPointerException` en `manejarDobleClicTabla`, asegurar que `tblPacientes != null` antes de `setOnMouseClicked`. Defensivo:
```java
if (tblPacientes != null) tblPacientes.setOnMouseClicked(this::manejarDobleClicTabla);
```

**Caso 5 — `cargarPestania` devuelve sin actualizar `bdpPrincipal.setCenter(contenido)` por excepcion silenciosa:**

En `controladorVentanaPrincipal#cargarPestania` (linea 230), la captura `catch (Exception e)` (linea 279) suprime errores. Modificar para que tras logear el error muestre un mensaje y deje un placeholder en el centro (NO dejar `bdpPrincipal` en estado inconsistente):

```java
} catch (Exception e) {
    System.err.println("Error al cargar pestaña " + nombrePestania + ": " + e.getMessage());
    e.printStackTrace();
    // Limpiar cache corrupto y mostrar placeholder
    cachePestanias.remove(nombrePestania);
    cacheControladores.remove(nombrePestania);
    Label errorLabel = new Label("No se pudo abrir la pestana \"" + nombrePestania
        + "\".\nRevise la conexion con el servidor o reinicie la aplicacion.");
    errorLabel.getStyleClass().add("label-error-pestania");
    VBox placeholder = new VBox(errorLabel);
    placeholder.setAlignment(javafx.geometry.Pos.CENTER);
    placeholder.getStyleClass().add("panel-card");
    bdpPrincipal.setCenter(placeholder);
    pestaniaActual = nombrePestania;
    VentanaUtil.mostrarVentanaInformativa(
        "Error al abrir la pestana de " + nombrePestania + ": " + e.getMessage(),
        VentanaUtil.TipoMensaje.ERROR);
}
```

Anadir en `tema_claro.css` y `tema_oscuro.css` al final:
```css
.label-error-pestania {
    -fx-text-fill: -color-peligro;
    -fx-font-size: 14px;
    -fx-text-alignment: center;
    -fx-wrap-text: true;
    -fx-padding: 30 30 30 30;
}
```

### G.2 — Validacion del fix

1. Reiniciar la API y la app.
2. Login y abrir la pestana de Pacientes → la tabla debe poblarse SIN excepciones en consola.
3. Apagar la API a proposito y volver a abrir la pestana → debe mostrar el modal `"Error al cargar pacientes: ..."` y dejar la tabla vacia (NO bloquear la app).
4. Levantar la API otra vez, cerrar y reabrir la pestana → la tabla se puebla.
5. `./gradlew test` debe seguir verde.

### G.3 — Tests

`desktop/src/test/java/com/javafx/Interface/ControladorVentanaPacientesTest.java` (test de integracion ligera con Mockito):
- `initialize_concapaApiCaida_noLanzaExcepcion`. Mockear `PacienteDAO#listarTodos` para que lance `ConexionException` y verificar que `tblPacientes` quede inicializada (no null) y la tabla este vacia.
- `cargarPacientes_concapaListaVacia_noRevienta`. `listarTodos` devuelve `Collections.emptyList()`.

Si la creacion de un controlador JavaFX en test es complicada (FXMLLoader requiere toolkit), usar `TestFx` solo si ya esta en `build.gradle`. Si NO esta, probar el metodo via instancia directa con campos `@FXML` inyectados manualmente con reflection, o mover la logica de carga a un metodo packagee-private testeable y dejar el test al nivel del DAO/Service.

---

## PHASE H — VISTA "VER TRATAMIENTO" (READ-ONLY)

> **Objetivo:** al pulsar (doble-click o boton "Ver") un tratamiento de la tabla, abrir la ficha del tratamiento en modo solo lectura. Reutilizar `VentanaAgregarTratamiento.fxml` mediante un nuevo modo `ver`. NO crear FXML nuevo.

### H.1 — Renombrar y anadir boton en `VentanaTratamientos.fxml`

El FXML actual tiene en la barra inferior los botones `Eliminar` y `Editar`. Cambiar el flujo:
- El boton "Editar" pasa a llamarse "Ver" y abre la ventana en modo `ver`.
- Anadir un nuevo boton "Editar" para los SPECIALIST que abre la ventana en modo `editar` (logica actual).
- El doble-click en la tabla pasa a abrir en modo `ver` (NO editar).

Editar `desktop/src/main/resources/VentanaTratamientos.fxml`, sustituir el HBox de botones inferior (lineas 110-115):

```xml
<HBox alignment="CENTER_RIGHT" prefHeight="50.0" prefWidth="200.0" spacing="10.0">
    <children>
        <Button fx:id="btnEliminarTratamiento" mnemonicParsing="false"
                onAction="#eliminarTratamientoSeleccionado"
                prefHeight="26.0" prefWidth="126.0"
                styleClass="button-peligro" text="Eliminar" textAlignment="CENTER" />
        <Button fx:id="btnEditarTratamiento" mnemonicParsing="false"
                onAction="#editarTratamientoSeleccionado"
                prefHeight="26.0" prefWidth="126.0"
                styleClass="button-secundario" text="Editar" />
        <Button fx:id="btnVerTratamiento" mnemonicParsing="false"
                onAction="#verTratamientoSeleccionado"
                prefHeight="26.0" prefWidth="126.0"
                styleClass="button-primario" text="Ver" />
    </children>
</HBox>
```

### H.2 — Cambios en `controladorVentanaTratamientos.java`

1. Anadir el campo:
   ```java
   @FXML
   private Button btnVerTratamiento;
   ```

2. Cambiar el doble-click para que abra en modo `ver` (linea 469):
   ```java
   private void manejarDobleClicTabla(MouseEvent event) {
       if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
           Tratamiento seleccionado = tblTratamientos.getSelectionModel().getSelectedItem();
           if (seleccionado != null) {
               verTratamientoSeleccionado(null);
           }
       }
   }
   ```

3. Anadir el handler `verTratamientoSeleccionado`:
   ```java
   /**
    * Abre el formulario en modo solo lectura para el tratamiento seleccionado.
    * Reutiliza VentanaAgregarTratamiento.fxml con la nueva flag "ver".
    */
   @FXML
   void verTratamientoSeleccionado(ActionEvent event) {
       Tratamiento seleccionado = tblTratamientos.getSelectionModel().getSelectedItem();
       if (seleccionado == null) {
           VentanaUtil.mostrarVentanaInformativa(
               "Debe seleccionar un tratamiento de la lista.",
               TipoMensaje.ADVERTENCIA);
           return;
       }
       try {
           FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaAgregarTratamiento.fxml"));
           Parent root = loader.load();
           controladorAgregarTratamiento controlador = loader.getController();
           controlador.cargarDatosParaVer(seleccionado);

           Scene scene = new Scene(root);
           controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

           Stage stage = new Stage();
           stage.setTitle("Ver Tratamiento");
           stage.setScene(scene);
           stage.initModality(Modality.APPLICATION_MODAL);
           stage.setResizable(false);
           VentanaUtil.establecerIconoVentana(stage);
           stage.showAndWait();
       } catch (Exception e) {
           System.err.println("Error al abrir vista de tratamiento: " + e.getMessage());
           e.printStackTrace();
           VentanaUtil.mostrarVentanaInformativa(
               "Error al abrir la vista del tratamiento.", TipoMensaje.ERROR);
       }
   }
   ```

4. RBAC en `configurarPermisos()`: el boton "Ver" SIEMPRE habilitado (incluso enfermero); "Editar"/"Eliminar"/"Anadir" siguen restringidos a SPECIALIST.

   Localizar el bloque `if (!sesion.esEspecialista()) { ... btnEditarTratamiento.setDisable(true); ... }` (lineas 289-309). Anadir DESPUES del `}` final, dentro del metodo:
   ```java
   // El boton "Ver" esta siempre disponible para todos los roles autorizados a esta pestana
   if (btnVerTratamiento != null) {
       btnVerTratamiento.setDisable(false);
       btnVerTratamiento.setOpacity(1.0);
   }
   ```

### H.3 — Anadir modo `ver` en `controladorAgregarTratamiento.java`

> Toda la logica de PDF y juegos ya esta en este controlador. Solo anadimos un modo nuevo que deshabilita inputs y oculta botones de mutacion.

1. Anadir un `enum` privado y un campo de estado al inicio de la clase (junto a `modoEdicion`):
   ```java
   /** Modos de uso del formulario. */
   private enum Modo { CREAR, EDITAR, VER }
   private Modo modo = Modo.CREAR;
   ```

   Mantener `modoEdicion` por compatibilidad con el codigo existente (`modoEdicion = (modo == Modo.EDITAR)`).

2. Anadir el metodo publico `cargarDatosParaVer(Tratamiento)`:
   ```java
   /**
    * Carga los datos del tratamiento en modo solo lectura.
    * Deshabilita todos los inputs, oculta los botones de mutacion (Importar/Eliminar PDF, Guardar)
    * y deja visible el boton Descargar PDF si existe PDF asociado.
    */
   public void cargarDatosParaVer(Tratamiento tratamiento) {
       // Reusar el cargado de datos del modo edicion
       cargarDatosParaEdicion(tratamiento);
       // Pero conmutar el modo y aplicar la mascara de solo lectura
       this.modo = Modo.VER;
       this.modoEdicion = false; // las validaciones de "guardar" no se ejecutan
       aplicarModoSoloLectura();
   }

   /**
    * Aplica la mascara de solo lectura: deshabilita inputs, oculta botones de mutacion
    * y reetiqueta el boton de cancelar a "Cerrar".
    */
   private void aplicarModoSoloLectura() {
       lblTituloVentana.setText("Ver Tratamiento");

       // Inputs: bloquear edicion (no usar setDisable para mantener legibilidad del texto)
       txtCodigo.setEditable(false);
       txtNombre.setEditable(false);
       txtAreaDefinicion.setEditable(false);
       cmbDiscapacidad.setDisable(true);
       cmbNivelProgresion.setDisable(true);

       // Tabla de juegos: solo lectura (mantener visible el check para que se vea cuales estan vinculados)
       tablaJuegos.setEditable(false);
       // Deshabilitar interaccion sobre la columna de seleccion: el usuario ve los marcados pero no puede tocar
       colJuegoSel.setEditable(false);

       // Botones de PDF: solo descargar
       btnImportarPdf.setVisible(false);
       btnImportarPdf.setManaged(false);
       btnEliminarPdf.setVisible(false);
       btnEliminarPdf.setManaged(false);
       // btnDescargarPdf permanece como esta — visible solo si hay PDF (ya gestionado por cargarMetadatosPdfDeTratamiento)

       // Botones del pie: ocultar Guardar, mantener Cancelar reetiquetado a "Cerrar"
       btnGuardar.setVisible(false);
       btnGuardar.setManaged(false);
       btnCancelar.setText("Cerrar");
       btnCancelar.getStyleClass().removeAll("button-peligro");
       if (!btnCancelar.getStyleClass().contains("button-secundario")) {
           btnCancelar.getStyleClass().add("button-secundario");
       }
   }
   ```

3. Localizar `cargarDatosParaEdicion(Tratamiento tratamiento)` (lineas 184+). Al inicio del metodo, antes de `lblTituloVentana.setText("Editar Tratamiento");`, anadir:
   ```java
   this.modo = Modo.EDITAR;
   ```
   Y al inicio de `initialize()` dejar implicito `this.modo = Modo.CREAR;` (es el default del campo).

4. Localizar `guardarTratamiento(...)` (buscar el metodo @FXML). Al inicio del metodo anadir:
   ```java
   if (modo == Modo.VER) {
       // Defensa en profundidad: btnGuardar esta oculto en modo ver, pero por si se llama por accion-key
       return;
   }
   ```

5. Verificar que `cargarMetadatosPdfDeTratamiento(...)` ya hace visible `btnDescargarPdf` cuando hay PDF (ya implementado). En modo `ver` con PDF presente el flujo es: usuario pulsa "Descargar" → `descargarPdf()` (ya existente) → FileChooser → guarda. NO requiere cambios.

### H.4 — Validacion manual

| # | Caso | Esperado |
|---|------|----------|
| H.4.1 | SPECIALIST: doble-click en un tratamiento | Abre en modo VER (titulo "Ver Tratamiento", inputs deshabilitados, botones Importar/Eliminar PDF/Guardar ocultos, "Cerrar" visible) |
| H.4.2 | SPECIALIST: pulsar boton "Ver" | Idem H.4.1 |
| H.4.3 | SPECIALIST: pulsar boton "Editar" | Abre en modo EDITAR (comportamiento previo, sin regresion) |
| H.4.4 | NURSE: la pestana de tratamientos es invisible (mantenido) o si por error es visible, los botones Editar/Eliminar/Anadir estan disabled y solo "Ver" funciona | Sin regresion |
| H.4.5 | Tratamiento con PDF: en modo ver, boton "Descargar" visible y funcional, "Importar"/"Quitar" ocultos | OK |
| H.4.6 | Tratamiento con 2 juegos vinculados: en modo ver, los checks aparecen marcados pero no se pueden cambiar | OK |
| H.4.7 | Pulsar "Cerrar" en modo ver | Cierra sin tocar nada |

### H.5 — Tests

`desktop/src/test/java/com/javafx/Interface/ControladorAgregarTratamientoModoVerTest.java`:
- `cargarDatosParaVer_ocultaBotonesDeMutacion`. Mockear `CatalogoService` y `VideojuegoDAO`. Cargar el FXML en TestFx (si esta en build) o instanciar el controlador a mano e inyectar campos `@FXML` por reflection. Verificar que `btnGuardar.isVisible() == false`, `btnImportarPdf.isVisible() == false`, `btnCancelar.getText().equals("Cerrar")`, `txtCodigo.isEditable() == false`.
- `guardarTratamiento_enModoVer_noLlamaServicio`. Forzar `modo = VER` por reflection y verificar que tras invocar `guardarTratamiento(null)` no se llamo a `catalogoService.crearTratamiento(...)` ni `actualizarTratamiento(...)`.

Si la suite de tests no soporta TestFx, contemplar el test al nivel de comportamiento del campo `modo` y la lista de visibilidad de botones (tests de invariantes).

---

## PHASE I — VISUALIZACION DE PROGRESO DEL PACIENTE (PREVIEW JAVAFX LINECHART)

> **Decision tomada:** previsualizacion JavaFX con `LineChart`. La generacion en JasperReports queda PARKED en backlog (`/desktop/CLAUDE.md` §7 Phase F).
>
> **Estado actual:** la infraestructura ya existe (`VentanaProgresoPaciente.fxml`, `controladorVentanaProgresoPaciente`, `GraficoUtil`, `ProgresoService`, `SyncProgresoService`, `ProgresoDAO`). Esta fase verifica el flujo end-to-end y completa los detalles que falten.

### I.0 — Verificacion del estado actual

Doer ejecuta antes de tocar nada:

1. `grep -n "abrirProgresoSeleccionado\|btnProgresoPaciente" /home/alaslibres/DAM/RehabiAPP/desktop/src/main/resources/VentanaPacientes.fxml /home/alaslibres/DAM/RehabiAPP/desktop/src/main/java/com/javafx/Interface/controladorVentanaPacientes.java`
2. `grep -rn "GraficoUtil.crearLineChart\|VentanaProgresoPaciente" /home/alaslibres/DAM/RehabiAPP/desktop/src/main`
3. Anotar en el PR cuales de los siguientes elementos YA estan presentes (`[x]`) o faltan (`[ ]`):
   - [x] `Videojuego.java`, `ProgresoEntrada.java`, `ProgresoTratamiento.java`, `CheckProgreso.java`, `PdfMetadato.java`
   - [x] `ProgresoDAO.java`, `VideojuegoDAO.java`
   - [x] `ProgresoService.java`, `SyncProgresoService.java`
   - [x] `GraficoUtil.java`
   - [x] `VentanaProgresoPaciente.fxml`, `controladorVentanaProgresoPaciente.java`
   - [x] Boton "Progreso" en `VentanaPacientes.fxml` (linea 114)
   - [x] Handler `abrirProgresoSeleccionado(...)` en `controladorVentanaPacientes` (linea 608)
   - [ ] Boton "Progreso" en `VentanaListarPaciente.fxml` (ficha del paciente — verificar)
   - [ ] Handler `abrirProgreso(...)` en `controladorVentanaPacienteListar.java`

### I.1 — Anadir boton "Progreso" en la ficha del paciente

Si el paso I.0 confirma que falta, anadir en `desktop/src/main/resources/VentanaListarPaciente.fxml` un `Button` en la cabecera de acciones de la ficha (junto a "Editar"/"Eliminar"):

```xml
<Button fx:id="btnProgresoPaciente" mnemonicParsing="false"
        onAction="#abrirProgreso"
        prefHeight="26.0" prefWidth="126.0"
        styleClass="button-secundario" text="Progreso" />
```

> Nota: localizar el HBox de botones existente (parecido al de `VentanaPacientes.fxml` lineas 107-122) y anadir el boton manteniendo el patron. NO romper la alineacion.

En `desktop/src/main/java/com/javafx/Interface/controladorVentanaPacienteListar.java` anadir:

```java
@FXML
private Button btnProgresoPaciente;

/** Abre la ventana de progreso clinico del paciente actual. */
@FXML
private void abrirProgreso(ActionEvent event) {
    if (dniPacienteActual == null || dniPacienteActual.isBlank()) {
        VentanaUtil.mostrarVentanaInformativa(
            "No se puede abrir el progreso: paciente no inicializado.",
            VentanaUtil.TipoMensaje.ADVERTENCIA);
        return;
    }
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaProgresoPaciente.fxml"));
        Parent root = loader.load();
        controladorVentanaProgresoPaciente ctrl = loader.getController();

        Scene scene = new Scene(root);
        controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

        Stage stage = new Stage();
        stage.setTitle("Progreso — " + dniPacienteActual);
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(true);
        VentanaUtil.establecerIconoVentana(stage);

        ctrl.inicializarConDni(dniPacienteActual);
        stage.showAndWait();
    } catch (Exception e) {
        System.err.println("Error al abrir progreso desde ficha: " + e.getMessage());
        e.printStackTrace();
        VentanaUtil.mostrarVentanaInformativa(
            "Error al abrir la ventana de progreso.",
            VentanaUtil.TipoMensaje.ERROR);
    }
}
```

> Si el campo que guarda el DNI del paciente actual NO se llama `dniPacienteActual`, usar el nombre real (probablemente `paciente.getDni()` o `dniPaciente`). NO renombrar campos existentes.

### I.2 — Endurecer `controladorVentanaProgresoPaciente`

Verificar las siguientes invariantes en el codigo actual (`controladorVentanaProgresoPaciente.java`). Aplicar fix solo donde NO se cumplan:

1. `inicializarConDni` debe llamarse SIEMPRE antes de `stage.showAndWait()`. Hoy se llama tras `stage.setScene(...)` y antes de `showAndWait()` (correcto en `controladorVentanaPacientes#abrirProgresoSeleccionado` linea 630). Verificar idem para el nuevo handler `abrirProgreso` de la ficha.

2. `cargarProgresoAsync` (linea 63) ya usa `Task` y `Platform.runLater` implicito via `setOnSucceeded`. Sin cambios.

3. `renderizarGraficos` (linea 90) limpia `contenedorGraficos` antes de redibujar. Sin cambios.

4. `cerrar` (linea 122) detiene el `syncService`. Verificar tambien que el `setOnCloseRequest` registrado en `inicializarConDni` (linea 53) detenga el sync al cerrar con la X. Hoy lo hace.

5. **Mejora opcional (NO bloqueante):** anadir un placeholder visual antes de cargar — un `ProgressIndicator` centrado en el contenedor mientras llega la respuesta. Hoy ya existe `indicadorCarga` (linea 30). Sin cambios.

### I.3 — `GraficoUtil.crearLineChart` — micro-revisiones

`GraficoUtil.java` actual instala los puntos baseline (primer punto) y actual (ultimo punto). Verificar:

1. Si `p.entradas()` esta vacia, NO romper. El metodo debe devolver un `LineChart` vacio con el titulo, sin lanzar `IndexOutOfBoundsException`. Si la condicion `if (!serie.getData().isEmpty())` ya cubre esto, sin cambios.

2. La fecha en el eje Y se formatea como `yyyy-MM-dd` (ya hecho). Si hay varias entradas en el mismo dia, JavaFX `CategoryAxis` los pinta superpuestos. **No es bug** — el caso real (1 sesion al dia) lo cubre. Si en pruebas reales aparecen sesiones agrupadas, escalar al Thinker.

3. Anadir tooltip con texto formateado claro (ya hecho con `Tooltip.install`).

4. CSS: las clases `.punto-baseline` y `.punto-actual` ya estan declaradas en el plan original (Phase D.4). Verificar que existen al final de `tema_claro.css` y `tema_oscuro.css`. Si no, anadir:
   ```css
   .grafico-progreso .chart-line-symbol.punto-baseline {
       -fx-background-color: -color-info, white;
       -fx-background-radius: 6px;
       -fx-padding: 4px;
   }
   .grafico-progreso .chart-line-symbol.punto-actual {
       -fx-background-color: -color-exito, white;
       -fx-background-radius: 6px;
       -fx-padding: 4px;
   }
   ```
   (Sustituir `-color-info`/`-color-exito` por el token CSS real ya definido al principio del tema.)

### I.4 — Mensaje de "no datos"

Si el paciente no tiene sesiones en MongoDB, `obtenerProgreso` devuelve lista vacia. `renderizarGraficos` ya muestra `"No hay datos de progreso para este paciente."` (linea 93). Sin cambios.

Tests manuales con un paciente que SI tiene sesiones (insertar manualmente en Mongo via `data` pipeline o usar el seed de `V14__datos_prueba_desarrollo.sql` si esta poblado): verificar que se pintan N graficos uno por (`tratamiento`, `parteCuerpo`).

### I.5 — Validacion E2E

| # | Caso | Esperado |
|---|------|----------|
| I.5.1 | VentanaPacientes + Progreso sin seleccion | Modal `"Selecciona un paciente para ver su progreso."` |
| I.5.2 | VentanaPacientes + Progreso con seleccion | Abre `VentanaProgresoPaciente` con titulo "Progreso — <DNI>" |
| I.5.3 | Ficha paciente (doble-click) + boton Progreso | Abre `VentanaProgresoPaciente` con titulo "Progreso — <DNI>" |
| I.5.4 | Paciente sin sesiones | Texto `"No hay datos de progreso para este paciente."` y sin graficos |
| I.5.5 | Paciente con N tratamientos y M sesiones | N `LineChart` apilados, scroll vertical, primer punto azul, ultimo verde |
| I.5.6 | Polling de 30s | Logs de la API muestran `GET /api/pacientes/<dni>/progreso/check` cada ~30s mientras la ventana este abierta |
| I.5.7 | Cerrar la ventana (X o boton Cerrar) | Logs muestran `shutdown` del executor del polling |
| I.5.8 | Apagar la API durante el polling | `lblEstado` queda en estado neutro, charts antiguos persisten, sin spam de modales |

### I.6 — Tests

Ya cubiertos por las pruebas existentes mencionadas en Phase D.8 / E.3 del plan original. Verificar que `./gradlew test` pasa al final de la fase.

---

## PHASE J — VERIFICACION GLOBAL Y TESTING

### J.1 — Compilacion y tests

```bash
cd /home/alaslibres/DAM/RehabiAPP/desktop
./gradlew clean compileJava test
```

Sin warnings nuevos. Sin tests rotos. Si MapStruct/Annotation processors generan warnings preexistentes, dejarlos.

### J.2 — Smoke test manual

| # | Caso | Esperado |
|---|------|----------|
| J.2.1 | Login ADMIN0000/admin → app abre Pacientes | Tabla poblada (Phase G OK) |
| J.2.2 | Apagar API y abrir Pacientes | Modal de error claro, app no se cuelga |
| J.2.3 | Pestana Tratamientos → boton Ver | Abre modo VER (Phase H OK) |
| J.2.4 | Pestana Tratamientos → doble-click | Abre modo VER (NO editar) |
| J.2.5 | Pestana Tratamientos → boton Editar | Abre modo EDITAR (sin regresion) |
| J.2.6 | Modo VER con PDF → Descargar | FileChooser → fichero PDF correcto |
| J.2.7 | Modo VER → Cerrar | Cierra modal sin cambios |
| J.2.8 | VentanaPacientes → seleccionar y pulsar Progreso | Modal con LineCharts (Phase I OK) |
| J.2.9 | Doble-click paciente → ficha → boton Progreso | Modal con LineCharts |
| J.2.10 | Cambiar tema (claro/oscuro) y repetir J.2.3..J.2.9 | Sin areas blancas en oscuro, sin regresiones |

### J.3 — TestSprite MCP

Tras smoke OK, delegar a TestSprite (root `/CLAUDE.md` §10). Marcar `[x]` en `/desktop/CLAUDE.md` §7 SOLO cuando TestSprite devuelva 100% en los items afectados.

### J.4 — Actualizar `/desktop/CLAUDE.md`

Una vez TestSprite verde, actualizar el checklist §7:
- Phase A.1 .. A.4 → `[x]` (ya lo estaban en el plan anterior, confirmar)
- Phase B.1 .. B.5 → `[x]` (ya implementado, confirmar)
- Phase C.1 .. C.6 → `[x]`
- Phase D.1 .. D.8 → `[x]`
- Phase E.1 .. E.4 → `[x]`
- Anadir entradas nuevas:
  - `[x] G.1 Pestana Pacientes resiliente a fallos del API.`
  - `[x] H.1 Vista "Ver Tratamiento" en modo solo lectura.`
  - `[x] I.1 Boton Progreso en ficha paciente y verificacion E2E del flujo de progreso.`

---

## K. ORDEN DE EJECUCION

```
Phase G (Pacientes fix)  --> independiente
Phase H (Ver Tratamiento) --> independiente
Phase I (Progreso preview) --> independiente
       |
       +--> Phase J (Testing al final)
```

Recomendacion: G → H → I → J. Las tres son ortogonales pero G corrige un bug bloqueante en la pantalla mas usada, asi que va primero.

---

## L. NON-NEGOTIABLES

- Comentarios y mensajes de UI en castellano. Nada en ingles en Java/FXML/CSS.
- Sin emojis.
- Sin `setStyle(...)` inline en Java.
- Sin nuevas dependencias en `build.gradle` (NO anadir Jasper para esta iteracion — es una decision explicita del Thinker).
- Toda I/O fuera del hilo JavaFX (`Task` / `ScheduledExecutorService`).
- Toda actualizacion UI desde background → `Platform.runLater`.
- TestSprite 100% antes de marcar `[x]` (root `/CLAUDE.md` §10.4).
- No tocar codigo fuera de los archivos listados en este plan sin aprobacion del Thinker.
- No renombrar metodos existentes; si hay conflicto, apuntar el `onAction` al metodo real ya presente.
- NO crear `VentanaVerTratamiento.fxml` separado. La decision es REUSAR `VentanaAgregarTratamiento.fxml` con un modo nuevo (ver §H).

---

## M. ARCHIVOS QUE EL DOER VA A CREAR / MODIFICAR

### Crear (nuevos)

```
desktop/src/test/java/com/javafx/Interface/ControladorVentanaPacientesTest.java
desktop/src/test/java/com/javafx/Interface/ControladorAgregarTratamientoModoVerTest.java
```

### Modificar

```
desktop/src/main/java/com/javafx/Interface/controladorVentanaPacientes.java     (Phase G — try/catch defensivo)
desktop/src/main/java/com/javafx/Interface/controladorVentanaPrincipal.java     (Phase G — placeholder en pestana fallida)
desktop/src/main/java/com/javafx/Interface/controladorVentanaTratamientos.java  (Phase H — boton Ver + handler + doble-click cambia a ver)
desktop/src/main/java/com/javafx/Interface/controladorAgregarTratamiento.java   (Phase H — modo VER + cargarDatosParaVer + aplicarModoSoloLectura)
desktop/src/main/java/com/javafx/Interface/controladorVentanaPacienteListar.java (Phase I — boton Progreso si falta)
desktop/src/main/resources/VentanaTratamientos.fxml                              (Phase H — anadir btnVerTratamiento)
desktop/src/main/resources/VentanaListarPaciente.fxml                            (Phase I — boton Progreso si falta)
desktop/src/main/resources/tema_claro.css                                        (Phase G + I — label-error-pestania, refuerzo punto-baseline/actual si falta)
desktop/src/main/resources/tema_oscuro.css                                        (idem)
```

### NO TOCAR

```
desktop/src/main/java/com/javafx/Clases/ApiClient.java                  (estable)
desktop/src/main/java/com/javafx/Clases/Paciente.java                   (estable)
desktop/src/main/java/com/javafx/Clases/Tratamiento.java                (estable)
desktop/src/main/java/com/javafx/Clases/Videojuego.java                 (estable)
desktop/src/main/java/com/javafx/Clases/ProgresoTratamiento.java        (estable)
desktop/src/main/java/com/javafx/Clases/ProgresoEntrada.java            (estable)
desktop/src/main/java/com/javafx/Clases/CheckProgreso.java              (estable)
desktop/src/main/java/com/javafx/Clases/PdfMetadato.java                (estable)
desktop/src/main/java/com/javafx/DAO/*.java                              (estables)
desktop/src/main/java/com/javafx/service/*.java                          (estables)
desktop/src/main/java/com/javafx/util/GraficoUtil.java                   (cambios solo si I.3 lo exige)
desktop/src/main/resources/VentanaProgresoPaciente.fxml                  (estable)
desktop/src/main/resources/VentanaAgregarTratamiento.fxml                (estable — el modo VER es solo logica del controlador, NO cambia el FXML)
api/**                                                                    (NUNCA — Doer 3 no toca la API)
data/**                                                                   (NUNCA)
```

---

## N. RESUMEN EJECUTIVO PARA EL DOER

1. **Bug bloqueante:** Phase G arregla la pestana Pacientes que no abre. Reproducir → leer traza → aplicar el fix correspondiente (caso 1 a 5). Endurecer `cargarPestania` para que un fallo nunca rompa la UI silenciosamente.
2. **Vista de tratamiento:** Phase H anade un nuevo modo VER al controlador `controladorAgregarTratamiento` reutilizando el FXML existente. Boton nuevo "Ver" en la pestana de tratamientos; doble-click abre VER (no editar). Sin nuevo FXML.
3. **Progreso del paciente:** Phase I confirma el flujo end-to-end (la mayor parte ya esta) y anade el boton "Progreso" en la ficha del paciente si faltara. Decision firme: previsualizacion JavaFX `LineChart` (NO Jasper).
4. **Testing:** Phase J pasa `./gradlew test`, smoke matrix manual, TestSprite 100%, y actualiza `/desktop/CLAUDE.md`.

Si algo no aparece arriba, NO inventar — escalar al Thinker.

---

## REFERENCIA — FASES YA COMPLETADAS (no tocar salvo necesidad)

> Las fases 0/A/B/C/D/E del plan anterior estan implementadas. Esta seccion existe solo para que el Doer pueda volver a leerlas si necesita el detalle exacto de un metodo o un FXML que ya existe.

### Phase 0 — Extender ApiClient
- `getBytes(String path)` y `uploadFile(...)` — IMPLEMENTADO en `ApiClient.java`.

### Phase A — UI fixes
- A.1 Cabeceras Discapacidades/Tratamientos homologadas con Sanitarios — IMPLEMENTADO.
- A.2 Botones Aceptar/Cancelar en `VentanaFiltroTratamientos.fxml` — IMPLEMENTADO.
- A.3 Centrar textos en modales — IMPLEMENTADO via clase `modal-texto-centrado`.
- A.4 Fondo blanco en tema oscuro corregido con clase `modal-root` — IMPLEMENTADO.

### Phase B — PDF tratamiento
- DAO + Service + UI + RBAC + tests — IMPLEMENTADO.

### Phase C — Asociacion tratamiento-juego
- DAO + Service + tabla en formulario + diff alta/baja al guardar + RBAC — IMPLEMENTADO.

### Phase D — Visualizacion progreso
- DTOs + DAO + Service + GraficoUtil + FXML + controlador — IMPLEMENTADO. La fase I de esta iteracion solo verifica end-to-end y anade el boton en la ficha del paciente si faltara.

### Phase E — Polling 30s
- `SyncProgresoService` + cleanup en cierre — IMPLEMENTADO.

---

*Fin del plan. Doer: ejecuta G → H → I → J en orden. Si algo no aparece arriba, NO inventar — escalar al Thinker.*
