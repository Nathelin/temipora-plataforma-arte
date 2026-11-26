-- Historial unificado del comprador: pedidos de servicio y compras de producto.

CREATE INDEX IF NOT EXISTS idx_pedido_cliente_fecha
    ON pedido (id_cliente, fecha_creacion DESC);

CREATE INDEX IF NOT EXISTS idx_compra_cliente_fecha
    ON compra (id_cliente, fecha_creacion DESC);

CREATE INDEX IF NOT EXISTS idx_compra_grupo_compra
    ON compra_grupo (id_compra);

CREATE INDEX IF NOT EXISTS idx_compra_item_grupo
    ON compra_item (id_compra_grupo);

CREATE OR REPLACE VIEW v_historial_cliente AS
SELECT
    p.id_cliente,
    'PEDIDO'::text AS tipo,
    p.id_pedido AS id_referencia,
    p.fecha_creacion AS fecha,
    COALESCE(s.nombre, 'Servicio')::text AS titulo,
    COALESCE(a.nombre_usuario, '')::text AS contraparte,
    COALESCE(pr.precio_final_cliente_cobro, 0) AS monto,
    COALESCE(mc.codigo, 'ARS')::text AS moneda,
    p.estado::text AS estado,
    CASE p.estado
        WHEN 'FINALIZADO' THEN 'FINALIZADO'
        WHEN 'EN_CURSO' THEN 'EN_CURSO'
        WHEN 'PENDIENTE_PAGO' THEN 'PENDIENTE'
        ELSE 'CANCELADO'
    END::text AS estado_grupo,
    COALESCE(pr.descuento_porcentaje, 0) AS descuento_porcentaje,
    COALESCE(pr.descuento_monto_cobro, 0) AS descuento_monto,
    1::bigint AS cantidad_items,
    1::bigint AS cantidad_grupos
FROM pedido p
JOIN servicio s ON s.id_servicio = p.id_servicio
JOIN usuario a ON a.id_usuario = p.id_artista
LEFT JOIN presupuesto pr ON pr.id_presupuesto = p.id_presupuesto
LEFT JOIN moneda mc ON mc.id_moneda = pr.id_moneda_cobro

UNION ALL

SELECT
    c.id_cliente,
    'COMPRA'::text AS tipo,
    c.id_compra AS id_referencia,
    c.fecha_creacion AS fecha,
    COALESCE((
        SELECT ci.titulo_producto
        FROM compra_item ci
        JOIN compra_grupo cg ON cg.id_compra_grupo = ci.id_compra_grupo
        WHERE cg.id_compra = c.id_compra
        ORDER BY ci.id_compra_item
        LIMIT 1
    ), 'Compra de productos')::text AS titulo,
    COALESCE((
        SELECT u.nombre_usuario
        FROM compra_grupo cg
        JOIN usuario u ON u.id_usuario = cg.id_artista
        WHERE cg.id_compra = c.id_compra
        ORDER BY cg.id_compra_grupo
        LIMIT 1
    ), '')::text AS contraparte,
    COALESCE(c.total, 0) AS monto,
    COALESCE(m.codigo, 'ARS')::text AS moneda,
    c.estado::text AS estado,
    CASE c.estado
        WHEN 'PAGA' THEN 'FINALIZADO'
        WHEN 'PENDIENTE_PAGO' THEN 'PENDIENTE'
        WHEN 'PARCIALMENTE_PAGA' THEN 'EN_CURSO'
        ELSE 'CANCELADO'
    END::text AS estado_grupo,
    0 AS descuento_porcentaje,
    0 AS descuento_monto,
    COALESCE((
        SELECT SUM(ci.cantidad)
        FROM compra_item ci
        JOIN compra_grupo cg ON cg.id_compra_grupo = ci.id_compra_grupo
        WHERE cg.id_compra = c.id_compra
    ), 0)::bigint AS cantidad_items,
    COALESCE((
        SELECT COUNT(*)
        FROM compra_grupo cg
        WHERE cg.id_compra = c.id_compra
    ), 0)::bigint AS cantidad_grupos
FROM compra c
JOIN moneda m ON m.id_moneda = c.id_moneda;
