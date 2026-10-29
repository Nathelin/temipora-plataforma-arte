-- Vínculo entre una imagen del portfolio y la entrega final que la respalda.
-- La imagen del portfolio es una copia pública derivada; el archivo original
-- de la entrega sigue en el almacenamiento privado.

CREATE TABLE IF NOT EXISTS portfolio_entrega_verificada (
    id_verificacion BIGSERIAL PRIMARY KEY,
    id_imagen BIGINT NOT NULL,
    id_archivo_entrega BIGINT NOT NULL,
    fecha_vinculacion TIMESTAMP NOT NULL DEFAULT now(),
    vigente BOOLEAN NOT NULL DEFAULT TRUE
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_portfolio_verificada_imagen'
    ) THEN
        ALTER TABLE portfolio_entrega_verificada
            ADD CONSTRAINT fk_portfolio_verificada_imagen
            FOREIGN KEY (id_imagen)
            REFERENCES imagen (id_imagen)
            ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_portfolio_verificada_archivo'
    ) THEN
        ALTER TABLE portfolio_entrega_verificada
            ADD CONSTRAINT fk_portfolio_verificada_archivo
            FOREIGN KEY (id_archivo_entrega)
            REFERENCES entrega_archivo (id_archivo);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_portfolio_verificada_imagen
    ON portfolio_entrega_verificada (id_imagen);

CREATE UNIQUE INDEX IF NOT EXISTS uk_portfolio_verificada_archivo_vigente
    ON portfolio_entrega_verificada (id_archivo_entrega)
    WHERE vigente = TRUE;

CREATE INDEX IF NOT EXISTS idx_portfolio_verificada_archivo
    ON portfolio_entrega_verificada (id_archivo_entrega);

CREATE INDEX IF NOT EXISTS idx_portfolio_verificada_vigente
    ON portfolio_entrega_verificada (vigente, id_imagen);
