CREATE TABLE IF NOT EXISTS item_solicitud_derecho (
    id_item_solicitud_derecho BIGSERIAL PRIMARY KEY,
    id_item_solicitud BIGINT NOT NULL,
    id_derecho_termino BIGINT NOT NULL,
    codigo VARCHAR(30),
    nombre VARCHAR(80) NOT NULL,
    porcentaje_extra NUMERIC(6,2) NOT NULL DEFAULT 0,
    orden INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_isd_item FOREIGN KEY (id_item_solicitud)
        REFERENCES item_solicitud (id_item_solicitud) ON DELETE CASCADE,
    CONSTRAINT fk_isd_derecho FOREIGN KEY (id_derecho_termino)
        REFERENCES derecho_termino (id_derecho_termino),
    CONSTRAINT uk_isd_item_derecho UNIQUE (id_item_solicitud, id_derecho_termino)
);

ALTER TABLE recargo_item_presupuesto
    ADD COLUMN IF NOT EXISTS automatico BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE recargo_item_presupuesto
    ADD COLUMN IF NOT EXISTS id_derecho_termino BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'fk_recargo_derecho') THEN
        ALTER TABLE recargo_item_presupuesto
            ADD CONSTRAINT fk_recargo_derecho
            FOREIGN KEY (id_derecho_termino)
            REFERENCES derecho_termino (id_derecho_termino);
    END IF;
END $$;
