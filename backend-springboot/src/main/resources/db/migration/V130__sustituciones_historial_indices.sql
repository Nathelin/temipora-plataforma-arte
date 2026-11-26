-- Consultas del historial administrativo de sustituciones.

CREATE INDEX IF NOT EXISTS idx_sustitucion_fecha_estado
    ON sustitucion (fecha_creacion DESC, estado);

CREATE INDEX IF NOT EXISTS idx_sustitucion_motivo_fecha
    ON sustitucion (motivo, fecha_creacion DESC);

CREATE INDEX IF NOT EXISTS idx_sustitucion_disparador_fecha
    ON sustitucion (disparador, fecha_creacion DESC);
