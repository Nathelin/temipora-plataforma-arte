-- Fecha de creación del servicio, para poder ordenar el catálogo por "más recientes".
--
-- La fecha no existía en la tabla: hasta ahora solo quedaba registrada en
-- evento_auditoria (accion = 'SERVICIO_CREAR'). Se agrega la columna y se
-- completa con esa fuente.
--
-- Decisión: los servicios sin fila de auditoría (creados antes de V63 o por una
-- vía que no audita) reciben la fecha más antigua conocida, para que ninguno
-- quede sin fecha y el orden sea total. Son datos de prueba, así que no hay
-- información real que inventar.

ALTER TABLE servicio ADD COLUMN IF NOT EXISTS fecha_creacion TIMESTAMP;

-- Backfill desde la auditoría: primera alta registrada de cada servicio.
UPDATE servicio s
   SET fecha_creacion = a.primera
  FROM (
        SELECT entidad_id::bigint AS id_servicio, MIN(ocurrido_en) AS primera
          FROM evento_auditoria
         WHERE accion = 'SERVICIO_CREAR'
           AND entidad = 'Servicio'
           AND entidad_id ~ '^[0-9]+$'
         GROUP BY entidad_id
       ) a
 WHERE s.id_servicio = a.id_servicio
   AND s.fecha_creacion IS NULL;

-- Los que quedaron sin fecha heredan la más antigua registrada, de modo que
-- siempre queden al final del orden "más recientes".
UPDATE servicio
   SET fecha_creacion = COALESCE(
        (SELECT MIN(ocurrido_en) FROM evento_auditoria WHERE accion = 'SERVICIO_CREAR'),
        NOW())
 WHERE fecha_creacion IS NULL;

ALTER TABLE servicio ALTER COLUMN fecha_creacion SET NOT NULL;
ALTER TABLE servicio ALTER COLUMN fecha_creacion SET DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_servicio_fecha_creacion
    ON servicio (fecha_creacion DESC);
