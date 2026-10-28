-- Aceptación de los términos de la plataforma por parte del cliente.
-- Se registra al confirmarse el primer pago del pedido y guarda el snapshot
-- vigente al momento de la aceptación.

CREATE TABLE IF NOT EXISTS aceptacion_terminos_plataforma (
    id_aceptacion BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    id_terminos_plataforma BIGINT NOT NULL,
    version INTEGER NOT NULL,
    snapshot TEXT NOT NULL,
    fecha_aceptacion TIMESTAMP NOT NULL DEFAULT now(),
    id_pedido BIGINT
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_aceptacion_usuario') THEN
        ALTER TABLE aceptacion_terminos_plataforma ADD CONSTRAINT fk_aceptacion_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_aceptacion_terminos') THEN
        ALTER TABLE aceptacion_terminos_plataforma ADD CONSTRAINT fk_aceptacion_terminos
            FOREIGN KEY (id_terminos_plataforma) REFERENCES terminos_plataforma (id_terminos_plataforma);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_aceptacion_pedido') THEN
        ALTER TABLE aceptacion_terminos_plataforma ADD CONSTRAINT fk_aceptacion_pedido
            FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_aceptacion_usuario_version
    ON aceptacion_terminos_plataforma (id_usuario, version);
CREATE INDEX IF NOT EXISTS idx_aceptacion_pedido
    ON aceptacion_terminos_plataforma (id_pedido);
