-- V90: licencia de producto versionada y desacople de los términos de servicio.
-- No hay productos creados, por eso las columnas legacy de producto y las
-- referencias de terminos.id_producto se pueden retirar sin migrar filas.

CREATE TABLE licencia_producto (
    id_licencia_producto BIGSERIAL PRIMARY KEY,
    id_producto BIGINT NOT NULL,
    permite_modificaciones BOOLEAN NOT NULL DEFAULT FALSE,
    requiere_credito BOOLEAN NOT NULL DEFAULT FALSE,
    porcentaje_regalia NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    texto_adicional VARCHAR(2000),
    version INTEGER NOT NULL DEFAULT 1,
    vigente BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_licencia_producto_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto) ON DELETE CASCADE,
    CONSTRAINT ck_licencia_producto_regalia
        CHECK (porcentaje_regalia >= 0 AND porcentaje_regalia <= 10),
    CONSTRAINT uk_licencia_producto_version
        UNIQUE (id_producto, version)
);

CREATE UNIQUE INDEX uk_licencia_producto_vigente
    ON licencia_producto (id_producto)
    WHERE vigente = TRUE;

CREATE INDEX idx_licencia_producto_producto
    ON licencia_producto (id_producto, vigente);

CREATE TABLE licencia_producto_derecho (
    id_licencia_producto BIGINT NOT NULL,
    id_derecho_termino BIGINT NOT NULL,
    fecha_asignacion TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (id_licencia_producto, id_derecho_termino),
    CONSTRAINT fk_licencia_derecho_licencia FOREIGN KEY (id_licencia_producto)
        REFERENCES licencia_producto (id_licencia_producto) ON DELETE CASCADE,
    CONSTRAINT fk_licencia_derecho_derecho FOREIGN KEY (id_derecho_termino)
        REFERENCES derecho_termino (id_derecho_termino)
);

CREATE INDEX idx_licencia_derecho_derecho
    ON licencia_producto_derecho (id_derecho_termino);

ALTER TABLE producto DROP COLUMN IF EXISTS permite_reventa;
ALTER TABLE producto DROP COLUMN IF EXISTS porcentaje_regalia;

ALTER TABLE terminos DROP CONSTRAINT IF EXISTS fk_terminos_producto;
DROP INDEX IF EXISTS uk_terminos_producto_version;
DROP INDEX IF EXISTS uk_terminos_producto_vigente;

DROP INDEX IF EXISTS uk_terminos_global_version;
DROP INDEX IF EXISTS uk_terminos_global_vigente;
CREATE UNIQUE INDEX uk_terminos_global_version
    ON terminos (id_artista, version)
    WHERE id_servicio IS NULL;
CREATE UNIQUE INDEX uk_terminos_global_vigente
    ON terminos (id_artista)
    WHERE id_servicio IS NULL AND vigente;

ALTER TABLE terminos DROP COLUMN IF EXISTS id_producto;
