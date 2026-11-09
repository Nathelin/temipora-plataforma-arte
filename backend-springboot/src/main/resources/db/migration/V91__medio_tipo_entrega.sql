-- V91: tipo de entrega del medio.
-- DIGITAL: entrega por la plataforma.
-- FISICA: el envío/entrega se coordina fuera de la plataforma.
ALTER TABLE medio
    ADD COLUMN tipo_entrega VARCHAR(20) NOT NULL DEFAULT 'DIGITAL';

UPDATE medio
SET tipo_entrega = 'FISICA'
WHERE lower(trim(nombre)) IN ('tradicional', 'fisico', 'físico', 'fisica', 'física');

UPDATE medio
SET observaciones = 'El envío y la entrega se coordinan directamente entre artista y comprador. Temipora no gestiona envíos.'
WHERE lower(trim(nombre)) = 'tradicional'
  AND (observaciones IS NULL OR btrim(observaciones) = '');

ALTER TABLE medio
    ADD CONSTRAINT ck_medio_tipo_entrega
    CHECK (tipo_entrega IN ('DIGITAL', 'FISICA'));
