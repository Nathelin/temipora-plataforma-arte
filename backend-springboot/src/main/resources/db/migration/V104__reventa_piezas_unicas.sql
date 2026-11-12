-- Reventa de piezas únicas (v1, sin regalías).
-- El tope de reventa y la comisión quedan configurables para las próximas versiones.

ALTER TABLE configuracion_comision
    ADD COLUMN porcentaje_reventa NUMERIC(5,2) NOT NULL DEFAULT 10.00;

ALTER TABLE configuracion_comision
    ADD CONSTRAINT ck_configuracion_comision_reventa
    CHECK (porcentaje_reventa >= 0 AND porcentaje_reventa <= 50);

-- Tope congelado en la pieza: se escribe una sola vez, en la primera venta.
ALTER TABLE producto_variante
    ADD COLUMN tipo_tope_reventa VARCHAR(30);

ALTER TABLE producto_variante
    ADD COLUMN precio_tope_reventa NUMERIC(12,2);

ALTER TABLE producto_variante
    ADD CONSTRAINT ck_variante_tope_reventa
    CHECK (precio_tope_reventa IS NULL OR precio_tope_reventa > 0);

ALTER TABLE producto_variante
    ADD CONSTRAINT ck_variante_tipo_tope
    CHECK (tipo_tope_reventa IS NULL OR tipo_tope_reventa IN ('PRECIO_PUBLICACION'));

CREATE TABLE publicacion_reventa (
    id_publicacion_reventa BIGSERIAL PRIMARY KEY,
    id_variante BIGINT NOT NULL,
    id_vendedor BIGINT NOT NULL,
    id_compra_item_origen BIGINT NOT NULL,
    id_moneda BIGINT NOT NULL,
    precio NUMERIC(12,2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PUBLICADA',
    version BIGINT NOT NULL DEFAULT 0,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP,
    fecha_publicacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_cierre TIMESTAMP,
    CONSTRAINT fk_publicacion_reventa_variante
        FOREIGN KEY (id_variante) REFERENCES producto_variante (id_variante),
    CONSTRAINT fk_publicacion_reventa_vendedor
        FOREIGN KEY (id_vendedor) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_publicacion_reventa_origen
        FOREIGN KEY (id_compra_item_origen) REFERENCES compra_item (id_compra_item),
    CONSTRAINT fk_publicacion_reventa_moneda
        FOREIGN KEY (id_moneda) REFERENCES moneda (id_moneda),
    CONSTRAINT ck_publicacion_reventa_estado
        CHECK (estado IN ('PUBLICADA','RESERVADA','VENDIDA','CANCELADA','VENCIDA')),
    CONSTRAINT ck_publicacion_reventa_precio
        CHECK (precio > 0)
);

CREATE UNIQUE INDEX uk_publicacion_reventa_activa
    ON publicacion_reventa (id_variante)
    WHERE estado IN ('PUBLICADA','RESERVADA');

CREATE INDEX idx_publicacion_reventa_vendedor
    ON publicacion_reventa (id_vendedor, estado);

ALTER TABLE carrito_item
    ADD COLUMN id_publicacion_reventa BIGINT;

ALTER TABLE carrito_item
    ADD CONSTRAINT fk_carrito_item_publicacion_reventa
    FOREIGN KEY (id_publicacion_reventa) REFERENCES publicacion_reventa (id_publicacion_reventa);

ALTER TABLE compra_item
    ADD COLUMN origen VARCHAR(20) NOT NULL DEFAULT 'PRIMERA_VENTA';

ALTER TABLE compra_item
    ADD COLUMN id_publicacion_reventa BIGINT;

ALTER TABLE compra_item
    ADD CONSTRAINT fk_compra_item_publicacion_reventa
    FOREIGN KEY (id_publicacion_reventa) REFERENCES publicacion_reventa (id_publicacion_reventa);

ALTER TABLE compra_item
    ADD CONSTRAINT ck_compra_item_origen
    CHECK (origen IN ('PRIMERA_VENTA','REVENTA'));

CREATE INDEX idx_compra_item_publicacion_reventa
    ON compra_item (id_publicacion_reventa);

-- Backfill: las piezas ya vendidas toman como tope el precio de lista de su primera venta.
UPDATE producto_variante v
SET tipo_tope_reventa = 'PRECIO_PUBLICACION',
    precio_tope_reventa = ci.precio_original
FROM (
    SELECT DISTINCT ON (e.id_variante) e.id_variante, e.id_compra_item
    FROM evento_ledger e
    WHERE e.tipo = 'VENTA' AND e.id_compra_item IS NOT NULL
    ORDER BY e.id_variante, e.secuencia
) x
JOIN compra_item ci ON ci.id_compra_item = x.id_compra_item
WHERE v.id_variante = x.id_variante;
