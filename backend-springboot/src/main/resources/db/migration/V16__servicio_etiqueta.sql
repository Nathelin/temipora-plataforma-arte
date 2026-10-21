-- Etiquetas asignadas a un servicio (UC-05).
CREATE TABLE IF NOT EXISTS servicio_etiqueta (
    id_servicio BIGINT NOT NULL,
    id_etiqueta BIGINT NOT NULL,
    fecha_asignacion TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (id_servicio, id_etiqueta)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_servicioetiqueta_servicio') THEN
        ALTER TABLE servicio_etiqueta ADD CONSTRAINT fk_servicioetiqueta_servicio
            FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_servicioetiqueta_etiqueta') THEN
        ALTER TABLE servicio_etiqueta ADD CONSTRAINT fk_servicioetiqueta_etiqueta
            FOREIGN KEY (id_etiqueta) REFERENCES etiqueta (id_etiqueta);
    END IF;
END $$;
