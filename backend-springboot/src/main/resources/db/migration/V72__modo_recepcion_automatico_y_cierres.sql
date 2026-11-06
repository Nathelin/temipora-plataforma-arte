-- Modo de recepción automático según cupos + alternativas al cerrarse una
-- solicitud o un pedido que el cliente no eligió perder.

-- Preferencia del artista vs. modo efectivo. NULL = el modo efectivo es una
-- decisión del artista. 'ABIERTA' = el sistema forzó SOLO_ESPERA porque el
-- servicio se llenó y debe restaurarlo cuando se libere un cupo.
ALTER TABLE servicio
    ADD COLUMN IF NOT EXISTS modo_recepcion_preferido VARCHAR(20);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_servicio_modo_preferido') THEN
        ALTER TABLE servicio ADD CONSTRAINT ck_servicio_modo_preferido
            CHECK (modo_recepcion_preferido IS NULL
                OR modo_recepcion_preferido IN ('ABIERTA','SOLO_ESPERA'));
    END IF;
END $$;

-- Vínculo del evento de cierre con el pedido (el de la solicitud ya existe).
ALTER TABLE sustitucion
    ADD COLUMN IF NOT EXISTS id_pedido BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sustitucion_pedido') THEN
        ALTER TABLE sustitucion ADD CONSTRAINT fk_sustitucion_pedido
            FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido) ON DELETE SET NULL;
    END IF;
END $$;

-- Motivos nuevos: cierres que el cliente no eligió.
ALTER TABLE sustitucion DROP CONSTRAINT IF EXISTS ck_sustitucion_motivo;
ALTER TABLE sustitucion ADD CONSTRAINT ck_sustitucion_motivo
    CHECK (motivo IN ('SIN_CUPOS','SOLO_ESPERA','CERRADA','PAUSADO','ARCHIVADO',
                      'SOLICITUD_RECHAZADA','PRESUPUESTO_RETIRADO',
                      'PRESUPUESTO_VENCIDO','SERVICIO_DADO_DE_BAJA',
                      'PEDIDO_IMPAGO'));

ALTER TABLE sustitucion DROP CONSTRAINT IF EXISTS ck_sustitucion_disparador;
ALTER TABLE sustitucion ADD CONSTRAINT ck_sustitucion_disparador
    CHECK (disparador IN ('DETALLE','EXPLORAR','PERFIL','FAVORITOS',
                          'INTENTO_SOLICITUD','CIERRE'));
