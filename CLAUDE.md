# CLAUDE.md - RehabiAPP SGE

Eres un asistente de desarrollo que trabaja en el proyecto RehabiAPP, una aplicacion de escritorio en JavaFX que funciona como Sistema de Gestion de Expedientes (SGE) para centros de rehabilitacion medica. Trabajas con Alejandro Pozo Perez en su TFG.

## Build Commands

```bash
# Compilar el proyecto
./gradlew compileJava

# Ejecutar la aplicacion
./gradlew run

# Limpiar artefactos
./gradlew clean
```

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

- Java 24 con JavaFX 23 y FXML (diseñados en SceneBuilder)
- PostgreSQL 15+ (base de datos relacional)
- JDBC con driver PostgreSQL (org.postgresql:postgresql:42.7.2)
- HikariCP 5.1.0 (pool de conexiones JDBC)
- Gradle 8.13 como build system
- JasperReports 7.0.1 para informes PDF y HTML
- CalendarFX para la vista de calendario de citas
- BCrypt (jBCrypt 0.4) para hash de contraseñas
- AES-256-GCM para cifrado de campos clinicos sensibles
- ControlsFX 11.2.2 para validaciones visuales
- CSS para temas claro y oscuro
- SLF4J 2.0.9 para logging
- IntelliJ IDEA como IDE
- Git para control de versiones

---

## ARQUITECTURA EN TRES CAPAS

```
CAPA DE PRESENTACION (FXML + Controladores)
    |
CAPA DE SERVICIO (Services - logica de negocio + auditoria + transacciones)
    |
CAPA DE ACCESO A DATOS (DAOs - operaciones SQL, excepciones tipadas)
    |
BASE DE DATOS (PostgreSQL via HikariCP)
```

Flujo de excepciones: DAO lanza excepcion tipada -> Service propaga -> Controlador captura y muestra mensaje al usuario.
Flujo de transacciones: Service obtiene Connection del pool, la pasa a los DAOs, hace commit/rollback global.

---

## ESTRUCTURA DE PAQUETES (estado actual del proyecto)

```
src/main/java/com/javafx/
    |-- Clases/
    |   |-- Main.java (punto de entrada, inicializa cifrado AES, shutdown hook para pool)
    |   |-- ConexionBD.java (HikariCP pool: maxPoolSize=10, minIdle=2, connectionTimeout=10s)
    |   |-- Paciente.java (modelo con propiedades JavaFX, incluye campos clinicos v2)
    |   |-- Sanitario.java (modelo con propiedades JavaFX)
    |   |-- Cita.java (modelo con propiedades JavaFX)
    |   |-- SesionUsuario.java (Singleton - datos del usuario logueado)
    |   |-- VentanaUtil.java, AnimacionUtil.java, InformeService.java, Persona.java
    |-- Interface/
    |   |-- controladorSesion.java (login con AuditService.login/logout)
    |   |-- controladorVentanaPrincipal.java (ventana principal con barra lateral)
    |   |-- controladorVentanaPacientes.java (tabla paginada de pacientes con col sexo)
    |   |-- controladorAgregarPaciente.java (formulario alta/edicion con campos clinicos + ValidacionException)
    |   |-- controladorVentanaPacienteListar.java (ficha detalle con datos clinicos + auditoria READ)
    |   |-- controladorVentanaSanitarios.java (tabla paginada de sanitarios)
    |   |-- controladorAgregarSanitario.java (formulario alta/edicion sanitario + ValidacionException)
    |   |-- controladorVentanaSanitarioListar.java (ficha detalle sanitario)
    |   |-- controladorVentanaOpciones.java (tema claro/oscuro + tamaño fuente)
    |   |-- (mas controladores para citas, perfil, filtros, etc.)
    |-- DAO/
    |   |-- BaseDAO.java (ejecutarTransaccion con conn.close() en finally)
    |   |-- PacienteDAO.java (void + excepciones tipadas, sobrecargas con Connection, cifrado AES, soft delete)
    |   |-- SanitarioDAO.java (void + excepciones tipadas, sobrecargas con Connection, BCrypt, soft delete)
    |   |-- CitaDAO.java (void + excepciones tipadas)
    |   |-- DireccionDAO.java
    |   |-- AuditLogDAO.java (solo INSERT, inmutable, fire-and-forget)
    |-- service/
    |   |-- PacienteService.java (transacciones atomicas: paciente+tel+foto, auditoria automatica)
    |   |-- SanitarioService.java (transacciones atomicas: sanitario+cargo+tel, auditoria automatica)
    |   |-- CitaService.java (void + excepciones)
    |   |-- AuditService.java (servicio centralizado fire-and-forget)
    |   |-- CifradoService.java (AES-256-GCM para campos clinicos)
    |-- util/
    |   |-- CifradoUtil.java (BCrypt para contraseñas)
    |   |-- PaginacionUtil.java
    |   |-- VentanaHelper.java
    |   |-- ConstantesApp.java
    |-- excepcion/
    |   |-- RehabiAppException.java (base, extiende RuntimeException, campo codigoError)
    |   |-- ConexionException.java (codigo "BD_CONEXION")
    |   |-- ValidacionException.java (codigo "VALIDACION", campo campo)
    |   |-- DuplicadoException.java (codigo "DUPLICADO", campo campo)
    |   |-- AutenticacionException.java (codigo "AUTENTICACION")
    |   |-- PermisoException.java (codigo "PERMISO_DENEGADO")

src/main/resources/
    |-- *.fxml (VentanaLogin, VentanaPrincipal, VentanaPacientes, VentanaAgregarPaciente, VentanaListarPaciente, VentanaSanitarios, etc.)
    |-- css/ (tema_claro.css, tema_oscuro.css)
    |-- config/ (database.properties, preferencias.properties, cifrado.properties [excluido de Git])
    |-- imagenes/
```

NOTA CRITICA: El paquete raiz real es `com.javafx`, NO `com.rehabiapp`. Respetar siempre esta estructura.

---

## PATRONES DE REFERENCIA RAPIDA

### Abrir ventana modal (OBLIGATORIO aplicar CSS)

```java
FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaName.fxml"));
Parent root = loader.load();
ControllerClass controlador = loader.getController();

Scene scene = new Scene(root);
controladorVentanaOpciones.aplicarConfiguracionAScene(scene); // SIEMPRE

Stage stage = new Stage();
stage.setTitle("Titulo");
stage.setScene(scene);
stage.initModality(Modality.APPLICATION_MODAL);
stage.setResizable(false);
VentanaUtil.establecerIconoVentana(stage);
stage.showAndWait();
```

### Comprobar permisos

```java
SesionUsuario sesion = SesionUsuario.getInstancia();
if (sesion.esEspecialista()) {
    // Acceso completo
} else {
    // Solo lectura (enfermero)
}
```

### Operaciones de base de datos (patron actual v2.1)

```java
// Los DAOs lanzan excepciones tipadas. Los Services gestionan transacciones + auditoria.
// Los controladores capturan excepciones y muestran mensajes al usuario.
PacienteService pacienteService = new PacienteService();
try {
    pacienteService.insertar(paciente, tel1, tel2, archivoFoto); // transaccion atomica
    VentanaUtil.mostrarVentanaInformativa("Registrado correctamente", TipoMensaje.EXITO);
} catch (DuplicadoException e) {
    VentanaUtil.mostrarVentanaInformativa("Duplicado: " + e.getCampo(), TipoMensaje.ERROR);
} catch (ConexionException e) {
    VentanaUtil.mostrarVentanaInformativa("Error de conexion", TipoMensaje.ERROR);
} catch (RehabiAppException e) {
    VentanaUtil.mostrarVentanaInformativa("Error: " + e.getMessage(), TipoMensaje.ERROR);
}
```

### Validaciones en formularios (patron actual v2.1)

```java
// validarCampos() lanza ValidacionException con el nombre del campo
try {
    validarCampos();
} catch (ValidacionException e) {
    VentanaUtil.mostrarVentanaInformativa(e.getMessage(), TipoMensaje.ADVERTENCIA);
    return;
}
```

### Transacciones compuestas en Services (patron actual v2.1)

```java
// El Service obtiene UNA Connection del pool, la pasa a los DAOs, commit/rollback global
Connection conn = ConexionBD.getConexion();
conn.setAutoCommit(false);
try {
    dao.insertar(conn, entidad);
    dao.insertarTelefonos(conn, dni, tel1, tel2);
    conn.commit();
    AuditService.insertEntidad(dni);
} catch (Exception e) {
    conn.rollback();
    throw e;
} finally {
    conn.close(); // devuelve al pool
}
```

---

## ESTADO ACTUAL DEL PROYECTO (v2.1 - 07/03/2026)

### YA IMPLEMENTADO Y FUNCIONANDO:

BASE DE DATOS:
- 12 tablas (sanitario, sanitario_agrega_sanitario, telefono_sanitario, localidad, cp, direccion, discapacidad, tratamiento, discapacidad_tratamiento, paciente, telefono_paciente, cita) + audit_log
- Campos clinicos en paciente: fecha_nacimiento, sexo, alergias, antecedentes, medicacion_actual, consentimiento_rgpd, fecha_consentimiento, activo, fecha_baja
- Campos activo y fecha_baja en sanitario
- Indices parciales en activo=TRUE para paciente y sanitario
- Indices de rendimiento en audit_log (fecha, usuario, entidad)
- Script de migracion idempotente en DOCUMENTACION/migracion_v2.sql
- NOTA: Existen tablas de catalogo clinico (discapacidad, tratamiento, discapacidad_tratamiento) pero NO estan conectadas con la tabla paciente. Los campos discapacidad_pac y tratamiento_pac son texto libre. La integracion es la TAREA ACTUAL (ver seccion TAREA ACTUAL).

POOL DE CONEXIONES (HikariCP):
- ConexionBD.java reescrito: singleton HikariDataSource en vez de Connection unica.
- maxPoolSize=10, minIdle=2, connectionTimeout=10s, idleTimeout=300s, maxLifetime=600s.
- Cada llamada a getConexion() devuelve Connection independiente del pool.
- Main.stop() cierra el pool al salir de la aplicacion.
- Todos los DAOs con transacciones manuales cierran Connection en finally.

EXCEPCIONES TIPADAS EN TODOS LOS DAOs:
- DAOs lanzan excepciones tipadas (DuplicadoException, ConexionException, ValidacionException) en vez de return boolean.
- Metodo traducirSQLException() en cada DAO mapea sqlState a excepcion personalizada (23505->DuplicadoException, 23503->ValidacionException, 08xxx->ConexionException).
- DuplicadoException.java creada con campo String campo para identificar el campo duplicado.
- Services propagan excepciones al controlador (void en vez de boolean).
- Controladores capturan excepciones especificas y muestran mensajes claros.
- Controladores de citas usan Task<Void> + setOnFailed() para operaciones asincronas.

TRANSACCIONES COMPUESTAS ATOMICAS:
- PacienteService: insertar(paciente, tel1, tel2, archivoFoto) = paciente + telefonos + foto en UNA transaccion. Si algo falla, rollback completo.
- SanitarioService: insertar(sanitario, tel1, tel2) = sanitario + cargo + telefonos en UNA transaccion.
- DAOs tienen sobrecargas que reciben Connection externa: insertar(Connection, entidad), insertarTelefonos(Connection, dni, t1, t2), insertarFoto(Connection, dni, archivo).
- Los metodos sin Connection delegan en los nuevos gestionando su propia transaccion.
- Foto integrada en la misma transaccion que el paciente (ya no se ejecuta separadamente).

VALIDACIONES EN FORMULARIOS:
- validarCampos() en controladorAgregarPaciente y controladorAgregarSanitario lanza ValidacionException con el nombre del campo que fallo.
- Validaciones nuevas alineadas con BD: numero de direccion obligatorio y numerico (INT NOT NULL), provincia obligatoria (NOT NULL), dos apellidos obligatorios (apellido1 y apellido2 NOT NULL).
- Validadores ControlsFX registrados para numero y provincia.
- El llamante captura ValidacionException y muestra mensaje con TipoMensaje.ADVERTENCIA.

SEGURIDAD:
- CifradoService.java: AES-256-GCM (NIST SP 800-38D) para campos clinicos (alergias, antecedentes, medicacion_actual). IV aleatorio 12 bytes por operacion, tag 128 bits, formato Base64(IV + ciphertext + tag). Clave en cifrado.properties excluido de Git via .gitignore.
- CifradoUtil.java: BCrypt factor 12 para contraseñas. Migracion perezosa de texto plano a BCrypt tras login exitoso.
- SSL/TLS preparado en ConexionBD (db.ssl y db.sslmode en database.properties), desactivado para entorno local.

AUDITORIA (ENS Nivel Alto):
- AuditLogDAO.java: Solo metodo registrar(), inmutable, fire-and-forget.
- AuditService.java: Metodos estaticos para login, logout, CRUD pacientes, CRUD sanitarios, consultaSensible, cambioContrasena.
- PacienteService.java y SanitarioService.java: Wrappers DAO + auditoria automatica tras commit.
- consultaSensible() conectado en controladorVentanaPacienteListar: al abrir ficha de paciente se registra READ en audit_log.
- Login/logout registrados en controladorSesion.
- cambioContrasena registrado en controladorPerfil.

INTERFAZ COMPLETA:
- Login funcional con indicador de conexion, animaciones, boton Enter por defecto
- Ventana principal con barra lateral y carga dinamica de pestañas
- Tabla pacientes con columna sexo, paginacion, busqueda, filtros avanzados
- Formulario agregar paciente con 6 campos clinicos (ComboBox sexo, DatePicker fecha nacimiento, TextAreas alergias/antecedentes/medicacion, CheckBox consentimiento RGPD)
- Ficha paciente con seccion datos clinicos (solo lectura, ScrollPane)
- CRUD sanitarios completo (tabla, formulario, ficha, filtros)
- Gestion de citas con CalendarFX
- Sistema de roles (especialista/enfermero) con permisos diferenciados
- Tema claro y oscuro con tamaño de fuente configurable
- Animaciones (FadeIn, brillo, hover)
- Paginacion de tablas (50 registros/pagina)
- Informes JasperReports (PDF individual y listado)
- Perfil de usuario con cambio de contraseña auditado
- Ventanas informativas y de confirmacion

CUMPLIMIENTO LEGAL:
- RGPD: Consentimiento explicito (checkbox + fecha), cifrado AES-256-GCM en campos clinicos, soft delete, registro de actividades.
- LOPDGDD: Datos sanitarios como categoria especial cifrados, RBAC por roles.
- Ley 41/2002: Soft delete con conservacion 5 años, audit_log de accesos a fichas clinicas (READ).
- ENS Nivel Alto: Cifrado en reposo (AES-256-GCM), trazabilidad inmutable (audit_log), RBAC, SSL preparado.

---

## TAREA ACTUAL: INTEGRACION DISCAPACIDADES, TRATAMIENTOS Y NIVELES DE PROGRESION

Esta es la tarea en curso. El documento completo con todos los detalles esta en `IMPLEMENTAR.md` en la raiz del proyecto.

### Problema actual

Las tablas de catalogo clinico (discapacidad, tratamiento, discapacidad_tratamiento) existen en la BD pero estan DESCONECTADAS de la tabla paciente. Los campos `discapacidad_pac` y `tratamiento_pac` en paciente son texto libre (VARCHAR/TEXT) sin relacion con las tablas de catalogo. Los tratamientos no tienen niveles de progresion clinica.

### Objetivo

Conectar pacientes con discapacidades y tratamientos a traves de tablas intermedias, organizando los tratamientos por niveles de progresion clinica. Un paciente tiene un nivel de progresion POR CADA discapacidad (no global). Un sanitario controla que tratamientos son visibles para cada paciente.

### Cambios en la BD (3 tablas nuevas + 1 modificacion)

```sql
-- Catalogo fijo de 4 niveles de progresion clinica
nivel_progresion(id_nivel INT PK, nombre, nombre_corto, descripcion, estado_paciente, tipos_ejercicio)

-- Relacion N:M paciente-discapacidad con nivel actual por discapacidad
paciente_discapacidad(dni_pac FK, cod_dis FK, id_nivel_actual FK DEFAULT 1, fecha_asignacion, notas) PK(dni_pac, cod_dis)

-- Tratamientos asignados a un paciente por discapacidad, con visibilidad
paciente_tratamiento(dni_pac FK, cod_trat FK, cod_dis FK, visible BOOLEAN DEFAULT FALSE, fecha_asignacion, dni_san_asigna FK) PK(dni_pac, cod_trat, cod_dis)

-- Modificacion: tratamiento ahora tiene FK a nivel_progresion
ALTER TABLE tratamiento ADD COLUMN id_nivel INT FK -> nivel_progresion(id_nivel)
```

### Los 4 niveles de progresion (datos fijos)

1. Fase Aguda (Control y Movilidad Pasiva) - Post-lesion/cirugia, dolor agudo, movilizacion pasiva
2. Fase Subaguda (Recuperacion del ROM) - Dolor reducido, movilidad activo-asistida
3. Fortalecimiento (Resistencia Progresiva) - Recorrido casi completo, ejercicios isotonicos
4. Funcional (Vuelta a la Normalidad) - Fuerza recuperada, propiocepcion y equilibrio

### Logica de visibilidad de tratamientos

Para que un tratamiento aparezca en la ficha de un paciente se deben cumplir 3 condiciones:
1. El tratamiento esta asociado a una discapacidad que tiene el paciente (via discapacidad_tratamiento)
2. El tratamiento pertenece al mismo nivel en que esta el paciente para esa discapacidad
3. El sanitario ha marcado ese tratamiento como visible para ese paciente

### Campos a deprecar en tabla paciente

`discapacidad_pac` y `tratamiento_pac` quedan OBSOLETOS. Mantenerlos como legacy hasta que la logica nueva este probada, despues eliminar.

### Consulta SQL clave

```sql
-- Tratamientos activos de un paciente, agrupados por discapacidad y nivel
SELECT d.nombre_dis, np.nombre_corto AS nivel_actual, t.nombre_trat, t.definicion_trat,
       pt.visible, pt.fecha_asignacion
FROM paciente_discapacidad pd
JOIN discapacidad d ON pd.cod_dis = d.cod_dis
JOIN nivel_progresion np ON pd.id_nivel_actual = np.id_nivel
JOIN discapacidad_tratamiento dt ON dt.cod_dis = pd.cod_dis
JOIN tratamiento t ON dt.cod_trat = t.cod_trat AND t.id_nivel = pd.id_nivel_actual
LEFT JOIN paciente_tratamiento pt ON pt.dni_pac = pd.dni_pac
    AND pt.cod_trat = t.cod_trat AND pt.cod_dis = pd.cod_dis
WHERE pd.dni_pac = ?
ORDER BY d.nombre_dis, np.id_nivel, t.nombre_trat;
```

### Orden de implementacion

1. Crear tabla nivel_progresion con los 4 registros de catalogo
2. Añadir columna id_nivel a tratamiento y actualizar tratamientos existentes
3. Crear tabla paciente_discapacidad
4. Crear tabla paciente_tratamiento
5. Crear indices de rendimiento necesarios
6. Crear/actualizar DAOs: NivelProgresionDAO (nuevo), DiscapacidadDAO (actualizar), TratamientoDAO (actualizar), PacienteDiscapacidadDAO (nuevo), PacienteTratamientoDAO (nuevo)
7. Crear/actualizar Services correspondientes
8. Actualizar interfaz grafica: ficha del paciente con discapacidades, niveles y tratamientos visibles
9. NO eliminar discapacidad_pac y tratamiento_pac de paciente hasta que todo este probado

### Diagrama de relaciones nuevas

```
nivel_progresion
    |
    | 1:N
    v
tratamiento ---- (N:M) ---- discapacidad
    |                            |
    | via                        | via
    | paciente_tratamiento       | paciente_discapacidad
    |                            |
    +-------- PACIENTE ----------+
                |
         paciente_discapacidad: nivel actual por discapacidad
         paciente_tratamiento: tratamientos visibles por discapacidad
```

### Flujo de uso en la aplicacion

1. Sanitario abre ficha de paciente
2. Ve lista de discapacidades asignadas (paciente_discapacidad)
3. Para cada discapacidad, ve el nivel de progresion actual
4. Clic en discapacidad -> ve tratamientos disponibles para esa discapacidad Y ese nivel
5. Sanitario puede marcar como visible/no visible cada tratamiento (toggle en paciente_tratamiento)
6. Cuando sube al paciente de nivel, se actualizan los tratamientos disponibles

Total de tablas en la BD tras los cambios: 15 (las 12 actuales + nivel_progresion + paciente_discapacidad + paciente_tratamiento) + audit_log.

---

## HOJA DE RUTA - TRABAJO PENDIENTE

### TAREA INMEDIATA (EN CURSO)
Implementacion de discapacidades, tratamientos y niveles de progresion (ver seccion TAREA ACTUAL arriba y IMPLEMENTAR.md para detalles completos).

### DESPUES DE LA TAREA ACTUAL - INTEGRACIONES TFG

Estos puntos son las integraciones avanzadas del TFG. Solo empezar cuando la tarea actual este terminada y probada:

1. INTEGRACION API OPENAI EN EL SGE:
    - Integrar un modelo de IA que permita automatizar procesos o dar veredictos de graficos y datos de pacientes.
    - Se usara la API de OpenAI desde el SGE.

2. SCANNER NFC DE TARJETAS SANITARIAS:
    - Implementar lectura de tarjetas sanitarias españolas mediante NFC para rellenar automaticamente los datos del paciente.
    - Se usara el escaner NFC del movil o un lector oficial.

3. ACTIVAR SSL/TLS PARA PRODUCCION:
    - El codigo ya esta preparado en ConexionBD. Solo hay que cambiar db.ssl=true y db.sslmode=require en database.properties y configurar los certificados en AWS RDS.

4. CONEXION CON API REST DE SPRING BOOT:
    - Conectar el SGE con la API REST del ecosistema (Spring Boot + Flyway) para interoperar con la app movil, los videojuegos y MongoDB.

### YA COMPLETADO (NO REIMPLEMENTAR)

BLOQUE 1 - DEUDA TECNICA (completado 07/03/2026, documentado en DOCUMENTACION/Bloque1_DeudaTecnica.md):
- [HECHO] Migracion de ConexionBD singleton a HikariCP pool de conexiones
- [HECHO] Excepciones personalizadas en todos los DAOs (DuplicadoException, ConexionException, ValidacionException)
- [HECHO] Foto integrada en la misma transaccion que el paciente
- [HECHO] Transacciones compuestas atomicas en PacienteService y SanitarioService
- [HECHO] ValidacionException en validarCampos() de controladorAgregarPaciente y controladorAgregarSanitario
- [HECHO] Validaciones alineadas con BD: numero direccion obligatorio+numerico, provincia obligatoria, dos apellidos obligatorios

PRIMERA ITERACION v2.0 (completado 06/03/2026, documentado en DOCUMENTACION/FinalPrimerCambio.md):
- [HECHO] Campos clinicos en tabla paciente (sexo, fecha_nacimiento, alergias, antecedentes, medicacion, consentimiento RGPD)
- [HECHO] CifradoService AES-256-GCM para campos clinicos
- [HECHO] CifradoUtil BCrypt para contraseñas con migracion perezosa
- [HECHO] AuditLogDAO + AuditService (auditoria inmutable fire-and-forget)
- [HECHO] Soft delete en paciente y sanitario
- [HECHO] Jerarquia de excepciones personalizadas
- [HECHO] Interfaz completa con campos clinicos, ficha paciente, columna sexo

---

## ROLES Y PERMISOS

MEDICO ESPECIALISTA:
- Acceso completo a todas las funcionalidades
- CRUD completo de pacientes (con soft delete)
- CRUD completo de sanitarios (con soft delete)
- Gestion de citas (crear, modificar, eliminar)
- Generar informes PDF
- Editar su propio perfil
- Gestionar discapacidades, tratamientos y niveles de progresion de pacientes (NUEVO)

ENFERMERO:
- Solo lectura sobre pacientes (consultar pero NO modificar, crear ni eliminar)
- NO tiene acceso a la gestion de sanitarios (la pestaña queda OCULTA)
- Gestion de citas (crear, modificar, eliminar)
- Editar su propio perfil

---

## BASE DE DATOS POSTGRESQL - ESQUEMA v2.1

```sql
-- Tablas del personal sanitario
sanitario(dni_san PK, nombre_san, apellido1_san, apellido2_san, email_san UNIQUE, num_de_pacientes, contrasena_san, activo BOOLEAN, fecha_baja TIMESTAMP)
sanitario_agrega_sanitario(dni_san PK/FK CASCADE, cargo CHECK('medico especialista','enfermero'))
telefono_sanitario(id_telefono SERIAL PK, dni_san FK CASCADE, telefono)

-- Tablas de direccion normalizada
localidad(nombre_localidad PK, provincia)
cp(cp PK, nombre_localidad FK)
direccion(id_direccion SERIAL PK, calle, numero, piso, cp FK)

-- Tablas de catalogo clinico (existentes, pendientes de integracion con paciente)
discapacidad(cod_dis PK, nombre_dis UNIQUE, descripcion_dis, necesita_protesis BOOLEAN)
tratamiento(cod_trat PK, nombre_trat UNIQUE, definicion_trat, id_nivel FK -> nivel_progresion) -- id_nivel PENDIENTE DE AÑADIR
discapacidad_tratamiento(cod_dis FK CASCADE, cod_trat FK CASCADE) -- PK compuesta N:M

-- Tabla de pacientes (v2.0 con campos clinicos)
paciente(dni_pac PK, dni_san FK RESTRICT, nombre_pac, apellido1_pac, apellido2_pac, edad_pac, email_pac UNIQUE, num_ss UNIQUE, id_direccion FK, discapacidad_pac [LEGACY], tratamiento_pac [LEGACY], estado_tratamiento, protesis, foto BYTEA, fecha_nacimiento DATE, sexo VARCHAR(1) CHECK('M','F','O'), alergias TEXT, antecedentes TEXT, medicacion_actual TEXT, consentimiento_rgpd BOOLEAN, fecha_consentimiento TIMESTAMP, activo BOOLEAN, fecha_baja TIMESTAMP)
telefono_paciente(id_telefono SERIAL PK, dni_pac FK CASCADE, telefono)

-- Tabla de citas (PK compuesta)
cita(dni_pac FK CASCADE, dni_san FK CASCADE, fecha_cita DATE, hora_cita TIME)

-- Tabla de auditoria (INMUTABLE - solo INSERT)
audit_log(id_audit BIGSERIAL PK, fecha_hora TIMESTAMP, dni_usuario, nombre_usuario, accion CHECK('LOGIN','LOGOUT','CREATE','READ','UPDATE','SOFT_DELETE','EXPORT','PRINT','CAMBIO_CONTRASENA'), entidad, id_entidad, detalle, ip_origen)

-- TABLAS NUEVAS (TAREA ACTUAL - pendientes de crear)
-- nivel_progresion(id_nivel INT PK, nombre, nombre_corto, descripcion, estado_paciente, tipos_ejercicio)
-- paciente_discapacidad(dni_pac FK, cod_dis FK, id_nivel_actual FK DEFAULT 1, fecha_asignacion, notas) PK(dni_pac, cod_dis)
-- paciente_tratamiento(dni_pac FK, cod_trat FK, cod_dis FK, visible BOOLEAN, fecha_asignacion, dni_san_asigna FK) PK(dni_pac, cod_trat, cod_dis)
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
| Numero direccion | Obligatorio, solo digitos |
| Provincia | Obligatorio |
| Apellidos | Obligatorio dos apellidos separados por espacio |

---

## USUARIO ADMIN POR DEFECTO

DNI: ADMIN0000
Contraseña: admin (se migra a BCrypt tras primer login)
Cargo: medico especialista
Se crea automaticamente en el primer inicio si no existe.

---

## DOCUMENTACION DE REFERENCIA

| Archivo | Contenido |
|---------|-----------|
| IMPLEMENTAR.md | Especificacion completa de la tarea actual (discapacidades, tratamientos, niveles) |
| DOCUMENTO_CONTEXTO.md | Descripcion funcional completa del SGE |
| DOCUMENTACION/Bloque1_DeudaTecnica.md | Informe del Bloque 1 completado (HikariCP, excepciones, transacciones) |
| DOCUMENTACION/FinalPrimerCambio.md | Informe de la v2.0 (campos clinicos, cifrado, auditoria) |
| DOCUMENTACION/migracion_v2.sql | Script DDL de migracion de la BD |
| DOCUMENTACION/rehabiAPPDB.sql | Script original de creacion de la BD |

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
