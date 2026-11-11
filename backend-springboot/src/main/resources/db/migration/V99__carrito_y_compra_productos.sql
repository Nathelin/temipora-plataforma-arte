-- V99: carrito y compra de productos.
-- Modelo preparado para un pago por artista hoy y Split 1:N en el futuro:
-- una compra tiene N grupos por artista, un intento de cobro puede cubrir
-- uno o varios grupos mediante intento_cobro_grupo.

CREATE TABLE IF NOT EXISTS carrito (
    id_carrito BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT fk_carrito_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT uk_carrito_usuario UNIQUE (id_usuario)
);

CREATE TABLE IF NOT EXISTS carrito_item (
    id_carrito_item BIGSERIAL PRIMARY KEY,
    id_carrito BIGINT NOT NULL,
    id_variante BIGINT NOT NULL,
    cantidad INTEGER NOT NULL DEFAULT 1,
    fecha_agregado TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_carrito_item_carrito FOREIGN KEY (id_carrito)
        REFERENCES carrito (id_carrito) ON DELETE CASCADE,
    CONSTRAINT fk_carrito_item_variante FOREIGN KEY (id_variante)
        REFERENCES producto_variante (id_variante),
    CONSTRAINT ck_carrito_item_cantidad CHECK (cantidad > 0),
    CONSTRAINT uk_carrito_item_variante UNIQUE (id_carrito, id_variante)
);

CREATE INDEX IF NOT EXISTS idx_carrito_item_carrito
    ON carrito_item (id_carrito);

CREATE TABLE IF NOT EXISTS compra (
    id_compra BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL,
    id_moneda BIGINT NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL DEFAULT 0,
    comision_total NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total NUMERIC(12, 2) NOT NULL DEFAULT 0,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_PAGO',
    clave_idempotencia VARCHAR(100),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT fk_compra_cliente FOREIGN KEY (id_cliente)
        REFERENCES usuario (id_usuario),
    CONSTRAINT fk_compra_moneda FOREIGN KEY (id_moneda)
        REFERENCES moneda (id_moneda),
    CONSTRAINT uk_compra_idempotencia UNIQUE (clave_idempotencia),
    CONSTRAINT ck_compra_estado CHECK (estado IN (
        'PENDIENTE_PAGO', 'PARCIALMENTE_PAGA', 'PAGA', 'VENCIDA', 'CANCELADA'))
);

CREATE INDEX IF NOT EXISTS idx_compra_cliente
    ON compra (id_cliente, fecha_creacion DESC);

CREATE TABLE IF NOT EXISTS compra_grupo (
    id_compra_grupo BIGSERIAL PRIMARY KEY,
    id_compra BIGINT NOT NULL,
    id_artista BIGINT NOT NULL,
    id_moneda BIGINT NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL DEFAULT 0,
    comision NUMERIC(12, 2) NOT NULL DEFAULT 0,
    neto_artista NUMERIC(12, 2) NOT NULL DEFAULT 0,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    id_cuenta_pago BIGINT,
    cuenta_externa_id VARCHAR(100),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP,
    fecha_pago TIMESTAMP,
    CONSTRAINT fk_compra_grupo_compra FOREIGN KEY (id_compra)
        REFERENCES compra (id_compra) ON DELETE CASCADE,
    CONSTRAINT fk_compra_grupo_artista FOREIGN KEY (id_artista)
        REFERENCES usuario (id_usuario),
    CONSTRAINT fk_compra_grupo_moneda FOREIGN KEY (id_moneda)
        REFERENCES moneda (id_moneda),
    CONSTRAINT fk_compra_grupo_cuenta FOREIGN KEY (id_cuenta_pago)
        REFERENCES cuenta_pago_usuario (id_cuenta_pago),
    CONSTRAINT uk_compra_grupo_artista UNIQUE (id_compra, id_artista),
    CONSTRAINT ck_compra_grupo_estado CHECK (estado IN (
        'PENDIENTE', 'PAGADO', 'VENCIDO', 'CANCELADO'))
);

CREATE INDEX IF NOT EXISTS idx_compra_grupo_compra
    ON compra_grupo (id_compra);

CREATE TABLE IF NOT EXISTS compra_item (
    id_compra_item BIGSERIAL PRIMARY KEY,
    id_compra_grupo BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    id_variante BIGINT NOT NULL,
    titulo_producto VARCHAR(100) NOT NULL,
    nombre_variante VARCHAR(100) NOT NULL,
    tipo_entrega VARCHAR(20) NOT NULL,
    cantidad INTEGER NOT NULL,
    precio_original NUMERIC(12, 2) NOT NULL,
    id_moneda_original BIGINT NOT NULL,
    cotizacion NUMERIC(18, 8) NOT NULL DEFAULT 1,
    precio_cobro NUMERIC(12, 2) NOT NULL,
    comision_porcentaje NUMERIC(5, 2) NOT NULL,
    comision_monto NUMERIC(12, 2) NOT NULL,
    sha256_archivo CHAR(64),
    licencia_snapshot TEXT,
    licencia_aceptada_en TIMESTAMP,
    estado_entrega VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_entrega TIMESTAMP,
    CONSTRAINT fk_compra_item_grupo FOREIGN KEY (id_compra_grupo)
        REFERENCES compra_grupo (id_compra_grupo) ON DELETE CASCADE,
    CONSTRAINT fk_compra_item_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto),
    CONSTRAINT fk_compra_item_variante FOREIGN KEY (id_variante)
        REFERENCES producto_variante (id_variante),
    CONSTRAINT fk_compra_item_moneda FOREIGN KEY (id_moneda_original)
        REFERENCES moneda (id_moneda),
    CONSTRAINT ck_compra_item_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_compra_item_estado CHECK (estado_entrega IN (
        'PENDIENTE', 'ENTREGADO', 'CANCELADO'))
);

CREATE INDEX IF NOT EXISTS idx_compra_item_grupo
    ON compra_item (id_compra_grupo);

CREATE TABLE IF NOT EXISTS reserva_stock (
    id_reserva BIGSERIAL PRIMARY KEY,
    id_compra_item BIGINT NOT NULL,
    id_variante BIGINT NOT NULL,
    cantidad INTEGER NOT NULL,
    expira_en TIMESTAMP NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT fk_reserva_item FOREIGN KEY (id_compra_item)
        REFERENCES compra_item (id_compra_item) ON DELETE CASCADE,
    CONSTRAINT fk_reserva_variante FOREIGN KEY (id_variante)
        REFERENCES producto_variante (id_variante),
    CONSTRAINT uk_reserva_item UNIQUE (id_compra_item),
    CONSTRAINT ck_reserva_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_reserva_estado CHECK (estado IN (
        'ACTIVA', 'CONFIRMADA', 'LIBERADA', 'VENCIDA'))
);

CREATE INDEX IF NOT EXISTS idx_reserva_variante_estado
    ON reserva_stock (id_variante, estado);
CREATE INDEX IF NOT EXISTS idx_reserva_expira
    ON reserva_stock (expira_en);

CREATE TABLE IF NOT EXISTS intento_cobro (
    id_intento_cobro BIGSERIAL PRIMARY KEY,
    id_compra BIGINT NOT NULL,
    proveedor VARCHAR(30) NOT NULL,
    external_reference VARCHAR(100) NOT NULL,
    id_preferencia_externa VARCHAR(100),
    id_pago_externo VARCHAR(100),
    instruccion TEXT,
    estado VARCHAR(30) NOT NULL DEFAULT 'CREADO',
    estado_externo VARCHAR(50),
    monto NUMERIC(12, 2) NOT NULL,
    id_moneda BIGINT NOT NULL,
    clave_idempotencia VARCHAR(100),
    payload TEXT,
    arancel_mp NUMERIC(12, 2),
    neto_recibido_mp NUMERIC(12, 2),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP,
    fecha_aprobacion TIMESTAMP,
    CONSTRAINT fk_intento_compra FOREIGN KEY (id_compra)
        REFERENCES compra (id_compra) ON DELETE CASCADE,
    CONSTRAINT fk_intento_moneda FOREIGN KEY (id_moneda)
        REFERENCES moneda (id_moneda),
    CONSTRAINT uk_intento_external_reference UNIQUE (external_reference),
    CONSTRAINT uk_intento_idempotencia UNIQUE (clave_idempotencia),
    CONSTRAINT ck_intento_estado CHECK (estado IN (
        'CREADO', 'PENDIENTE', 'APROBADO', 'RECHAZADO', 'EXPIRADO', 'CANCELADO'))
);

CREATE INDEX IF NOT EXISTS idx_intento_compra
    ON intento_cobro (id_compra);

CREATE TABLE IF NOT EXISTS intento_cobro_grupo (
    id_intento_cobro_grupo BIGSERIAL PRIMARY KEY,
    id_intento_cobro BIGINT NOT NULL,
    id_compra_grupo BIGINT NOT NULL,
    monto NUMERIC(12, 2) NOT NULL,
    comision NUMERIC(12, 2) NOT NULL,
    neto NUMERIC(12, 2) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    receiver_id VARCHAR(100),
    CONSTRAINT fk_intento_grupo_intento FOREIGN KEY (id_intento_cobro)
        REFERENCES intento_cobro (id_intento_cobro) ON DELETE CASCADE,
    CONSTRAINT fk_intento_grupo_grupo FOREIGN KEY (id_compra_grupo)
        REFERENCES compra_grupo (id_compra_grupo),
    CONSTRAINT uk_intento_grupo UNIQUE (id_intento_cobro, id_compra_grupo),
    CONSTRAINT ck_intento_grupo_estado CHECK (estado IN (
        'CREADO', 'PENDIENTE', 'APROBADO', 'RECHAZADO', 'EXPIRADO', 'CANCELADO'))
);

CREATE INDEX IF NOT EXISTS idx_intento_grupo_grupo
    ON intento_cobro_grupo (id_compra_grupo);

COMMENT ON TABLE carrito IS
    'Carrito activo por usuario. Solo guarda variante y cantidad; los precios se recalculan.';
COMMENT ON TABLE compra IS
    'Orden de compra de productos. Agrupa uno o más grupos por artista.';
COMMENT ON TABLE compra_grupo IS
    'Unidad de liquidación por artista. Hoy se paga con una preferencia 1:1.';
COMMENT ON TABLE intento_cobro IS
    'Intento de cobro de una compra. Puede cubrir uno o varios grupos.';
COMMENT ON TABLE intento_cobro_grupo IS
    'Asignación de un intento de cobro a un grupo. Permite Split 1:N sin migrar.';
