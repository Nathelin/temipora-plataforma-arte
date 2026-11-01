-- Token de acceso de un solo uso para el magic link de las campañas.
-- Se guarda hasheado; el valor crudo solo viaja en el correo.

ALTER TABLE campana_reactivacion
    ADD COLUMN token_acceso_hash VARCHAR(64),
    ADD COLUMN token_acceso_expira TIMESTAMP,
    ADD COLUMN token_acceso_usado_en TIMESTAMP;

CREATE UNIQUE INDEX uk_campana_reactivacion_token_acceso_hash
    ON campana_reactivacion (token_acceso_hash)
    WHERE token_acceso_hash IS NOT NULL;
