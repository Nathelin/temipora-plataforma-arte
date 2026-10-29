-- Módulo de reseñas (fase 1: servicio y cliente, de uso interno).
--
-- La entidad Resenia original era un stub sin repositorio, servicio ni endpoint,
-- por lo que su tabla nunca tuvo datos. Hibernate la creó con "estado BOOLEAN",
-- incompatible con el enum nuevo; se elimina solo en ese caso para no tocar una
-- tabla ya migrada.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'resenia'
          AND column_name = 'estado'
          AND data_type = 'boolean'
    ) THEN
        DROP TABLE resenia;
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- Catálogo de criterios de evaluación
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS criterio_resenia (
    id_criterio_resenia BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(250) NOT NULL,
    observaciones VARCHAR(500),
    ambito VARCHAR(20) NOT NULL,
    escala VARCHAR(20) NOT NULL,
    obligatorio BOOLEAN NOT NULL DEFAULT FALSE,
    orden INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_criterio_resenia_ambito
        CHECK (ambito IN ('SERVICIO', 'CLIENTE', 'PRODUCTO')),
    CONSTRAINT ck_criterio_resenia_escala
        CHECK (escala IN ('ESTRELLAS', 'OPCIONES_MULTIPLES'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_criterio_resenia_ambito_nombre
    ON criterio_resenia (ambito, lower(nombre));

CREATE INDEX IF NOT EXISTS idx_criterio_resenia_ambito_activo
    ON criterio_resenia (ambito, activo, orden);

CREATE TABLE IF NOT EXISTS opcion_criterio (
    id_opcion_criterio BIGSERIAL PRIMARY KEY,
    id_criterio BIGINT NOT NULL,
    etiqueta VARCHAR(50) NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_opcion_criterio_criterio'
    ) THEN
        ALTER TABLE opcion_criterio
            ADD CONSTRAINT fk_opcion_criterio_criterio
            FOREIGN KEY (id_criterio)
            REFERENCES criterio_resenia (id_criterio_resenia)
            ON DELETE CASCADE;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_opcion_criterio_criterio_etiqueta
    ON opcion_criterio (id_criterio, lower(etiqueta));

CREATE INDEX IF NOT EXISTS idx_opcion_criterio_criterio
    ON opcion_criterio (id_criterio, activo, orden);

-- ---------------------------------------------------------------------------
-- Reseñas versionadas
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS resenia (
    id_resenia BIGSERIAL PRIMARY KEY,
    id_pedido BIGINT NOT NULL,
    id_autor BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PUBLICADA',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP,
    eliminada_en TIMESTAMP,
    motivo_baja VARCHAR(500),
    CONSTRAINT ck_resenia_estado
        CHECK (estado IN ('PUBLICADA', 'ELIMINADA'))
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_resenia_pedido'
    ) THEN
        ALTER TABLE resenia
            ADD CONSTRAINT fk_resenia_pedido
            FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_resenia_autor'
    ) THEN
        ALTER TABLE resenia
            ADD CONSTRAINT fk_resenia_autor
            FOREIGN KEY (id_autor) REFERENCES usuario (id_usuario);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_resenia_pedido_autor
    ON resenia (id_pedido, id_autor);

CREATE INDEX IF NOT EXISTS idx_resenia_pedido ON resenia (id_pedido);
CREATE INDEX IF NOT EXISTS idx_resenia_autor ON resenia (id_autor);

CREATE TABLE IF NOT EXISTS resenia_version (
    id_version BIGSERIAL PRIMARY KEY,
    id_resenia BIGINT NOT NULL,
    numero INTEGER NOT NULL,
    titulo VARCHAR(100) NOT NULL,
    descripcion VARCHAR(1000) NOT NULL,
    calificacion_general INTEGER NOT NULL,
    fecha_version TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT ck_resenia_version_calificacion
        CHECK (calificacion_general BETWEEN 1 AND 5)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_resenia_version_resenia'
    ) THEN
        ALTER TABLE resenia_version
            ADD CONSTRAINT fk_resenia_version_resenia
            FOREIGN KEY (id_resenia) REFERENCES resenia (id_resenia)
            ON DELETE CASCADE;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_resenia_version_numero
    ON resenia_version (id_resenia, numero);

CREATE TABLE IF NOT EXISTS resenia_valor (
    id_valor BIGSERIAL PRIMARY KEY,
    id_version BIGINT NOT NULL,
    id_criterio BIGINT NOT NULL,
    valor INTEGER NOT NULL,
    CONSTRAINT ck_resenia_valor_rango
        CHECK (valor BETWEEN 1 AND 5)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_resenia_valor_version'
    ) THEN
        ALTER TABLE resenia_valor
            ADD CONSTRAINT fk_resenia_valor_version
            FOREIGN KEY (id_version) REFERENCES resenia_version (id_version)
            ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_resenia_valor_criterio'
    ) THEN
        ALTER TABLE resenia_valor
            ADD CONSTRAINT fk_resenia_valor_criterio
            FOREIGN KEY (id_criterio) REFERENCES criterio_resenia (id_criterio_resenia);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_resenia_valor_version_criterio
    ON resenia_valor (id_version, id_criterio);

CREATE INDEX IF NOT EXISTS idx_resenia_valor_version
    ON resenia_valor (id_version);

CREATE TABLE IF NOT EXISTS resenia_opcion (
    id_opcion_resenia BIGSERIAL PRIMARY KEY,
    id_version BIGINT NOT NULL,
    id_opcion BIGINT NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_resenia_opcion_version'
    ) THEN
        ALTER TABLE resenia_opcion
            ADD CONSTRAINT fk_resenia_opcion_version
            FOREIGN KEY (id_version) REFERENCES resenia_version (id_version)
            ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_resenia_opcion_opcion'
    ) THEN
        ALTER TABLE resenia_opcion
            ADD CONSTRAINT fk_resenia_opcion_opcion
            FOREIGN KEY (id_opcion) REFERENCES opcion_criterio (id_opcion_criterio);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_resenia_opcion_version_opcion
    ON resenia_opcion (id_version, id_opcion);

CREATE INDEX IF NOT EXISTS idx_resenia_opcion_version
    ON resenia_opcion (id_version);

-- ---------------------------------------------------------------------------
-- Configuración (fila única)
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS configuracion_resenia (
    id_configuracion BIGINT PRIMARY KEY,
    dias_ventana_creacion INTEGER NOT NULL DEFAULT 30,
    dias_edicion INTEGER NOT NULL DEFAULT 3,
    minimo_resenias_etiqueta INTEGER NOT NULL DEFAULT 3,
    umbral_excelente NUMERIC(3,2) NOT NULL DEFAULT 4.50,
    umbral_buena NUMERIC(3,2) NOT NULL DEFAULT 3.50,
    fecha_actualizacion TIMESTAMP
);

INSERT INTO configuracion_resenia (
    id_configuracion, dias_ventana_creacion, dias_edicion,
    minimo_resenias_etiqueta, umbral_excelente, umbral_buena
)
SELECT 1, 30, 3, 3, 4.50, 3.50
WHERE NOT EXISTS (
    SELECT 1 FROM configuracion_resenia WHERE id_configuracion = 1
);

ALTER TABLE configuracion_resenia
    DROP CONSTRAINT IF EXISTS ck_configuracion_resenia_rangos;
ALTER TABLE configuracion_resenia
    ADD CONSTRAINT ck_configuracion_resenia_rangos
    CHECK (
        dias_ventana_creacion BETWEEN 0 AND 365
        AND dias_edicion BETWEEN 1 AND 30
        AND minimo_resenias_etiqueta BETWEEN 1 AND 50
        AND umbral_excelente BETWEEN 1.00 AND 5.00
        AND umbral_buena BETWEEN 1.00 AND 5.00
        AND umbral_excelente > umbral_buena
    );

-- ---------------------------------------------------------------------------
-- Criterios por defecto
-- ---------------------------------------------------------------------------

INSERT INTO criterio_resenia (
    nombre, descripcion, ambito, escala, obligatorio, orden, activo
)
SELECT v.nombre, v.descripcion, v.ambito, v.escala, v.obligatorio, v.orden, TRUE
FROM (VALUES
    ('Calidad de lo recibido',
     'Qué tan bien estuvo lo entregado respecto de lo acordado.',
     'SERVICIO', 'ESTRELLAS', TRUE, 1),
    ('Comunicación',
     'Claridad y predisposición del artista durante el trabajo.',
     'SERVICIO', 'ESTRELLAS', TRUE, 2),
    ('Cumplimiento de plazos',
     'Si las entregas llegaron en los tiempos acordados.',
     'SERVICIO', 'ESTRELLAS', TRUE, 3),
    ('Relación calidad-precio',
     'Si lo recibido justificó lo que pagaste.',
     'SERVICIO', 'ESTRELLAS', FALSE, 4),
    ('Aspectos destacados',
     'Cosas que sobresalieron en el trabajo.',
     'SERVICIO', 'OPCIONES_MULTIPLES', FALSE, 5),
    ('Claridad del pedido',
     'Si el pedido y las indicaciones fueron claros y completos.',
     'CLIENTE', 'ESTRELLAS', TRUE, 1),
    ('Comunicación',
     'Predisposición y claridad del cliente durante el trabajo.',
     'CLIENTE', 'ESTRELLAS', TRUE, 2),
    ('Cumplimiento de plazos de pago',
     'Si los pagos se hicieron en los tiempos acordados.',
     'CLIENTE', 'ESTRELLAS', FALSE, 3),
    ('Aspectos destacados',
     'Actitudes que sobresalieron durante el trabajo.',
     'CLIENTE', 'OPCIONES_MULTIPLES', FALSE, 4)
) AS v(nombre, descripcion, ambito, escala, obligatorio, orden)
WHERE NOT EXISTS (
    SELECT 1 FROM criterio_resenia c
    WHERE c.ambito = v.ambito AND lower(c.nombre) = lower(v.nombre)
);

INSERT INTO opcion_criterio (id_criterio, etiqueta, orden)
SELECT c.id_criterio_resenia, v.etiqueta, v.orden
FROM criterio_resenia c
JOIN (VALUES
    ('SERVICIO', 'Aspectos destacados', 'Comunicación clara', 1),
    ('SERVICIO', 'Aspectos destacados', 'Respondió rápido', 2),
    ('SERVICIO', 'Aspectos destacados', 'Aceptó ajustes', 3),
    ('SERVICIO', 'Aspectos destacados', 'Entrega cuidada', 4),
    ('SERVICIO', 'Aspectos destacados', 'Superó lo esperado', 5),
    ('CLIENTE', 'Aspectos destacados', 'Brief claro', 1),
    ('CLIENTE', 'Aspectos destacados', 'Respondió rápido', 2),
    ('CLIENTE', 'Aspectos destacados', 'Puntual con los pagos', 3),
    ('CLIENTE', 'Aspectos destacados', 'Trato respetuoso', 4),
    ('CLIENTE', 'Aspectos destacados', 'Aceptó ajustes razonables', 5)
) AS v(ambito, criterio, etiqueta, orden)
    ON c.ambito = v.ambito AND c.nombre = v.criterio
WHERE NOT EXISTS (
    SELECT 1 FROM opcion_criterio o
    WHERE o.id_criterio = c.id_criterio_resenia
      AND lower(o.etiqueta) = lower(v.etiqueta)
);

-- ---------------------------------------------------------------------------
-- Tipo de notificación
-- ---------------------------------------------------------------------------

INSERT INTO tipo_notificacion (codigo, nombre, frecuencia, estado)
SELECT 'RESENIA_RECIBIDA', 'Reseña de servicio recibida', 'EVENTO', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_notificacion WHERE codigo = 'RESENIA_RECIBIDA'
);
