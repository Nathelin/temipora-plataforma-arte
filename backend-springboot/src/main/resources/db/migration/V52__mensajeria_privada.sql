-- UC-34 / UC-35: mensajería privada.
-- Una conversación reutiliza la misma fila para la solicitud, su aceptación y el hilo.

CREATE TABLE IF NOT EXISTS conversacion (
    id_conversacion BIGSERIAL PRIMARY KEY,
    id_iniciador BIGINT NOT NULL,
    id_destinatario BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    motivo_contacto VARCHAR(200) NOT NULL,
    fecha_solicitud TIMESTAMP NOT NULL DEFAULT now(),
    fecha_respuesta TIMESTAMP,
    fecha_ultimo_mensaje TIMESTAMP,
    fecha_actualizacion TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_conversacion_iniciador') THEN
        ALTER TABLE conversacion ADD CONSTRAINT fk_conversacion_iniciador
            FOREIGN KEY (id_iniciador) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_conversacion_destinatario') THEN
        ALTER TABLE conversacion ADD CONSTRAINT fk_conversacion_destinatario
            FOREIGN KEY (id_destinatario) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_conversacion_participantes_distintos') THEN
        ALTER TABLE conversacion ADD CONSTRAINT ck_conversacion_participantes_distintos
            CHECK (id_iniciador <> id_destinatario);
    END IF;
END $$;

-- Un solo hilo por par, sin importar quién inició.
CREATE UNIQUE INDEX IF NOT EXISTS uk_conversacion_par
    ON conversacion (least(id_iniciador, id_destinatario), greatest(id_iniciador, id_destinatario));

CREATE INDEX IF NOT EXISTS idx_conversacion_ultimo_mensaje
    ON conversacion (fecha_ultimo_mensaje DESC);

CREATE TABLE IF NOT EXISTS conversacion_participante (
    id_participante BIGSERIAL PRIMARY KEY,
    id_conversacion BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    ultimo_leido_en TIMESTAMP,
    archivada BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_conversacion_participante_conversacion') THEN
        ALTER TABLE conversacion_participante ADD CONSTRAINT fk_conversacion_participante_conversacion
            FOREIGN KEY (id_conversacion) REFERENCES conversacion (id_conversacion) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_conversacion_participante_usuario') THEN
        ALTER TABLE conversacion_participante ADD CONSTRAINT fk_conversacion_participante_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_conversacion_participante
    ON conversacion_participante (id_conversacion, id_usuario);
CREATE INDEX IF NOT EXISTS idx_conversacion_participante_usuario
    ON conversacion_participante (id_usuario, archivada);

CREATE TABLE IF NOT EXISTS mensaje (
    id_mensaje BIGSERIAL PRIMARY KEY,
    id_conversacion BIGINT NOT NULL,
    id_remitente BIGINT NOT NULL,
    contenido VARCHAR(4000) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ENVIADO',
    fecha_envio TIMESTAMP NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_mensaje_conversacion') THEN
        ALTER TABLE mensaje ADD CONSTRAINT fk_mensaje_conversacion
            FOREIGN KEY (id_conversacion) REFERENCES conversacion (id_conversacion) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_mensaje_remitente') THEN
        ALTER TABLE mensaje ADD CONSTRAINT fk_mensaje_remitente
            FOREIGN KEY (id_remitente) REFERENCES usuario (id_usuario);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_mensaje_conversacion
    ON mensaje (id_conversacion, id_mensaje DESC);
CREATE INDEX IF NOT EXISTS idx_mensaje_remitente_fecha
    ON mensaje (id_remitente, fecha_envio);

CREATE TABLE IF NOT EXISTS conversacion_accion (
    id_accion BIGSERIAL PRIMARY KEY,
    id_conversacion BIGINT NOT NULL,
    tipo_accion VARCHAR(20) NOT NULL,
    id_usuario BIGINT NOT NULL,
    motivo VARCHAR(200),
    fecha TIMESTAMP NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_conversacion_accion_conversacion') THEN
        ALTER TABLE conversacion_accion ADD CONSTRAINT fk_conversacion_accion_conversacion
            FOREIGN KEY (id_conversacion) REFERENCES conversacion (id_conversacion) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_conversacion_accion_usuario') THEN
        ALTER TABLE conversacion_accion ADD CONSTRAINT fk_conversacion_accion_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_conversacion_accion_conversacion
    ON conversacion_accion (id_conversacion, fecha);
CREATE INDEX IF NOT EXISTS idx_conversacion_accion_usuario
    ON conversacion_accion (id_usuario, tipo_accion, fecha);

CREATE TABLE IF NOT EXISTS configuracion_mensajeria (
    id_configuracion BIGINT PRIMARY KEY,
    longitud_maxima_mensaje INTEGER NOT NULL DEFAULT 2000,
    longitud_maxima_motivo INTEGER NOT NULL DEFAULT 200,
    horas_espera_reenvio INTEGER NOT NULL DEFAULT 168,
    max_solicitudes_pendientes INTEGER NOT NULL DEFAULT 5,
    max_solicitudes_por_dia INTEGER NOT NULL DEFAULT 10,
    max_mensajes_por_dia INTEGER NOT NULL DEFAULT 100,
    fecha_actualizacion TIMESTAMP
);

INSERT INTO configuracion_mensajeria (id_configuracion)
SELECT 1
WHERE NOT EXISTS (
    SELECT 1 FROM configuracion_mensajeria WHERE id_configuracion = 1
);

INSERT INTO tipo_notificacion (codigo, nombre, frecuencia, estado)
SELECT v.codigo, v.nombre, 'EVENTO', TRUE
FROM (VALUES
    ('SOLICITUD_MENSAJE_RECIBIDA', 'Solicitud de mensaje recibida'),
    ('SOLICITUD_MENSAJE_ACEPTADA', 'Solicitud de mensaje aceptada'),
    ('SOLICITUD_MENSAJE_RECHAZADA', 'Solicitud de mensaje rechazada'),
    ('SOLICITUD_MENSAJE_CANCELADA', 'Solicitud de mensaje cancelada'),
    ('MENSAJE_RECIBIDO', 'Mensaje privado recibido')
) AS v(codigo, nombre)
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_notificacion t WHERE t.codigo = v.codigo
);
