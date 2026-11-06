-- Token de acceso genérico para los enlaces de los mails automáticos.
-- Reemplaza el token propio de las campañas de reactivación y sirve también
-- para los mails de cierre.

CREATE TABLE IF NOT EXISTS token_acceso (
    id_token_acceso BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    proposito VARCHAR(20) NOT NULL,
    id_referencia BIGINT,
    token_hash VARCHAR(64) NOT NULL,
    expira TIMESTAMP NOT NULL,
    usado_en TIMESTAMP,
    creado_en TIMESTAMP NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_token_acceso_proposito') THEN
        ALTER TABLE token_acceso ADD CONSTRAINT ck_token_acceso_proposito
            CHECK (proposito IN ('OFERTA','CIERRE'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_token_acceso_usuario') THEN
        ALTER TABLE token_acceso ADD CONSTRAINT fk_token_acceso_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_token_acceso_hash') THEN
        ALTER TABLE token_acceso ADD CONSTRAINT uk_token_acceso_hash
            UNIQUE (token_hash);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_token_acceso_usuario
    ON token_acceso (id_usuario, proposito);
CREATE INDEX IF NOT EXISTS idx_token_acceso_expira
    ON token_acceso (expira);

-- Los tokens de las campañas ya enviadas siguen funcionando.
INSERT INTO token_acceso (
    id_usuario, proposito, id_referencia,
    token_hash, expira, usado_en, creado_en)
SELECT id_usuario, 'OFERTA', id_campana,
       token_acceso_hash, token_acceso_expira, token_acceso_usado_en, fecha_creacion
  FROM campana_reactivacion
 WHERE token_acceso_hash IS NOT NULL
   AND NOT EXISTS (
       SELECT 1 FROM token_acceso t
        WHERE t.token_hash = campana_reactivacion.token_acceso_hash);

ALTER TABLE campana_reactivacion
    DROP COLUMN IF EXISTS token_acceso_hash,
    DROP COLUMN IF EXISTS token_acceso_expira,
    DROP COLUMN IF EXISTS token_acceso_usado_en;
