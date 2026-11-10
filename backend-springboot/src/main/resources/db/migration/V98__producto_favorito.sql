-- V98: favoritos de producto.
--
-- Mismo patrón que `servicio_favorito`: PK compuesta (usuario, producto) y
-- baja en cascada desde ambos lados.

CREATE TABLE IF NOT EXISTS producto_favorito (
    id_usuario BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    fecha_agregado TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (id_usuario, id_producto),
    CONSTRAINT fk_producto_favorito_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_producto_favorito_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_producto_favorito_producto
    ON producto_favorito (id_producto);
