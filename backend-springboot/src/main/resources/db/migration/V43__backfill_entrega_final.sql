-- Backfill: los pedidos ya finalizados antes de la galería pública
-- necesitan que su entrega final vigente quede marcada como es_final.

WITH ultima_entrega AS (
    SELECT DISTINCT ON (p.id_pedido) e.id_entrega
    FROM pedido p
    JOIN hito_pedido h ON h.id_pedido = p.id_pedido
    JOIN entrega_hito e ON e.id_hito_pedido = h.id_hito_pedido
    WHERE p.estado = 'FINALIZADO'
      AND e.vigente = TRUE
      AND e.retirada = FALSE
      AND h.estado_entrega IN ('APROBADO', 'APROBADO_AUTOMATICAMENTE')
    ORDER BY p.id_pedido, h.orden DESC, e.numero DESC
)
UPDATE entrega_hito e
SET es_final = TRUE
FROM ultima_entrega u
WHERE e.id_entrega = u.id_entrega
  AND e.es_final = FALSE;
