-- Pedidos: instancia de ejecución del presupuesto aceptado.
-- Se descartan las tablas viejas de Pedido/Pago, que eran entidades huérfanas
-- sin datos ni referencias.

DROP TABLE IF EXISTS "Pedido";
DROP TABLE IF EXISTS pedido CASCADE;
DROP TABLE IF EXISTS pago CASCADE;

CREATE TABLE IF NOT EXISTS pedido (
    id_pedido BIGSERIAL PRIMARY KEY,
    id_solicitud BIGINT NOT NULL,
    id_presupuesto BIGINT NOT NULL,
    id_cliente BIGINT NOT NULL,
    id_artista BIGINT NOT NULL,
    id_servicio BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE_PAGO',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_inicio DATE,
    limite_entrega DATE,
    finalizado_en TIMESTAMP,
    precio_final NUMERIC(12,2) NOT NULL,
    precio_final_cobro NUMERIC(12,2) NOT NULL,
    id_moneda BIGINT NOT NULL,
    id_moneda_cobro BIGINT NOT NULL,
    cotizacion_cobro NUMERIC(18,8) NOT NULL,
    comision_porcentaje NUMERIC(5,2) NOT NULL,
    terminos_plataforma_version INTEGER,
    terminos_plataforma_snapshot TEXT,
    terminos_plataforma_aceptados_en TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pedido_solicitud') THEN
        ALTER TABLE pedido ADD CONSTRAINT fk_pedido_solicitud
            FOREIGN KEY (id_solicitud) REFERENCES solicitud (id_solicitud);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pedido_presupuesto') THEN
        ALTER TABLE pedido ADD CONSTRAINT fk_pedido_presupuesto
            FOREIGN KEY (id_presupuesto) REFERENCES presupuesto (id_presupuesto);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pedido_cliente') THEN
        ALTER TABLE pedido ADD CONSTRAINT fk_pedido_cliente
            FOREIGN KEY (id_cliente) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pedido_artista') THEN
        ALTER TABLE pedido ADD CONSTRAINT fk_pedido_artista
            FOREIGN KEY (id_artista) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pedido_servicio') THEN
        ALTER TABLE pedido ADD CONSTRAINT fk_pedido_servicio
            FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pedido_moneda') THEN
        ALTER TABLE pedido ADD CONSTRAINT fk_pedido_moneda
            FOREIGN KEY (id_moneda) REFERENCES moneda (id_moneda);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pedido_moneda_cobro') THEN
        ALTER TABLE pedido ADD CONSTRAINT fk_pedido_moneda_cobro
            FOREIGN KEY (id_moneda_cobro) REFERENCES moneda (id_moneda);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_pedido_estado') THEN
        ALTER TABLE pedido ADD CONSTRAINT ck_pedido_estado
            CHECK (estado IN ('PENDIENTE_PAGO', 'EN_CURSO', 'FINALIZADO', 'CANCELADO', 'IMPAGO'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_pedido_montos') THEN
        ALTER TABLE pedido ADD CONSTRAINT ck_pedido_montos
            CHECK (precio_final >= 0 AND precio_final_cobro >= 0
               AND cotizacion_cobro > 0);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_pedido_solicitud
    ON pedido (id_solicitud);
CREATE UNIQUE INDEX IF NOT EXISTS uk_pedido_presupuesto
    ON pedido (id_presupuesto);
CREATE INDEX IF NOT EXISTS idx_pedido_cliente
    ON pedido (id_cliente, fecha_creacion DESC);
CREATE INDEX IF NOT EXISTS idx_pedido_artista
    ON pedido (id_artista, fecha_creacion DESC);

CREATE TABLE IF NOT EXISTS hito_pedido (
    id_hito_pedido BIGSERIAL PRIMARY KEY,
    id_pedido BIGINT NOT NULL,
    id_hito_presupuesto BIGINT NOT NULL,
    orden INTEGER NOT NULL,
    nombre VARCHAR(60) NOT NULL,
    tipo VARCHAR(10) NOT NULL,
    requiere_entrega BOOLEAN NOT NULL DEFAULT TRUE,
    monto NUMERIC(12,2) NOT NULL,
    monto_cobro NUMERIC(12,2) NOT NULL,
    comision_monto NUMERIC(12,2) NOT NULL,
    comision_monto_cobro NUMERIC(12,2) NOT NULL,
    monto_neto NUMERIC(12,2) NOT NULL,
    monto_neto_cobro NUMERIC(12,2) NOT NULL,
    estado_pago VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    estado_entrega VARCHAR(30) NOT NULL DEFAULT 'NO_INICIADO',
    fecha_limite_pago TIMESTAMP,
    fecha_pago TIMESTAMP,
    id_pago_aprobado BIGINT,
    fecha_entrega TIMESTAMP,
    fecha_limite_aprobacion TIMESTAMP,
    fecha_aprobacion TIMESTAMP,
    entrega_descripcion VARCHAR(500),
    entrega_imagen VARCHAR(255)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_hito_pedido_pedido') THEN
        ALTER TABLE hito_pedido ADD CONSTRAINT fk_hito_pedido_pedido
            FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_hito_pedido_origen') THEN
        ALTER TABLE hito_pedido ADD CONSTRAINT fk_hito_pedido_origen
            FOREIGN KEY (id_hito_presupuesto) REFERENCES hito_presupuesto (id_hito_presupuesto);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_hito_pedido_tipo') THEN
        ALTER TABLE hito_pedido ADD CONSTRAINT ck_hito_pedido_tipo
            CHECK (tipo IN ('SENA', 'CUOTA', 'ENTREGA'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_hito_pedido_estado_pago') THEN
        ALTER TABLE hito_pedido ADD CONSTRAINT ck_hito_pedido_estado_pago
            CHECK (estado_pago IN ('PENDIENTE', 'PAGADO', 'VENCIDO', 'REEMBOLSADO', 'CANCELADO'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_hito_pedido_estado_entrega') THEN
        ALTER TABLE hito_pedido ADD CONSTRAINT ck_hito_pedido_estado_entrega
            CHECK (estado_entrega IN ('NO_INICIADO', 'EN_PROCESO', 'ENTREGADO', 'APROBADO', 'APROBADO_AUTOMATICAMENTE', 'CON_REVISION'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_hito_pedido_montos') THEN
        ALTER TABLE hito_pedido ADD CONSTRAINT ck_hito_pedido_montos
            CHECK (monto >= 0 AND monto_cobro >= 0
               AND comision_monto >= 0 AND comision_monto_cobro >= 0
               AND monto_neto >= 0 AND monto_neto_cobro >= 0);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_hito_pedido_orden
    ON hito_pedido (id_pedido, orden);
CREATE UNIQUE INDEX IF NOT EXISTS uk_hito_pedido_origen
    ON hito_pedido (id_hito_presupuesto);
CREATE INDEX IF NOT EXISTS idx_hito_pedido_pedido
    ON hito_pedido (id_pedido, orden);
CREATE INDEX IF NOT EXISTS idx_hito_pedido_aprobacion
    ON hito_pedido (estado_entrega, fecha_limite_aprobacion);
CREATE INDEX IF NOT EXISTS idx_hito_pedido_vencimiento
    ON hito_pedido (estado_pago, fecha_limite_pago);

CREATE TABLE IF NOT EXISTS pago (
    id_pago BIGSERIAL PRIMARY KEY,
    id_hito_pedido BIGINT NOT NULL,
    id_usuario_pagador BIGINT NOT NULL,
    monto NUMERIC(12,2) NOT NULL,
    id_moneda BIGINT NOT NULL,
    monto_cobro NUMERIC(12,2) NOT NULL,
    id_moneda_cobro BIGINT NOT NULL,
    cotizacion_cobro NUMERIC(18,8) NOT NULL,
    comision_monto_cobro NUMERIC(12,2) NOT NULL,
    monto_neto_cobro NUMERIC(12,2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    proveedor VARCHAR(30) NOT NULL,
    id_externo VARCHAR(100),
    clave_idempotencia VARCHAR(100),
    payload TEXT,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_acreditacion TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pago_hito') THEN
        ALTER TABLE pago ADD CONSTRAINT fk_pago_hito
            FOREIGN KEY (id_hito_pedido) REFERENCES hito_pedido (id_hito_pedido);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pago_usuario') THEN
        ALTER TABLE pago ADD CONSTRAINT fk_pago_usuario
            FOREIGN KEY (id_usuario_pagador) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pago_moneda') THEN
        ALTER TABLE pago ADD CONSTRAINT fk_pago_moneda
            FOREIGN KEY (id_moneda) REFERENCES moneda (id_moneda);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pago_moneda_cobro') THEN
        ALTER TABLE pago ADD CONSTRAINT fk_pago_moneda_cobro
            FOREIGN KEY (id_moneda_cobro) REFERENCES moneda (id_moneda);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_pago_estado') THEN
        ALTER TABLE pago ADD CONSTRAINT ck_pago_estado
            CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO', 'REEMBOLSADO', 'CANCELADO'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_pago_montos') THEN
        ALTER TABLE pago ADD CONSTRAINT ck_pago_montos
            CHECK (monto >= 0 AND monto_cobro >= 0
               AND cotizacion_cobro > 0
               AND comision_monto_cobro >= 0 AND monto_neto_cobro >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_hito_pedido_pago_aprobado') THEN
        ALTER TABLE hito_pedido ADD CONSTRAINT fk_hito_pedido_pago_aprobado
            FOREIGN KEY (id_pago_aprobado) REFERENCES pago (id_pago);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_pago_externo
    ON pago (id_externo) WHERE id_externo IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_pago_idempotencia
    ON pago (clave_idempotencia) WHERE clave_idempotencia IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_pago_hito
    ON pago (id_hito_pedido, fecha_creacion DESC);
