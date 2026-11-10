-- V92: borradores de producto y entregables declarados.
--
-- Un borrador es un producto incompleto: los campos de publicación pasan a
-- ser opcionales y la obligatoriedad se revalida al publicar. El índice de
-- unicidad de título excluye los borradores para que dos borradores puedan
-- compartir título y tipo sin bloquearse entre sí.

ALTER TABLE producto
    ALTER COLUMN titulo DROP NOT NULL,
    ALTER COLUMN descripcion DROP NOT NULL,
    ALTER COLUMN id_tipo_producto DROP NOT NULL,
    ALTER COLUMN id_medio DROP NOT NULL,
    ALTER COLUMN id_moneda DROP NOT NULL;

ALTER TABLE producto DROP CONSTRAINT ck_producto_estado;
ALTER TABLE producto ADD CONSTRAINT ck_producto_estado
    CHECK (estado IN ('BORRADOR', 'DISPONIBLE', 'PAUSADO', 'ARCHIVADO'));

DROP INDEX IF EXISTS uk_producto_artista_titulo_tipo;
CREATE UNIQUE INDEX uk_producto_artista_titulo_tipo
    ON producto (id_artista, lower(titulo), id_tipo_producto)
    WHERE estado NOT IN ('ARCHIVADO', 'BORRADOR');

-- Un borrador puede tener una presentación todavía sin precio.
ALTER TABLE producto_variante DROP CONSTRAINT ck_variante_precio;
ALTER TABLE producto_variante ADD CONSTRAINT ck_variante_precio
    CHECK (precio >= 0);

CREATE TABLE producto_entregable (
    id_entregable BIGSERIAL PRIMARY KEY,
    id_producto BIGINT NOT NULL,
    id_variante BIGINT,
    alcance VARCHAR(20) NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    mime VARCHAR(100),
    tamano_bytes BIGINT,
    clave_archivo VARCHAR(255) NOT NULL,
    sha256 CHAR(64),
    orden INTEGER NOT NULL DEFAULT 0,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_entregable_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto) ON DELETE CASCADE,
    CONSTRAINT fk_entregable_variante FOREIGN KEY (id_variante)
        REFERENCES producto_variante (id_variante) ON DELETE CASCADE,
    CONSTRAINT ck_entregable_alcance
        CHECK (alcance IN ('COMUN', 'VARIANTE')),
    CONSTRAINT ck_entregable_alcance_variante
        CHECK ((alcance = 'VARIANTE' AND id_variante IS NOT NULL)
            OR (alcance = 'COMUN' AND id_variante IS NULL))
);

CREATE UNIQUE INDEX uk_entregable_comun
    ON producto_entregable (id_producto, orden)
    WHERE alcance = 'COMUN';

CREATE UNIQUE INDEX uk_entregable_variante
    ON producto_entregable (id_variante, orden)
    WHERE alcance = 'VARIANTE';

CREATE INDEX idx_entregable_producto
    ON producto_entregable (id_producto);
