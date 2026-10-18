-- Términos y condiciones de uso y servicio del artista (UC-09).
-- Híbrido: secciones de texto + "qué hago/no hago" (temas con estado) + derechos de uso.

-- Catálogo de temas ("qué hago / qué no hago")
CREATE TABLE IF NOT EXISTS tema_termino (
    id_tema_termino BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO tema_termino (nombre, orden, activo)
SELECT v.nombre, v.orden, TRUE
FROM (VALUES
    ('NSFW / contenido adulto', 1),
    ('Gore / violencia', 2),
    ('Mecha', 3),
    ('Furries / anthro', 4),
    ('Fanart (personajes de otros)', 5),
    ('OCs (personajes originales)', 6),
    ('Diseño de personajes', 7),
    ('Fondos / escenarios', 8),
    ('Chibi', 9),
    ('Animación', 10)
) AS v(nombre, orden)
WHERE NOT EXISTS (SELECT 1 FROM tema_termino t WHERE t.nombre = v.nombre);

-- Catálogo de derechos de uso
CREATE TABLE IF NOT EXISTS derecho_termino (
    id_derecho_termino BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO derecho_termino (nombre, orden, activo)
SELECT v.nombre, v.orden, TRUE
FROM (VALUES
    ('Uso personal', 1),
    ('Uso comercial', 2),
    ('Reventa', 3),
    ('Merchandising', 4),
    ('Entrenamiento de IA', 5)
) AS v(nombre, orden)
WHERE NOT EXISTS (SELECT 1 FROM derecho_termino d WHERE d.nombre = v.nombre);

-- Términos (global del artista; nullable producto/servicio para override futuro)
CREATE TABLE IF NOT EXISTS terminos (
    id_terminos BIGSERIAL PRIMARY KEY,
    id_artista BIGINT,
    id_producto BIGINT,
    id_servicio BIGINT,
    version INTEGER NOT NULL DEFAULT 1,
    informacion_general TEXT,
    politica_revisiones TEXT,
    politica_cancelaciones TEXT,
    uso_artista TEXT,
    uso_cliente TEXT,
    notas TEXT,
    pago_extra VARCHAR(250),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_terminos_artista') THEN
        ALTER TABLE terminos
            ADD CONSTRAINT fk_terminos_artista
            FOREIGN KEY (id_artista) REFERENCES usuario (id_usuario);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_terminos_artista_global
    ON terminos (id_artista) WHERE id_producto IS NULL AND id_servicio IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_terminos_producto
    ON terminos (id_producto) WHERE id_producto IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_terminos_servicio
    ON terminos (id_servicio) WHERE id_servicio IS NOT NULL;

-- Estado del artista por tema
CREATE TABLE IF NOT EXISTS termino_tema (
    id_termino_tema BIGSERIAL PRIMARY KEY,
    id_terminos BIGINT NOT NULL,
    id_tema_termino BIGINT NOT NULL,
    estado VARCHAR(15) NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_terminotema_terminos') THEN
        ALTER TABLE termino_tema
            ADD CONSTRAINT fk_terminotema_terminos
            FOREIGN KEY (id_terminos) REFERENCES terminos (id_terminos) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_terminotema_tema') THEN
        ALTER TABLE termino_tema
            ADD CONSTRAINT fk_terminotema_tema
            FOREIGN KEY (id_tema_termino) REFERENCES tema_termino (id_tema_termino);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_termino_tema') THEN
        ALTER TABLE termino_tema
            ADD CONSTRAINT uk_termino_tema UNIQUE (id_terminos, id_tema_termino);
    END IF;
END $$;

-- Derechos otorgados
CREATE TABLE IF NOT EXISTS termino_derecho (
    id_termino_derecho BIGSERIAL PRIMARY KEY,
    id_terminos BIGINT NOT NULL,
    id_derecho_termino BIGINT NOT NULL,
    otorgado BOOLEAN NOT NULL DEFAULT FALSE
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_terminoderecho_terminos') THEN
        ALTER TABLE termino_derecho
            ADD CONSTRAINT fk_terminoderecho_terminos
            FOREIGN KEY (id_terminos) REFERENCES terminos (id_terminos) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_terminoderecho_derecho') THEN
        ALTER TABLE termino_derecho
            ADD CONSTRAINT fk_terminoderecho_derecho
            FOREIGN KEY (id_derecho_termino) REFERENCES derecho_termino (id_derecho_termino);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_termino_derecho') THEN
        ALTER TABLE termino_derecho
            ADD CONSTRAINT uk_termino_derecho UNIQUE (id_terminos, id_derecho_termino);
    END IF;
END $$;
