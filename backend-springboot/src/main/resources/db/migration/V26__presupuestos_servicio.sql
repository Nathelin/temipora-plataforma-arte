-- UC-18: presupuestos de servicio.
-- Un presupuesto por solicitud, con recargos y notas por dibujo.
-- Incluye la vigencia global configurable y los tipos de notificación asociados.

CREATE TABLE IF NOT EXISTS configuracion_presupuesto (
    id_configuracion BIGINT PRIMARY KEY,
    vigencia_horas INTEGER NOT NULL DEFAULT 72,
    fecha_actualizacion TIMESTAMP
);

INSERT INTO configuracion_presupuesto (id_configuracion, vigencia_horas)
SELECT 1, 72
WHERE NOT EXISTS (
    SELECT 1 FROM configuracion_presupuesto WHERE id_configuracion = 1
);

ALTER TABLE configuracion_presupuesto
    DROP CONSTRAINT IF EXISTS ck_configuracion_presupuesto_vigencia;
ALTER TABLE configuracion_presupuesto
    ADD CONSTRAINT ck_configuracion_presupuesto_vigencia
    CHECK (vigencia_horas BETWEEN 1 AND 8760);

CREATE TABLE IF NOT EXISTS presupuesto (
    id_presupuesto BIGSERIAL PRIMARY KEY,
    id_solicitud BIGINT NOT NULL,
    precio_base_total NUMERIC(12,2) NOT NULL,
    precio_final NUMERIC(12,2) NOT NULL,
    id_moneda BIGINT NOT NULL,
    tiempo_entrega_valor INTEGER NOT NULL,
    tiempo_entrega_unidad VARCHAR(10) NOT NULL,
    tiempo_entrega_dias INTEGER NOT NULL,
    mensaje VARCHAR(500),
    fecha_emision TIMESTAMP NOT NULL DEFAULT now(),
    fecha_vencimiento TIMESTAMP NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_presupuesto_solicitud') THEN
        ALTER TABLE presupuesto ADD CONSTRAINT fk_presupuesto_solicitud
            FOREIGN KEY (id_solicitud) REFERENCES solicitud (id_solicitud) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_presupuesto_moneda') THEN
        ALTER TABLE presupuesto ADD CONSTRAINT fk_presupuesto_moneda
            FOREIGN KEY (id_moneda) REFERENCES moneda (id_moneda);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_presupuesto_precio_base_total') THEN
        ALTER TABLE presupuesto ADD CONSTRAINT ck_presupuesto_precio_base_total
            CHECK (precio_base_total >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_presupuesto_precio_final') THEN
        ALTER TABLE presupuesto ADD CONSTRAINT ck_presupuesto_precio_final
            CHECK (precio_final >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_presupuesto_tiempo_entrega_valor') THEN
        ALTER TABLE presupuesto ADD CONSTRAINT ck_presupuesto_tiempo_entrega_valor
            CHECK (tiempo_entrega_valor > 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_presupuesto_tiempo_entrega_dias') THEN
        ALTER TABLE presupuesto ADD CONSTRAINT ck_presupuesto_tiempo_entrega_dias
            CHECK (tiempo_entrega_dias > 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_presupuesto_tiempo_entrega_unidad') THEN
        ALTER TABLE presupuesto ADD CONSTRAINT ck_presupuesto_tiempo_entrega_unidad
            CHECK (tiempo_entrega_unidad IN ('DIAS', 'SEMANAS', 'MESES'));
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_presupuesto_solicitud
    ON presupuesto (id_solicitud);
CREATE INDEX IF NOT EXISTS idx_presupuesto_vencimiento
    ON presupuesto (fecha_vencimiento);

CREATE TABLE IF NOT EXISTS item_presupuesto (
    id_item_presupuesto BIGSERIAL PRIMARY KEY,
    id_presupuesto BIGINT NOT NULL,
    id_item_solicitud BIGINT NOT NULL,
    precio_base NUMERIC(12,2) NOT NULL,
    precio_final NUMERIC(12,2) NOT NULL,
    nota VARCHAR(250),
    orden INTEGER NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_item_presupuesto_presupuesto') THEN
        ALTER TABLE item_presupuesto ADD CONSTRAINT fk_item_presupuesto_presupuesto
            FOREIGN KEY (id_presupuesto) REFERENCES presupuesto (id_presupuesto) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_item_presupuesto_item_solicitud') THEN
        ALTER TABLE item_presupuesto ADD CONSTRAINT fk_item_presupuesto_item_solicitud
            FOREIGN KEY (id_item_solicitud) REFERENCES item_solicitud (id_item_solicitud) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_item_presupuesto_precio_base') THEN
        ALTER TABLE item_presupuesto ADD CONSTRAINT ck_item_presupuesto_precio_base
            CHECK (precio_base >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_item_presupuesto_precio_final') THEN
        ALTER TABLE item_presupuesto ADD CONSTRAINT ck_item_presupuesto_precio_final
            CHECK (precio_final >= 0);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_item_presupuesto_item_solicitud
    ON item_presupuesto (id_item_solicitud);
CREATE INDEX IF NOT EXISTS idx_item_presupuesto_presupuesto
    ON item_presupuesto (id_presupuesto);

CREATE TABLE IF NOT EXISTS recargo_item_presupuesto (
    id_recargo BIGSERIAL PRIMARY KEY,
    id_item_presupuesto BIGINT NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    valor NUMERIC(12,2) NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_recargo_item_presupuesto_item') THEN
        ALTER TABLE recargo_item_presupuesto ADD CONSTRAINT fk_recargo_item_presupuesto_item
            FOREIGN KEY (id_item_presupuesto) REFERENCES item_presupuesto (id_item_presupuesto) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_recargo_item_presupuesto_tipo') THEN
        ALTER TABLE recargo_item_presupuesto ADD CONSTRAINT ck_recargo_item_presupuesto_tipo
            CHECK (tipo IN ('FIJO', 'PORCENTAJE'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_recargo_item_presupuesto_valor') THEN
        ALTER TABLE recargo_item_presupuesto ADD CONSTRAINT ck_recargo_item_presupuesto_valor
            CHECK (valor > 0);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_recargo_item_presupuesto_item
    ON recargo_item_presupuesto (id_item_presupuesto, orden);

-- El CHECK original de V22 solo permitía RECHAZO y CANCELACION, pero el servicio de
-- retiro de presupuesto ya inserta RETIRO_PRESUPUESTO. Se corrige acá.
ALTER TABLE accion_solicitud
    DROP CONSTRAINT IF EXISTS ck_accion_solicitud_tipo;
ALTER TABLE accion_solicitud
    ADD CONSTRAINT ck_accion_solicitud_tipo
    CHECK (tipo_accion IN ('RECHAZO', 'CANCELACION', 'RETIRO_PRESUPUESTO'));

INSERT INTO tipo_notificacion (codigo, nombre, frecuencia, estado)
SELECT v.codigo, v.nombre, 'EVENTO', TRUE
FROM (VALUES
    ('PRESUPUESTO_RECIBIDO', 'Presupuesto de servicio recibido'),
    ('PRESUPUESTO_VENCIDO', 'Presupuesto de servicio vencido')
) AS v(codigo, nombre)
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_notificacion t WHERE t.codigo = v.codigo
);
