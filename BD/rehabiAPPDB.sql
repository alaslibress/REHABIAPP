--Creacion de la base de datos para RehabiAPP SGE
DROP DATABASE IF EXISTS rehabiapp_db;
CREATE DATABASE rehabiapp_db;

--Conexion a la base de datos
\c rehabiapp_db;

--Tabla sanitario
CREATE TABLE sanitario(
    dni_san VARCHAR(9) PRIMARY KEY,
    nombre_san VARCHAR(20) NOT NULL,
    apellido1_san VARCHAR(20) NOT NULL,
    apellido2_san VARCHAR(20) NOT NULL,
    email_san VARCHAR(100) UNIQUE NOT NULL,
    num_de_pacientes INT DEFAULT 0,
    contrasena_san VARCHAR(255) NOT NULL,
    fecha_alta TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--Tabla cargo del sanitario (relacion 1:1)
CREATE TABLE sanitario_agrega_sanitario(
    dni_san VARCHAR(9) PRIMARY KEY,
    cargo VARCHAR(20) CHECK(cargo IN ('medico especialista', 'enfermero')) NOT NULL,
    FOREIGN KEY (dni_san) REFERENCES sanitario(dni_san) ON DELETE CASCADE
);

--Tabla telefonos del sanitario (relacion 1:N)
CREATE TABLE telefono_sanitario(
    id_telefono SERIAL PRIMARY KEY,
    dni_san VARCHAR(9) NOT NULL,
    telefono VARCHAR(13) NOT NULL,
    FOREIGN KEY (dni_san) REFERENCES sanitario(dni_san) ON DELETE CASCADE
);

--Tabla localidad
CREATE TABLE localidad(
    nombre_localidad VARCHAR(50) PRIMARY KEY,
    provincia VARCHAR(50) NOT NULL
);

--Tabla codigo postal
CREATE TABLE cp(
    cp VARCHAR(5) PRIMARY KEY,
    nombre_localidad VARCHAR(50) NOT NULL,
    FOREIGN KEY (nombre_localidad) REFERENCES localidad(nombre_localidad) ON DELETE CASCADE
);

--Tabla direccion
CREATE TABLE direccion(
    id_direccion SERIAL PRIMARY KEY,
    calle VARCHAR(100) NOT NULL,
    numero INT NOT NULL,
    piso VARCHAR(10),
    cp VARCHAR(5) NOT NULL,
    FOREIGN KEY (cp) REFERENCES cp(cp) ON DELETE CASCADE
);

--Tabla paciente
CREATE TABLE paciente(
    dni_pac VARCHAR(9) PRIMARY KEY,
    dni_san VARCHAR(9) NOT NULL,
    nombre_pac VARCHAR(20) NOT NULL,
    apellido1_pac VARCHAR(20) NOT NULL,
    apellido2_pac VARCHAR(20) NOT NULL,
    email_pac VARCHAR(100) UNIQUE NOT NULL,
    num_ss VARCHAR(12) UNIQUE NOT NULL,
    id_direccion INT NOT NULL,
    foto VARCHAR(255),
    discapacidad_pac VARCHAR(100),
    tratamiento_pac TEXT,
    estado_tratamiento TEXT,
    protesis INT DEFAULT 0,
    edad_pac INT NOT NULL,
    fecha_alta TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (dni_san) REFERENCES sanitario(dni_san) ON DELETE CASCADE,
    FOREIGN KEY (id_direccion) REFERENCES direccion(id_direccion) ON DELETE CASCADE
);

--Tabla telefonos del paciente (relacion 1:N)
CREATE TABLE telefono_paciente(
    id_telefono SERIAL PRIMARY KEY,
    dni_pac VARCHAR(9) NOT NULL,
    telefono VARCHAR(13) NOT NULL,
    FOREIGN KEY (dni_pac) REFERENCES paciente(dni_pac) ON DELETE CASCADE
);

-- Crear tabla cita (relacion N:M entre paciente y sanitario)
CREATE TABLE IF NOT EXISTS cita (
    dni_pac VARCHAR(9) NOT NULL,
    dni_san VARCHAR(9) NOT NULL,
    fecha_cita DATE NOT NULL,
    hora_cita TIME NOT NULL,
    informe BYTEA,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Clave primaria compuesta
    PRIMARY KEY (dni_pac, dni_san, fecha_cita, hora_cita),
    
    -- Claves foraneas
    CONSTRAINT fk_cita_paciente FOREIGN KEY (dni_pac) 
        REFERENCES paciente(dni_pac) ON DELETE CASCADE,
    CONSTRAINT fk_cita_sanitario FOREIGN KEY (dni_san) 
        REFERENCES sanitario(dni_san) ON DELETE CASCADE
);

--Indices para optimizar busquedas del SGE
CREATE INDEX idx_sanitario_nombre ON sanitario(nombre_san, apellido1_san, apellido2_san); --Buscar por nombre completo en la barra de búsqueda
CREATE INDEX idx_paciente_nombre ON paciente(nombre_pac, apellido1_pac, apellido2_pac); --Buscar pacientes
CREATE INDEX idx_paciente_sanitario ON paciente(dni_san); --Para listar todos los pacientes de un sanitario
CREATE INDEX idx_paciente_num_ss ON paciente(num_ss); --Buscar por número de ss
CREATE INDEX idx_cita_fecha ON cita(fecha_cita); --Buscar fechas de citas
CREATE INDEX idx_cita_paciente ON cita(dni_pac); --Buscar citas por paciente en concreto
CREATE INDEX idx_cita_sanitario ON cita(dni_san); --Buscar citas por realizar por un sanitario

--Comentarios sobre las tablas
COMMENT ON TABLE sanitario IS 'Personal medico que atiende pacientes';
COMMENT ON TABLE paciente IS 'Pacientes en rehabilitacion';
COMMENT ON TABLE cita IS 'Tabla de citas medicas - Relacion N:M entre pacientes y sanitarios';
COMMENT ON COLUMN cita.dni_pac IS 'DNI del paciente';
COMMENT ON COLUMN cita.dni_san IS 'DNI del sanitario';
COMMENT ON COLUMN cita.fecha_cita IS 'Fecha de la cita';
COMMENT ON COLUMN cita.hora_cita IS 'Hora de la cita';
COMMENT ON COLUMN cita.informe IS 'Informe PDF de la cita (opcional)';
COMMENT ON TABLE direccion IS 'Direcciones normalizadas de los pacientes';


--INSERCIONES DE EJEMPLO:
-- 1. INSERTAR SANITARIO: Alejandro Pozo Pérez
INSERT INTO sanitario (dni_san, nombre_san, apellido1_san, apellido2_san, email_san, num_de_pacientes, contrasena_san)
VALUES ('78834700J', 'Alejandro', 'Pozo', 'Pérez', 'alejandro.pozo@rehabiapp.com', 1, 'alejandro123');

-- 2. INSERTAR CARGO DEL SANITARIO (medico especialista)
INSERT INTO sanitario_agrega_sanitario (dni_san, cargo)
VALUES ('78834700J', 'medico especialista');

-- 3. INSERTAR TELEFONOS DEL SANITARIO
INSERT INTO telefono_sanitario (dni_san, telefono)
VALUES 
    ('78834700J', '626284359'),
    ('78834700J', '953278820');

-- 4. INSERTAR LOCALIDAD
INSERT INTO localidad (nombre_localidad, provincia)
VALUES ('Jaén', 'Jaén');

-- 5. INSERTAR CODIGO POSTAL
INSERT INTO cp (cp, nombre_localidad)
VALUES ('23001', 'Jaén');

-- 6. INSERTAR DIRECCION DEL PACIENTE
INSERT INTO direccion (calle, numero, piso, cp)
VALUES ('Calle Real', 15, '2A', '23001');

-- 7. INSERTAR PACIENTE: David Ortega Delgado
INSERT INTO paciente (
    dni_pac, 
    dni_san, 
    nombre_pac, 
    apellido1_pac, 
    apellido2_pac, 
    email_pac, 
    num_ss, 
    id_direccion, 
    foto, 
    discapacidad_pac, 
    tratamiento_pac, 
    estado_tratamiento, 
    protesis, 
    edad_pac
) VALUES (
    '12345678A',                        -- 1. dni_pac
    '78834700J',                        -- 2. dni_san
    'David',                            -- 3. nombre_pac
    'Ortega',                           -- 4. apellido1_pac
    'Delgado',                          -- 5. apellido2_pac
    'david.ortega.delgado@email.com',   -- 6. email_pac
    '231234567890',                     -- 7. num_ss
    1,                                  -- 8. id_direccion
    '/fotos/david.jpg',                 -- 9. foto
    'Ninguna',                          -- 10. discapacidad_pac
    'Rehabilitación rodilla',           -- 11. tratamiento_pac
    'Activo',                           -- 12. estado_tratamiento
    0,                                  -- 13. protesis
    34                                  -- 14. edad_pac
);

-- 8. INSERTAR TELEFONOS DEL PACIENTE
INSERT INTO telefono_paciente (dni_pac, telefono)
VALUES 
    ('12345678A', '612345678'),
    ('12345678A', '953123456');

-- Insertar algunas citas de ejemplo
INSERT INTO cita (dni_pac, dni_san, fecha_cita, hora_cita) VALUES
('12345678A', '78834700J', CURRENT_DATE, '09:00:00'),
('12345678A', '78834700J', CURRENT_DATE + 1, '10:30:00');

--Solucioné un problema con el campo foto:
-- Modificar la columna foto de VARCHAR(255) a BYTEA
ALTER TABLE paciente 
ALTER COLUMN foto TYPE BYTEA 
USING NULL;

-- ==================================================================
-- Script para crear usuario administrador por defecto en PostgreSQL

DO $$
BEGIN
    -- Verificar si existe el admin
    IF NOT EXISTS (SELECT 1 FROM sanitario WHERE dni_san = 'ADMIN0000') THEN
        
        -- 1. Insertar sanitario admin
        INSERT INTO sanitario (dni_san, nombre_san, apellido1_san, apellido2_san, 
                               email_san, num_de_pacientes, contrasena_san)
        VALUES ('ADMIN0000', 'Administrador', 'Sistema', '', 
                'admin@rehabiapp.com', 0, 'admin');
        
        -- 2. Asignar cargo en minusculas segun el CHECK constraint
        INSERT INTO sanitario_agrega_sanitario (dni_san, cargo)
        VALUES ('ADMIN0000', 'medico especialista');
        
        RAISE NOTICE 'Usuario admin creado correctamente';
        RAISE NOTICE 'Credenciales: DNI=ADMIN0000, Contraseña=admin';
        
    ELSE
        RAISE NOTICE 'El usuario admin ya existe';
    END IF;
END $$;