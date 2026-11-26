-- Historial de promociones: momentos en que la oferta se acepta y se usa.

ALTER TABLE oferta_reactivacion
    ADD COLUMN IF NOT EXISTS fecha_reserva TIMESTAMP;

ALTER TABLE oferta_reactivacion
    ADD COLUMN IF NOT EXISTS fecha_utilizada TIMESTAMP;

UPDATE oferta_reactivacion o
   SET fecha_reserva = s.fecha_creacion
  FROM solicitud s
 WHERE s.id_solicitud = o.id_solicitud
   AND o.fecha_reserva IS NULL;

UPDATE oferta_reactivacion o
   SET fecha_utilizada = p.fecha_emision
  FROM presupuesto p
 WHERE p.id_oferta_reactivacion = o.id_oferta
   AND o.estado = 'UTILIZADA'
   AND o.fecha_utilizada IS NULL;

CREATE INDEX IF NOT EXISTS idx_campana_reactivacion_cierre
    ON campana_reactivacion (estado, fecha_vencimiento);

CREATE INDEX IF NOT EXISTS idx_oferta_reactivacion_estado_fecha
    ON oferta_reactivacion (fecha_creacion, estado);

CREATE INDEX IF NOT EXISTS idx_presupuesto_oferta_reactivacion
    ON presupuesto (id_oferta_reactivacion);

CREATE INDEX IF NOT EXISTS idx_pedido_fecha_creacion
    ON pedido (fecha_creacion);
