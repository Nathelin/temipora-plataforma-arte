-- Vencimiento automático de solicitudes de mensaje pendientes.

ALTER TABLE configuracion_mensajeria
    ADD COLUMN IF NOT EXISTS dias_vencimiento_solicitud INTEGER NOT NULL DEFAULT 30;

INSERT INTO tipo_notificacion (codigo, nombre, frecuencia, estado)
SELECT 'SOLICITUD_MENSAJE_VENCIDA', 'Solicitud de mensaje vencida', 'EVENTO', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_notificacion t WHERE t.codigo = 'SOLICITUD_MENSAJE_VENCIDA'
);
