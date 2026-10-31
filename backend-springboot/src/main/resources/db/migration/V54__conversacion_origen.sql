-- El hilo entre dos usuarios puede abrirse automáticamente cuando existe una
-- solicitud de servicio, sin pasar por una solicitud de mensaje.

ALTER TABLE conversacion
    ADD COLUMN IF NOT EXISTS origen VARCHAR(30) NOT NULL DEFAULT 'SOLICITUD_MENSAJE';
