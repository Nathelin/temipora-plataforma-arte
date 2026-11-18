-- Estados de la publicación de reventa: baja lógica del dueño y suspensión del admin.

ALTER TABLE publicacion_reventa
    DROP CONSTRAINT ck_publicacion_reventa_estado;

ALTER TABLE publicacion_reventa
    ADD CONSTRAINT ck_publicacion_reventa_estado
    CHECK (estado IN (
        'PUBLICADA',
        'RESERVADA',
        'VENDIDA',
        'CANCELADA',
        'ARCHIVADA',
        'SUSPENDIDA',
        'VENCIDA'
    ));
