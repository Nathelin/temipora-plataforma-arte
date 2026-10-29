-- Ajustes del módulo de reseñas:
--   * la reseña deja de tener título y la descripción pasa a ser opcional;
--   * las opciones de chips llevan polaridad para mostrarse según la calificación;
--   * se reemplaza el catálogo de prueba por el set por defecto acordado.

ALTER TABLE resenia_version DROP COLUMN IF EXISTS titulo;
ALTER TABLE resenia_version ALTER COLUMN descripcion DROP NOT NULL;

ALTER TABLE opcion_criterio
    ADD COLUMN IF NOT EXISTS polaridad VARCHAR(20) NOT NULL DEFAULT 'POSITIVA';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_opcion_criterio_polaridad'
    ) THEN
        ALTER TABLE opcion_criterio
            ADD CONSTRAINT ck_opcion_criterio_polaridad
            CHECK (polaridad IN ('POSITIVA', 'NEGATIVA'));
    END IF;
END $$;

-- Limpieza: se eliminan opciones y criterios que no tengan ninguna respuesta.
-- Nada que esté en uso se toca.
DELETE FROM opcion_criterio o
WHERE NOT EXISTS (
    SELECT 1 FROM resenia_opcion ro
    WHERE ro.id_opcion = o.id_opcion_criterio
);

DELETE FROM criterio_resenia c
WHERE NOT EXISTS (
    SELECT 1 FROM resenia_valor rv
    WHERE rv.id_criterio = c.id_criterio_resenia
)
AND NOT EXISTS (
    SELECT 1
    FROM resenia_opcion ro
    JOIN opcion_criterio oc ON oc.id_opcion_criterio = ro.id_opcion
    WHERE oc.id_criterio = c.id_criterio_resenia
);

-- Catálogo por defecto: 3 estrellas obligatorias + 1 grupo de chips por ámbito.
INSERT INTO criterio_resenia (
    nombre, descripcion, ambito, escala, obligatorio, orden, activo
)
SELECT v.nombre, v.descripcion, v.ambito, v.escala, v.obligatorio, v.orden, TRUE
FROM (VALUES
    ('Calidad de lo recibido',
     'Qué tan bien estuvo lo entregado respecto de lo acordado.',
     'SERVICIO', 'ESTRELLAS', TRUE, 1),
    ('Comunicación',
     'Claridad y predisposición del artista durante el trabajo.',
     'SERVICIO', 'ESTRELLAS', TRUE, 2),
    ('Cumplimiento de plazos',
     'Si las entregas llegaron en los tiempos acordados.',
     'SERVICIO', 'ESTRELLAS', TRUE, 3),
    ('Aspectos destacados',
     'Lo que sobresalió o lo que no funcionó durante el trabajo.',
     'SERVICIO', 'OPCIONES_MULTIPLES', FALSE, 4),
    ('Claridad del pedido',
     'Si el pedido y las indicaciones fueron claros y completos.',
     'CLIENTE', 'ESTRELLAS', TRUE, 1),
    ('Comunicación',
     'Predisposición y claridad del cliente durante el trabajo.',
     'CLIENTE', 'ESTRELLAS', TRUE, 2),
    ('Cumplimiento de plazos de pago',
     'Si los pagos se hicieron en los tiempos acordados.',
     'CLIENTE', 'ESTRELLAS', TRUE, 3),
    ('Aspectos destacados',
     'Actitudes que sobresalieron o que no funcionaron durante el trabajo.',
     'CLIENTE', 'OPCIONES_MULTIPLES', FALSE, 4)
) AS v(nombre, descripcion, ambito, escala, obligatorio, orden)
WHERE NOT EXISTS (
    SELECT 1 FROM criterio_resenia c
    WHERE c.ambito = v.ambito AND lower(c.nombre) = lower(v.nombre)
);

INSERT INTO opcion_criterio (id_criterio, etiqueta, orden, polaridad)
SELECT c.id_criterio_resenia, v.etiqueta, v.orden, v.polaridad
FROM criterio_resenia c
JOIN (VALUES
    ('SERVICIO', 'Aspectos destacados', 'Comunicación clara', 1, 'POSITIVA'),
    ('SERVICIO', 'Aspectos destacados', 'Respondió rápido', 2, 'POSITIVA'),
    ('SERVICIO', 'Aspectos destacados', 'Aceptó ajustes', 3, 'POSITIVA'),
    ('SERVICIO', 'Aspectos destacados', 'Entrega cuidada', 4, 'POSITIVA'),
    ('SERVICIO', 'Aspectos destacados', 'Superó lo esperado', 5, 'POSITIVA'),
    ('SERVICIO', 'Aspectos destacados', 'Comunicación poco clara', 6, 'NEGATIVA'),
    ('SERVICIO', 'Aspectos destacados', 'Respondió tarde', 7, 'NEGATIVA'),
    ('SERVICIO', 'Aspectos destacados', 'No aceptó ajustes', 8, 'NEGATIVA'),
    ('SERVICIO', 'Aspectos destacados', 'Entrega descuidada', 9, 'NEGATIVA'),
    ('SERVICIO', 'Aspectos destacados', 'Por debajo de lo esperado', 10, 'NEGATIVA'),
    ('CLIENTE', 'Aspectos destacados', 'Brief claro', 1, 'POSITIVA'),
    ('CLIENTE', 'Aspectos destacados', 'Respondió rápido', 2, 'POSITIVA'),
    ('CLIENTE', 'Aspectos destacados', 'Puntual con los pagos', 3, 'POSITIVA'),
    ('CLIENTE', 'Aspectos destacados', 'Trato respetuoso', 4, 'POSITIVA'),
    ('CLIENTE', 'Aspectos destacados', 'Aceptó ajustes razonables', 5, 'POSITIVA'),
    ('CLIENTE', 'Aspectos destacados', 'Brief poco claro', 6, 'NEGATIVA'),
    ('CLIENTE', 'Aspectos destacados', 'Respondió tarde', 7, 'NEGATIVA'),
    ('CLIENTE', 'Aspectos destacados', 'Pagos fuera de plazo', 8, 'NEGATIVA'),
    ('CLIENTE', 'Aspectos destacados', 'Trato irrespetuoso', 9, 'NEGATIVA'),
    ('CLIENTE', 'Aspectos destacados', 'Cambios constantes', 10, 'NEGATIVA')
) AS v(ambito, criterio, etiqueta, orden, polaridad)
    ON c.ambito = v.ambito AND c.nombre = v.criterio
WHERE NOT EXISTS (
    SELECT 1 FROM opcion_criterio o
    WHERE o.id_criterio = c.id_criterio_resenia
      AND lower(o.etiqueta) = lower(v.etiqueta)
);
