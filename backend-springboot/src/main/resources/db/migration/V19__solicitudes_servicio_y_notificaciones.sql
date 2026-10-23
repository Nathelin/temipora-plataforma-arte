-- UC-21: alta de solicitud de servicio, lista de espera y notificaciones.

ALTER TABLE servicio ADD COLUMN IF NOT EXISTS permite_lista_espera BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS grupo_solicitud (
    id_grupo_solicitud BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL,
    id_servicio BIGINT NOT NULL,
    id_artista BIGINT NOT NULL,
    id_terminos BIGINT,
    terminos_version INTEGER NOT NULL,
    terminos_aceptados_en TIMESTAMP NOT NULL,
    terminos_snapshot TEXT NOT NULL,
    prioridad_definida_por_cliente BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_grupo_solicitud_cliente') THEN
        ALTER TABLE grupo_solicitud ADD CONSTRAINT fk_grupo_solicitud_cliente
            FOREIGN KEY (id_cliente) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_grupo_solicitud_servicio') THEN
        ALTER TABLE grupo_solicitud ADD CONSTRAINT fk_grupo_solicitud_servicio
            FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_grupo_solicitud_artista') THEN
        ALTER TABLE grupo_solicitud ADD CONSTRAINT fk_grupo_solicitud_artista
            FOREIGN KEY (id_artista) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_grupo_solicitud_terminos') THEN
        ALTER TABLE grupo_solicitud ADD CONSTRAINT fk_grupo_solicitud_terminos
            FOREIGN KEY (id_terminos) REFERENCES terminos (id_terminos) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_grupo_solicitud_cliente
    ON grupo_solicitud (id_cliente, id_servicio);

CREATE TABLE IF NOT EXISTS solicitud (
    id_solicitud BIGSERIAL PRIMARY KEY,
    id_grupo_solicitud BIGINT NOT NULL,
    id_servicio BIGINT NOT NULL,
    id_cliente BIGINT NOT NULL,
    id_artista BIGINT NOT NULL,
    id_formulario BIGINT NOT NULL,
    estado VARCHAR(30) NOT NULL,
    cantidad_dibujos INTEGER NOT NULL,
    precio_base NUMERIC(12,2),
    id_moneda BIGINT,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_solicitud_grupo') THEN
        ALTER TABLE solicitud ADD CONSTRAINT fk_solicitud_grupo
            FOREIGN KEY (id_grupo_solicitud) REFERENCES grupo_solicitud (id_grupo_solicitud) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_solicitud_servicio') THEN
        ALTER TABLE solicitud ADD CONSTRAINT fk_solicitud_servicio
            FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_solicitud_cliente') THEN
        ALTER TABLE solicitud ADD CONSTRAINT fk_solicitud_cliente
            FOREIGN KEY (id_cliente) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_solicitud_artista') THEN
        ALTER TABLE solicitud ADD CONSTRAINT fk_solicitud_artista
            FOREIGN KEY (id_artista) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_solicitud_formulario') THEN
        ALTER TABLE solicitud ADD CONSTRAINT fk_solicitud_formulario
            FOREIGN KEY (id_formulario) REFERENCES formulario (id_formulario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_solicitud_moneda') THEN
        ALTER TABLE solicitud ADD CONSTRAINT fk_solicitud_moneda
            FOREIGN KEY (id_moneda) REFERENCES moneda (id_moneda);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_solicitud_cantidad_dibujos') THEN
        ALTER TABLE solicitud ADD CONSTRAINT ck_solicitud_cantidad_dibujos
            CHECK (cantidad_dibujos > 0);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_solicitud_grupo ON solicitud (id_grupo_solicitud);
CREATE INDEX IF NOT EXISTS idx_solicitud_servicio_estado ON solicitud (id_servicio, estado);
CREATE INDEX IF NOT EXISTS idx_solicitud_cliente_servicio ON solicitud (id_cliente, id_servicio);

CREATE UNIQUE INDEX IF NOT EXISTS uk_solicitud_activa
    ON solicitud (id_servicio, id_cliente)
    WHERE estado IN ('PENDIENTE', 'PRESUPUESTADA', 'ACEPTADA');

CREATE UNIQUE INDEX IF NOT EXISTS uk_solicitud_en_espera
    ON solicitud (id_servicio, id_cliente)
    WHERE estado = 'EN_LISTA_ESPERA';

CREATE TABLE IF NOT EXISTS item_solicitud (
    id_item_solicitud BIGSERIAL PRIMARY KEY,
    id_solicitud BIGINT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0,
    orden_prioridad INTEGER,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE'
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_item_solicitud_solicitud') THEN
        ALTER TABLE item_solicitud ADD CONSTRAINT fk_item_solicitud_solicitud
            FOREIGN KEY (id_solicitud) REFERENCES solicitud (id_solicitud) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_item_solicitud_solicitud ON item_solicitud (id_solicitud);

CREATE TABLE IF NOT EXISTS respuesta (
    id_respuesta BIGSERIAL PRIMARY KEY,
    id_item_solicitud BIGINT NOT NULL,
    id_pregunta BIGINT,
    id_tipo_campo BIGINT NOT NULL,
    enunciado VARCHAR(250) NOT NULL,
    respuesta TEXT,
    orden INTEGER NOT NULL DEFAULT 0,
    suborden INTEGER NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_respuesta_item') THEN
        ALTER TABLE respuesta ADD CONSTRAINT fk_respuesta_item
            FOREIGN KEY (id_item_solicitud) REFERENCES item_solicitud (id_item_solicitud) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_respuesta_pregunta') THEN
        ALTER TABLE respuesta ADD CONSTRAINT fk_respuesta_pregunta
            FOREIGN KEY (id_pregunta) REFERENCES pregunta (id_pregunta) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_respuesta_tipo_campo') THEN
        ALTER TABLE respuesta ADD CONSTRAINT fk_respuesta_tipo_campo
            FOREIGN KEY (id_tipo_campo) REFERENCES tipo_campo (id_tipo_campo);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_respuesta_item ON respuesta (id_item_solicitud, orden, suborden);

CREATE TABLE IF NOT EXISTS lista_espera (
    id_lista_espera BIGSERIAL PRIMARY KEY,
    id_servicio BIGINT NOT NULL,
    id_solicitud BIGINT NOT NULL,
    id_cliente BIGINT NOT NULL,
    cantidad_dibujos INTEGER NOT NULL,
    posicion INTEGER NOT NULL,
    fecha_ingreso TIMESTAMP NOT NULL DEFAULT now(),
    estado VARCHAR(20) NOT NULL DEFAULT 'ESPERANDO',
    fecha_actualizacion TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_lista_espera_servicio') THEN
        ALTER TABLE lista_espera ADD CONSTRAINT fk_lista_espera_servicio
            FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_lista_espera_solicitud') THEN
        ALTER TABLE lista_espera ADD CONSTRAINT fk_lista_espera_solicitud
            FOREIGN KEY (id_solicitud) REFERENCES solicitud (id_solicitud) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_lista_espera_cliente') THEN
        ALTER TABLE lista_espera ADD CONSTRAINT fk_lista_espera_cliente
            FOREIGN KEY (id_cliente) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_lista_espera_cantidad') THEN
        ALTER TABLE lista_espera ADD CONSTRAINT ck_lista_espera_cantidad
            CHECK (cantidad_dibujos > 0);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_lista_espera_solicitud
    ON lista_espera (id_solicitud);
CREATE INDEX IF NOT EXISTS idx_lista_espera_servicio_estado
    ON lista_espera (id_servicio, estado, posicion);

CREATE TABLE IF NOT EXISTS tipo_notificacion (
    id_tipo_notificacion BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    frecuencia VARCHAR(20) NOT NULL DEFAULT 'EVENTO',
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_inicio TIMESTAMP,
    fecha_vencimiento TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tipo_notificacion_codigo
    ON tipo_notificacion (codigo);

INSERT INTO tipo_notificacion (codigo, nombre, frecuencia, estado)
SELECT v.codigo, v.nombre, 'EVENTO', TRUE
FROM (VALUES
    ('SOLICITUD_RECIBIDA', 'Solicitud de servicio recibida'),
    ('SOLICITUD_EN_LISTA_ESPERA', 'Solicitud agregada a lista de espera')
) AS v(codigo, nombre)
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_notificacion t WHERE t.codigo = v.codigo
);

CREATE TABLE IF NOT EXISTS notificacion (
    id_notificacion BIGSERIAL PRIMARY KEY,
    id_tipo_notificacion BIGINT NOT NULL,
    id_administrador BIGINT,
    es_sistema BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(250),
    id_referencia BIGINT,
    tipo_referencia VARCHAR(30),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_notificacion_tipo') THEN
        ALTER TABLE notificacion ADD CONSTRAINT fk_notificacion_tipo
            FOREIGN KEY (id_tipo_notificacion) REFERENCES tipo_notificacion (id_tipo_notificacion);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_notificacion_administrador') THEN
        ALTER TABLE notificacion ADD CONSTRAINT fk_notificacion_administrador
            FOREIGN KEY (id_administrador) REFERENCES usuario (id_usuario);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_notificacion_referencia
    ON notificacion (tipo_referencia, id_referencia);

CREATE TABLE IF NOT EXISTS notificacion_usuario (
    id_notificacion_usuario BIGSERIAL PRIMARY KEY,
    id_notificacion BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_lectura TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_notificacion_usuario_notificacion') THEN
        ALTER TABLE notificacion_usuario ADD CONSTRAINT fk_notificacion_usuario_notificacion
            FOREIGN KEY (id_notificacion) REFERENCES notificacion (id_notificacion) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_notificacion_usuario_usuario') THEN
        ALTER TABLE notificacion_usuario ADD CONSTRAINT fk_notificacion_usuario_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE CASCADE;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_notificacion_usuario
    ON notificacion_usuario (id_notificacion, id_usuario);
CREATE INDEX IF NOT EXISTS idx_notificacion_usuario_usuario
    ON notificacion_usuario (id_usuario, leida);

CREATE TABLE IF NOT EXISTS notificacion_rol (
    id_notificacion_rol BIGSERIAL PRIMARY KEY,
    id_notificacion BIGINT NOT NULL,
    id_rol BIGINT NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_notificacion_rol_notificacion') THEN
        ALTER TABLE notificacion_rol ADD CONSTRAINT fk_notificacion_rol_notificacion
            FOREIGN KEY (id_notificacion) REFERENCES notificacion (id_notificacion) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_notificacion_rol_rol') THEN
        ALTER TABLE notificacion_rol ADD CONSTRAINT fk_notificacion_rol_rol
            FOREIGN KEY (id_rol) REFERENCES rol (id_rol);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_notificacion_rol
    ON notificacion_rol (id_notificacion, id_rol);
