-- Categoría de cada tipo de notificación, para filtrar la bandeja.

ALTER TABLE tipo_notificacion
    ADD COLUMN IF NOT EXISTS categoria VARCHAR(30);

UPDATE tipo_notificacion SET categoria = 'SOLICITUDES'
WHERE categoria IS NULL AND codigo IN (
    'SOLICITUD_RECIBIDA',
    'SOLICITUD_EN_LISTA_ESPERA',
    'PRESUPUESTO_RECIBIDO',
    'PRESUPUESTO_VENCIDO',
    'SOLICITUD_RECHAZADA',
    'SOLICITUD_CANCELADA'
);

UPDATE tipo_notificacion SET categoria = 'PEDIDOS'
WHERE categoria IS NULL AND codigo IN (
    'CUOTA_PENDIENTE',
    'CUOTA_VENCIDA',
    'PAGO_RECIBIDO',
    'PAGO_RECHAZADO',
    'HITO_ENTREGADO',
    'HITO_APROBADO',
    'HITO_APROBADO_AUTOMATICAMENTE',
    'HITO_CON_REVISION',
    'ENTREGA_LISTA',
    'PEDIDO_FINALIZADO',
    'PEDIDO_CANCELADO',
    'PEDIDO_IMPAGO',
    'PEDIDO_PUBLICACION_AUTORIZADA',
    'PEDIDO_PUBLICACION_RETIRADA',
    'AVANCE_PEDIDO'
);

UPDATE tipo_notificacion SET categoria = 'MENSAJES'
WHERE categoria IS NULL AND codigo IN (
    'SOLICITUD_MENSAJE_RECIBIDA',
    'SOLICITUD_MENSAJE_ACEPTADA',
    'SOLICITUD_MENSAJE_RECHAZADA',
    'SOLICITUD_MENSAJE_CANCELADA',
    'SOLICITUD_MENSAJE_VENCIDA',
    'MENSAJE_RECIBIDO'
);

UPDATE tipo_notificacion SET categoria = 'RESENIAS'
WHERE categoria IS NULL AND codigo IN (
    'RESENIA_RECIBIDA',
    'RESENIA_REPORTADA',
    'RESENIA_OCULTA',
    'RESENIA_RESTAURADA'
);

UPDATE tipo_notificacion SET categoria = 'PROMOCIONES'
WHERE categoria IS NULL AND codigo IN (
    'OFERTA_REACTIVACION'
);

CREATE INDEX IF NOT EXISTS idx_tipo_notificacion_categoria
    ON tipo_notificacion (categoria);

CREATE INDEX IF NOT EXISTS idx_notificacion_fecha
    ON notificacion (fecha_creacion DESC);
