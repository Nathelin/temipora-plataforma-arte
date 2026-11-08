-- Fase 4.0 — Saneamiento de datos de prueba.
-- Ejecutar una sola vez sobre la base actual, con backup previo.
-- NO es una migración Flyway: es remediación de datos específica del entorno.

BEGIN;

-- 1) Hito 23 / pedido 12.
-- El pedido vale 10000 y sus hitos deben sumar 10000.
-- El valor correcto del hito es 3000 (3000 + 3500 + 3500 = 10000).
-- Los pagos 22/23/24 eran de prueba.
UPDATE hito_pedido
   SET id_pago_aprobado = NULL
 WHERE id_hito_pedido = 23;

DELETE FROM pago
 WHERE id_hito_pedido = 23;

UPDATE hito_pedido
   SET monto = 3000.00,
       monto_cobro = 3000.00,
       comision_monto = 60.00,
       comision_monto_cobro = 60.00,
       monto_neto = 2940.00,
       monto_neto_cobro = 2940.00,
       estado_pago = 'PENDIENTE',
       fecha_pago = NULL
 WHERE id_hito_pedido = 23;

UPDATE pedido
   SET estado = 'PENDIENTE_PAGO',
       fecha_inicio = NULL,
       limite_entrega = NULL
 WHERE id_pedido = 12;

-- 2) Pedido 14: backfill de TOS de plataforma con la aceptación 10 (versión 4).
UPDATE pedido p
   SET terminos_plataforma_version = 4,
       terminos_plataforma_aceptados_en = a.fecha_aceptacion,
       terminos_plataforma_snapshot = t.contenido
  FROM aceptacion_terminos_plataforma a
  JOIN terminos_plataforma t ON t.id_terminos_plataforma = a.id_terminos_plataforma
 WHERE a.id_usuario = p.id_cliente
   AND a.version = 4
   AND p.id_pedido = 14;

COMMIT;

-- ---------------------------------------------------------------------------
-- Verificación
-- ---------------------------------------------------------------------------
SELECT id_hito_pedido, id_pedido, monto, comision_monto, monto_neto, estado_pago
  FROM hito_pedido
 WHERE id_hito_pedido = 23;

SELECT id_pedido, estado, fecha_inicio, limite_entrega
  FROM pedido
 WHERE id_pedido = 12;

SELECT sum(monto) AS suma_hitos_pedido_12
  FROM hito_pedido
 WHERE id_pedido = 12;

SELECT count(*) AS pagos_hito_23
  FROM pago
 WHERE id_hito_pedido = 23;

SELECT id_pedido, terminos_plataforma_version, terminos_plataforma_aceptados_en,
       (terminos_plataforma_snapshot IS NOT NULL) AS tiene_snapshot
  FROM pedido
 WHERE id_pedido = 14;

SELECT 'pedido_vs_presupuesto' AS chequeo, count(*) AS diferencias
  FROM pedido p
  JOIN presupuesto pr ON pr.id_presupuesto = p.id_presupuesto
 WHERE p.precio_final <> pr.precio_final
    OR p.precio_final_cobro <> pr.precio_final_cobro
    OR p.comision_porcentaje <> pr.comision_porcentaje
    OR p.cotizacion_cobro <> pr.cotizacion_cobro;

SELECT 'hito_vs_presupuesto' AS chequeo, count(*) AS diferencias
  FROM hito_pedido hp
  JOIN hito_presupuesto hpr ON hpr.id_hito_presupuesto = hp.id_hito_presupuesto
 WHERE hp.monto <> hpr.monto
    OR hp.monto_cobro <> hpr.monto_cobro
    OR hp.comision_monto <> hpr.comision_monto
    OR hp.comision_monto_cobro <> hpr.comision_monto_cobro
    OR hp.monto_neto <> hpr.monto_neto
    OR hp.monto_neto_cobro <> hpr.monto_neto_cobro;

SELECT 'pedidos_sin_terminos_artista' AS chequeo, count(*) AS cantidad
  FROM pedido p
  JOIN solicitud s ON s.id_solicitud = p.id_solicitud
 WHERE s.id_terminos IS NULL;
