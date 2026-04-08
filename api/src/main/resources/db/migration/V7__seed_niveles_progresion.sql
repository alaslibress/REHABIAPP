-- ============================================================
-- V7__seed_niveles_progresion.sql
-- Seed del catalogo de niveles de progresion clinica.
-- Las 4 fases terapeuticas estandar de rehabilitacion fisica,
-- ordenadas de forma ascendente segun la secuencia de recuperacion
-- del paciente. Este catalogo es necesario en TODOS los entornos
-- (dev, staging, produccion) para que el desktop pueda mostrar
-- y asignar niveles de progresion a las discapacidades del paciente.
--
-- La tabla nivel_progresion fue creada en V2__tablas_nuevas.sql.
-- Se usa ON CONFLICT DO NOTHING para idempotencia en entornos
-- donde los datos ya existian antes de la integracion con Flyway.
-- ============================================================

INSERT INTO nivel_progresion (id_nivel, nombre, nombre_corto, descripcion, estado_pac, tipos_ejercicio, orden) VALUES
(
    1,
    'Fase Aguda o de Maxima Proteccion',
    'Fase Aguda',
    'Comienza inmediatamente despues de la lesion o cirugia. Tejido inflamado, fragil, dolor agudo. Objetivo: evitar atrofia severa y controlar inflamacion.',
    'Movilidad muy reducida, dolor intenso ante el esfuerzo, posible inmovilizacion parcial.',
    'Movilizacion pasiva, ejercicios isometricos submaximos.',
    1
),
(
    2,
    'Fase Subaguda o de Movilidad Controlada',
    'Fase Subaguda',
    'El dolor agudo y la inflamacion han bajado. El paciente necesita volver a mover la extremidad en todo su recorrido natural.',
    'Menos dolor en reposo, debilidad notable.',
    'Movilidad activo-asistida (poleas, baston, hidroterapia), movilidad activa libre.',
    2
),
(
    3,
    'Fase de Fortalecimiento y Remodelacion',
    'Fortalecimiento',
    'El paciente tiene recorrido articular casi completo y sin dolor. Objetivo: reconstruir masa muscular y resistencia.',
    'Articulacion funcional, pero el musculo se fatiga rapido.',
    'Ejercicios isotonicos (bandas, mancuernas, maquinas), cadena cinetica cerrada (sentadillas, flexiones pared).',
    3
),
(
    4,
    'Fase Funcional y Propioceptiva',
    'Funcional',
    'Fuerza recuperada, rango de movimiento completo, sin dolor. Objetivo: reeducar sistema nervioso para reacciones automaticas.',
    'Musculo fuerte, rango completo, necesita recuperar confianza.',
    'Propiocepcion y equilibrio (Bosu, superficies inestables), ejercicios funcionales (vida diaria, pliometria).',
    4
)
ON CONFLICT (id_nivel) DO NOTHING;
