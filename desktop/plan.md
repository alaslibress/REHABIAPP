# Plan: Diagnostico y reparacion de la conexion Desktop ↔ API REST

> **Fecha:** 2026-04-07
> **Rama:** desktop-final
> **Autor (Thinker):** Agent 3 - Opus
> **Audiencia (Doer):** Agent 3 - Sonnet
> **Prerequisito:** El plan anterior (correccion de errores de ejecucion JavaFX desde IntelliJ) se asume completado segun el commit `17f79c6`. Si alguna fase de aquel plan no se hubiera aplicado, debe completarse antes de empezar este.

> ⚠ **ACTUALIZACION 2026-04-07 (post-FASE 0):** durante los smoke tests el Doer ha confirmado que los sanitarios actuales en BD tienen contrasenas en texto plano, lo que provoca **500 Internal Server Error** al hacer login. Ver la nueva subseccion **"Actualizacion 2026-04-07: reseed completo"** al inicio de la FASE 3, que **sustituye** la subseccion 3.3 original. La FASE 3 ha cambiado de objetivo: ya no es solo "sembrar usuarios" sino "borrar todos los usuarios actuales y dejar 3 sanitarios + 1 paciente de prueba bien formados".

---

## Contexto

Tras la migracion completa del desktop ERP de JDBC directo a consumo de la REST API central (`/api`), la aplicacion JavaFX:

- Compila correctamente con Gradle 8.14 + Java 24.
- Tiene 74 tests unitarios verdes (todos mockeados, ninguno integra contra una API real).
- Arranca sin errores en Linux despues de los ajustes recientes del plan IntelliJ.
- **No consigue establecer comunicacion real con la base de datos a traves de la API en runtime.**

El sintoma exacto que reporta el desarrollador es **"no conecta con la base de datos"**. Sin trazas no se puede afirmar si el fallo esta en:

1. La API no esta arrancada o no escucha en `localhost:8080`.
2. La API arranca pero no consigue conectar con PostgreSQL (mismatch de credenciales muy probable).
3. El desktop no carga `api.properties` y usa una URL incorrecta.
4. El desktop pega a la URL correcta pero no envia el JWT bien.
5. El usuario no existe en la BD o el hash BCrypt no coincide.
6. Otra causa todavia no contemplada.

Este plan establece una secuencia de fases ordenadas que permitiran al Doer aislar la causa raiz, repararla y dejar el sistema con visibilidad suficiente para que un fallo similar nunca vuelva a ser opaco.

---

## Hallazgos previos del Thinker (estado actual del codigo, ya verificado)

### En /desktop

| # | Fichero | Hallazgo | Severidad |
|---|---------|----------|-----------|
| H1 | `src/main/java/com/javafx/Clases/ApiClient.java:55` | URL base = `http://localhost:8080`, cargada desde `api.properties` con fallback hardcoded silencioso. | Alta (sin visibilidad) |
| H2 | `src/main/java/com/javafx/Clases/ApiClient.java:101-113` | `probarConexion()` pega a `/actuator/health` con timeout 5 s. Captura toda excepcion y devuelve `false` SIN log. | Critica |
| H3 | `src/main/java/com/javafx/Clases/ApiClient.java` (entero) | CERO trazas SLF4J / System.out / System.err en TODA la clase. `slf4j-simple` esta en build.gradle pero no se usa. | Critica |
| H4 | `src/main/java/com/javafx/Clases/ApiClient.java:47-54` | `cargarPropiedades()` silencia IOException. Si `api.properties` no esta en classpath, la app usa el fallback hardcoded sin avisar. | Alta |
| H5 | `src/main/java/com/javafx/DAO/PacienteDAO.java:163,165,182,184` | `insertarFoto()` y `obtenerFoto()` crean su propio `HttpClient` y hardcodean `http://localhost:8080/api/pacientes/{dni}/foto`. NO usan `ApiClient.baseUrl`. | Media (rompe en cualquier entorno != localhost) |
| H6 | `src/main/resources/config/api.properties` | Contenido correcto: `api.base-url=http://localhost:8080`, `api.timeout-ms=30000`. | OK |
| H7 | `src/main/resources/config/ip.properties` | Legacy, solo referencias a host PostgreSQL. No se usa. | Bajo (limpieza) |
| H8 | `src/main/resources/config/database.properties` | Legacy, credenciales JDBC plaintext. No se usa. | Bajo (limpieza) |
| H9 | `src/main/java/com/javafx/Interface/controladorSesion.java:189` | El mensaje de error al usuario menciona literalmente `localhost:8080`. Util para el usuario, pero hardcodea la URL en la UI. | Bajo |
| H10 | `build.gradle` | Limpio: sin `postgresql`, `HikariCP`, `jBCrypt`. Jackson y SLF4J presentes. | OK |
| H11 | `src/test/java/com/javafx/Clases/ApiClientTest.java` | 16 tests; todos con `HttpClient` mockeado. No se ejerce ningun extremo real. | OK pero insuficiente para integracion |

### En /api

| # | Fichero | Hallazgo | Implicacion para desktop |
|---|---------|----------|--------------------------|
| A1 | `src/main/resources/application.yml:39` | Puerto = `${SERVER_PORT:8080}`. Coincide con desktop. | OK |
| A2 | `src/main/java/com/rehabiapp/api/presentation/controller/AuthController.java:24` | `POST /api/auth/login`, payload `{dni, contrasena}`, respuesta `{accessToken, refreshToken, rol}` en body. | Coincide exactamente con lo que envia el desktop. |
| A3 | `src/main/java/com/rehabiapp/api/infrastructure/config/SecurityConfig.java:55-61` | `permitAll` en `/api/auth/**` y `/actuator/health/**`. Resto requiere JWT `Authorization: Bearer`. | Coincide con desktop. |
| A4 | `src/main/resources/application-local.yml:5-10` | `spring.datasource.url=jdbc:postgresql://localhost:5432/rehabiapp`, usuario `rehabiapp_dev`, password `dev_password_change_me`. | **Mismatch con docker-compose.** |
| A5 | `infra/docker-compose.yml:6-27` | El contenedor PG levanta con `POSTGRES_USER=admin`, `POSTGRES_PASSWORD=admin`, `POSTGRES_DB=rehabiapp`. | **Mismatch con application-local.yml. Sospechoso #1.** |
| A6 | `db/migration/V1..V4` | Esquema completo + Envers + `tratamiento.id_nivel`. | OK |
| A7 | `presentation/controller/*` | Todos los endpoints que el desktop consume existen con la firma esperada. | OK |
| A8 | `application.yml:48-68` | `actuator/health`, `liveness`, `readiness`, `prometheus` habilitados sin auth. | OK |

### Hipotesis ordenadas por probabilidad (segun el Thinker)

1. **(Muy probable)** La API falla al arrancar porque sus credenciales (`rehabiapp_dev`/`dev_password_change_me`) no coinciden con las del PostgreSQL del docker-compose (`admin`/`admin`). La app desktop intenta conectar y nunca obtiene respuesta porque la API ni siquiera levanta.
2. **(Probable)** La API arranca pero no hay ningun sanitario sembrado en la BD, el login devuelve 401, y la UI lo interpreta como "no hay conexion".
3. **(Probable)** `api.properties` no se empaqueta correctamente en el classpath y el desktop usa el fallback hardcoded silenciosamente. Si el desarrollador esta apuntando a una API remota, fallara.
4. **(Posible)** El usuario semilla existe pero su hash BCrypt no se generó con el cost factor 12 que espera la API.
5. **(Menos probable)** Algun firewall o iptables local en Fedora 43 esta bloqueando el puerto 8080.
6. **(Muy poco probable)** Un cambio en Spring Security 6 / Spring Boot 4 esta filtrando peticiones del HttpClient nativo de Java por User-Agent (no hay evidencia de esto en SecurityConfig pero merece descartarse).

El orden de las fases del plan refleja esta prioridad. La FASE 0 valida la hipotesis 1 y 2 antes de tocar codigo.

---

## FASE 0: Smoke tests del entorno (sin tocar codigo)

**Objetivo:** Antes de modificar nada, verificar que cada eslabon del stack esta vivo y reportar exactamente donde se rompe la cadena. Esta fase entrega un diagnostico, no un fix.

### 0.1 Verificar PostgreSQL

```bash
docker ps --filter "name=rehabiapp" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

- Si NO aparece ningun contenedor: levantarlo con `cd /home/alaslibres/DAM/RehabiAPP && docker compose -f infra/docker-compose.yml up -d postgres` (o el nombre de servicio que corresponda).
- Si aparece pero no esta `Up`: `docker logs <nombre_contenedor>` y reportar las ultimas 50 lineas al thinker.
- Si esta `Up`, validar conexion directa con `psql`:

```bash
PGPASSWORD=admin psql -h localhost -p 5432 -U admin -d rehabiapp -c "\dt"
```

- Reportar al thinker: numero de tablas listadas, contenido de `sanitario` (`SELECT dni_san, email_san, activo FROM sanitario LIMIT 5;`).

### 0.2 Verificar arranque de la API

```bash
curl -v --max-time 5 http://localhost:8080/actuator/health
```

- Si responde `200 {"status":"UP"}` -> la API esta viva. Saltar a 0.4.
- Si responde `Connection refused` o `Connection reset` -> la API no esta arrancada. Continuar en 0.3.

### 0.3 Arrancar la API en modo verboso

**No usar `nohup`. El doer debe ver los logs en vivo en una terminal dedicada.**

```bash
cd /home/alaslibres/DAM/RehabiAPP/api
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Observar la consola hasta una de estas situaciones:

| Sintoma en consola | Diagnostico | Accion del doer |
|--------------------|-------------|-----------------|
| `Started ApiApplication in X seconds` y queda escuchando | API OK | Continuar a 0.4 |
| `FATAL: password authentication failed for user "rehabiapp_dev"` | **Mismatch de credenciales (hipotesis #1 confirmada)** | Saltar a 0.3.bis |
| `Connection refused` al puerto 5432 | PostgreSQL no esta corriendo | Volver a 0.1 |
| `Flyway migration failed` | Migracion de Flyway en conflicto con datos existentes | Reportar al thinker la version de migracion que falla |
| `Address already in use: 8080` | Algo ya ocupa el puerto 8080 | `ss -tlnp | grep 8080` y reportar el proceso |

#### 0.3.bis Si el problema son las credenciales

Hay tres opciones, ordenadas por preferencia del thinker:

**Opcion A (recomendada):** alinear via variables de entorno sin tocar yml ni docker-compose. Crear `/home/alaslibres/DAM/RehabiAPP/api/.env.local` (NO commitear, añadir al `.gitignore` si no esta) con:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/rehabiapp
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=admin
SPRING_PROFILES_ACTIVE=local
```

Luego arrancar con:

```bash
cd /home/alaslibres/DAM/RehabiAPP/api
set -a && source .env.local && set +a && ./mvnw spring-boot:run
```

**Opcion B:** modificar el contenedor PG para que use `rehabiapp_dev/dev_password_change_me`. Requiere `docker compose down -v` (DESTRUCTIVO: borra el volumen). El doer NO debe ejecutar esto sin aprobacion explicita del thinker porque destruye datos.

**Opcion C:** modificar `application-local.yml` directamente. **Prohibido para el doer**: tocar `/api` esta fuera del dominio del Agente 3. Si la opcion A no funciona, escalar al thinker y este coordinara con Agente 1.

### 0.4 Test manual de login con curl

Una vez la API responda al health, verificar que existe al menos un sanitario activo en la BD (ver 0.1) y probar el endpoint de login:

```bash
curl -v -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"dni":"<DNI_REAL_DE_LA_BD>","contrasena":"<CONTRASENA_CONOCIDA>"}'
```

| Status | Diagnostico | Siguiente paso |
|--------|-------------|----------------|
| 200 con `{accessToken, refreshToken, rol}` | Camino feliz, API operativa | Continuar a FASE 1 |
| 401 Unauthorized | Usuario inexistente o BCrypt incorrecto | Saltar a FASE 3 (semilla de datos) |
| 400 Bad Request | Payload mal formado (no deberia ocurrir con curl literal) | Reportar body de respuesta al thinker |
| 500 Internal Server Error | Fallo en la API. Mirar logs de la terminal donde corre `mvn spring-boot:run` | Reportar excepcion completa al thinker |
| Sin respuesta / timeout | La API se cayo entre 0.3 y 0.4 | Volver a 0.3 |

### 0.5 Reporte de la FASE 0

El doer debe entregar al thinker un reporte breve con:

- Estado de PG (Up / Down / contenido de sanitario).
- Estado de la API (Up / Down / log de arranque).
- Resultado del curl de login (status + body).
- Si se usaron variables de entorno, cuales.
- Conclusion: que hipotesis quedo confirmada.

**No avanzar a FASE 1 sin que el thinker valide el reporte.**

### Criterio de aceptacion de la FASE 0

- [ ] PG corriendo y `sanitario` con al menos 1 fila activa.
- [ ] API responde 200 a `/actuator/health`.
- [ ] curl de login devuelve 200 con un JWT valido.
- [ ] Reporte entregado al thinker.

---

## FASE 1: Habilitar logging diagnostico en ApiClient

**Objetivo:** dar visibilidad runtime a TODAS las llamadas HTTP del desktop. Sin esto cualquier futuro diagnostico volveria a ser ciego.

**Justificacion (thinker):** el problema central que motivo este plan es que `probarConexion()` devuelve `false` y la app dice "no hay conexion" sin que nadie pueda saber por que. Esto es una deuda tecnica de observabilidad que debe pagarse antes que cualquier otra cosa, porque sin trazas no podemos validar las fases siguientes.

### 1.1 Anadir SLF4J a ApiClient

`src/main/java/com/javafx/Clases/ApiClient.java`

- Importar `org.slf4j.Logger` y `org.slf4j.LoggerFactory`.
- Declarar `private static final Logger LOG = LoggerFactory.getLogger(ApiClient.class);`.
- Anadir trazas en estos puntos exactos:

| Metodo | Nivel | Mensaje sugerido | Datos a incluir |
|--------|-------|------------------|-----------------|
| Constructor | INFO | `"ApiClient inicializado: baseUrl={}, timeoutMs={}"` | `baseUrl`, `timeoutMs` |
| `cargarPropiedades()` exito | INFO | `"api.properties cargado correctamente"` | ruta del recurso |
| `cargarPropiedades()` fallo | ERROR | `"No se pudo cargar api.properties, usando fallback hardcoded {}"` | URL fallback, `e.getMessage()` |
| `probarConexion()` antes | DEBUG | `"Probando conexion a {}"` | URL completa |
| `probarConexion()` exito | INFO | `"Conexion API verificada: {} -> {}"` | URL, statusCode |
| `probarConexion()` excepcion | ERROR | `"Conexion API fallida: {} -> {}"` | URL, `e.getClass().getSimpleName() + ": " + e.getMessage()` |
| `login()` antes | INFO | `"Login: POST {}/api/auth/login (dni={})"` | baseUrl, dni |
| `login()` 200 | INFO | `"Login OK para {}, rol={}"` | dni, rol |
| `login()` 4xx/5xx | WARN | `"Login fallido para {}: status={} body={}"` | dni, statusCode, body recortado a 500 chars |
| `login()` excepcion red | ERROR | `"Login network error: {}"` | clase + mensaje de excepcion |
| `get/post/put/delete()` | DEBUG | `"{} {} -> {}"` | metodo, URL completa, statusCode |
| Cualquier 4xx/5xx | WARN | `"Error HTTP {} en {} {}: body={}"` | status, metodo, URL, body recortado a 500 chars |

**Reglas para el doer:**

- Las trazas DEBEN estar en castellano.
- NO loggear el contenido completo del body de respuestas exitosas (puede contener datos clinicos en otros endpoints).
- NO loggear nunca la contrasena en plano (ni siquiera en DEBUG).
- NO loggear nunca el accessToken o refreshToken completos. Si quieres trazar su existencia, usa `accessToken != null ? "presente" : "ausente"`.
- Los logs van a stdout via `slf4j-simple`. NO inventar appenders ni anadir logback.

### 1.2 Configuracion de SimpleLogger

Crear `src/main/resources/simplelogger.properties`:

```
org.slf4j.simpleLogger.defaultLogLevel=info
org.slf4j.simpleLogger.log.com.javafx.Clases.ApiClient=debug
org.slf4j.simpleLogger.showDateTime=true
org.slf4j.simpleLogger.dateTimeFormat=yyyy-MM-dd HH:mm:ss
org.slf4j.simpleLogger.showThreadName=true
org.slf4j.simpleLogger.showShortLogName=true
```

Esto da:
- INFO global para la app.
- DEBUG solo para `ApiClient` (donde lo necesitamos).
- Sin logs spammy del resto.

### 1.3 Verificar tests existentes

`./gradlew test` despues del cambio. **Los 74 tests deben seguir pasando**. Si algun `ApiClientTest` rompe porque ahora hay logs en stdout, NO silenciar el log; corregir el assert para que ignore stdout.

### 1.4 Entregable de la FASE 1

- Diff aplicado a `ApiClient.java`.
- Nuevo `simplelogger.properties`.
- Captura de consola del primer arranque tras el cambio mostrando las trazas.

### Criterio de aceptacion de la FASE 1

- [ ] `./gradlew run` arranca y emite por stdout: `ApiClient inicializado: baseUrl=http://localhost:8080, timeoutMs=30000`.
- [ ] Tras pulsar el indicador de conexion del login, aparece traza `Probando conexion a http://localhost:8080/actuator/health` seguida de `Conexion API verificada` o `Conexion API fallida` con la causa real.
- [ ] `./gradlew test` -> 74 tests verdes.

---

## FASE 2: Verificar que `api.properties` se carga en runtime

**Objetivo:** descartar la hipotesis #3. Aunque el fichero existe en source, no esta confirmado que Gradle lo empaquete y que el classloader lo encuentre.

### 2.1 Confirmar empaquetado en el JAR

```bash
cd /home/alaslibres/DAM/RehabiAPP/desktop
./gradlew clean jar
unzip -l build/libs/desktop.jar | grep -E "(api\.properties|simplelogger)"
```

Debe listar ambos ficheros. Si NO aparecen:

- Revisar `build.gradle` bloque `processResources` o `sourceSets.main.resources.srcDirs`.
- Hallazgo H7/H8: si el bloque excluye explicitamente `**/*.properties`, esa es la causa.
- Reportar al thinker antes de modificar build.gradle (podria romper la inclusion de `*.jrxml` de JasperReports).

### 2.2 Confirmar carga en runtime

Tras la FASE 1 ya tenemos el log del constructor. El doer debe arrancar la app y verificar:

```
ApiClient inicializado: baseUrl=http://localhost:8080, timeoutMs=30000
api.properties cargado correctamente
```

Si en su lugar aparece:

```
No se pudo cargar api.properties, usando fallback hardcoded http://localhost:8080
```

entonces hay un problema de classpath. Posibles causas y orden de investigacion:

1. El recurso esta en `src/main/resources/config/api.properties` pero el codigo lo busca en `/api.properties` (sin `/config/`) o viceversa. Verificar la ruta exacta en `ApiClient.cargarPropiedades()`.
2. `getClass().getResourceAsStream(...)` vs `getClass().getClassLoader().getResourceAsStream(...)`: la primera busca relativa al paquete, la segunda absoluta. Confirmar cual se usa y si la ruta es coherente.

### 2.3 Endurecer la carga (mejora opcional pero recomendada)

Anadir un fallback adicional por variable de entorno:

```java
// pseudo-codigo, el doer lo escribe limpio
String envUrl = System.getenv("REHABIAPP_API_URL");
if (envUrl != null && !envUrl.isBlank()) {
    baseUrl = envUrl;
    LOG.info("baseUrl override desde REHABIAPP_API_URL = {}", baseUrl);
}
```

**Justificacion (thinker):** facilita pruebas en distintas maquinas (local, AWS, CI) sin recompilar. Es una mejora pequena de portabilidad.

### Criterio de aceptacion de la FASE 2

- [ ] `desktop.jar` contiene `config/api.properties` y `simplelogger.properties`.
- [ ] El log de arranque confirma la carga del fichero.
- [ ] Si se aplica 2.3, el override por env funciona (probado lanzando con `REHABIAPP_API_URL=http://test:9090 ./gradlew run` y verificando el log).

---

## FASE 3: Sembrar usuarios de prueba en la BD

**Objetivo:** validar el extremo /api/auth/login con credenciales reales reproducibles. Resuelve la hipotesis #2 y #4.

**Importante:** esto es responsabilidad del Doer del Agente 3 SOLO en la parte de generacion del SQL y documentacion. La modificacion del esquema de migraciones de Flyway pertenece al Agente 1. El Doer entregara el script al thinker, que decidira si delega al Agente 1 o lo aplica como script ad-hoc local.

---

### Actualizacion 2026-04-07: reseed completo (SUSTITUYE a la subseccion 3.3 original)

#### Hallazgo durante la ejecucion de FASE 0

Al ejecutar los smoke tests, el Doer Sonnet descubrio que:

1. PG levanta correctamente con `admin/admin` (docker-compose).
2. La API arranca correctamente con `.env.local` apuntando a esas credenciales (FASE 0.3 Opcion A aplicada con exito).
3. `/actuator/health` responde 200 OK (UP).
4. **Pero el login con curl devuelve `500 Internal Server Error`**, no 401.

La causa raiz: los 3 sanitarios sembrados en la BD por desarrollo previo tienen las contrasenas almacenadas en **texto plano**:

| dni_san | contrasena_san (plain) |
|---------|------------------------|
| ADMIN0000 | admin |
| 78834700J | alejandro123 |
| 11111111B | maria123 |

`AuthApplicationService` invoca `BCrypt.checkpw(input, storedHash)` y BCrypt **lanza `IllegalArgumentException`** porque "admin" no tiene la estructura `$2a$12$...`. La excepcion sube al `GlobalExceptionHandler` y se traduce a 500 con body `{"error":"Error interno del servidor"}`.

Esto confirma que la causa raiz original de "el desktop no conecta" es una combinacion de:
- (resuelto en FASE 0.3) Mismatch de credenciales API <-> PG.
- (pendiente, esta seccion) Datos de usuario incorrectos en BD.

#### Decision del Thinker (Agente 3): reseed completo

**Borramos los tres usuarios actuales y rehacemos la semilla** con un conjunto minimo de usuarios de prueba que cumplan TODAS las constraints del esquema y tengan contrasenas BCrypt validas. Esto sustituye lo descrito en la subseccion 3.3 original (que queda marcada como SUPERSEDED y se conserva mas abajo solo como referencia historica).

#### Composicion del nuevo seed (4 entidades: 3 sanitarios + 1 paciente)

| Tipo | DNI | Rol | Credenciales | Proposito |
|------|-----|-----|--------------|-----------|
| Sanitario | `admin0000` | SPECIALIST | `admin0000` / `admin` | Usuario administrador unico para acceso pleno durante desarrollo |
| Sanitario | (DNI espanol valido inventado) | SPECIALIST | DNI / `medico1234` | Medico especialista de ejemplo |
| Sanitario | (DNI espanol valido inventado) | NURSE | DNI / `enfermero1234` | Enfermero de ejemplo |
| Paciente | (DNI espanol valido inventado) | n/a | n/a (no hace login) | Paciente de ejemplo asignado al medico especialista |

**Aclaraciones importantes para el Doer:**

- En este sistema **no existe un rol "ADMIN" distinto**. Solo hay `SPECIALIST` y `NURSE` (segun `SecurityConfig.java` y la columna `cargo` de `sanitario_agrega_sanitario`). El "admin" es simplemente un sanitario con rol `SPECIALIST` cuyo DNI es `admin0000`.
- DNI `admin0000` no cumple el formato DNI espanol estandar (8 digitos + letra). El esquema actual NO enforza CHECK de formato sobre `dni_san`, asi que se acepta tal cual. Si el doer encuentra una constraint del esquema que lo bloquee, **escalar al thinker antes de modificar nada del esquema**.
- Los demas DNIs deben inventarse con formato espanol valido (8 digitos + letra, letra calculada con el algoritmo modulo 23). El doer puede usar la tabla estandar `T R W A G M Y F P D X B N J Z S Q V H L C K E` (resto de dividir el numero entre 23 -> letra correspondiente).
- Nombres, apellidos, emails, telefonos, direcciones — todo inventado, plausible, en castellano, sin emojis, sin datos reales de personas.
- `email_san` (en sanitario) y `email_pac` + `num_ss` (en paciente) son UNIQUE.
- Los emails no deben colisionar con los actuales (`admin@rehabiapp.com`, `alejandro.pozo@rehabiapp.com`, `maria.lopez@rehabiapp.com`). Como ademas vamos a borrar esos usuarios, tampoco habra colision posible.

#### Pasos del Doer (sustituyen 3.1, 3.2, 3.3, 3.4 originales)

**Paso A — Generar hashes BCrypt cost 12** (comando ya verificado funcional en la FASE 0):

```bash
htpasswd -nbBC 12 "" admin          | tr -d ':\n' | sed 's/\$2y\$/\$2a\$/'
htpasswd -nbBC 12 "" medico1234     | tr -d ':\n' | sed 's/\$2y\$/\$2a\$/'
htpasswd -nbBC 12 "" enfermero1234  | tr -d ':\n' | sed 's/\$2y\$/\$2a\$/'
```

Guardar los tres hashes para usarlos en el script SQL. Si htpasswd cambia los hashes en cada llamada (es BCrypt con sal aleatoria), eso es esperado y correcto.

**Paso B — Crear `desktop/scripts/reseed-dev.sql`** con la siguiente estructura:

```sql
-- Reseed de datos de desarrollo para el desktop SGE.
-- SOLO para desarrollo local. NO ejecutar en produccion.
-- Genera: 3 sanitarios (admin SPECIALIST, medico SPECIALIST, enfermero NURSE) + 1 paciente.
-- Credenciales documentadas en desktop/scripts/README.md.

BEGIN;

-- 1. Limpiar datos previos en orden FK-safe
DELETE FROM cita;
DELETE FROM telefono_paciente;
DELETE FROM telefono_sanitario;
DELETE FROM paciente_tratamiento;     -- si existe la tabla
DELETE FROM paciente_discapacidad;    -- si existe la tabla
DELETE FROM paciente;
DELETE FROM sanitario_agrega_sanitario;
DELETE FROM sanitario;

-- 2. Asegurar localidad y CP de prueba
INSERT INTO localidad (nombre_localidad, provincia)
  VALUES ('Madrid', 'Madrid')
  ON CONFLICT (nombre_localidad) DO NOTHING;

INSERT INTO cp (cp, nombre_localidad)
  VALUES ('28001', 'Madrid')
  ON CONFLICT (cp) DO NOTHING;

-- 3. Direccion de prueba (capturamos id_direccion para el paciente)
WITH dir AS (
  INSERT INTO direccion (calle, numero, piso, cp)
  VALUES ('Calle de la Prueba', '1', '1A', '28001')
  RETURNING id_direccion
)
SELECT id_direccion FROM dir;
-- El doer puede tambien hacer un INSERT independiente y leer el currval/RETURNING.

-- 4. Sanitarios
INSERT INTO sanitario (
  dni_san, nombre_san, apellido1_san, apellido2_san, email_san,
  num_de_pacientes, contrasena_san, activo
) VALUES
  ('admin0000', 'Admin',  'Sistema',  NULL,    'admin@rehabiapp.local',
   0, '<HASH_BCRYPT_admin>', TRUE),
  ('<DNI_MEDICO>', 'Carlos', 'Garcia', 'Lopez', 'carlos.garcia@rehabiapp.local',
   1, '<HASH_BCRYPT_medico1234>', TRUE),
  ('<DNI_ENFERMERO>', 'Lucia', 'Martinez', 'Ruiz', 'lucia.martinez@rehabiapp.local',
   0, '<HASH_BCRYPT_enfermero1234>', TRUE);

INSERT INTO sanitario_agrega_sanitario (dni_san, cargo) VALUES
  ('admin0000',     'SPECIALIST'),
  ('<DNI_MEDICO>',  'SPECIALIST'),
  ('<DNI_ENFERMERO>','NURSE');

-- 5. Telefonos (opcional pero recomendado para que la UI no muestre listas vacias)
INSERT INTO telefono_sanitario (dni_san, telefono) VALUES
  ('admin0000',     '600000000'),
  ('<DNI_MEDICO>',  '600111111'),
  ('<DNI_ENFERMERO>','600222222');

-- 6. Paciente de prueba asignado al MEDICO ESPECIALISTA (no al admin)
-- IMPORTANTE: alergias / antecedentes / medicacion_actual se dejan NULL
-- porque la app los espera cifrados con AES-256-GCM. Si el seed los pone en
-- texto plano, el ApiClient/Service revientan al deserializar.
INSERT INTO paciente (
  dni_pac, dni_san, nombre_pac, apellido1_pac, apellido2_pac, edad_pac,
  email_pac, num_ss, id_direccion,
  discapacidad_pac, tratamiento_pac, estado_tratamiento,
  protesis, fecha_nacimiento, sexo,
  alergias, antecedentes, medicacion_actual,
  consentimiento_rgpd, fecha_consentimiento, activo
) VALUES (
  '<DNI_PACIENTE>', '<DNI_MEDICO>', 'Pedro', 'Sanchez', 'Gomez', 45,
  'pedro.sanchez@example.local', '281234567890',
  (SELECT MAX(id_direccion) FROM direccion),
  NULL, NULL, NULL,
  FALSE, '1980-05-15', 'H',
  NULL, NULL, NULL,
  TRUE, '2026-04-07', TRUE
);

COMMIT;

-- Verificacion final
SELECT dni_san, email_san, activo FROM sanitario ORDER BY dni_san;
SELECT s.dni_san, sas.cargo FROM sanitario s
  LEFT JOIN sanitario_agrega_sanitario sas USING (dni_san)
  ORDER BY s.dni_san;
SELECT dni_pac, dni_san, activo FROM paciente;
```

**Notas tecnicas para el Doer al rellenar el script:**

- Sustituir los 4 placeholders `<DNI_MEDICO>`, `<DNI_ENFERMERO>`, `<DNI_PACIENTE>`, y los 3 `<HASH_BCRYPT_*>` por valores reales antes de ejecutar.
- Si el esquema de `sexo` usa CHECK con valores distintos a `'H'` / `'M'` (puede ser `'HOMBRE'` / `'MUJER'` o codigos numericos), el doer debe inspeccionar `V1__esquema_core.sql` y ajustar.
- Si la tabla `paciente_discapacidad` o `paciente_tratamiento` aun no existe (segun checklist de `desktop/CLAUDE.md` seccion 7 estaban pendientes de crear), comentar el `DELETE` correspondiente con `--`.
- Si alguna FK impide el orden de borrado propuesto, NO usar `TRUNCATE ... CASCADE`. Reordenar los DELETE manualmente segun el grafo de dependencias y, si no se puede, escalar al thinker.
- El bloque `WITH dir AS ...` del paso 3 es ilustrativo; el doer puede simplificarlo a un `INSERT ... RETURNING` separado y luego usar `currval('direccion_id_direccion_seq')` o un `SELECT MAX(id_direccion)`.
- **NO borrar:** `discapacidad`, `tratamiento`, `discapacidad_tratamiento`, `nivel_progresion`, `localidad`, `cp`, `direccion`. Son catalogos / datos de soporte que pueden reusarse. Si el doer quiere empezar 100% limpio, escalar al thinker antes.

**Paso C — Aplicar el script** en la BD via docker exec:

```bash
docker exec -i rehabiapp-db psql -U admin -d rehabiapp \
  < /home/alaslibres/DAM/RehabiAPP/desktop/scripts/reseed-dev.sql
```

Capturar la salida completa. Cualquier error de `ERROR:` debe pararse y reportarse al thinker antes de continuar.

**Paso D — Verificar con curl** los 3 logins:

```bash
# admin
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"dni":"admin0000","contrasena":"admin"}' | jq .

# medico ejemplo
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"dni":"<DNI_MEDICO>","contrasena":"medico1234"}' | jq .

# enfermero ejemplo
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"dni":"<DNI_ENFERMERO>","contrasena":"enfermero1234"}' | jq .
```

Los tres deben devolver 200 con `accessToken`, `refreshToken` y el `rol` correcto (`SPECIALIST`, `SPECIALIST`, `NURSE`).

**Paso E — Documentar credenciales** en `desktop/scripts/README.md`:

```markdown
# Scripts de desarrollo

## reseed-dev.sql

Borra todos los sanitarios y pacientes actuales y crea un seed minimo
para desarrollo local. **NO USAR EN PRODUCCION.**

### Aplicar

    docker exec -i rehabiapp-db psql -U admin -d rehabiapp < desktop/scripts/reseed-dev.sql

### Credenciales generadas

| DNI | Password | Rol | Tipo |
|-----|----------|-----|------|
| admin0000 | admin | SPECIALIST | Sanitario admin |
| <DNI_MEDICO> | medico1234 | SPECIALIST | Sanitario medico ejemplo |
| <DNI_ENFERMERO> | enfermero1234 | NURSE | Sanitario enfermero ejemplo |
| <DNI_PACIENTE> | n/a (no logea) | n/a | Paciente ejemplo asignado al medico |

Las contrasenas se almacenan como hash BCrypt cost 12.
Las contrasenas en plano de esta tabla son SOLO para desarrollo local.
```

#### Criterio de aceptacion del reseed

- [ ] `SELECT COUNT(*) FROM sanitario;` devuelve **exactamente 3**.
- [ ] `SELECT COUNT(*) FROM paciente;` devuelve **exactamente 1**.
- [ ] Los 3 sanitarios tienen `activo = TRUE` y un `cargo` valido en `sanitario_agrega_sanitario`.
- [ ] Los 3 logins curl devuelven 200 con el rol correcto.
- [ ] No queda rastro de los usuarios anteriores (`ADMIN0000`, `78834700J`, `11111111B`).
- [ ] `desktop/scripts/reseed-dev.sql` y `desktop/scripts/README.md` estan creados.
- [ ] `./gradlew test` sigue verde (no se han tocado tests, deben seguir 74).

#### Que pasa con la subseccion 3.3 original

La subseccion 3.3 original ("Script de seed" con `00000001A` y `00000002B`) queda **SUPERSEDED**. **El doer NO debe ejecutarla.** Se conserva mas abajo en el documento solo como referencia historica del razonamiento previo del thinker.

---

### 3.1 Comprobar estado actual de `sanitario`

```bash
PGPASSWORD=admin psql -h localhost -p 5432 -U admin -d rehabiapp \
  -c "SELECT COUNT(*) FROM sanitario;"
```

- Si > 0, listar para ver si hay alguno usable: `SELECT dni_san, email_san, activo FROM sanitario LIMIT 10;`
- Si = 0, generar script de seed (3.2).

### 3.2 Generar hash BCrypt

El doer NO debe inventar hashes. Generarlos con un comando reproducible:

```bash
cd /home/alaslibres/DAM/RehabiAPP/desktop
./gradlew -q --console=plain --no-daemon \
  -Pmain=com.javafx.util.GenerarHashBCrypt run --args='dev1234'
```

Si la utility class `GenerarHashBCrypt` no existe (y NO existe en el codigo actual porque jBCrypt fue eliminado), el doer **no** debe recrearla en el desktop. En su lugar, usar el comando equivalente desde la API que sigue teniendo BCrypt:

```bash
cd /home/alaslibres/DAM/RehabiAPP/api
./mvnw -q exec:java \
  -Dexec.mainClass=org.springframework.security.crypto.bcrypt.BCrypt \
  -Dexec.args="dev1234"
```

O alternativamente, usar `htpasswd -bnBC 12 "" dev1234 | tr -d ':\n' | sed 's/$2y/$2a/'` si htpasswd esta instalado.

Documentar exactamente que comando funciono y guardar los hashes resultantes.

### 3.3 Script de seed  ⚠ SUPERSEDED por la "Actualizacion 2026-04-07: reseed completo" al inicio de la FASE 3. NO EJECUTAR. Conservada solo como referencia historica.

Crear `/home/alaslibres/DAM/RehabiAPP/desktop/scripts/seed-dev.sql` (nuevo fichero, NO va a Flyway):

```sql
-- Seed de datos de desarrollo SOLO para pruebas locales del desktop.
-- NO ejecutar en produccion.
-- Usuarios: dev_specialist / dev1234, dev_nurse / nurse1234

-- direccion ficticia
INSERT INTO localidad (nombre_localidad, provincia) VALUES ('Madrid', 'Madrid')
  ON CONFLICT DO NOTHING;
INSERT INTO cp (cp, nombre_localidad) VALUES ('28001', 'Madrid')
  ON CONFLICT DO NOTHING;

-- sanitario especialista
INSERT INTO sanitario (
  dni_san, nombre_san, apellido1_san, apellido2_san, email_san,
  num_de_pacientes, contrasena_san, activo
) VALUES (
  '00000001A', 'Dev', 'Specialist', NULL, 'dev.specialist@local',
  0, '<HASH_BCRYPT_DE_dev1234>', TRUE
) ON CONFLICT (dni_san) DO NOTHING;

INSERT INTO sanitario_agrega_sanitario (dni_san, cargo)
  VALUES ('00000001A', 'SPECIALIST') ON CONFLICT DO NOTHING;

-- sanitario enfermero
INSERT INTO sanitario (
  dni_san, nombre_san, apellido1_san, apellido2_san, email_san,
  num_de_pacientes, contrasena_san, activo
) VALUES (
  '00000002B', 'Dev', 'Nurse', NULL, 'dev.nurse@local',
  0, '<HASH_BCRYPT_DE_nurse1234>', TRUE
) ON CONFLICT (dni_san) DO NOTHING;

INSERT INTO sanitario_agrega_sanitario (dni_san, cargo)
  VALUES ('00000002B', 'NURSE') ON CONFLICT DO NOTHING;
```

El doer sustituira `<HASH_BCRYPT_DE_xxx>` por los hashes reales generados en 3.2.

### 3.4 Ejecutar el script

```bash
PGPASSWORD=admin psql -h localhost -p 5432 -U admin -d rehabiapp \
  -f /home/alaslibres/DAM/RehabiAPP/desktop/scripts/seed-dev.sql
```

Validar:

```bash
PGPASSWORD=admin psql -h localhost -p 5432 -U admin -d rehabiapp \
  -c "SELECT dni_san, email_san, activo FROM sanitario WHERE activo = TRUE;"
```

### 3.5 Re-test del login con curl

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"dni":"00000001A","contrasena":"dev1234"}' | jq .
```

Debe devolver `accessToken`, `refreshToken`, `rol="SPECIALIST"`.

### Criterio de aceptacion de la FASE 3

- [ ] Tabla `sanitario` contiene al menos los dos usuarios seed.
- [ ] curl al login devuelve 200 con JWT.
- [ ] El script `seed-dev.sql` queda commiteado en `desktop/scripts/` con su README explicando como ejecutarlo.

---

## FASE 4: Prueba end-to-end del login desde la UI JavaFX

**Objetivo:** validar que el flujo completo Desktop -> API -> PG funciona desde la interfaz, no solo desde curl.

### 4.1 Setup del entorno

Tres terminales en paralelo:

| Terminal | Comando | Que observar |
|----------|---------|--------------|
| 1 | `docker compose -f infra/docker-compose.yml up postgres` (sin `-d`) | Logs de PG |
| 2 | `cd api && SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run` (con `.env.local` cargado) | Logs de la API. Si quieres mas verbosidad, anadir `--debug` |
| 3 | `cd desktop && ./gradlew run` | Logs del desktop incluyendo trazas SLF4J de la FASE 1 |

### 4.2 Subir nivel de log de Spring Security (temporal, en el lado API)

**Restriccion:** el Doer del Agente 3 NO toca `application-local.yml`. En su lugar, usar variable de entorno al arrancar:

```bash
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=DEBUG \
  ./mvnw spring-boot:run
```

Esto es solo para la sesion de pruebas; al cerrar la terminal vuelve a INFO.

### 4.3 Caso de prueba: login

1. En la ventana de login, observar el indicador de conexion. Debe pintarse en verde tras 1-2 segundos. Si esta rojo, mirar la traza `Probando conexion a ...` en la terminal 3 para ver la causa exacta.
2. Introducir `00000001A` / `dev1234`.
3. Pulsar Iniciar Sesion.
4. Comportamiento esperado:
   - Terminal 3 (desktop): traza `Login: POST http://localhost:8080/api/auth/login (dni=00000001A)`, seguida de `Login OK para 00000001A, rol=SPECIALIST`.
   - Terminal 2 (API): peticion 200 al endpoint con tiempo de respuesta.
   - UI: se cierra el login y abre la VentanaPrincipal.

### 4.4 Caso de prueba: listado de pacientes

Tras el login, navegar al tab de pacientes. Debe ejecutarse `GET /api/pacientes?page=0&size=10000&sort=nombrePac,asc`.

- Si la tabla esta vacia (porque no hay pacientes en BD), eso es OK; lo importante es que la peticion devuelva 200, no que haya datos.
- Si devuelve 401, el JWT no se esta enviando. Revisar `ApiClient.get()` y el header `Authorization`.
- Si devuelve 403, el rol no tiene permiso (no deberia ocurrir con SPECIALIST).
- Si revuelve 500, mirar logs API.

### 4.5 Caso de prueba: cierre de sesion

Cerrar sesion -> volver al login. El indicador debe seguir en verde. Reintentar con el usuario NURSE para verificar que el RBAC funciona (la pestana de sanitarios debe estar oculta).

### 4.6 Captura de evidencias

El doer debe guardar:

- Captura del terminal 3 mostrando la secuencia completa de logs del login + listado de pacientes.
- Captura del terminal 2 mostrando las peticiones recibidas en la API.
- Screenshot de la app abierta tras el login (sin datos sensibles).

Estas evidencias acompanan el reporte final al thinker.

### Criterio de aceptacion de la FASE 4

- [ ] Login con SPECIALIST exitoso desde la UI.
- [ ] Login con NURSE exitoso desde la UI.
- [ ] Listado de pacientes carga sin error.
- [ ] Cierre de sesion limpio.
- [ ] Trazas SLF4J coherentes en cada paso.

---

## FASE 5: Eliminar URL hardcoded en `PacienteDAO` (fotos)

**Objetivo:** corregir el hallazgo H5. Hoy las fotos solo funcionan si la API esta literalmente en `localhost:8080`. Si manana movemos la API a otra IP o puerto, las fotos romperian sin avisar.

### 5.1 Exponer baseUrl en ApiClient

`src/main/java/com/javafx/Clases/ApiClient.java`:

- Si no existe ya, anadir `public String getBaseUrl() { return baseUrl; }`.
- No exponer setter publico.

### 5.2 Refactorizar `PacienteDAO.insertarFoto` y `obtenerFoto`

`src/main/java/com/javafx/DAO/PacienteDAO.java:146-197`:

- Sustituir la URL hardcoded `"http://localhost:8080/api/pacientes/" + dni + "/foto"` por `apiClient.getBaseUrl() + "/api/pacientes/" + dni + "/foto"`.
- Usar el campo `apiClient` ya inyectado en el constructor; no usar `ApiClient.getInstancia()` desde dentro del metodo (los tests ya inyectan via constructor).
- Si el doer considera que el HttpClient local sigue siendo necesario porque ApiClient no soporta multipart, esto es aceptable, PERO debe documentarse con un comentario en castellano que explique el por que.

### 5.3 Inyectar el JWT en las llamadas de fotos

Hoy estas llamadas no envian `Authorization: Bearer`, por lo que la API las rechazara con 401 (los endpoints de foto requieren `isAuthenticated()`). Comprobar y, si es asi, anadir el header tomandolo de `apiClient.getAccessToken()` (anadir getter package-private si no existe).

### 5.4 Tests

Anadir a `PacienteDAOTest`:

- `insertarFoto_usaBaseUrlInyectada()`: configura mock con baseUrl `http://test:9090`, llama insertarFoto, captura la URL real construida y assertEquals.
- `insertarFoto_envia_authorization_header()`: configura accessToken en el mock, captura el HttpRequest y verifica `Authorization` presente.
- Ditto para `obtenerFoto`.

### Criterio de aceptacion de la FASE 5

- [ ] `grep -rn "localhost:8080" desktop/src/main/java/` devuelve solo el mensaje de UI de `controladorSesion` (H9).
- [ ] Tests nuevos verdes.
- [ ] Foto sube y se descarga desde la UI tras setear `REHABIAPP_API_URL=http://localhost:8080` (no se rompe el camino feliz).

---

## FASE 6: Limpieza de configuracion legacy

**Objetivo:** eliminar ficheros que ya no se usan y que generan confusion para futuros agentes (hallazgos H7, H8).

### 6.1 Eliminar `ip.properties`

```bash
cd /home/alaslibres/DAM/RehabiAPP/desktop
git rm src/main/resources/config/ip.properties
```

Antes de borrar, **verificar con grep** que ningun .java lo referencia:

```bash
```

(Usar el tool Grep, no bash.) Buscar `ip.properties`, `local.ip`, `local.port` en `src/`. Esperado: ninguna ocurrencia.

### 6.2 Eliminar `database.properties`

Mismo procedimiento. Antes de borrar, grep para `database.properties`, `db.url`, `db.usuario`, `db.password`, `db.driver` en `src/`. Esperado: ninguna ocurrencia tras la migracion.

### 6.3 Verificar `.gitignore`

Asegurar que `cifrado.properties` sigue listado (no se debe versionar). El nuevo fichero `.env.local` de la API NO esta en este repo, pero si la convencion fuese tenerlo en `/api/.gitignore`, escalar al thinker para que coordine con Agente 1.

### 6.4 Actualizar `/desktop/CLAUDE.md`

- Seccion 4 PACKAGE STRUCTURE: eliminar la mencion a `database.properties` (linea actual: `database.properties, preferencias.properties, cifrado.properties (excluded from Git)`).
- Seccion 1 PROJECT DEFINITION: actualizar el parrafo "El SGE has direct JDBC connection to PostgreSQL..." -> "El SGE consume la REST API central (`/api`) sin acceso directo a la base de datos. La conexion JDBC legacy fue eliminada en la migracion completada en marzo-abril de 2026."
- Seccion 6 SECURITY RULES: la mencion a "SSL/TLS: Code prepared in ConexionBD" ya no aplica porque ConexionBD fue eliminada. Reescribir o eliminar esa linea.
- Seccion 8 DATABASE SCHEMA REFERENCE: marcar como "esquema de la API, mantenida por Agente 1" para evitar futuras confusiones sobre quien la modifica.

### Criterio de aceptacion de la FASE 6

- [ ] `ip.properties` y `database.properties` eliminados.
- [ ] `git status` solo muestra los cambios planificados.
- [ ] `./gradlew run` y `./gradlew test` siguen verdes (74 tests).
- [ ] CLAUDE.md actualizado y coherente con el estado real.

---

## FASE 7: Runbook de arranque local

**Objetivo:** documentar el procedimiento exacto para que cualquier desarrollador (o agente futuro) pueda levantar el stack de desarrollo en menos de 5 minutos.

### 7.1 Crear seccion "Runbook local" en `/desktop/CLAUDE.md`

Anadir al final del archivo (antes de `## Memory`) una seccion nueva:

```markdown
## RUNBOOK LOCAL

### Levantar el stack completo (orden obligatorio)

1. PostgreSQL (terminal 1):
   docker compose -f /home/alaslibres/DAM/RehabiAPP/infra/docker-compose.yml up postgres

2. API Spring Boot (terminal 2):
   cd /home/alaslibres/DAM/RehabiAPP/api
   set -a && source .env.local && set +a
   ./mvnw spring-boot:run

   Esperar a "Started ApiApplication".

3. Desktop JavaFX (terminal 3):
   cd /home/alaslibres/DAM/RehabiAPP/desktop
   ./gradlew run

### Variables de entorno necesarias (api/.env.local)

SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/rehabiapp
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=admin
SPRING_PROFILES_ACTIVE=local

### Credenciales de prueba (seed-dev.sql)

| DNI | Password | Rol |
|-----|----------|-----|
| 00000001A | dev1234 | SPECIALIST |
| 00000002B | nurse1234 | NURSE |

### Health checks

- PG: PGPASSWORD=admin psql -h localhost -U admin -d rehabiapp -c "SELECT 1;"
- API: curl http://localhost:8080/actuator/health
- Desktop: pulsar el indicador del login -> debe pintarse verde

### Errores comunes

- "JavaFX runtime components are missing" -> ver historial de plan IntelliJ (commit 17f79c6).
- "password authentication failed" en logs API -> revisar .env.local; las credenciales deben coincidir con docker-compose.yml.
- Indicador de login en rojo -> revisar trazas SLF4J de ApiClient en la terminal 3 (busca "Conexion API fallida").
- 401 al login con credenciales correctas -> ejecutar seed-dev.sql.
- 401 en GET /api/pacientes despues de login OK -> el JWT no se esta enviando; revisar ApiClient.get() y header Authorization.
```

### 7.2 Crear `/desktop/scripts/README.md`

Documentar como usar `seed-dev.sql` (creado en FASE 3) y advertir que es solo para desarrollo.

### Criterio de aceptacion de la FASE 7

- [ ] Seccion "RUNBOOK LOCAL" anadida a `desktop/CLAUDE.md`.
- [ ] `desktop/scripts/README.md` creado.
- [ ] Un desarrollador siguiendo el runbook desde cero puede levantar el stack y hacer login en menos de 5 minutos.

---

## Criterios de exito globales del plan

- [ ] FASE 0: diagnostico entregado y validado por el thinker.
- [ ] FASE 1: ApiClient con trazas SLF4J en todos los puntos clave.
- [ ] FASE 2: api.properties verificado en classpath.
- [ ] FASE 3: usuarios seed disponibles y login curl verde.
- [ ] FASE 4: login + listado de pacientes funcionando desde la UI con dos roles.
- [ ] FASE 5: PacienteDAO sin URL hardcoded; tests verdes.
- [ ] FASE 6: legacy eliminado; CLAUDE.md actualizado.
- [ ] FASE 7: runbook escrito.
- [ ] `./gradlew test` -> 74 tests verdes en TODO el plan (anade nuevos tests en FASE 5, no rompe los existentes).

---

## Reglas inviolables para el Doer (Sonnet)

1. **No cruzar dominios.** El Doer del Agente 3 SOLO modifica ficheros bajo `/desktop`. Si una causa raiz esta en `/api` o `/infra`, escalar al thinker; el thinker coordinara con el Agente 1 o el Agente 0.
2. **Una fase, un commit.** Cada fase termina con `./gradlew test` verde y un commit con mensaje descriptivo. NO mezclar fases.
3. **No hacer git push** sin aprobacion explicita.
4. **No marcar una tarea como completa** si los tests no estan verdes o si hubo errores no resueltos.
5. **No silenciar errores.** Si una excepcion aparece en la consola, hay que entenderla, no taparla.
6. **No tocar la arquitectura del ApiClient ni de los DAOs** mas alla de lo descrito en este plan, sin aprobacion del thinker.
7. **No commitear .env.local, hashes BCrypt en texto plano, o cualquier secreto.**
8. **Reportar al thinker** al final de cada fase con: que se cambio, que tests se anadieron, evidencia de que el criterio de aceptacion se cumple.
9. **Si una fase no se puede completar**, NO seguir a la siguiente. Documentar el bloqueo y devolver al thinker.

---

## Orden de ejecucion

| Orden | Fase | Bloquea a | Prerequisito |
|-------|------|-----------|--------------|
| 1 | FASE 0 | Todas | Ninguno |
| 2 | FASE 1 | FASE 4 | FASE 0 reportada |
| 3 | FASE 2 | FASE 4 | FASE 1 |
| 4 | FASE 3 | FASE 4 | FASE 0 (saber si hace falta) |
| 5 | FASE 4 | FASE 5,6,7 | FASE 1+2+3 |
| 6 | FASE 5 | - | FASE 4 verde |
| 7 | FASE 6 | - | FASE 4 verde |
| 8 | FASE 7 | - | Todas las anteriores verdes |

FASE 2 y FASE 3 se pueden hacer en paralelo si dos doers trabajan a la vez. FASE 5, 6, 7 son independientes entre si una vez FASE 4 esta verde.

---

*Este plan es responsabilidad del Thinker Agente 3 (Opus). El Doer (Sonnet) lo ejecuta sin alterar la arquitectura. Cualquier desviacion debe ser aprobada por el Thinker antes de continuar.*
