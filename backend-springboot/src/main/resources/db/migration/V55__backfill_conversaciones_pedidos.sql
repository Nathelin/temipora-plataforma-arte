-- Backfill: todo par cliente/artista con un pedido debe tener un hilo abierto.
-- Reproduce lo que hace ConversacionService.asegurarPorSolicitud para la data
-- anterior a la apertura automática. No modifica mensajes existentes.

WITH pares AS (
    SELECT DISTINCT ON (
            LEAST(p.id_cliente, p.id_artista),
            GREATEST(p.id_cliente, p.id_artista))
        p.id_cliente,
        p.id_artista,
        p.id_pedido,
        p.id_servicio,
        p.fecha_creacion
    FROM pedido p
    WHERE p.id_cliente <> p.id_artista
    ORDER BY
        LEAST(p.id_cliente, p.id_artista),
        GREATEST(p.id_cliente, p.id_artista),
        p.id_pedido
)
INSERT INTO conversacion (
    id_iniciador,
    id_destinatario,
    estado,
    origen,
    motivo_contacto,
    fecha_solicitud,
    fecha_respuesta,
    fecha_actualizacion)
SELECT
    pares.id_cliente,
    pares.id_artista,
    'ACEPTADA',
    'SOLICITUD_SERVICIO',
    left(
        'Pedido #' || pares.id_pedido || ' · ' || COALESCE(s.nombre, 'Servicio'),
        200),
    COALESCE(pares.fecha_creacion, now()),
    COALESCE(pares.fecha_creacion, now()),
    now()
FROM pares
LEFT JOIN servicio s ON s.id_servicio = pares.id_servicio
WHERE NOT EXISTS (
    SELECT 1
    FROM conversacion c
    WHERE LEAST(c.id_iniciador, c.id_destinatario)
              = LEAST(pares.id_cliente, pares.id_artista)
      AND GREATEST(c.id_iniciador, c.id_destinatario)
              = GREATEST(pares.id_cliente, pares.id_artista)
);

UPDATE conversacion c
SET estado = 'ACEPTADA',
    origen = 'SOLICITUD_SERVICIO',
    fecha_respuesta = now(),
    fecha_actualizacion = now()
WHERE c.estado <> 'ACEPTADA'
  AND EXISTS (
      SELECT 1
      FROM pedido p
      WHERE LEAST(p.id_cliente, p.id_artista)
                = LEAST(c.id_iniciador, c.id_destinatario)
        AND GREATEST(p.id_cliente, p.id_artista)
                = GREATEST(c.id_iniciador, c.id_destinatario)
  );

INSERT INTO conversacion_participante (id_conversacion, id_usuario, archivada)
SELECT c.id_conversacion, participantes.id_usuario, FALSE
FROM conversacion c
CROSS JOIN LATERAL (
    VALUES (c.id_iniciador), (c.id_destinatario)
) AS participantes(id_usuario)
WHERE c.origen = 'SOLICITUD_SERVICIO'
  AND NOT EXISTS (
      SELECT 1
      FROM conversacion_participante p
      WHERE p.id_conversacion = c.id_conversacion
        AND p.id_usuario = participantes.id_usuario
  );

UPDATE conversacion_participante p
SET archivada = FALSE
FROM conversacion c
WHERE p.id_conversacion = c.id_conversacion
  AND c.origen = 'SOLICITUD_SERVICIO'
  AND c.estado = 'ACEPTADA'
  AND p.archivada = TRUE
  AND EXISTS (
      SELECT 1
      FROM pedido pe
      WHERE LEAST(pe.id_cliente, pe.id_artista)
                = LEAST(c.id_iniciador, c.id_destinatario)
        AND GREATEST(pe.id_cliente, pe.id_artista)
                = GREATEST(c.id_iniciador, c.id_destinatario)
  );
