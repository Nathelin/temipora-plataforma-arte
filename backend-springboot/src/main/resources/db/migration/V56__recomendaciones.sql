-- Recomendaciones: visitas de usuarios y configuración del ranking.
--
-- Las visitas se agregan por usuario/entidad/día (upsert) para no inflar filas
-- con cada clic. La configuración es un singleton id = 1, igual que el resto
-- de las configuraciones de la plataforma.

CREATE TABLE IF NOT EXISTS visita_artista (
    id_usuario BIGINT NOT NULL,
    id_artista BIGINT NOT NULL,
    fecha DATE NOT NULL,
    cantidad INTEGER NOT NULL DEFAULT 1,
    ultima_visita TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (id_usuario, id_artista, fecha),
    CONSTRAINT chk_visita_artista_distintos CHECK (id_usuario <> id_artista)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_visita_artista_usuario') THEN
        ALTER TABLE visita_artista ADD CONSTRAINT fk_visita_artista_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_visita_artista_artista') THEN
        ALTER TABLE visita_artista ADD CONSTRAINT fk_visita_artista_artista
            FOREIGN KEY (id_artista) REFERENCES usuario (id_usuario) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_visita_artista_usuario_fecha
    ON visita_artista (id_usuario, fecha);
CREATE INDEX IF NOT EXISTS idx_visita_artista_artista_fecha
    ON visita_artista (id_artista, fecha);

CREATE TABLE IF NOT EXISTS visita_servicio (
    id_usuario BIGINT NOT NULL,
    id_servicio BIGINT NOT NULL,
    fecha DATE NOT NULL,
    cantidad INTEGER NOT NULL DEFAULT 1,
    ultima_visita TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (id_usuario, id_servicio, fecha)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_visita_servicio_usuario') THEN
        ALTER TABLE visita_servicio ADD CONSTRAINT fk_visita_servicio_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_visita_servicio_servicio') THEN
        ALTER TABLE visita_servicio ADD CONSTRAINT fk_visita_servicio_servicio
            FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_visita_servicio_usuario_fecha
    ON visita_servicio (id_usuario, fecha);
CREATE INDEX IF NOT EXISTS idx_visita_servicio_servicio_fecha
    ON visita_servicio (id_servicio, fecha);

CREATE TABLE IF NOT EXISTS configuracion_recomendacion (
    id_configuracion BIGINT PRIMARY KEY,
    peso_favorito_artista NUMERIC(5,4) NOT NULL DEFAULT 0.2000,
    peso_favorito_servicio NUMERIC(5,4) NOT NULL DEFAULT 0.1000,
    peso_historial NUMERIC(5,4) NOT NULL DEFAULT 0.2000,
    peso_co_compra NUMERIC(5,4) NOT NULL DEFAULT 0.1800,
    peso_co_visita NUMERIC(5,4) NOT NULL DEFAULT 0.1000,
    peso_etiquetas NUMERIC(5,4) NOT NULL DEFAULT 0.1000,
    peso_rating NUMERIC(5,4) NOT NULL DEFAULT 0.0600,
    peso_recencia NUMERIC(5,4) NOT NULL DEFAULT 0.0400,
    peso_precio NUMERIC(5,4) NOT NULL DEFAULT 0.0200,
    max_sugerencias INTEGER NOT NULL DEFAULT 12,
    max_por_artista INTEGER NOT NULL DEFAULT 2,
    min_franjas_precio INTEGER NOT NULL DEFAULT 3,
    rango_precio_pct NUMERIC(5,2) NOT NULL DEFAULT 40.00,
    dias_decaimiento INTEGER NOT NULL DEFAULT 90,
    dias_visitas INTEGER NOT NULL DEFAULT 180,
    fecha_actualizacion TIMESTAMP
);

INSERT INTO configuracion_recomendacion (id_configuracion)
SELECT 1
WHERE NOT EXISTS (
    SELECT 1 FROM configuracion_recomendacion WHERE id_configuracion = 1
);

ALTER TABLE configuracion_recomendacion
    DROP CONSTRAINT IF EXISTS ck_configuracion_recomendacion_rango;
ALTER TABLE configuracion_recomendacion
    ADD CONSTRAINT ck_configuracion_recomendacion_rango
    CHECK (peso_favorito_artista BETWEEN 0 AND 1
       AND peso_favorito_servicio BETWEEN 0 AND 1
       AND peso_historial BETWEEN 0 AND 1
       AND peso_co_compra BETWEEN 0 AND 1
       AND peso_co_visita BETWEEN 0 AND 1
       AND peso_etiquetas BETWEEN 0 AND 1
       AND peso_rating BETWEEN 0 AND 1
       AND peso_recencia BETWEEN 0 AND 1
       AND peso_precio BETWEEN 0 AND 1
       AND max_sugerencias > 0
       AND max_por_artista > 0
       AND min_franjas_precio >= 0
       AND rango_precio_pct BETWEEN 0 AND 100
       AND dias_decaimiento > 0
       AND dias_visitas > 0);
