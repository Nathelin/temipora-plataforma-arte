-- Galería pública de pedidos y publicación consentida por ambas partes.

ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS pedidos_publicos BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE pedido
    ADD COLUMN IF NOT EXISTS publicacion_autorizada BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pedido
    ADD COLUMN IF NOT EXISTS publicacion_autorizada_en TIMESTAMP;
ALTER TABLE pedido
    ADD COLUMN IF NOT EXISTS publicacion_ocultada_cliente BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pedido
    ADD COLUMN IF NOT EXISTS publicacion_ocultada_en TIMESTAMP;

ALTER TABLE entrega_hito
    ADD COLUMN IF NOT EXISTS es_final BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE entrega_archivo
    ADD COLUMN IF NOT EXISTS clave_limpia VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_pedido_publicacion
    ON pedido (estado, publicacion_autorizada, publicacion_ocultada_cliente);

CREATE INDEX IF NOT EXISTS idx_entrega_hito_final
    ON entrega_hito (es_final, vigente);

INSERT INTO tipo_notificacion (codigo, nombre, frecuencia, estado)
SELECT v.codigo, v.nombre, 'EVENTO', TRUE
FROM (VALUES
    ('PEDIDO_PUBLICACION_AUTORIZADA', 'Pedido autorizado para la galería del cliente'),
    ('PEDIDO_PUBLICACION_RETIRADA', 'Publicación de pedido retirada')
) AS v(codigo, nombre)
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_notificacion t WHERE t.codigo = v.codigo
);
