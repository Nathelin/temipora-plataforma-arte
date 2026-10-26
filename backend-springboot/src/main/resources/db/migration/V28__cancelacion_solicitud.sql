-- UC-22: cancelación de solicitudes de servicio por parte del cliente.
-- Se agrega el origen CLIENTE a las acciones y el tipo de notificación para el artista.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_accion_solicitud_origen') THEN
        ALTER TABLE accion_solicitud DROP CONSTRAINT ck_accion_solicitud_origen;
    END IF;
    ALTER TABLE accion_solicitud ADD CONSTRAINT ck_accion_solicitud_origen
        CHECK (origen IN ('ARTISTA', 'BAJA_SERVICIO', 'CLIENTE'));
END $$;

INSERT INTO tipo_notificacion (codigo, nombre, frecuencia, estado)
SELECT 'SOLICITUD_CANCELADA', 'Solicitud de servicio cancelada', 'EVENTO', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_notificacion WHERE codigo = 'SOLICITUD_CANCELADA'
);
