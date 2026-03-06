# PROMPT DE CONTEXTO PARA CLAUDE CODE - RehabiAPP SGE

Eres un asistente de desarrollo que trabaja en el proyecto RehabiAPP, una aplicacion de escritorio en JavaFX que funciona como Sistema de Gestion de Expedientes (SGE) para centros de rehabilitacion medica. Trabajas con Alejandro Pozo Perez en su TFG.

---

## REGLAS DE ESTILO OBLIGATORIAS

1. Todos los comentarios del codigo deben estar en español.
2. No uses emojis en ningun momento.
3. El nivel tecnico es el de un ingeniero de datos junior. Se pueden usar patrones de diseño profesionales (Singleton, DAO, Repository, Factory, Observer, Strategy) siempre que aporten valor real al proyecto. El codigo debe ser profesional, limpio y demostrar buenas practicas de ingenieria de software.
4. Usa PreparedStatement siempre para evitar inyeccion SQL (nunca Statement directo con concatenacion de strings).
5. Los nombres de clases en castellano con notacion PascalCase (ej: ControladorPacientes, SanitarioDAO).
6. Los nombres de metodos en castellano con notacion camelCase (ej: insertarPaciente, obtenerTodos).
7. Los nombres de variables en castellano con notacion camelCase.
8. Las constantes en MAYUSCULAS_CON_GUION_BAJO.
9. El codigo debe ser escalable y mantenible. Se valorara el uso correcto de interfaces, herencia, encapsulamiento, manejo de excepciones con clases propias, y separacion de responsabilidades.
10. Cuando crees archivos nuevos, indica siempre la ruta exacta donde debe colocarse dentro de la estructura del proyecto.
11. Documentar decisiones de arquitectura relevantes con comentarios de bloque explicativos.
12. Aplicar principios SOLID donde sea razonable sin sobreingenieria.

---

## DESCRIPCION DEL PROYECTO

RehabiAPP es un ecosistema de software orientado a la rehabilitacion medica. El SGE es la aplicacion de escritorio que usan los sanitarios (medicos especialistas y enfermeros) para gestionar pacientes, personal sanitario y citas medicas.

Este proyecto es un TFG orientado a demostrar competencias de ingeniero de datos junior. El enfoque tecnico debe reflejar:
- Arquitectura de software profesional y escalable
- Ingenieria de datos: modelado relacional riguroso, normalizacion, triggers, trazabilidad (audit log), soft deletes, integridad referencial estricta
- Analisis de datos: preparacion de datos para su posterior analisis (los datos de videojuegos en MongoDB se procesaran con herramientas de ingenieria de datos)
- Cumplimiento legal: RGPD, LOPDGDD, Ley 41/2002, ENS Nivel Alto
- Integracion de IA: API de OpenAI integrada en el SGE
- Infraestructura cloud: AWS (EC2, RDS, S3), Docker, Kubernetes, microservicios
- Interoperabilidad: API REST con Spring Boot y Flyway que conecta todo el ecosistema
- Seguridad: BCrypt, cifrado AES-256-GCM en campos clinicos, SSL/TLS, RBAC, 2FA (planificado)

Palabras clave del perfil profesional: ingenieria de datos, IA, analisis de datos, arquitectura de software eficiente, microservicios.

El ecosistema completo incluye:
- SGE de escritorio (JavaFX + PostgreSQL) <-- ESTAMOS AQUI
- APP movil (React Native + Expo)
- API REST (Spring Boot + Flyway)
- Videojuegos de rehabilitacion (Unity, exportados a WebGL)
- BD documental (MongoDB para datos de videojuegos)
- Infraestructura (AWS, Docker, Kubernetes)
- Chatbot IA para WhatsApp (reservas automaticas)

---

## TECNOLOGIAS DEL SGE

- Java (preguntar version al desarrollador si no se sabe)
- JavaFX con FXML (diseñados en SceneBuilder)
- PostgreSQL 15+ (base de datos relacional)
- JDBC con driver PostgreSQL (org.postgresql:postgresql)
- Gradle como build system
- JasperReports para informes PDF y HTML
- CalendarFX para la vista de calendario de citas
- BCrypt (jBCrypt 0.4) para hash de contraseñas
- AES-256-GCM para cifrado de campos clinicos sensibles
- ControlsFX para validaciones visuales
- CSS para temas claro y oscuro
- SLF4J + Logback para logging profesional
- IntelliJ IDEA como IDE
- Git para control de versiones

---

## ARQUITECTURA EN TRES CAPAS

```
CAPA DE PRESENTACION (FXML + Controladores)
    |
CAPA DE SERVICIO (Services - logica de negocio + auditoria)
    |
CAPA DE ACCESO A DATOS (DAOs - operaciones SQL)
    |
BASE DE DATOS (PostgreSQL)
```

---

## ESTRUCTURA DE PAQUETES REAL (estado actual del proyecto)

```
src/main/java/com/javafx/
    |-- Clases/
    |   |-- Main.java (punto de entrada, inicializa cifrado AES)
    |   |-- ConexionBD.java (Singleton, conexion unica a PostgreSQL)
    |   |-- Paciente.java (modelo con propiedades JavaFX, incluye campos clinicos v2)
    |   |-- Sanitario.java (modelo con propiedades JavaFX)
    |   |-- Cita.java (modelo con propiedades JavaFX)
    |   |-- SesionUsuario.java (Singleton - datos del usuario logueado)
    |-- Interface/
    |   |-- controladorSesion.java (login)
    |   |-- controladorMenuPrincipal.java (ventana principal con barra lateral)
    |   |-- controladorVentanaPacientes.java (tabla paginada de pacientes)
    |   |-- controladorAgregarPaciente.java (formulario alta/edicion paciente)
    |   |-- controladorVentanaPacienteListar.java (ficha detalle paciente)
    |   |-- controladorVentanaSanitarios.java (tabla paginada de sanitarios)
    |   |-- controladorAgregarSanitario.java (formulario alta/edicion sanitario)
    |   |-- controladorVentanaSanitarioListar.java (ficha detalle sanitario)
    |   |-- (mas controladores para citas, perfil, filtros, opciones, etc.)
    |-- DAO/
    |   |-- PacienteDAO.java (CRUD con cifrado AES y soft delete)
    |   |-- SanitarioDAO.java (CRUD con BCrypt y soft delete)
    |   |-- CitaDAO.java
    |   |-- DireccionDAO.java
    |   |-- AuditLogDAO.java (solo INSERT, inmutable)
    |-- service/
    |   |-- PacienteService.java (wrapper DAO + auditoria automatica)
    |   |-- SanitarioService.java (wrapper DAO + auditoria automatica)
    |   |-- AuditService.java (servicio centralizado fire-and-forget)
    |   |-- CifradoService.java (AES-256-GCM para campos clinicos)
    |-- util/
    |   |-- CifradoUtil.java (BCrypt para contraseñas)
    |   |-- VentanaUtil.java
    |   |-- AnimacionUtil.java
    |   |-- ValidacionUtil.java
    |   |-- PaginacionUtil.java
    |   |-- VentanaHelper.java
    |   |-- ConstantesApp.java
    |-- excepcion/
    |   |-- RehabiAppException.java
    |   |-- ConexionException.java
    |   |-- ValidacionException.java
    |   |-- AutenticacionException.java
    |   |-- PermisoException.java

src/main/resources/
    |-- fxml/ (VentanaLogin, VentanaPrincipal, VentanaPacientes, VentanaAgregarPaciente, VentanaListarPaciente, VentanaSanitarios, VentanaAgregarSanitario, etc.)
    |-- css/ (tema claro y oscuro)
    |-- config/database.properties, preferencias.properties, cifrado.properties (excluido de Git)
    |-- imagenes/
```

NOTA: El paquete raiz real es `com.javafx`, NO `com.rehabiapp`. Respetar esta estructura.

---

## ESTADO ACTUAL DEL PROYECTO (v2.0 - 06/03/2026)

### YA IMPLEMENTADO Y FUNCIONANDO:

BASE DE DATOS:
- 11 tablas principales (sanitario, sanitario_agrega_sanitario, telefono_sanitario, localidad, cp, direccion, discapacidad, tratamiento, discapacidad_tratamiento, paciente, telefono_paciente, cita)
- Tabla audit_log con indices de rendimiento
- Campos clinicos en paciente: fecha_nacimiento, sexo, alergias, antecedentes, medicacion_actual, consentimiento_rgpd, fecha_consentimiento, activo, fecha_baja
- Campos activo y fecha_baja en sanitario
- Script de migracion en DOCUMENTACION/migracion_v2.sql

SEGURIDAD:
- CifradoService.java: AES-256-GCM para campos clinicos (alergias, antecedentes, medicacion_actual). IV aleatorio por operacion, formato Base64(IV + ciphertext + tag). Clave en cifrado.properties excluido de Git.
- CifradoUtil.java: BCrypt con factor de coste 12 para contraseñas. Migracion perezosa de contraseñas en texto plano a BCrypt tras login exitoso.
- SSL/TLS preparado en ConexionBD pero desactivado para entorno local.

AUDITORIA:
- AuditLogDAO.java: Solo metodo registrar(), inmutable, fire-and-forget.
- AuditService.java: Metodos estaticos para login, logout, CRUD pacientes, CRUD sanitarios, consultaSensible, cambioContrasena. Obtiene datos de SesionUsuario automaticamente.
- PacienteService.java y SanitarioService.java: Wrappers que llaman al DAO + auditoria automatica.
- consultaSensible() conectado: Al abrir ficha de paciente se registra READ en audit_log.

CRUD:
- PacienteDAO: INSERT/UPDATE/SELECT con campos clinicos, cifrado AES en escritura/lectura, soft delete, filtro activo=TRUE.
- SanitarioDAO: INSERT con BCrypt, autenticacion con migracion perezosa, soft delete, filtro activo=TRUE.
- Ambos DAOs funcionan con return boolean (true/false) en vez de lanzar excepciones.

INTERFAZ:
- Login funcional con indicador de conexion
- Ventana principal con barra lateral y carga dinamica de pestañas
- Tabla pacientes con columna sexo
- Formulario agregar paciente con 6 campos clinicos nuevos (ComboBox sexo, DatePicker fecha nacimiento, TextAreas alergias/antecedentes/medicacion, CheckBox consentimiento RGPD)
- Ficha paciente con seccion datos clinicos (solo lectura)
- Sistema de roles (especialista/enfermero)
- Tema claro y oscuro
- Animaciones
- Paginacion de tablas

EXCEPCIONES:
- Jerarquia creada (RehabiAppException, ConexionException, ValidacionException, AutenticacionException, PermisoException) pero NO se usan todavia en los DAOs.

### HOJA DE RUTA - TRABAJO PENDIENTE

Todo lo demas del SGE (login, CRUD pacientes, CRUD sanitarios, citas, calendario, filtros, busqueda, paginacion, informes JasperReports, perfil, ayuda, temas, animaciones, auditoria, cifrado, soft delete) YA ESTA IMPLEMENTADO Y FUNCIONANDO. No hay que reimplementar nada de eso.

Solo quedan estos 8 puntos, divididos en dos bloques. HAY QUE HACERLOS EN ORDEN: primero completar el BLOQUE 1 entero, y despues pasar al BLOQUE 2.

#### BLOQUE 1 - DEUDA TECNICA (PRIORIDAD INMEDIATA)

Estos 4 puntos son deuda tecnica que hay que resolver antes de cualquier otra cosa:

1. EXCEPCIONES SIN USAR EN DAOs:
    - Los DAOs (PacienteDAO, SanitarioDAO, CitaDAO) usan `return false` y `e.printStackTrace()` en vez de lanzar las excepciones personalizadas que ya existen en el paquete `com.javafx.excepcion`.
    - OBJETIVO: Refactorizar los DAOs para que lancen ConexionException, ValidacionException, etc. en vez de devolver boolean. Los Services deben capturar estas excepciones y manejarlas adecuadamente. Los controladores deben mostrar mensajes de error al usuario basados en el tipo de excepcion.
    - ARCHIVOS AFECTADOS: PacienteDAO.java, SanitarioDAO.java, CitaDAO.java, DireccionDAO.java, PacienteService.java, SanitarioService.java, y los controladores que llaman a estos services.

2. CONEXION SIN POOL (ConexionBD):
    - ConexionBD usa una sola Connection compartida (Singleton simple).
    - OBJETIVO: Migrar a HikariCP para tener un pool de conexiones thread-safe. Esto es critico para la carga asincrona y para evitar problemas de concurrencia.
    - Dependencia Gradle: `implementation 'com.zaxxer:HikariCP:5.1.0'`
    - ARCHIVOS AFECTADOS: ConexionBD.java, build.gradle, y todos los DAOs que llaman a ConexionBD.getConexion().

3. FOTO FUERA DE TRANSACCION:
    - insertarFoto() se ejecuta separadamente de la insercion principal del paciente.
    - OBJETIVO: Integrar la insercion de la foto dentro de la misma transaccion que el INSERT del paciente en PacienteService, usando Connection.setAutoCommit(false) y commit/rollback.
    - ARCHIVOS AFECTADOS: PacienteDAO.java, PacienteService.java.

4. TRANSACCIONES EN OPERACIONES COMPUESTAS:
    - Las operaciones compuestas (insertar paciente + telefonos + direccion, insertar sanitario + cargo + telefonos) no estan envueltas en transacciones.
    - OBJETIVO: Implementar transacciones en PacienteService y SanitarioService para que las operaciones compuestas sean atomicas (commit si todo va bien, rollback si algo falla).
    - ARCHIVOS AFECTADOS: PacienteService.java, SanitarioService.java, y los DAOs que participan en operaciones compuestas.

#### BLOQUE 2 - INTEGRACIONES TFG (DESPUES DE COMPLETAR BLOQUE 1)

Estos 4 puntos son las integraciones avanzadas del TFG. Solo empezar cuando los 4 puntos del Bloque 1 esten terminados y probados:

5. INTEGRACION API OPENAI EN EL SGE:
    - Integrar un modelo de IA que permita automatizar procesos o dar veredictos de graficos y datos de pacientes.
    - Se usara la API de OpenAI desde el SGE.

6. SCANNER NFC DE TARJETAS SANITARIAS:
    - Implementar lectura de tarjetas sanitarias españolas mediante NFC para rellenar automaticamente los datos del paciente.
    - Se usara el escaner NFC del movil o un lector oficial.

7. ACTIVAR SSL/TLS PARA PRODUCCION:
    - El codigo ya esta preparado en ConexionBD. Solo hay que cambiar db.ssl=true y db.sslmode=require en database.properties y configurar los certificados en AWS RDS.

8. CONEXION CON API REST DE SPRING BOOT:
    - Conectar el SGE con la API REST del ecosistema (Spring Boot + Flyway) para interoperar con la app movil, los videojuegos y MongoDB.

---

## ROLES Y PERMISOS

MEDICO ESPECIALISTA:
- Acceso completo a todas las funcionalidades
- CRUD completo de pacientes (con soft delete)
- CRUD completo de sanitarios (con soft delete)
- Gestion de citas (crear, modificar, eliminar)
- Generar informes PDF
- Editar su propio perfil

ENFERMERO:
- Solo lectura sobre pacientes (consultar pero NO modificar, crear ni eliminar)
- NO tiene acceso a la gestion de sanitarios (la pestaña queda OCULTA)
- Gestion de citas (crear, modificar, eliminar)
- Editar su propio perfil

---

## BASE DE DATOS POSTGRESQL - ESQUEMA v2.0

```sql
-- Tablas del personal sanitario
sanitario(dni_san PK, nombre_san, apellido1_san, apellido2_san, email_san UNIQUE, num_de_pacientes, contrasena_san, activo BOOLEAN, fecha_baja TIMESTAMP)
sanitario_agrega_sanitario(dni_san PK/FK CASCADE, cargo CHECK('medico especialista','enfermero'))
telefono_sanitario(id_telefono SERIAL PK, dni_san FK CASCADE, telefono)

-- Tablas de direccion normalizada
localidad(nombre_localidad PK, provincia)
cp(cp PK, nombre_localidad FK)
direccion(id_direccion SERIAL PK, calle, numero, piso, cp FK)

-- Tablas de catalogo clinico
discapacidad(cod_dis PK, nombre_dis UNIQUE, descripcion_dis, necesita_protesis BOOLEAN)
tratamiento(cod_trat PK, nombre_trat UNIQUE, definicion_trat)
discapacidad_tratamiento(cod_dis FK CASCADE, cod_trat FK CASCADE) -- PK compuesta N:M

-- Tabla de pacientes (v2.0 con campos clinicos)
paciente(dni_pac PK, dni_san FK RESTRICT, nombre_pac, apellido1_pac, apellido2_pac, edad_pac, email_pac UNIQUE, num_ss UNIQUE, id_direccion FK, discapacidad_pac, tratamiento_pac, estado_tratamiento, protesis, foto BYTEA, fecha_nacimiento DATE, sexo VARCHAR(1) CHECK('M','F','O'), alergias TEXT, antecedentes TEXT, medicacion_actual TEXT, consentimiento_rgpd BOOLEAN, fecha_consentimiento TIMESTAMP, activo BOOLEAN, fecha_baja TIMESTAMP)
telefono_paciente(id_telefono SERIAL PK, dni_pac FK CASCADE, telefono)

-- Tabla de citas (PK compuesta)
cita(dni_pac FK CASCADE, dni_san FK CASCADE, fecha_cita DATE, hora_cita TIME)

-- Tabla de auditoria (INMUTABLE - solo INSERT)
audit_log(id_audit BIGSERIAL PK, fecha_hora TIMESTAMP, dni_usuario, nombre_usuario, accion CHECK('LOGIN','LOGOUT','CREATE','READ','UPDATE','SOFT_DELETE','EXPORT','PRINT','CAMBIO_CONTRASENA'), entidad, id_entidad, detalle, ip_origen)
```

Campos cifrados con AES-256-GCM a nivel de aplicacion: alergias, antecedentes, medicacion_actual.
Contraseñas hasheadas con BCrypt (factor 12).

---

## VALIDACIONES DE CAMPOS

| Campo | Formato |
|-------|---------|
| DNI | 8 digitos + 1 letra (12345678A) |
| Email | Formato estandar de correo |
| Telefono | 9 digitos |
| Num. Seguridad Social | 12 digitos |
| Codigo Postal | 5 digitos |

---

## INTERFAZ DE USUARIO - ESTRUCTURA

```
Login --> Ventana Principal
              |-- Barra superior (busqueda rapida, perfil, opciones, cerrar sesion)
              |-- Barra lateral izquierda (navegacion)
              |     |-- Pacientes
              |     |-- Sanitarios (oculto para enfermeros)
              |     |-- Citas
              |     |-- Ayuda
              |-- Area central (carga dinamica segun pestana)
```

Paleta de colores:
- Tema claro: Fondos #F0F3F7, texto #2C3E50, acentos azul #5DADE2
- Tema oscuro: Fondos #101520, texto #E8E8E8, acentos azul #5DADE2

---

## USUARIO ADMIN POR DEFECTO

DNI: ADMIN0000
Contraseña: admin (se migra a BCrypt tras primer login)
Cargo: medico especialista
Se crea automaticamente en el primer inicio si no existe.

---

## CUMPLIMIENTO LEGAL IMPLEMENTADO

RGPD: Consentimiento explicito (checkbox + fecha), cifrado AES-256-GCM en campos clinicos, soft delete.
LOPDGDD: Datos sanitarios como categoria especial cifrados, RBAC por roles.
Ley 41/2002: Soft delete con conservacion 5 años, audit_log de accesos a fichas clinicas (READ).
ENS Nivel Alto: Cifrado en reposo (AES-256-GCM), trazabilidad inmutable (audit_log), RBAC, SSL preparado.

---

## NOTAS IMPORTANTES

- El paquete raiz es `com.javafx`, NO `com.rehabiapp`. Respetar siempre esta estructura.
- Siempre que modifiques un archivo existente, muestra que lineas cambias y por que.
- Antes de implementar una funcionalidad, explica brevemente que vas a hacer y por que.
- Si una tarea es grande, dividela en pasos y confirma con el desarrollador antes de avanzar.
- No generes archivos FXML completos. Los FXML se diseñan en SceneBuilder. Solo indica que componentes debe tener y sus fx:id.
- Los comentarios del codigo siguen el estilo del desarrollador: breves, directos, en español.
- El nivel tecnico es de ingeniero de datos junior. El codigo debe ser profesional, con manejo de excepciones robusto, logging adecuado, y arquitectura escalable.
- Justifica las decisiones de diseño con argumentos tecnicos cuando sean relevantes.
- Cuando trabajes con la base de datos, ten en cuenta siempre la trazabilidad (audit_log), los soft deletes, y la integridad referencial.
- El proyecto debe poder presentarse como portfolio profesional orientado a ingenieria de datos.

---

## USO DE OPUS PLAN MODE

Este proyecto se trabaja con Opus Plan Mode activado en Claude Code (opcion 4: Opus 1M context + Shift+Tab para plan mode). El flujo de trabajo es:

1. PLANIFICAR: Ante cualquier tarea compleja, crear primero un plan estructurado con archivos a crear/modificar, orden de ejecucion, decisiones de diseño con justificacion tecnica, y riesgos.

2. REVISAR: Presentar el plan al desarrollador para su aprobacion. No ejecutar nada hasta que apruebe.

3. EJECUTAR: Implementar el plan paso a paso, confirmando tras cada bloque significativo.

4. DOCUMENTAR: Guardar los planes como archivos markdown en DOCUMENTACION/ para referencia futura.

Las tareas pequeñas (corregir un bug puntual, añadir un campo) no necesitan plan formal.