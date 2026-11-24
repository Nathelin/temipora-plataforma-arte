-- Prórrogas del plazo de entrega de un pedido.
-- La promesa original (pedido.limite_entrega) no se modifica: los días
-- aceptados se suman a la entrega prevista.

CREATE TABLE IF NOT EXISTS prorroga_pedido (
    id_prorroga BIGSERIAL PRIMARY KEY,
    id_pedido BIGINT NOT NULL,
    dias_solicitados INTEGER NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    motivo_respuesta VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_solicitud TIMESTAMP NOT NULL DEFAULT now(),
    fecha_limite_respuesta TIMESTAMP NOT NULL,
    fecha_respuesta TIMESTAMP,
    entrega_prevista_antes DATE,
    entrega_prevista_despues DATE
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_prorroga_pedido_pedido'
    ) THEN
        ALTER TABLE prorroga_pedido ADD CONSTRAINT fk_prorroga_pedido_pedido
            FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_prorroga_estado'
    ) THEN
        ALTER TABLE prorroga_pedido ADD CONSTRAINT ck_prorroga_estado
            CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'VENCIDA'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_prorroga_dias'
    ) THEN
        ALTER TABLE prorroga_pedido ADD CONSTRAINT ck_prorroga_dias
            CHECK (dias_solicitados BETWEEN 1 AND 365);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_prorroga_pedido
    ON prorroga_pedido (id_pedido, fecha_solicitud DESC);

-- Una sola propuesta viva por pedido, garantizado por la base.
CREATE UNIQUE INDEX IF NOT EXISTS uq_prorroga_pendiente
    ON prorroga_pedido (id_pedido) WHERE estado = 'PENDIENTE';

-- Ventana de respuesta configurable por el administrador.
ALTER TABLE configuracion_pedido
    ADD COLUMN IF NOT EXISTS horas_respuesta_prorroga INTEGER NOT NULL DEFAULT 24;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_config_prorroga_rangos'
    ) THEN
        ALTER TABLE configuracion_pedido ADD CONSTRAINT ck_config_prorroga_rangos
            CHECK (horas_respuesta_prorroga BETWEEN 1 AND 168);
    END IF;
END $$;
