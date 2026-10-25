-- Servicios favoritos de un usuario.
CREATE TABLE IF NOT EXISTS servicio_favorito (
    id_usuario BIGINT NOT NULL,
    id_servicio BIGINT NOT NULL,
    fecha_agregado TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (id_usuario, id_servicio)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_servicio_favorito_usuario') THEN
        ALTER TABLE servicio_favorito ADD CONSTRAINT fk_servicio_favorito_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_servicio_favorito_servicio') THEN
        ALTER TABLE servicio_favorito ADD CONSTRAINT fk_servicio_favorito_servicio
            FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_servicio_favorito_servicio
    ON servicio_favorito (id_servicio);
