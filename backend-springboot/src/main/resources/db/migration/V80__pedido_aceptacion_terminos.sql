-- Fase 4.1: el pedido referencia la aceptación exacta de TOS de plataforma.
--
-- Antes el pedido guardaba version/snapshot/fecha por su cuenta. Ahora se
-- referencia la aceptación (usuario + versión + snapshot + fecha), que ya vive
-- en aceptacion_terminos_plataforma. Se conserva el snapshot del artista en la
-- solicitud y los montos en presupuesto/hito_presupuesto.

ALTER TABLE pedido
    ADD COLUMN IF NOT EXISTS id_aceptacion_terminos_plataforma BIGINT;

-- Backfill: el cliente del pedido debe tener aceptada la versión del pedido.
UPDATE pedido p
   SET id_aceptacion_terminos_plataforma = a.id_aceptacion
  FROM aceptacion_terminos_plataforma a
 WHERE a.id_usuario = p.id_cliente
   AND a.version = p.terminos_plataforma_version
   AND p.id_aceptacion_terminos_plataforma IS NULL;

-- Verificar antes de NOT NULL.
DO $$
DECLARE
    faltantes INTEGER;
BEGIN
    SELECT count(*) INTO faltantes
      FROM pedido
     WHERE id_aceptacion_terminos_plataforma IS NULL;
    IF faltantes > 0 THEN
        RAISE EXCEPTION
            'Hay % pedidos sin aceptación de TOS de plataforma; revisar antes de continuar',
            faltantes;
    END IF;
END $$;

ALTER TABLE pedido
    ALTER COLUMN id_aceptacion_terminos_plataforma SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_pedido_aceptacion_terminos'
    ) THEN
        ALTER TABLE pedido
            ADD CONSTRAINT fk_pedido_aceptacion_terminos
            FOREIGN KEY (id_aceptacion_terminos_plataforma)
            REFERENCES aceptacion_terminos_plataforma (id_aceptacion);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_pedido_aceptacion_terminos
    ON pedido (id_aceptacion_terminos_plataforma);

-- La relación inversa (aceptación -> pedido) era redundante y estaba NULL.
ALTER TABLE aceptacion_terminos_plataforma
    DROP COLUMN IF EXISTS id_pedido;
DROP INDEX IF EXISTS idx_aceptacion_pedido;

-- Garantizar una sola versión vigente de TOS de plataforma.
CREATE UNIQUE INDEX IF NOT EXISTS uk_terminos_plataforma_vigente
    ON terminos_plataforma (vigente) WHERE vigente;
