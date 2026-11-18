-- Reseñas de producto.
--
-- Hasta ahora `resenia` solo podía apuntar a un pedido de servicio. Se agrega
-- el ámbito explícito y un destino alternativo: el producto comprado. Una
-- reseña apunta exactamente a un pedido o a un producto, nunca a ambos.

ALTER TABLE resenia
    ADD COLUMN IF NOT EXISTS ambito VARCHAR(20);
ALTER TABLE resenia
    ADD COLUMN IF NOT EXISTS id_producto BIGINT;
ALTER TABLE resenia
    ADD COLUMN IF NOT EXISTS id_compra_item BIGINT;

-- Las reseñas existentes son de servicio/cliente: el ámbito se deduce de quién
-- escribió respecto del pedido.
UPDATE resenia r
SET ambito = CASE
        WHEN r.id_autor = p.id_cliente THEN 'SERVICIO'
        ELSE 'CLIENTE'
    END
FROM pedido p
WHERE r.id_pedido = p.id_pedido
  AND r.ambito IS NULL;

UPDATE resenia SET ambito = 'SERVICIO' WHERE ambito IS NULL;

ALTER TABLE resenia ALTER COLUMN ambito SET NOT NULL;
ALTER TABLE resenia ALTER COLUMN id_pedido DROP NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_resenia_ambito'
    ) THEN
        ALTER TABLE resenia
            ADD CONSTRAINT ck_resenia_ambito
            CHECK (ambito IN ('SERVICIO', 'CLIENTE', 'PRODUCTO'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_resenia_producto'
    ) THEN
        ALTER TABLE resenia
            ADD CONSTRAINT fk_resenia_producto
            FOREIGN KEY (id_producto)
            REFERENCES producto (id_producto);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_resenia_compra_item'
    ) THEN
        ALTER TABLE resenia
            ADD CONSTRAINT fk_resenia_compra_item
            FOREIGN KEY (id_compra_item)
            REFERENCES compra_item (id_compra_item);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_resenia_objeto'
    ) THEN
        ALTER TABLE resenia
            ADD CONSTRAINT ck_resenia_objeto
            CHECK (
                (id_pedido IS NOT NULL AND id_producto IS NULL)
                OR (id_pedido IS NULL AND id_producto IS NOT NULL)
            );
    END IF;
END $$;

-- Una sola reseña por producto y autor, aunque lo haya comprado varias veces.
CREATE UNIQUE INDEX IF NOT EXISTS uk_resenia_producto_autor
    ON resenia (id_producto, id_autor)
    WHERE id_producto IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_resenia_producto
    ON resenia (id_producto, estado);

-- ---------------------------------------------------------------------------
-- Criterios por defecto del producto
-- ---------------------------------------------------------------------------

INSERT INTO criterio_resenia (
    nombre, descripcion, ambito, escala, obligatorio, orden, activo
)
SELECT v.nombre, v.descripcion, v.ambito, v.escala, v.obligatorio, v.orden, TRUE
FROM (VALUES
    ('Calidad de la pieza',
     'Qué tan buena es la obra o el archivo que recibiste.',
     'PRODUCTO', 'ESTRELLAS', TRUE, 1),
    ('Fidelidad a lo publicado',
     'Si lo recibido coincide con las fotos y la descripción del producto.',
     'PRODUCTO', 'ESTRELLAS', TRUE, 2),
    ('Presentación y entrega',
     'Si el empaque o el archivo digital llegó en las condiciones esperadas.',
     'PRODUCTO', 'ESTRELLAS', TRUE, 3),
    ('Aspectos destacados',
     'Lo que sobresalió o lo que no funcionó de la pieza.',
     'PRODUCTO', 'OPCIONES_MULTIPLES', FALSE, 4)
) AS v(nombre, descripcion, ambito, escala, obligatorio, orden)
WHERE NOT EXISTS (
    SELECT 1 FROM criterio_resenia c
    WHERE c.ambito = v.ambito AND lower(c.nombre) = lower(v.nombre)
);

INSERT INTO opcion_criterio (id_criterio, etiqueta, orden, polaridad)
SELECT c.id_criterio_resenia, v.etiqueta, v.orden, v.polaridad
FROM criterio_resenia c
JOIN (VALUES
    ('PRODUCTO', 'Aspectos destacados', 'Tal como se veía', 1, 'POSITIVA'),
    ('PRODUCTO', 'Aspectos destacados', 'Buen acabado', 2, 'POSITIVA'),
    ('PRODUCTO', 'Aspectos destacados', 'Empaque cuidado', 3, 'POSITIVA'),
    ('PRODUCTO', 'Aspectos destacados', 'Archivo de buena calidad', 4, 'POSITIVA'),
    ('PRODUCTO', 'Aspectos destacados', 'Superó lo esperado', 5, 'POSITIVA'),
    ('PRODUCTO', 'Aspectos destacados', 'No coincide con las fotos', 6, 'NEGATIVA'),
    ('PRODUCTO', 'Aspectos destacados', 'Acabado deficiente', 7, 'NEGATIVA'),
    ('PRODUCTO', 'Aspectos destacados', 'Llegó dañado', 8, 'NEGATIVA'),
    ('PRODUCTO', 'Aspectos destacados', 'Archivo de baja calidad', 9, 'NEGATIVA'),
    ('PRODUCTO', 'Aspectos destacados', 'Por debajo de lo esperado', 10, 'NEGATIVA')
) AS v(ambito, criterio, etiqueta, orden, polaridad)
    ON c.ambito = v.ambito AND c.nombre = v.criterio
WHERE NOT EXISTS (
    SELECT 1 FROM opcion_criterio o
    WHERE o.id_criterio = c.id_criterio_resenia
      AND lower(o.etiqueta) = lower(v.etiqueta)
);

-- ---------------------------------------------------------------------------
-- Notificación de reseña de producto
-- ---------------------------------------------------------------------------

INSERT INTO tipo_notificacion (codigo, nombre, frecuencia, estado)
SELECT 'RESENIA_PRODUCTO_RECIBIDA', 'Reseña de producto recibida', 'EVENTO', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_notificacion WHERE codigo = 'RESENIA_PRODUCTO_RECIBIDA'
);
