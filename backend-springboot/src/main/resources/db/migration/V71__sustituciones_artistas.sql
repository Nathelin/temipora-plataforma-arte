-- Proceso de sustituciones de artistas: cuando un servicio no puede recibir
-- una solicitud, se ofrecen servicios similares de otros artistas que sí
-- tienen cupo. El evento y las opciones mostradas quedan registrados para
-- medir conversión y auditar el proceso.

CREATE TABLE IF NOT EXISTS configuracion_sustitucion (
    id_configuracion BIGINT PRIMARY KEY,
    habilitado BOOLEAN NOT NULL DEFAULT TRUE,
    max_alternativas INTEGER NOT NULL DEFAULT 6,
    max_por_artista INTEGER NOT NULL DEFAULT 1,
    rango_precio_pct NUMERIC(5,2) NOT NULL DEFAULT 40.00,
    bonus_favorito NUMERIC(5,3) NOT NULL DEFAULT 0.100,
    factor_tiempo_max NUMERIC(4,2) NOT NULL DEFAULT 3.00,
    mostrar_en_explorar BOOLEAN NOT NULL DEFAULT TRUE,
    minutos_dedupe INTEGER NOT NULL DEFAULT 30,
    dias_retencion INTEGER NOT NULL DEFAULT 180,
    fecha_actualizacion TIMESTAMP
);

INSERT INTO configuracion_sustitucion (id_configuracion)
SELECT 1
WHERE NOT EXISTS (
    SELECT 1 FROM configuracion_sustitucion WHERE id_configuracion = 1
);

ALTER TABLE configuracion_sustitucion
    DROP CONSTRAINT IF EXISTS ck_configuracion_sustitucion_rango;
ALTER TABLE configuracion_sustitucion
    ADD CONSTRAINT ck_configuracion_sustitucion_rango
    CHECK (max_alternativas BETWEEN 1 AND 20
       AND max_por_artista BETWEEN 1 AND 5
       AND rango_precio_pct BETWEEN 0 AND 200
       AND bonus_favorito BETWEEN 0 AND 1
       AND factor_tiempo_max BETWEEN 1 AND 10
       AND minutos_dedupe BETWEEN 0 AND 1440
       AND dias_retencion >= 0);

CREATE TABLE IF NOT EXISTS sustitucion (
    id_sustitucion BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT,
    id_servicio_origen BIGINT NOT NULL,
    id_artista_origen BIGINT NOT NULL,
    motivo VARCHAR(30) NOT NULL,
    disparador VARCHAR(30) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'ABIERTA',
    cantidad_opciones INTEGER NOT NULL DEFAULT 0,
    id_servicio_elegido BIGINT,
    id_solicitud BIGINT,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_resolucion TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_sustitucion_motivo') THEN
        ALTER TABLE sustitucion ADD CONSTRAINT ck_sustitucion_motivo
            CHECK (motivo IN ('SIN_CUPOS','SOLO_ESPERA','CERRADA','PAUSADO','ARCHIVADO'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_sustitucion_disparador') THEN
        ALTER TABLE sustitucion ADD CONSTRAINT ck_sustitucion_disparador
            CHECK (disparador IN ('DETALLE','EXPLORAR','PERFIL','FAVORITOS','INTENTO_SOLICITUD'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_sustitucion_estado') THEN
        ALTER TABLE sustitucion ADD CONSTRAINT ck_sustitucion_estado
            CHECK (estado IN ('ABIERTA','ALTERNATIVA_ELEGIDA','RESUELTA_ALTERNATIVA',
                              'RESUELTA_COLA','DESCARTADA','EXPIRADA'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sustitucion_cliente') THEN
        ALTER TABLE sustitucion ADD CONSTRAINT fk_sustitucion_cliente
            FOREIGN KEY (id_cliente) REFERENCES usuario (id_usuario) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sustitucion_servicio_origen') THEN
        ALTER TABLE sustitucion ADD CONSTRAINT fk_sustitucion_servicio_origen
            FOREIGN KEY (id_servicio_origen) REFERENCES servicio (id_servicio) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sustitucion_artista_origen') THEN
        ALTER TABLE sustitucion ADD CONSTRAINT fk_sustitucion_artista_origen
            FOREIGN KEY (id_artista_origen) REFERENCES usuario (id_usuario) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sustitucion_servicio_elegido') THEN
        ALTER TABLE sustitucion ADD CONSTRAINT fk_sustitucion_servicio_elegido
            FOREIGN KEY (id_servicio_elegido) REFERENCES servicio (id_servicio) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sustitucion_solicitud') THEN
        ALTER TABLE sustitucion ADD CONSTRAINT fk_sustitucion_solicitud
            FOREIGN KEY (id_solicitud) REFERENCES solicitud (id_solicitud) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_sustitucion_cliente
    ON sustitucion (id_cliente, fecha_creacion DESC);
CREATE INDEX IF NOT EXISTS idx_sustitucion_servicio_origen
    ON sustitucion (id_servicio_origen, fecha_creacion DESC);
CREATE INDEX IF NOT EXISTS idx_sustitucion_abierta
    ON sustitucion (estado) WHERE estado = 'ABIERTA';

CREATE TABLE IF NOT EXISTS sustitucion_opcion (
    id_sustitucion_opcion BIGSERIAL PRIMARY KEY,
    id_sustitucion BIGINT NOT NULL,
    id_servicio BIGINT NOT NULL,
    id_artista BIGINT NOT NULL,
    orden INTEGER NOT NULL,
    score NUMERIC(6,4) NOT NULL,
    motivo VARCHAR(80),
    precio_momento NUMERIC(12,2) NOT NULL,
    id_moneda BIGINT NOT NULL,
    elegida BOOLEAN NOT NULL DEFAULT FALSE
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sustitucion_opcion_sustitucion') THEN
        ALTER TABLE sustitucion_opcion ADD CONSTRAINT fk_sustitucion_opcion_sustitucion
            FOREIGN KEY (id_sustitucion) REFERENCES sustitucion (id_sustitucion) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sustitucion_opcion_servicio') THEN
        ALTER TABLE sustitucion_opcion ADD CONSTRAINT fk_sustitucion_opcion_servicio
            FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sustitucion_opcion_artista') THEN
        ALTER TABLE sustitucion_opcion ADD CONSTRAINT fk_sustitucion_opcion_artista
            FOREIGN KEY (id_artista) REFERENCES usuario (id_usuario) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sustitucion_opcion_moneda') THEN
        ALTER TABLE sustitucion_opcion ADD CONSTRAINT fk_sustitucion_opcion_moneda
            FOREIGN KEY (id_moneda) REFERENCES moneda (id_moneda);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_sustitucion_opcion_servicio') THEN
        ALTER TABLE sustitucion_opcion ADD CONSTRAINT uk_sustitucion_opcion_servicio
            UNIQUE (id_sustitucion, id_servicio);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_sustitucion_opcion_sustitucion
    ON sustitucion_opcion (id_sustitucion, orden);
