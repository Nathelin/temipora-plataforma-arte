-- Rediseño de cupos: una solicitud/pedido ocupa un solo cupo.
-- El cupo se ocupa al presupuestar y se libera al finalizar o cancelar.

ALTER TABLE servicio ADD COLUMN IF NOT EXISTS modo_recepcion VARCHAR(20) NOT NULL DEFAULT 'ABIERTA';
ALTER TABLE servicio ADD COLUMN IF NOT EXISTS max_dibujos_por_solicitud INTEGER NOT NULL DEFAULT 3;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_servicio_modo_recepcion') THEN
        ALTER TABLE servicio ADD CONSTRAINT ck_servicio_modo_recepcion
            CHECK (modo_recepcion IN ('ABIERTA', 'SOLO_ESPERA', 'CERRADA'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_servicio_max_dibujos') THEN
        ALTER TABLE servicio ADD CONSTRAINT ck_servicio_max_dibujos
            CHECK (max_dibujos_por_solicitud BETWEEN 1 AND 50);
    END IF;
END $$;

DROP INDEX IF EXISTS uk_solicitud_activa;
DROP INDEX IF EXISTS uk_solicitud_en_espera;

UPDATE solicitud
SET estado = 'PAGADA', fecha_actualizacion = now()
WHERE estado = 'ACEPTADA';

-- La regla anterior permitía una solicitud activa y una en espera a la vez.
-- Antes de aplicar la nueva unicidad conservamos solo la más antigua que no esté en espera.
WITH duplicadas AS (
    SELECT id_solicitud,
           row_number() OVER (
               PARTITION BY id_servicio, id_cliente
               ORDER BY CASE WHEN estado = 'EN_LISTA_ESPERA' THEN 2 ELSE 1 END,
                        fecha_creacion,
                        id_solicitud
           ) AS orden
    FROM solicitud
    WHERE estado IN ('PENDIENTE', 'EN_LISTA_ESPERA', 'PRESUPUESTADA', 'PAGADA')
)
UPDATE solicitud s
SET estado = 'CANCELADA', fecha_actualizacion = now()
FROM duplicadas d
WHERE s.id_solicitud = d.id_solicitud
  AND d.orden > 1;

UPDATE lista_espera l
SET estado = 'CANCELADA', fecha_actualizacion = now()
FROM solicitud s
WHERE l.id_solicitud = s.id_solicitud
  AND s.estado = 'CANCELADA'
  AND l.estado = 'ESPERANDO';

CREATE UNIQUE INDEX IF NOT EXISTS uk_solicitud_activa
    ON solicitud (id_servicio, id_cliente)
    WHERE estado IN ('PENDIENTE', 'EN_LISTA_ESPERA', 'PRESUPUESTADA', 'PAGADA');
