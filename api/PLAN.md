# PLAN.md — API: Implementacion Segura con JWT, Hibernate y Flyway

> **Agent:** Sonnet (Doer) bajo Agent 1 (API + Data)
> **Dominio:** `/api/`
> **Prerrequisitos OBLIGATORIOS:** Leer `api/CLAUDE.md` y `api/.claude/skills/springboot4-postgresql/SKILL.md` ANTES de empezar.
> **Alcance:** Implementar la capa de seguridad (JWT, BCrypt, AES-256-GCM), el esquema real de Flyway, las entidades JPA con Hibernate Envers, y los servicios de la API REST. Todo compatible con el despliegue en Kubernetes definido en `/infra/`.
> **Base:** Spring Boot 4.0.5, Java 24, Maven. El skeleton K8s-ready ya existe (perfiles, probes, logging JSON, Prometheus).

---

## Contexto

El skeleton de la API ya esta preparado para Kubernetes:
- Puerto 8080, configurable via `SERVER_PORT`
- Probes: `/actuator/health/liveness` y `/actuator/health/readiness`
- Perfiles: `local` (desarrollo), `aws` (EKS), `production` (seguridad)
- Secretos: `/mnt/secrets/` (AWS CSI Driver) o variables de entorno (local)
- Filesystem de solo lectura (Tomcat basedir `/tmp`)
- Logging JSON estructurado (Logstash encoder)
- Metricas Prometheus en `/actuator/prometheus`

Este plan construye SOBRE ese skeleton la logica de negocio, seguridad y persistencia.

### Esquema de BD existente (desktop)

```
sanitario(dni_san PK, nombre_san, apellido1_san, apellido2_san, email_san UNIQUE,
          num_de_pacientes, contrasena_san, activo, fecha_baja)

sanitario_agrega_sanitario(dni_san PK/FK CASCADE, cargo CHECK)

telefono_sanitario(id_telefono SERIAL PK, dni_san FK CASCADE, telefono)

localidad(nombre_localidad PK, provincia)
cp(cp PK, nombre_localidad FK)
direccion(id_direccion SERIAL PK, calle, numero, piso, cp FK)

discapacidad(cod_dis PK, nombre_dis UNIQUE, descripcion_dis, necesita_protesis)
tratamiento(cod_trat PK, nombre_trat UNIQUE, definicion_trat)
discapacidad_tratamiento(cod_dis FK, cod_trat FK -- PK compuesta)

paciente(dni_pac PK, dni_san FK RESTRICT, nombre_pac, apellido1_pac, apellido2_pac,
         edad_pac, email_pac UNIQUE, num_ss UNIQUE, id_direccion FK,
         discapacidad_pac, tratamiento_pac, estado_tratamiento, protesis, foto BYTEA,
         fecha_nacimiento, sexo, alergias, antecedentes, medicacion_actual,
         consentimiento_rgpd, fecha_consentimiento, activo, fecha_baja)

telefono_paciente(id_telefono SERIAL PK, dni_pac FK CASCADE, telefono)

cita(dni_pac FK CASCADE, dni_san FK CASCADE, fecha_cita, hora_cita -- PK compuesta)

audit_log(id_audit BIGSERIAL PK, fecha_hora, dni_usuario, nombre_usuario,
          accion CHECK, entidad, id_entidad, detalle, ip_origen)
```

Tablas pendientes de crear: `nivel_progresion`, `paciente_discapacidad`, `paciente_tratamiento`.

### Topologia K8s relevante

```
Internet --> ALB (TLS 1.3 + WAFv2)
  --> rehabiapp-api:8080 (namespace: rehabiapp-api, 2-6 replicas)
  --> mobile-backend:3000 (namespace: rehabiapp-mobile)

Interno:
  mobile-backend --> rehabiapp-api:8080
  rehabiapp-api  --> postgresql:5432 (local) / RDS (AWS)
  rehabiapp-api  --> rehabiapp-data:8081
```

Seguridad de pods: non-root UID 1000, readOnlyRootFilesystem, drop ALL capabilities, seccomp RuntimeDefault. Secretos K8s disponibles en el entorno local:
- `jwt-signing-key` — clave de firma JWT (Secret `api-secrets`)
- `encryption-key` — clave AES-256-GCM para campos clinicos (Secret `api-secrets`)
- `username` / `password` — credenciales PostgreSQL (Secret `postgresql-credentials`)

En AWS: los mismos secretos se montan via AWS Secrets Manager CSI Driver en `/mnt/secrets/`.

---

## Fase 1: Migraciones Flyway — Esquema real de la BD

> **Objetivo:** Reemplazar `V0__placeholder.sql` con el esquema completo. Crear las tablas nuevas y las tablas de auditoria de Hibernate Envers. Todas las migraciones se ejecutan contra PostgreSQL 16.

### Paso 1.1: Migracion V1 — Esquema core del desktop

**Archivo:** `src/main/resources/db/migration/V1__esquema_core.sql`

Crear TODAS las tablas existentes del desktop en este orden (respetando dependencias FK):

1. `localidad` (sin dependencias)
2. `cp` (FK -> localidad)
3. `direccion` (FK -> cp)
4. `sanitario` (sin dependencias)
5. `sanitario_agrega_sanitario` (FK -> sanitario)
6. `telefono_sanitario` (FK -> sanitario)
7. `discapacidad` (sin dependencias)
8. `tratamiento` (sin dependencias)
9. `discapacidad_tratamiento` (FK -> discapacidad, tratamiento)
10. `paciente` (FK -> sanitario, direccion)
11. `telefono_paciente` (FK -> paciente)
12. `cita` (FK -> paciente, sanitario)
13. `audit_log` (sin dependencias, tabla independiente)

**Especificaciones tecnicas:**
- Respetar EXACTAMENTE los nombres de columnas y tipos del desktop (compatibilidad JDBC directa)
- Campos clinicos sensibles (`alergias`, `antecedentes`, `medicacion_actual`) almacenados como `TEXT` — el cifrado es a nivel de aplicacion (AttributeConverter AES-256-GCM), no de BD
- `contrasena_san` es `TEXT` — almacena hashes BCrypt
- `activo` en sanitario y paciente: `BOOLEAN DEFAULT TRUE`
- `fecha_baja`: `TIMESTAMP` nullable
- `audit_log.accion`: CHECK con valores `('CREAR', 'LEER', 'ACTUALIZAR', 'ELIMINAR')`
- Indices: `CREATE INDEX` en FKs principales (dni_san en paciente, dni_pac en cita, etc.)

**Validacion:** `./mvnw flyway:migrate -Dspring.profiles.active=local` sin errores.

### Paso 1.2: Migracion V2 — Tablas nuevas del ecosistema

**Archivo:** `src/main/resources/db/migration/V2__tablas_nuevas.sql`

Crear las tablas pendientes:

```sql
-- Niveles de progresion para tratamientos de rehabilitacion
nivel_progresion(
    id_nivel    SERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL UNIQUE,
    orden       INTEGER NOT NULL UNIQUE,
    descripcion TEXT
);

-- Asignacion paciente-discapacidad con nivel de progresion
paciente_discapacidad(
    dni_pac     VARCHAR(20) REFERENCES paciente(dni_pac) ON DELETE CASCADE,
    cod_dis     VARCHAR(20) REFERENCES discapacidad(cod_dis),
    id_nivel    INTEGER REFERENCES nivel_progresion(id_nivel),
    fecha_asignacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notas       TEXT,
    PRIMARY KEY (dni_pac, cod_dis)
);

-- Visibilidad de tratamientos por paciente
paciente_tratamiento(
    dni_pac     VARCHAR(20) REFERENCES paciente(dni_pac) ON DELETE CASCADE,
    cod_trat    VARCHAR(20) REFERENCES tratamiento(cod_trat),
    visible     BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_asignacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (dni_pac, cod_trat)
);
```

### Paso 1.3: Migracion V3 — Tablas de Hibernate Envers

**Archivo:** `src/main/resources/db/migration/V3__envers_auditoria.sql`

> **OBLIGATORIO por SKILL.md:** Las tablas de Envers se definen en Flyway, NO se auto-generan.

Crear:

```sql
-- Tabla de revisiones de Envers
revinfo(
    rev         SERIAL PRIMARY KEY,
    revtstmp    BIGINT NOT NULL,
    usuario     VARCHAR(20),    -- DNI del usuario que realizo el cambio
    ip_origen   VARCHAR(45)     -- IP del cliente
);

-- Tablas _audit para cada entidad auditada (sufijo _audit segun application.yml)
sanitario_audit(
    dni_san         VARCHAR(20) NOT NULL,
    rev             INTEGER NOT NULL REFERENCES revinfo(rev),
    rev_type        SMALLINT,   -- 0=ADD, 1=MOD, 2=DEL
    -- Todas las columnas de sanitario (excepto contrasena_san)
    nombre_san      VARCHAR(100),
    apellido1_san   VARCHAR(100),
    apellido2_san   VARCHAR(100),
    email_san       VARCHAR(200),
    num_de_pacientes INTEGER,
    activo          BOOLEAN,
    fecha_baja      TIMESTAMP,
    PRIMARY KEY (dni_san, rev)
);

paciente_audit(
    dni_pac         VARCHAR(20) NOT NULL,
    rev             INTEGER NOT NULL REFERENCES revinfo(rev),
    rev_type        SMALLINT,
    -- Todas las columnas de paciente (los campos cifrados se auditan cifrados)
    nombre_pac      VARCHAR(100),
    apellido1_pac   VARCHAR(100),
    apellido2_pac   VARCHAR(100),
    edad_pac        INTEGER,
    email_pac       VARCHAR(200),
    num_ss          VARCHAR(20),
    -- ... (todas las columnas relevantes)
    activo          BOOLEAN,
    fecha_baja      TIMESTAMP,
    PRIMARY KEY (dni_pac, rev)
);

cita_audit(
    dni_pac         VARCHAR(20) NOT NULL,
    dni_san         VARCHAR(20) NOT NULL,
    fecha_cita      DATE NOT NULL,
    hora_cita       TIME NOT NULL,
    rev             INTEGER NOT NULL REFERENCES revinfo(rev),
    rev_type        SMALLINT,
    PRIMARY KEY (dni_pac, dni_san, fecha_cita, hora_cita, rev)
);

paciente_discapacidad_audit(
    dni_pac         VARCHAR(20) NOT NULL,
    cod_dis         VARCHAR(20) NOT NULL,
    rev             INTEGER NOT NULL REFERENCES revinfo(rev),
    rev_type        SMALLINT,
    id_nivel        INTEGER,
    notas           TEXT,
    PRIMARY KEY (dni_pac, cod_dis, rev)
);

paciente_tratamiento_audit(
    dni_pac         VARCHAR(20) NOT NULL,
    cod_trat        VARCHAR(20) NOT NULL,
    rev             INTEGER NOT NULL REFERENCES revinfo(rev),
    rev_type        SMALLINT,
    visible         BOOLEAN,
    PRIMARY KEY (dni_pac, cod_trat, rev)
);
```

### Paso 1.4: Eliminar migracion placeholder

Eliminar `V0__placeholder.sql` y configurar Flyway para que inicie desde V1:

```yaml
# application.yml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 0
```

Esto permite que Flyway ignore V0 si ya se ejecuto y empiece desde V1 en entornos nuevos.

**Criterio de aceptacion Fase 1:**
- [ ] `./mvnw flyway:migrate -Dspring.profiles.active=local` ejecuta todas las migraciones sin errores
- [ ] Las tablas se crean en PostgreSQL con los tipos y constraints correctos
- [ ] Las tablas `_audit` y `revinfo` existen con la estructura compatible con Envers

---

## Fase 2: Capa de Dominio — Entidades JPA con Hibernate Envers

> **Objetivo:** Crear todas las entidades JPA, value objects, enums y repositorios. Aplicar `@Audited` de Envers en las entidades de datos clinicos. Usar Java 24 records para DTOs (no en esta fase, se crean en Fase 4).

### Paso 2.1: Entidad de revision personalizada de Envers

**Archivo:** `src/main/java/com/rehabiapp/api/domain/audit/RehabiRevisionEntity.java`

```java
@Entity
@Table(name = "revinfo")
@RevisionEntity(RehabiRevisionListener.class)
public class RehabiRevisionEntity { ... }
```

Campos: `rev` (PK), `revtstmp`, `usuario` (DNI del usuario autenticado), `ipOrigen`.

**Archivo:** `src/main/java/com/rehabiapp/api/domain/audit/RehabiRevisionListener.java`

Implementa `RevisionListener`. En `newRevision()`, extrae del `SecurityContext`:
- El DNI del usuario autenticado (claim del JWT)
- La IP de origen (del `HttpServletRequest` via `RequestContextHolder`)

### Paso 2.2: Enums del dominio

**Directorio:** `src/main/java/com/rehabiapp/api/domain/enums/`

| Enum | Valores | Uso |
|------|---------|-----|
| `Rol` | `SPECIALIST`, `NURSE` | RBAC en JWT y SecurityConfig |
| `AccionAuditoria` | `CREAR`, `LEER`, `ACTUALIZAR`, `ELIMINAR` | Columna `accion` en `audit_log` |
| `Sexo` | `MASCULINO`, `FEMENINO`, `OTRO` | Campo `sexo` en paciente |

### Paso 2.3: Entidades JPA principales

**Directorio:** `src/main/java/com/rehabiapp/api/domain/entity/`

Todas las entidades siguen estas reglas:
- `@Entity` + `@Table(name = "nombre_tabla")`
- `@Audited` en sanitario, paciente, cita, paciente_discapacidad, paciente_tratamiento
- Campos clinicos sensibles (`alergias`, `antecedentes`, `medicacionActual`) con `@Convert(converter = CampoClinicoConverter.class)` — el converter se implementa en Fase 3
- Relaciones con `FetchType.LAZY` por defecto — consultas optimizadas con `@EntityGraph` o `JOIN FETCH`
- Soft delete en sanitario y paciente: campo `activo` (boolean) + `fechaBaja` (LocalDateTime)
- Usar `@NotAudited` en campos que no deben auditarse (ej: `contrasena` en sanitario)

**Entidades a crear:**

| Entidad | Tabla | PK | Audited | Notas |
|---------|-------|----|---------|-------|
| `Sanitario` | sanitario | `dniSan` (String) | Si | `@NotAudited` en contrasena. Relacion `@OneToMany` con telefonos |
| `SanitarioRol` | sanitario_agrega_sanitario | `dniSan` (String) | No | Campo `cargo` mapea al enum `Rol` |
| `TelefonoSanitario` | telefono_sanitario | `idTelefono` (Long) | No | `@ManyToOne` con Sanitario |
| `Localidad` | localidad | `nombreLocalidad` (String) | No | Catalogo estatico |
| `CodigoPostal` | cp | `cp` (String) | No | FK a Localidad |
| `Direccion` | direccion | `idDireccion` (Long) | No | FK a CodigoPostal |
| `Discapacidad` | discapacidad | `codDis` (String) | No | Catalogo estatico |
| `Tratamiento` | tratamiento | `codTrat` (String) | No | Catalogo estatico |
| `DiscapacidadTratamiento` | discapacidad_tratamiento | Compuesta | No | Relacion M:N |
| `Paciente` | paciente | `dniPac` (String) | Si | Campos cifrados, soft delete, foto BYTEA |
| `TelefonoPaciente` | telefono_paciente | `idTelefono` (Long) | No | `@ManyToOne` con Paciente |
| `Cita` | cita | Compuesta (4 campos) | Si | `@IdClass` o `@EmbeddedId` |
| `NivelProgresion` | nivel_progresion | `idNivel` (Integer) | No | Catalogo ordenado |
| `PacienteDiscapacidad` | paciente_discapacidad | Compuesta | Si | FK a NivelProgresion |
| `PacienteTratamiento` | paciente_tratamiento | Compuesta | Si | Campo `visible` (boolean) |
| `AuditLog` | audit_log | `idAudit` (Long) | No | Solo INSERT, nunca UPDATE/DELETE |

### Paso 2.4: Interfaces de repositorio

**Directorio:** `src/main/java/com/rehabiapp/api/domain/repository/`

Crear interfaces que extiendan `JpaRepository` (o `JpaSpecificationExecutor` si se necesitan queries dinamicas). Definir queries personalizadas con `@Query` usando JPQL parametrizado:

| Repositorio | Metodos personalizados |
|-------------|----------------------|
| `SanitarioRepository` | `findByDniSanAndActivoTrue(String dni)`, `findAllByActivoTrue(Pageable)` |
| `PacienteRepository` | `findByDniPacAndActivoTrue(String dni)`, `findAllByDniSan(String dniSan, Pageable)` |
| `CitaRepository` | `findByFechaCita(LocalDate fecha, Pageable)`, `findByDniSan(String dniSan, Pageable)` |
| `DiscapacidadRepository` | `findByCodDis(String cod)` |
| `TratamientoRepository` | `findByCodTrat(String cod)`, consulta con JOIN a discapacidad_tratamiento por cod_dis y nivel |
| `NivelProgresionRepository` | `findAllByOrderByOrdenAsc()` |
| `PacienteDiscapacidadRepository` | `findByDniPac(String dniPac)` |
| `PacienteTratamientoRepository` | `findByDniPac(String dniPac)` |
| `AuditLogRepository` | Solo `save()` (JpaRepository default) — nunca exponer delete/update |

> **SKILL.md:** Todos los metodos de listado DEBEN aceptar `Pageable`. Consultas con relaciones DEBEN usar `@EntityGraph` o `JOIN FETCH` para prevenir N+1.

**Criterio de aceptacion Fase 2:**
- [ ] Todas las entidades compilan sin errores (`./mvnw compile`)
- [ ] Las anotaciones `@Audited` estan en sanitario, paciente, cita, paciente_discapacidad, paciente_tratamiento
- [ ] Los campos clinicos tienen `@Convert(converter = CampoClinicoConverter.class)`
- [ ] Todas las relaciones son `LAZY` con `@EntityGraph` definidos donde se necesiten
- [ ] Los repositorios usan `Pageable` en metodos de listado

---

## Fase 3: Infraestructura de Seguridad

> **Objetivo:** Implementar JWT stateless (critico para escalado horizontal en K8s), BCrypt, AES-256-GCM, RBAC y auditoria de operaciones. Los secretos se leen del entorno K8s.

### Paso 3.1: Configuracion de propiedades de seguridad

**Archivo:** `src/main/java/com/rehabiapp/api/infrastructure/config/SecurityProperties.java`

`@ConfigurationProperties(prefix = "rehabiapp.security")` con:

```java
public record SecurityProperties(
    String jwtSigningKey,       // Inyectado desde K8s Secret
    long jwtExpirationMs,       // Default: 900000 (15 minutos)
    long jwtRefreshExpirationMs,// Default: 604800000 (7 dias)
    String encryptionKey        // Clave AES-256-GCM desde K8s Secret
) {}
```

**Configuracion en application.yml:**

```yaml
rehabiapp:
  security:
    jwt-signing-key: ${JWT_SIGNING_KEY:dev-jwt-signing-key-placeholder-change-in-production}
    jwt-expiration-ms: 900000
    jwt-refresh-expiration-ms: 604800000
    encryption-key: ${ENCRYPTION_KEY:dev-encryption-key-placeholder-change-in-production}
```

> **K8s:** En local, los valores vienen del Secret `api-secrets` via variables de entorno. En AWS, del configtree montado en `/mnt/secrets/` que Spring Boot importa automaticamente.

### Paso 3.2: Utilidad JWT

**Archivo:** `src/main/java/com/rehabiapp/api/infrastructure/security/JwtService.java`

Servicio que gestiona tokens JWT:

| Metodo | Descripcion |
|--------|-------------|
| `generarAccessToken(String dni, Rol rol)` | Genera JWT firmado con HMAC-SHA512. Claims: `sub`=dni, `rol`=rol, `iat`, `exp` (15 min) |
| `generarRefreshToken(String dni)` | JWT con `sub`=dni, `exp` (7 dias), sin claims de rol |
| `validarToken(String token)` | Verifica firma y expiracion. Retorna `Claims` o lanza excepcion |
| `extraerDni(String token)` | Extrae `sub` del token |
| `extraerRol(String token)` | Extrae claim `rol` del access token |

**Dependencia:** Usar la libreria `io.jsonwebtoken:jjwt-api` + `jjwt-impl` + `jjwt-jackson` (anadir al pom.xml).

```xml
<jjwt.version>0.12.6</jjwt.version>
<!-- ... -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>${jjwt.version}</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
```

> **K8s:** JWT stateless es OBLIGATORIO. No usar sesiones de servidor — las replicas del pod no comparten estado. Cada request lleva su propio token.

### Paso 3.3: Filtro de autenticacion JWT

**Archivo:** `src/main/java/com/rehabiapp/api/infrastructure/security/JwtAuthenticationFilter.java`

Extiende `OncePerRequestFilter`:

1. Extraer header `Authorization: Bearer <token>`
2. Si no hay header, continuar cadena (endpoints publicos)
3. Validar token con `JwtService`
4. Crear `UsernamePasswordAuthenticationToken` con authorities del rol
5. Establecer en `SecurityContextHolder`
6. Continuar cadena de filtros

Excluir de filtrado: `/api/auth/**`, `/actuator/**` (probes K8s).

### Paso 3.4: Configuracion de Spring Security

**Archivo:** `src/main/java/com/rehabiapp/api/infrastructure/config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig { ... }
```

Configurar `SecurityFilterChain`:

1. **CSRF deshabilitado** — API stateless con JWT, no sesiones
2. **Sesiones STATELESS** — `SessionCreationPolicy.STATELESS`
3. **Rutas publicas:**
   - `POST /api/auth/login` — login
   - `POST /api/auth/refresh` — renovar token
   - `GET /actuator/health/**` — probes K8s (liveness, readiness)
   - `GET /actuator/prometheus` — metricas
4. **Rutas protegidas:** todo lo demas requiere JWT valido
5. **RBAC por metodo:** usar `@PreAuthorize("hasRole('SPECIALIST')")` en controllers
6. Registrar `JwtAuthenticationFilter` antes de `UsernamePasswordAuthenticationFilter`
7. **CORS:** Configurar origenes permitidos via propiedad (para el frontend mobile y games)

**Reglas RBAC:**

| Rol | Permisos |
|-----|----------|
| `SPECIALIST` | CRUD completo en todas las entidades. Gestion de sanitarios. |
| `NURSE` | Lectura de pacientes (sin campos sensibles extra). Gestion de citas. Sin gestion de sanitarios. |

### Paso 3.5: Cifrado AES-256-GCM para campos clinicos

**Archivo:** `src/main/java/com/rehabiapp/api/infrastructure/encryption/CampoClinicoConverter.java`

> **SKILL.md OBLIGATORIO:** Usar JPA `AttributeConverter` con AES-256-GCM.

```java
@Converter
public class CampoClinicoConverter implements AttributeConverter<String, String> {
    // IV aleatorio de 96 bits por escritura
    // Clave desde SecurityProperties.encryptionKey
    // Resultado: Base64(IV + ciphertext + tag)
    // Debe ser compatible con el CifradoService del desktop
}
```

**Especificaciones:**
- Algoritmo: `AES/GCM/NoPadding`
- Clave: 256 bits derivada de `rehabiapp.security.encryption-key`
- IV: 96 bits aleatorios generados con `SecureRandom` en cada `convertToDatabaseColumn()`
- Formato almacenado: `Base64(iv || ciphertext || authTag)`
- `convertToEntityAttribute()`: decodifica Base64, separa IV, descifra
- `null` input retorna `null` output (campos opcionales)

### Paso 3.6: Hashing BCrypt para contrasenas

**Archivo:** `src/main/java/com/rehabiapp/api/infrastructure/security/PasswordService.java`

- Usar `BCryptPasswordEncoder` de Spring Security con cost factor 12
- Metodo `hashear(String plana)` -> hash BCrypt
- Metodo `verificar(String plana, String hash)` -> boolean
- Compatibilidad con hashes existentes del desktop (BCrypt estandar con jBCrypt 0.4)

### Paso 3.7: Servicio de auditoria de operaciones

**Archivo:** `src/main/java/com/rehabiapp/api/infrastructure/audit/AuditService.java`

Registra TODAS las operaciones en la tabla `audit_log`, incluyendo LECTURAS de datos clinicos (obligatorio por Ley 41/2002).

| Campo | Origen |
|-------|--------|
| `fechaHora` | `LocalDateTime.now()` |
| `dniUsuario` | Del `SecurityContext` (claim JWT) |
| `nombreUsuario` | Del `SecurityContext` o consulta a sanitario |
| `accion` | Enum `AccionAuditoria` |
| `entidad` | Nombre de la tabla afectada |
| `idEntidad` | PK del registro afectado |
| `detalle` | Descripcion breve de la operacion |
| `ipOrigen` | Del `HttpServletRequest` (`X-Forwarded-For` en K8s detras de ALB) |

> **K8s:** La IP de origen en K8s viene en el header `X-Forwarded-For` porque el trafico pasa por el ALB. Usar `request.getHeader("X-Forwarded-For")` con fallback a `request.getRemoteAddr()`.

### Paso 3.8: Endpoint de autenticacion

**Archivo:** `src/main/java/com/rehabiapp/api/presentation/controller/AuthController.java`

| Endpoint | Metodo | Body | Respuesta | Auth |
|----------|--------|------|-----------|------|
| `/api/auth/login` | POST | `{ "dni": "...", "contrasena": "..." }` | `{ "accessToken": "...", "refreshToken": "...", "rol": "..." }` | Publico |
| `/api/auth/refresh` | POST | `{ "refreshToken": "..." }` | `{ "accessToken": "...", "refreshToken": "..." }` | Publico |

**Flujo de login:**
1. Buscar sanitario por DNI (activo=true)
2. Verificar contrasena con BCrypt
3. Obtener rol de `sanitario_agrega_sanitario`
4. Generar access token + refresh token
5. Registrar en audit_log: accion=LEER, entidad=sanitario, detalle="Login exitoso"
6. Retornar tokens

**Flujo de refresh:**
1. Validar refresh token (firma + expiracion)
2. Extraer DNI
3. Verificar que sanitario sigue activo
4. Generar nuevo par de tokens
5. Retornar tokens

**Criterio de aceptacion Fase 3:**
- [ ] Dependencia jjwt anadida al pom.xml
- [ ] SecurityProperties lee secretos del entorno K8s
- [ ] JwtService genera y valida tokens correctamente
- [ ] JwtAuthenticationFilter filtra requests y establece SecurityContext
- [ ] SecurityConfig: CSRF off, stateless, rutas publicas para probes y auth
- [ ] CampoClinicoConverter cifra/descifra con AES-256-GCM y IV aleatorio
- [ ] PasswordService usa BCrypt cost 12, compatible con desktop
- [ ] AuditService registra operaciones incluyendo IP via X-Forwarded-For
- [ ] POST /api/auth/login retorna JWT valido
- [ ] POST /api/auth/refresh renueva tokens
- [ ] Tests unitarios para JwtService, CampoClinicoConverter, PasswordService

---

## Fase 4: Capa de Aplicacion — Servicios, DTOs y Mappers

> **Objetivo:** Implementar la logica de negocio. DTOs como Java records. MapStruct para mapeo entidad-DTO. Servicios transaccionales con auditoria.

### Paso 4.1: DTOs (Java records)

**Directorio:** `src/main/java/com/rehabiapp/api/application/dto/`

> **SKILL.md:** Usar Java 24 records para DTOs.

Crear records para request y response de cada entidad. Ejemplo:

```java
// Response — nunca expone contrasena ni campos internos
public record SanitarioResponse(
    String dniSan, String nombreSan, String apellido1San, String apellido2San,
    String emailSan, int numDePacientes, boolean activo, List<String> telefonos
) {}

// Request — validacion con Jakarta Bean Validation
public record SanitarioRequest(
    @NotBlank String dniSan, @NotBlank String nombreSan,
    @NotBlank String apellido1San, String apellido2San,
    @Email String emailSan, @NotBlank String contrasena
) {}

// Paciente response — campos clinicos SOLO si el usuario tiene permiso
public record PacienteResponse(
    String dniPac, String nombrePac, String apellido1Pac, String apellido2Pac,
    int edadPac, String emailPac, String numSs, boolean activo,
    // Campos clinicos — null si el usuario no tiene permiso
    String alergias, String antecedentes, String medicacionActual
) {}
```

DTOs necesarios (minimo):
- `SanitarioRequest`, `SanitarioResponse`
- `PacienteRequest`, `PacienteResponse`
- `CitaRequest`, `CitaResponse`
- `DiscapacidadResponse`
- `TratamientoResponse`
- `NivelProgresionResponse`
- `PacienteDiscapacidadRequest`, `PacienteDiscapacidadResponse`
- `PacienteTratamientoResponse`
- `LoginRequest`, `LoginResponse`, `RefreshRequest`, `TokenResponse`
- `PageResponse<T>` — wrapper generico para respuestas paginadas

### Paso 4.2: MapStruct Mappers

**Directorio:** `src/main/java/com/rehabiapp/api/application/mapper/`

Un mapper por entidad principal. MapStruct genera la implementacion en compilacion:

```java
@Mapper(componentModel = "spring")
public interface SanitarioMapper {
    SanitarioResponse toResponse(Sanitario entity);
    Sanitario toEntity(SanitarioRequest dto);
    // Ignorar campos que no vienen del request (activo, fechaBaja, etc.)
}
```

> **CLAUDE.md:** NUNCA retornar `@Entity` directamente desde controllers. Siempre mapear a DTO.

### Paso 4.3: Servicios de aplicacion

**Directorio:** `src/main/java/com/rehabiapp/api/application/service/`

Cada servicio:
- Anotado con `@Service` y `@Transactional`
- Inyecta repositorios via constructor
- Llama a `AuditService` para registrar cada operacion
- Retorna DTOs (nunca entidades)
- Lanza excepciones de dominio (`RecursoNoEncontradoException`, `AccesoNoPermitidoException`, etc.)

| Servicio | Responsabilidades |
|----------|-------------------|
| `SanitarioService` | CRUD sanitarios, soft delete, gestion telefonos |
| `PacienteService` | CRUD pacientes, soft delete, gestion telefonos, busqueda por sanitario |
| `CitaService` | CRUD citas, busqueda por fecha y sanitario |
| `CatalogoService` | Lectura de discapacidades, tratamientos, niveles de progresion |
| `AsignacionService` | Asignar discapacidad a paciente, actualizar nivel, toggle visibilidad tratamiento |
| `HistorialService` | Consultas historicas via Envers AuditReader API |

> **SKILL.md:** Usar `AuditReader` de Envers para consultas historicas (ej: "estado del paciente hace 3 meses").

**Logica de soft delete (sanitario y paciente):**
```
1. Marcar activo = false
2. Establecer fechaBaja = ahora
3. Registrar en audit_log con accion = ELIMINAR
4. NO borrar fisicamente (Ley 41/2002: retencion 5 anos)
```

**Criterio de aceptacion Fase 4:**
- [ ] DTOs como Java records con validacion Jakarta
- [ ] MapStruct mappers compilan y generan codigo correcto
- [ ] Servicios transaccionales con auditoria en cada operacion
- [ ] Soft delete implementado para sanitario y paciente
- [ ] HistorialService usa Envers AuditReader
- [ ] `./mvnw compile` exitoso

---

## Fase 5: Capa de Presentacion — Controllers REST

> **Objetivo:** Exponer endpoints REST. Controllers solo manejan HTTP — cero logica de negocio.

### Paso 5.1: Manejador global de excepciones

**Archivo:** `src/main/java/com/rehabiapp/api/presentation/handler/GlobalExceptionHandler.java`

`@RestControllerAdvice` que mapea excepciones a respuestas HTTP estandar:

| Excepcion | HTTP Status | Respuesta |
|-----------|-------------|-----------|
| `RecursoNoEncontradoException` | 404 | `{ "error": "No encontrado", "detalle": "..." }` |
| `AccesoNoPermitidoException` | 403 | `{ "error": "Acceso denegado", "detalle": "..." }` |
| `MethodArgumentNotValidException` | 400 | `{ "error": "Validacion fallida", "campos": {...} }` |
| `DataIntegrityViolationException` | 409 | `{ "error": "Conflicto de datos", "detalle": "..." }` |
| `JwtException` | 401 | `{ "error": "Token invalido", "detalle": "..." }` |
| `Exception` (generico) | 500 | `{ "error": "Error interno" }` (sin stacktrace en produccion) |

### Paso 5.2: Controllers REST

**Directorio:** `src/main/java/com/rehabiapp/api/presentation/controller/`

Base path: `/api/`

| Controller | Base Path | Endpoints |
|------------|-----------|-----------|
| `AuthController` | `/api/auth` | POST `/login`, POST `/refresh` |
| `SanitarioController` | `/api/sanitarios` | GET `/` (paginado), GET `/{dni}`, POST `/`, PUT `/{dni}`, DELETE `/{dni}` |
| `PacienteController` | `/api/pacientes` | GET `/` (paginado), GET `/{dni}`, POST `/`, PUT `/{dni}`, DELETE `/{dni}` |
| `CitaController` | `/api/citas` | GET `/` (por fecha, paginado), GET `/sanitario/{dni}` (paginado), POST `/`, PUT `/`, DELETE `/` |
| `CatalogoController` | `/api/catalogo` | GET `/discapacidades`, GET `/discapacidades/{cod}`, GET `/tratamientos`, GET `/tratamientos/{cod}`, GET `/niveles-progresion` |
| `AsignacionController` | `/api/pacientes/{dni}/discapacidades` | GET `/`, POST `/`, PUT `/{cod}` |
| `TratamientoVisibilidadController` | `/api/pacientes/{dni}/tratamientos` | GET `/`, PUT `/{cod}/visibilidad` |

**Reglas para TODOS los controllers:**
1. `@RestController` + `@RequestMapping("/api/...")`
2. Inyectar servicio via constructor (nunca `@Autowired` en campo)
3. Validar requests con `@Valid`
4. Retornar `ResponseEntity<T>` con DTOs
5. Usar `@PreAuthorize` para RBAC donde sea necesario
6. Metodos de listado SIEMPRE aceptan `Pageable` (parametros `page`, `size`, `sort`)
7. Documentar con anotaciones de Swagger/OpenAPI (si se anade la dependencia)

### Paso 5.3: Interceptor de auditoria para lecturas

**Archivo:** `src/main/java/com/rehabiapp/api/infrastructure/audit/AuditReadInterceptor.java`

`HandlerInterceptor` que registra en `audit_log` cada lectura de datos clinicos (GET de pacientes).

> **Ley 41/2002:** Las lecturas de historiales clinicos tambien deben quedar registradas.

**Criterio de aceptacion Fase 5:**
- [ ] GlobalExceptionHandler maneja todas las excepciones documentadas
- [ ] Todos los controllers compilan y usan DTOs
- [ ] Endpoints protegidos requieren JWT valido
- [ ] RBAC funciona (SPECIALIST vs NURSE)
- [ ] Listados aceptan paginacion
- [ ] Lecturas de pacientes se auditan

---

## Fase 6: Testing e Integracion

> **Objetivo:** Verificar que todo funciona correctamente. Tests unitarios para servicios de seguridad. Tests de integracion para endpoints.

### Paso 6.1: Tests unitarios

**Directorio:** `src/test/java/com/rehabiapp/api/`

| Test | Que verifica |
|------|-------------|
| `JwtServiceTest` | Generacion, validacion, expiracion, claims |
| `CampoClinicoConverterTest` | Cifrado/descifrado AES-256-GCM, IV aleatorio, null handling |
| `PasswordServiceTest` | BCrypt hash y verificacion, compatibilidad con hashes existentes |
| `SanitarioServiceTest` | CRUD con mocks, soft delete, auditoria |
| `PacienteServiceTest` | CRUD con mocks, campos cifrados, soft delete |

### Paso 6.2: Tests de integracion

| Test | Que verifica |
|------|-------------|
| `AuthControllerIT` | Login, refresh, credenciales invalidas, usuario inactivo |
| `SanitarioControllerIT` | CRUD con JWT, RBAC (nurse no puede crear sanitarios) |
| `PacienteControllerIT` | CRUD con JWT, campos cifrados en BD, soft delete |
| `CitaControllerIT` | CRUD con JWT, busqueda por fecha y sanitario |

Usar `@SpringBootTest` + `@ActiveProfiles("local")` + `@AutoConfigureMockMvc`.

### Paso 6.3: Validacion final

```bash
./mvnw clean test
./mvnw flyway:migrate -Dspring.profiles.active=local
./mvnw spring-boot:run -Dspring.profiles.active=local
# Verificar probes K8s:
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
# Verificar login:
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"dni":"...", "contrasena":"..."}'
```

**Criterio de aceptacion Fase 6:**
- [ ] `./mvnw clean test` pasa todos los tests
- [ ] La aplicacion arranca con perfil local sin errores
- [ ] Los probes de K8s responden 200
- [ ] El flujo completo login -> token -> request protegido funciona
- [ ] Los campos clinicos se almacenan cifrados en la BD

---

## Checklist general

### Fase 1: Migraciones Flyway
- [x] Paso 1.1: V1 — esquema core del desktop
- [x] Paso 1.2: V2 — tablas nuevas (nivel_progresion, paciente_discapacidad, paciente_tratamiento)
- [x] Paso 1.3: V3 — tablas Hibernate Envers (revinfo + _audit)
- [x] Paso 1.4: Eliminar placeholder V0, configurar baseline

### Fase 2: Capa de Dominio
- [x] Paso 2.1: Entidad de revision personalizada (Envers)
- [x] Paso 2.2: Enums del dominio
- [x] Paso 2.3: Entidades JPA con @Audited y @Convert
- [x] Paso 2.4: Interfaces de repositorio con Pageable

### Fase 3: Infraestructura de Seguridad
- [x] Paso 3.1: SecurityProperties con secretos K8s
- [x] Paso 3.2: JwtService (generar, validar, extraer claims)
- [x] Paso 3.3: JwtAuthenticationFilter
- [x] Paso 3.4: SecurityConfig (stateless, RBAC, rutas publicas para probes)
- [x] Paso 3.5: CampoClinicoConverter (AES-256-GCM)
- [x] Paso 3.6: PasswordService (BCrypt cost 12)
- [x] Paso 3.7: AuditService (con IP via X-Forwarded-For)
- [x] Paso 3.8: AuthController (login + refresh)

### Fase 4: Capa de Aplicacion
- [x] Paso 4.1: DTOs como Java records con validacion
- [x] Paso 4.2: MapStruct mappers
- [x] Paso 4.3: Servicios transaccionales con auditoria

### Fase 5: Capa de Presentacion
- [x] Paso 5.1: GlobalExceptionHandler
- [x] Paso 5.2: Controllers REST con paginacion y RBAC
- [x] Paso 5.3: Interceptor de auditoria para lecturas

### Fase 6: Testing e Integracion
- [x] Paso 6.1: Tests unitarios (JWT, cifrado, BCrypt, servicios)
- [x] Paso 6.2: Tests de integracion (controllers con MockMvc)
- [x] Paso 6.3: Validacion final (24/24 tests, BUILD SUCCESS)

---

## Dependencias entre fases

```
Fase 1 (Flyway) ───> Fase 2 (Entidades) ───> Fase 4 (Servicios) ───> Fase 5 (Controllers)
                          |                        |                        |
                     Fase 3 (Seguridad) ──────────┘                   Fase 6 (Testing)
```

- **Fase 1** no tiene dependencias — empezar aqui
- **Fase 2** requiere Fase 1 (las entidades mapean las tablas)
- **Fase 3** puede desarrollarse en paralelo con Fase 2 (excepto el Converter que depende de SecurityProperties)
- **Fase 4** requiere Fase 2 y Fase 3
- **Fase 5** requiere Fase 4
- **Fase 6** requiere Fase 5

---

## Notas para el implementador

1. **LEER el skill `springboot4-postgresql/SKILL.md` ANTES de cada fase.** Es obligatorio y contiene restricciones tecnicas criticas.
2. **Comentarios en espanol** en todo el codigo Java.
3. **No usar `spring.jpa.hibernate.ddl-auto=create`** — Flyway controla el esquema. Solo `none` (local) o `validate` (produccion).
4. **No almacenar secretos en codigo.** Siempre via variables de entorno o archivos montados.
5. **IP de origen:** Siempre usar `X-Forwarded-For` con fallback. En K8s, la IP real del cliente esta en ese header.
6. **Campos `@Lob` / BYTEA (foto):** Para el campo `foto` del paciente, considerar servir via endpoint dedicado en lugar de incluir en el DTO principal (rendimiento).
7. **Compatibilidad desktop:** El desktop tiene conexion JDBC directa a la misma BD. Las migraciones Flyway NO deben romper las queries existentes del desktop.
8. **Hibernate Envers `store_data_at_delete: true`** ya configurado en application.yml — en un DELETE, Envers almacena los datos completos del registro eliminado.

---

## Fase 6: Contenerizacion y Despliegue en Kubernetes

Esta fase establece la estrategia de contenerizacion Docker y el despliegue en Kubernetes para el servicio API. Los manifiestos base ya existen en `/infra/k8s/base/api/` y deben actualizarse segun esta especificacion.

### Paso 6.1: Crear Dockerfile en `/api/Dockerfile`

Crear un Dockerfile multi-stage en el directorio raiz del servicio API:

**Stage 1 - Builder:**
```dockerfile
FROM eclipse-temurin:24-jdk-alpine AS builder
WORKDIR /build
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw clean package -DskipTests -B
```

**Stage 2 - Runtime:**
```dockerfile
FROM eclipse-temurin:24-jre-alpine AS runtime
RUN addgroup -g 1000 appgroup && adduser -u 1000 -G appgroup -D -h /app appuser
WORKDIR /app
RUN mkdir -p /app/tmp /app/cache && chown -R 1000:1000 /app
COPY --from=builder --chown=1000:1000 /build/target/*.jar /app/app.jar
USER 1000:1000
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.io.tmpdir=/app/tmp", "-jar", "app.jar"]
```

Crear `/api/.dockerignore`:
```
target/
.mvn/repository/
.git
.idea
*.iml
.env
.env.*
```

**Nota:** Este Dockerfile reemplaza al existente en `/infra/docker/api/Dockerfile`. El contexto de build es el directorio `/api/` (no la raiz del monorepo).

### Paso 6.2: Actualizar Deployment a 3 replicas

Modificar los manifiestos K8s existentes para garantizar alta disponibilidad:

- `/infra/k8s/base/api/deployment.yaml`: cambiar `replicas: 2` a `replicas: 3`
- `/infra/k8s/base/api/hpa.yaml`: cambiar `minReplicas: 2` a `minReplicas: 3`

### Paso 6.3: Crear ConfigMap `rehabiapp-api-config`

Crear `/infra/k8s/base/api/configmap.yaml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: rehabiapp-api-config
  namespace: rehabiapp-api
  labels:
    app: rehabiapp-api
    tier: backend
data:
  SPRING_PROFILES_ACTIVE: "production"
  SERVER_PORT: "8080"
  JAVA_TOOL_OPTIONS: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
  REHABIAPP_DATA_SERVICE_URL: "http://rehabiapp-data.rehabiapp-data.svc.cluster.local:8081"
  MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: "health,info,prometheus"
```

Actualizar `/infra/k8s/base/api/kustomization.yaml` para incluir `- configmap.yaml` en la lista de resources.

### Paso 6.4: Refactorizar variables de entorno del Deployment

Modificar la seccion `containers[0]` del Deployment para separar configuracion general (ConfigMap) de credenciales (Secrets):

```yaml
envFrom:
  - configMapRef:
      name: rehabiapp-api-config
env:
  - name: SPRING_DATASOURCE_USERNAME
    valueFrom:
      secretKeyRef:
        name: postgresql-credentials
        key: username
  - name: SPRING_DATASOURCE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: postgresql-credentials
        key: password
  - name: JWT_SIGNING_KEY
    valueFrom:
      secretKeyRef:
        name: api-secrets
        key: jwt-signing-key
  - name: ENCRYPTION_KEY
    valueFrom:
      secretKeyRef:
        name: api-secrets
        key: encryption-key
```

Eliminar las variables hardcodeadas (`SPRING_PROFILES_ACTIVE`, `SERVER_PORT`) que ahora provienen del ConfigMap.

### Paso 6.5: Topologia K8s del servicio API

Resumen completo de la arquitectura Kubernetes para referencia del implementador:

| Recurso | Nombre | Especificacion |
|---------|--------|----------------|
| Deployment | `rehabiapp-api` | 3 replicas, puerto 8080 |
| Service | `rehabiapp-api` | ClusterIP, puerto 8080 |
| ConfigMap | `rehabiapp-api-config` | 5 claves de configuracion general |
| Secrets | `postgresql-credentials`, `api-secrets` | Credenciales BD + JWT + AES key |
| HPA | `rehabiapp-api` | min 3, max 6 replicas, CPU 70%, mem 80% |
| PDB | `rehabiapp-api` | minAvailable: 1 |
| NetworkPolicy | `allow-api-traffic` | Ingress: ingress-nginx + mobile-backend; Egress: PostgreSQL:5432 + data:8081 + kube-dns |
| ServiceAccount | `rehabiapp-api-sa` | IRSA en overlay AWS |

**Probes:**
- Startup: `GET /actuator/health/liveness` (initialDelaySeconds: 10, failureThreshold: 30)
- Liveness: `GET /actuator/health/liveness` (periodSeconds: 10)
- Readiness: `GET /actuator/health/readiness` (periodSeconds: 5)

**Recursos por pod:**
- Requests: 250m CPU, 512Mi memoria
- Limits: 1000m CPU, 1Gi memoria

**Seguridad (ENS Alto):**
- Usuario no-root UID 1000:1000
- `readOnlyRootFilesystem: true` (volumenes emptyDir en `/tmp` y `/app/cache`)
- `allowPrivilegeEscalation: false`
- Drop ALL capabilities
- seccomp profile: RuntimeDefault

### Checklist Fase 6

- [x] Paso 6.1: Dockerfile creado en `/api/Dockerfile` (multi-stage, Eclipse Temurin 24, UID 1000)
- [x] Paso 6.1: `.dockerignore` creado en `/api/.dockerignore`
- [x] Paso 6.2: Deployment replicas actualizado de 2 a 3
- [x] Paso 6.2: HPA minReplicas actualizado de 2 a 3
- [x] Paso 6.3: ConfigMap `rehabiapp-api-config` creado en `/infra/k8s/base/api/configmap.yaml`
- [x] Paso 6.3: Kustomization actualizado para incluir configmap.yaml
- [x] Paso 6.4: Deployment refactorizado con `envFrom` ConfigMap + `env` Secret refs
- [x] Verificacion: `docker build -t rehabiapp-api:dev -f api/Dockerfile api/` ejecutado con exito
- [x] Verificacion: `kubectl kustomize infra/k8s/overlays/local/` valida sin errores
