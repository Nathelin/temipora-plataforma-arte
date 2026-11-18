-- V95: endurece el registro de autenticidad (ledger).
--
-- Problema 1: el hash del evento no cubría la columna `datos`, que guarda el
--   JSON con título, artista y hash de licencia. Ese campo se podía reescribir
--   sin que la verificación lo detectara.
-- Problema 2: nada a nivel base impedía un UPDATE o DELETE sobre el historial.
--   La inmutabilidad era solo una convención del servicio.
--
-- Solución:
--   * `version_hash` permite convivir con los eventos ya emitidos. Los previos
--     quedan en la versión 1 (hash sin datos) y siguen verificando; los nuevos
--     se emiten en la versión 2, que incluye `hash_datos`.
--   * Un trigger bloquea UPDATE y DELETE sobre la tabla.

ALTER TABLE evento_ledger
    ADD COLUMN IF NOT EXISTS version_hash INTEGER NOT NULL DEFAULT 1;

ALTER TABLE evento_ledger
    ADD COLUMN IF NOT EXISTS hash_datos CHAR(64);

COMMENT ON COLUMN evento_ledger.version_hash IS
    'Formato del hash: 1 = sin datos, 2 = incluye hash_datos (protege la columna datos).';
COMMENT ON COLUMN evento_ledger.hash_datos IS
    'SHA-256 de la columna datos; entra en el hash del evento desde la versión 2.';

ALTER TABLE evento_ledger
    DROP CONSTRAINT IF EXISTS ck_ledger_version_hash;
ALTER TABLE evento_ledger
    ADD CONSTRAINT ck_ledger_version_hash
    CHECK (version_hash IN (1, 2));

-- Un evento de versión 2 siempre debe traer el hash de sus datos.
ALTER TABLE evento_ledger
    DROP CONSTRAINT IF EXISTS ck_ledger_v2_requiere_hash_datos;
ALTER TABLE evento_ledger
    ADD CONSTRAINT ck_ledger_v2_requiere_hash_datos
    CHECK (version_hash = 1 OR hash_datos IS NOT NULL);

CREATE OR REPLACE FUNCTION evento_ledger_inmutable()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION
        'El historial de autenticidad es inmutable: no se permite % en evento_ledger.',
        TG_OP
        USING ERRCODE = 'restrict_violation';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_evento_ledger_inmutable ON evento_ledger;
CREATE TRIGGER trg_evento_ledger_inmutable
    BEFORE UPDATE OR DELETE ON evento_ledger
    FOR EACH ROW
    EXECUTE FUNCTION evento_ledger_inmutable();
