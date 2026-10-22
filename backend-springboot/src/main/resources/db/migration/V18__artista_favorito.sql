-- Artistas favoritos de un usuario (UC-28 y UC-53).
CREATE TABLE IF NOT EXISTS artista_favorito (
    id_usuario BIGINT NOT NULL,
    id_artista BIGINT NOT NULL,
    fecha_agregado TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (id_usuario, id_artista),
    CONSTRAINT chk_artista_favorito_distintos CHECK (id_usuario <> id_artista)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_artista_favorito_usuario') THEN
        ALTER TABLE artista_favorito ADD CONSTRAINT fk_artista_favorito_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_artista_favorito_artista') THEN
        ALTER TABLE artista_favorito ADD CONSTRAINT fk_artista_favorito_artista
            FOREIGN KEY (id_artista) REFERENCES usuario (id_usuario) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_artista_favorito_artista
    ON artista_favorito (id_artista);
