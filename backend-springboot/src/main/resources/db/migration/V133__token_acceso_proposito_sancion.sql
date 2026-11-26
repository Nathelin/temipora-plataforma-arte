-- El propósito SANCION identifica los enlaces de acceso de los avisos de sanción.
ALTER TABLE token_acceso DROP CONSTRAINT IF EXISTS ck_token_acceso_proposito;
ALTER TABLE token_acceso ADD CONSTRAINT ck_token_acceso_proposito
    CHECK (proposito IN ('OFERTA', 'CIERRE', 'RECORDATORIO', 'SANCION'));
