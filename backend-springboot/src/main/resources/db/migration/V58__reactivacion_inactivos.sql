-- Proceso automático de descuentos a usuarios inactivos.

CREATE TABLE IF NOT EXISTS configuracion_inactividad (
    id_configuracion BIGINT PRIMARY KEY,
    dias_sin_compra INTEGER NOT NULL DEFAULT 60,
    dias_sin_conexion INTEGER NOT NULL DEFAULT 30,
    dias_cooldown INTEGER NOT NULL DEFAULT 30,
    dias_minimos_cuenta INTEGER NOT NULL DEFAULT 7,
    porcentaje_descuento NUMERIC(5,2) NOT NULL DEFAULT 10.00,
    vigencia_horas INTEGER NOT NULL DEFAULT 72,
    max_ofertas_por_usuario INTEGER NOT NULL DEFAULT 3,
    max_usuarios_por_corrida INTEGER NOT NULL DEFAULT 50,
    fecha_actualizacion TIMESTAMP
);

INSERT INTO configuracion_inactividad (id_configuracion)
SELECT 1
WHERE NOT EXISTS (
    SELECT 1 FROM configuracion_inactividad WHERE id_configuracion = 1
);

ALTER TABLE configuracion_inactividad
    DROP CONSTRAINT IF EXISTS ck_configuracion_inactividad_rango;
ALTER TABLE configuracion_inactividad
    ADD CONSTRAINT ck_configuracion_inactividad_rango
    CHECK (dias_sin_compra >= 0
       AND dias_sin_conexion >= 0
       AND dias_cooldown >= 0
       AND dias_minimos_cuenta >= 0
       AND porcentaje_descuento BETWEEN 0 AND 50
       AND vigencia_horas > 0
       AND max_ofertas_por_usuario > 0
       AND max_usuarios_por_corrida > 0);

CREATE TABLE IF NOT EXISTS campana_reactivacion (
    id_campana BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    token VARCHAR(64) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_vencimiento TIMESTAMP NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_campana_reactivacion_usuario') THEN
        ALTER TABLE campana_reactivacion ADD CONSTRAINT fk_campana_reactivacion_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_campana_reactivacion_token') THEN
        ALTER TABLE campana_reactivacion ADD CONSTRAINT uk_campana_reactivacion_token
            UNIQUE (token);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_campana_reactivacion_usuario
    ON campana_reactivacion (id_usuario, fecha_creacion DESC);

CREATE TABLE IF NOT EXISTS oferta_reactivacion (
    id_oferta BIGSERIAL PRIMARY KEY,
    id_campana BIGINT NOT NULL,
    id_servicio BIGINT NOT NULL,
    id_artista BIGINT NOT NULL,
    porcentaje_descuento NUMERIC(5,2) NOT NULL,
    comision_tope_porcentaje NUMERIC(5,2) NOT NULL,
    motivo VARCHAR(120),
    estado VARCHAR(20) NOT NULL DEFAULT 'OFRECIDA',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_vencimiento TIMESTAMP NOT NULL,
    id_solicitud BIGINT
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_oferta_reactivacion_campana') THEN
        ALTER TABLE oferta_reactivacion ADD CONSTRAINT fk_oferta_reactivacion_campana
            FOREIGN KEY (id_campana) REFERENCES campana_reactivacion (id_campana) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_oferta_reactivacion_servicio') THEN
        ALTER TABLE oferta_reactivacion ADD CONSTRAINT fk_oferta_reactivacion_servicio
            FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_oferta_reactivacion_artista') THEN
        ALTER TABLE oferta_reactivacion ADD CONSTRAINT fk_oferta_reactivacion_artista
            FOREIGN KEY (id_artista) REFERENCES usuario (id_usuario) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_oferta_reactivacion_solicitud') THEN
        ALTER TABLE oferta_reactivacion ADD CONSTRAINT fk_oferta_reactivacion_solicitud
            FOREIGN KEY (id_solicitud) REFERENCES solicitud (id_solicitud) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_oferta_reactivacion_campana_servicio') THEN
        ALTER TABLE oferta_reactivacion ADD CONSTRAINT uk_oferta_reactivacion_campana_servicio
            UNIQUE (id_campana, id_servicio);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_oferta_reactivacion_campana
    ON oferta_reactivacion (id_campana);
CREATE INDEX IF NOT EXISTS idx_oferta_reactivacion_usuario_estado
    ON oferta_reactivacion (id_artista, estado);

-- Descuento congelado en el presupuesto y en el pedido.
ALTER TABLE solicitud
    ADD COLUMN IF NOT EXISTS id_oferta_reactivacion BIGINT;
ALTER TABLE presupuesto
    ADD COLUMN IF NOT EXISTS id_oferta_reactivacion BIGINT;
ALTER TABLE presupuesto
    ADD COLUMN IF NOT EXISTS descuento_porcentaje NUMERIC(5,2) NOT NULL DEFAULT 0;
ALTER TABLE presupuesto
    ADD COLUMN IF NOT EXISTS descuento_monto NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE presupuesto
    ADD COLUMN IF NOT EXISTS descuento_monto_cobro NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE presupuesto
    ADD COLUMN IF NOT EXISTS precio_final_cliente NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE presupuesto
    ADD COLUMN IF NOT EXISTS precio_final_cliente_cobro NUMERIC(12,2) NOT NULL DEFAULT 0;

ALTER TABLE pedido
    ADD COLUMN IF NOT EXISTS id_oferta_reactivacion BIGINT;
ALTER TABLE pedido
    ADD COLUMN IF NOT EXISTS descuento_porcentaje NUMERIC(5,2) NOT NULL DEFAULT 0;
ALTER TABLE pedido
    ADD COLUMN IF NOT EXISTS descuento_monto NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE pedido
    ADD COLUMN IF NOT EXISTS descuento_monto_cobro NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE pedido
    ADD COLUMN IF NOT EXISTS precio_final_cliente NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE pedido
    ADD COLUMN IF NOT EXISTS precio_final_cliente_cobro NUMERIC(12,2) NOT NULL DEFAULT 0;

UPDATE presupuesto
   SET precio_final_cliente = precio_final,
       precio_final_cliente_cobro = precio_final_cobro
 WHERE precio_final_cliente = 0;

UPDATE pedido
   SET precio_final_cliente = precio_final,
       precio_final_cliente_cobro = precio_final_cobro
 WHERE precio_final_cliente = 0;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_solicitud_oferta_reactivacion') THEN
        ALTER TABLE solicitud ADD CONSTRAINT fk_solicitud_oferta_reactivacion
            FOREIGN KEY (id_oferta_reactivacion) REFERENCES oferta_reactivacion (id_oferta) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_presupuesto_oferta_reactivacion') THEN
        ALTER TABLE presupuesto ADD CONSTRAINT fk_presupuesto_oferta_reactivacion
            FOREIGN KEY (id_oferta_reactivacion) REFERENCES oferta_reactivacion (id_oferta) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pedido_oferta_reactivacion') THEN
        ALTER TABLE pedido ADD CONSTRAINT fk_pedido_oferta_reactivacion
            FOREIGN KEY (id_oferta_reactivacion) REFERENCES oferta_reactivacion (id_oferta) ON DELETE SET NULL;
    END IF;
END $$;

INSERT INTO tipo_notificacion (codigo, nombre, frecuencia, estado)
SELECT 'OFERTA_REACTIVACION', 'Ofertas para retomar compras', 'EVENTO', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_notificacion t WHERE t.codigo = 'OFERTA_REACTIVACION'
);
