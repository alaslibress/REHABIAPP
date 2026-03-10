# TAREA: Integracion de Discapacidades, Tratamientos y Niveles de Progresion

## CONTEXTO

Actualmente la base de datos tiene las tablas `discapacidad`, `tratamiento` y `discapacidad_tratamiento` (relacion N:M), pero estan desconectadas de la tabla `paciente`. En `paciente` los campos `discapacidad_pac` y `tratamiento_pac` son texto libre (VARCHAR/TEXT) sin relacion con las tablas de catalogo.

Ademas, el campo `protesis` en `paciente` es un entero que indica cuantas protesis tiene, pero no esta vinculado a ninguna discapacidad que lo justifique.

Los tratamientos deben organizarse por niveles de progresion clinica, de forma que un paciente solo vea los tratamientos correspondientes a su nivel actual dentro de cada discapacidad asignada.

---

## REQUISITOS FUNCIONALES

### Relaciones entre entidades

1. Un PACIENTE puede tener UNA O MAS discapacidades asignadas (N:M entre paciente y discapacidad).

2. Una DISCAPACIDAD puede estar asignada a UNO O MAS pacientes (N:M).

3. Una DISCAPACIDAD puede tratarse con MINIMO 1 o mas tratamientos (N:M entre discapacidad y tratamiento, ya existe la tabla `discapacidad_tratamiento`).

4. Un TRATAMIENTO puede servir para tratar MAS DE UNA discapacidad (N:M).

5. Los TRATAMIENTOS se organizan por NIVELES DE PROGRESION. Cada tratamiento pertenece a un nivel de progresion concreto.

6. Un PACIENTE tiene asignado un NIVEL DE PROGRESION por cada discapacidad que tiene. No es un nivel global del paciente, sino un nivel por discapacidad (porque un paciente puede estar en fase 3 para su rodilla pero en fase 1 para su hombro).

7. Un SANITARIO (medico especialista) puede hacer visibles o no los tratamientos de un paciente dentro de su nivel de progresion actual. Es decir, aunque un tratamiento corresponda al nivel del paciente, no se muestra en su ficha hasta que el sanitario lo active explicitamente.

### Logica de visibilidad de tratamientos

Para que un tratamiento aparezca en la ficha de un paciente deben cumplirse TRES condiciones simultaneamente:

- El tratamiento esta asociado a una discapacidad que tiene el paciente (via discapacidad_tratamiento).
- El tratamiento pertenece al mismo nivel de progresion en el que se encuentra el paciente para esa discapacidad.
- El sanitario ha marcado ese tratamiento como visible/activo para ese paciente.

---

## NIVELES DE PROGRESION (datos fijos)

Existen 4 niveles de progresion clinica. Son datos de catalogo que no cambian:

### Nivel 1: Fase Aguda o de Maxima Proteccion (Control y Movilidad Pasiva)
- Comienza inmediatamente despues de la lesion o cirugia.
- Tejido inflamado, fragil, dolor agudo.
- Objetivo: evitar la atrofia severa y controlar la inflamacion.
- Estado del paciente: movilidad muy reducida, dolor intenso ante el esfuerzo, posible inmovilizacion parcial.
- Tipos de ejercicio: movilizacion pasiva (el fisioterapeuta mueve la articulacion), ejercicios isometricos submaximos (contracciones estaticas sin mover la articulacion).

### Nivel 2: Fase Subaguda o de Movilidad Controlada (Recuperacion del ROM)
- El dolor agudo y la inflamacion han bajado.
- El paciente necesita volver a mover la extremidad en todo su recorrido natural.
- Estado del paciente: menos dolor en reposo, debilidad notable.
- Tipos de ejercicio: movilidad activo-asistida (con poleas, baston, hidroterapia), movilidad activa libre (sin pesas ni resistencia).

### Nivel 3: Fase de Fortalecimiento y Remodelacion (Resistencia Progresiva)
- El paciente tiene recorrido articular casi completo y sin dolor.
- Objetivo: reconstruir masa muscular y resistencia.
- Estado del paciente: articulacion funcional, pero el musculo se fatiga rapido.
- Tipos de ejercicio: ejercicios isotonicos (bandas elasticas, mancuernas, maquinas), ejercicios de cadena cinetica cerrada (sentadillas, flexiones contra pared).

### Nivel 4: Fase Funcional y Propioceptiva (Vuelta a la Normalidad/Deporte)
- Fuerza recuperada, rango de movimiento completo, sin dolor.
- Objetivo: reeducar el sistema nervioso para reacciones automaticas a desequilibrios.
- Estado del paciente: musculo fuerte, necesita recuperar confianza.
- Tipos de ejercicio: propiocepcion y equilibrio (superficies inestables, Bosu), ejercicios funcionales (simulacion de vida diaria, pliometria, cambios de direccion).

---

## CAMBIOS EN LA BASE DE DATOS

### Tabla nueva: nivel_progresion (catalogo fijo)

```sql
CREATE TABLE nivel_progresion (
    id_nivel INT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    nombre_corto VARCHAR(50) NOT NULL,
    descripcion TEXT,
    estado_paciente TEXT,
    tipos_ejercicio TEXT
);
```

Insertar los 4 niveles:

```sql
INSERT INTO nivel_progresion (id_nivel, nombre, nombre_corto, descripcion, estado_paciente, tipos_ejercicio) VALUES
(1, 'Fase Aguda o de Maxima Proteccion', 'Fase Aguda', 
 'Comienza inmediatamente despues de la lesion o cirugia. Tejido inflamado, fragil, dolor agudo. Objetivo: evitar atrofia severa y controlar inflamacion.',
 'Movilidad muy reducida, dolor intenso ante el esfuerzo, posible inmovilizacion parcial.',
 'Movilizacion pasiva, ejercicios isometricos submaximos.'),

(2, 'Fase Subaguda o de Movilidad Controlada', 'Fase Subaguda',
 'El dolor agudo y la inflamacion han bajado. El paciente necesita volver a mover la extremidad en todo su recorrido natural.',
 'Menos dolor en reposo, debilidad notable.',
 'Movilidad activo-asistida (poleas, baston, hidroterapia), movilidad activa libre.'),

(3, 'Fase de Fortalecimiento y Remodelacion', 'Fortalecimiento',
 'El paciente tiene recorrido articular casi completo y sin dolor. Objetivo: reconstruir masa muscular y resistencia.',
 'Articulacion funcional, pero el musculo se fatiga rapido.',
 'Ejercicios isotonicos (bandas, mancuernas, maquinas), cadena cinetica cerrada (sentadillas, flexiones pared).'),

(4, 'Fase Funcional y Propioceptiva', 'Funcional',
 'Fuerza recuperada, rango de movimiento completo, sin dolor. Objetivo: reeducar sistema nervioso para reacciones automaticas.',
 'Musculo fuerte, rango completo, necesita recuperar confianza.',
 'Propiocepcion y equilibrio (Bosu, superficies inestables), ejercicios funcionales (vida diaria, pliometria).');
```

### Modificar tabla existente: tratamiento

Añadir FK al nivel de progresion al que pertenece cada tratamiento:

```sql
ALTER TABLE tratamiento ADD COLUMN id_nivel INT;
ALTER TABLE tratamiento ADD CONSTRAINT fk_tratamiento_nivel 
    FOREIGN KEY (id_nivel) REFERENCES nivel_progresion(id_nivel);
```

Un tratamiento pertenece a un nivel de progresion. Un nivel de progresion puede tener muchos tratamientos. Relacion N:1.

### Tabla nueva: paciente_discapacidad (relacion N:M entre paciente y discapacidad)

```sql
CREATE TABLE paciente_discapacidad (
    dni_pac VARCHAR(9) NOT NULL,
    cod_dis VARCHAR(10) NOT NULL,
    id_nivel_actual INT NOT NULL DEFAULT 1,
    fecha_asignacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notas TEXT,
    
    PRIMARY KEY (dni_pac, cod_dis),
    FOREIGN KEY (dni_pac) REFERENCES paciente(dni_pac) ON DELETE CASCADE,
    FOREIGN KEY (cod_dis) REFERENCES discapacidad(cod_dis),
    FOREIGN KEY (id_nivel_actual) REFERENCES nivel_progresion(id_nivel)
);
```

Esta tabla registra:
- Que discapacidades tiene cada paciente.
- En que nivel de progresion esta el paciente para CADA discapacidad.
- Cuando se le asigno esa discapacidad.
- Notas del sanitario sobre esa discapacidad concreta.

### Tabla nueva: paciente_tratamiento (tratamientos visibles para un paciente)

```sql
CREATE TABLE paciente_tratamiento (
    dni_pac VARCHAR(9) NOT NULL,
    cod_trat VARCHAR(10) NOT NULL,
    cod_dis VARCHAR(10) NOT NULL,
    visible BOOLEAN DEFAULT FALSE,
    fecha_asignacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    dni_san_asigna VARCHAR(9) NOT NULL,
    
    PRIMARY KEY (dni_pac, cod_trat, cod_dis),
    FOREIGN KEY (dni_pac) REFERENCES paciente(dni_pac) ON DELETE CASCADE,
    FOREIGN KEY (cod_trat) REFERENCES tratamiento(cod_trat),
    FOREIGN KEY (cod_dis) REFERENCES discapacidad(cod_dis),
    FOREIGN KEY (dni_san_asigna) REFERENCES sanitario(dni_san)
);
```

Esta tabla registra:
- Que tratamientos tiene asignados cada paciente, para que discapacidad.
- Si el sanitario lo ha marcado como visible (activo) o no.
- Que sanitario hizo la asignacion.
- Solo pueden existir registros donde el tratamiento corresponda a la discapacidad (validar en la aplicacion) y donde el nivel del tratamiento coincida con el nivel actual del paciente para esa discapacidad.

### Campos a deprecar en tabla paciente

Los campos `discapacidad_pac` (VARCHAR) y `tratamiento_pac` (VARCHAR) quedan OBSOLETOS. Ahora la discapacidad y los tratamientos se gestionan a traves de las tablas intermedias `paciente_discapacidad` y `paciente_tratamiento`.

Opciones:
A) Eliminar los campos de la tabla paciente.
B) Mantenerlos como texto legacy y dejar de usarlos en el codigo nuevo.

Recomendacion: Opcion B por ahora. No romper nada que funcione. Cuando toda la logica nueva este probada, se eliminan.

---

## DIAGRAMA DE RELACIONES NUEVAS

```
nivel_progresion
    |
    | 1:N
    v
tratamiento ──── (N:M) ──── discapacidad
    |                            |
    | via                        | via
    | paciente_tratamiento       | paciente_discapacidad
    |                            |
    └──────── PACIENTE ──────────┘
                |
         paciente_discapacidad: nivel actual por discapacidad
         paciente_tratamiento: tratamientos visibles por discapacidad
```

### Flujo de uso en la aplicacion

1. El sanitario abre la ficha de un paciente.
2. Ve la lista de discapacidades asignadas al paciente (tabla paciente_discapacidad).
3. Para cada discapacidad, ve el nivel de progresion actual del paciente.
4. Al hacer clic en una discapacidad, ve los tratamientos disponibles para esa discapacidad Y ese nivel (cruzando discapacidad_tratamiento con tratamiento.id_nivel = paciente_discapacidad.id_nivel_actual).
5. El sanitario puede marcar como visible/no visible cada tratamiento (toggle en paciente_tratamiento).
6. El paciente (en la app movil, futuro) solo ve los tratamientos marcados como visibles.
7. Cuando el sanitario decide subir al paciente de nivel (ej: de fase 1 a fase 2), actualiza id_nivel_actual en paciente_discapacidad. Los tratamientos del nivel anterior dejan de ser relevantes y aparecen los del nuevo nivel para que el sanitario los active.

### Consulta SQL clave: obtener tratamientos visibles de un paciente

```sql
-- Tratamientos activos de un paciente, agrupados por discapacidad y nivel
SELECT 
    d.nombre_dis,
    np.nombre_corto AS nivel_actual,
    t.nombre_trat,
    t.definicion_trat,
    pt.visible,
    pt.fecha_asignacion
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

---

## RESUMEN DE CAMBIOS EN LA BD

| Accion | Tabla | Descripcion |
|--------|-------|-------------|
| CREAR | nivel_progresion | Catalogo fijo de 4 niveles de progresion clinica |
| CREAR | paciente_discapacidad | Relacion N:M entre paciente y discapacidad, con nivel actual por discapacidad |
| CREAR | paciente_tratamiento | Tratamientos asignados a un paciente por discapacidad, con visibilidad controlada por el sanitario |
| MODIFICAR | tratamiento | Añadir columna id_nivel (FK a nivel_progresion) |
| DEPRECAR | paciente.discapacidad_pac | Campo texto libre, reemplazado por paciente_discapacidad |
| DEPRECAR | paciente.tratamiento_pac | Campo texto libre, reemplazado por paciente_tratamiento |

Total de tablas en la BD tras los cambios: 15 (las 12 actuales + nivel_progresion + paciente_discapacidad + paciente_tratamiento).

---

## ORDEN DE IMPLEMENTACION

1. Crear tabla nivel_progresion con los 4 registros de catalogo.
2. Añadir columna id_nivel a la tabla tratamiento y actualizar los tratamientos existentes con su nivel correspondiente.
3. Crear tabla paciente_discapacidad.
4. Crear tabla paciente_tratamiento.
5. Crear indices de rendimiento necesarios.
6. Actualizar los DAOs: DiscapacidadDAO, TratamientoDAO, NivelProgresionDAO (nuevo), PacienteDiscapacidadDAO (nuevo), PacienteTratamientoDAO (nuevo).
7. Actualizar los Services correspondientes.
8. Actualizar la interfaz grafica: ficha del paciente debe mostrar las discapacidades con su nivel y los tratamientos visibles.
9. NO eliminar los campos discapacidad_pac y tratamiento_pac de paciente hasta que todo lo nuevo este probado.