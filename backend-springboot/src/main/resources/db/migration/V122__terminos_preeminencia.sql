-- Preeminencia de los Términos de la Plataforma sobre los del artista (fase 7).

ALTER TABLE terminos
    ADD COLUMN IF NOT EXISTS acepta_preeminencia BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE terminos
    ADD COLUMN IF NOT EXISTS id_terminos_plataforma_aceptados BIGINT;
ALTER TABLE terminos
    ADD COLUMN IF NOT EXISTS preeminencia_aceptada_en TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_terminos_plataforma_preeminencia'
    ) THEN
        ALTER TABLE terminos ADD CONSTRAINT fk_terminos_plataforma_preeminencia
            FOREIGN KEY (id_terminos_plataforma_aceptados)
            REFERENCES terminos_plataforma (id_terminos_plataforma);
    END IF;
END $$;
