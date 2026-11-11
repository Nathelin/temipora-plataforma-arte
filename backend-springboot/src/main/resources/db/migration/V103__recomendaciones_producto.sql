-- V103: tracking y configuración del recomendador de productos.
--
-- Mismo patrón que V56 (recomendaciones de servicios): las visitas se agregan
-- por usuario/producto/día con upsert y la configuración es un singleton id=1.
-- Se reutilizan artista_favorito, producto_favorito, producto_etiqueta y las
-- compras (compra_grupo.estado = 'PAGADO') como señales de interés.

CREATE TABLE IF NOT EXISTS visita_producto (
    id_usuario BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    fecha DATE NOT NULL,
    cantidad INTEGER NOT NULL DEFAULT 1,
    ultima_visita TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (id_usuario, id_producto, fecha)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_visita_producto_usuario') THEN
        ALTER TABLE visita_producto ADD CONSTRAINT fk_visita_producto_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_visita_producto_producto') THEN
        ALTER TABLE visita_producto ADD CONSTRAINT fk_visita_producto_producto
            FOREIGN KEY (id_producto) REFERENCES producto (id_producto) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_visita_producto_usuario_fecha
    ON visita_producto (id_usuario, fecha);
CREATE INDEX IF NOT EXISTS idx_visita_producto_producto_fecha
    ON visita_producto (id_producto, fecha);

CREATE TABLE IF NOT EXISTS configuracion_recomendacion_producto (
    id_configuracion BIGINT PRIMARY KEY,
    peso_favorito_producto NUMERIC(5,4) NOT NULL DEFAULT 0.1800,
    peso_favorito_artista NUMERIC(5,4) NOT NULL DEFAULT 0.1500,
    peso_historial NUMERIC(5,4) NOT NULL DEFAULT 0.1500,
    peso_co_compra NUMERIC(5,4) NOT NULL DEFAULT 0.1800,
    peso_co_visita NUMERIC(5,4) NOT NULL DEFAULT 0.0800,
    peso_etiquetas NUMERIC(5,4) NOT NULL DEFAULT 0.1400,
    peso_calidad NUMERIC(5,4) NOT NULL DEFAULT 0.0600,
    peso_recencia NUMERIC(5,4) NOT NULL DEFAULT 0.0300,
    peso_precio NUMERIC(5,4) NOT NULL DEFAULT 0.0300,
    max_sugerencias INTEGER NOT NULL DEFAULT 12,
    max_por_artista INTEGER NOT NULL DEFAULT 2,
    min_franjas_precio INTEGER NOT NULL DEFAULT 3,
    rango_precio_pct NUMERIC(5,2) NOT NULL DEFAULT 40.00,
    dias_decaimiento INTEGER NOT NULL DEFAULT 90,
    dias_visitas INTEGER NOT NULL DEFAULT 180,
    fecha_actualizacion TIMESTAMP
);

INSERT INTO configuracion_recomendacion_producto (id_configuracion)
SELECT 1
WHERE NOT EXISTS (
    SELECT 1 FROM configuracion_recomendacion_producto WHERE id_configuracion = 1
);

ALTER TABLE configuracion_recomendacion_producto
    DROP CONSTRAINT IF EXISTS ck_configuracion_recomendacion_producto_rango;
ALTER TABLE configuracion_recomendacion_producto
    ADD CONSTRAINT ck_configuracion_recomendacion_producto_rango
    CHECK (peso_favorito_producto BETWEEN 0 AND 1
       AND peso_favorito_artista BETWEEN 0 AND 1
       AND peso_historial BETWEEN 0 AND 1
       AND peso_co_compra BETWEEN 0 AND 1
       AND peso_co_visita BETWEEN 0 AND 1
       AND peso_etiquetas BETWEEN 0 AND 1
       AND peso_calidad BETWEEN 0 AND 1
       AND peso_recencia BETWEEN 0 AND 1
       AND peso_precio BETWEEN 0 AND 1
       AND max_sugerencias > 0
       AND max_por_artista > 0
       AND min_franjas_precio >= 0
       AND rango_precio_pct BETWEEN 0 AND 100
       AND dias_decaimiento > 0
       AND dias_visitas > 0);
