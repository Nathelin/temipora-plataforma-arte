-- Las monedas pasan a ser una entidad gestionable por el administrador.
-- Se crea la tabla `moneda`, se siembran ARS y USD, y las columnas de texto
-- (servicio.moneda, usuario.moneda_preferida, conversion_moneda.*, 
-- configuracion_cotizacion.*) se reemplazan por claves foráneas.

CREATE TABLE IF NOT EXISTS moneda (
    id_moneda BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(3) NOT NULL UNIQUE,
    nombre VARCHAR(60) NOT NULL,
    simbolo VARCHAR(6),
    predeterminada BOOLEAN NOT NULL DEFAULT FALSE,
    estado BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO moneda (codigo, nombre, simbolo, predeterminada, estado)
SELECT 'ARS', 'Peso argentino', '$', TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM moneda WHERE codigo = 'ARS');

INSERT INTO moneda (codigo, nombre, simbolo, predeterminada, estado)
SELECT 'USD', 'Dólar estadounidense', 'US$', FALSE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM moneda WHERE codigo = 'USD');

-- ---------------------------------------------------------------
-- servicio.moneda (varchar) -> servicio.id_moneda (FK)
-- ---------------------------------------------------------------
ALTER TABLE servicio ADD COLUMN IF NOT EXISTS id_moneda BIGINT;
UPDATE servicio s SET id_moneda = m.id_moneda
    FROM moneda m
    WHERE s.id_moneda IS NULL AND s.moneda = m.codigo;
UPDATE servicio s SET id_moneda = (SELECT id_moneda FROM moneda WHERE predeterminada LIMIT 1)
    WHERE s.id_moneda IS NULL;
ALTER TABLE servicio ALTER COLUMN id_moneda SET NOT NULL;
ALTER TABLE servicio DROP COLUMN IF EXISTS moneda;
ALTER TABLE servicio DROP CONSTRAINT IF EXISTS fk_servicio_moneda;
ALTER TABLE servicio ADD CONSTRAINT fk_servicio_moneda
    FOREIGN KEY (id_moneda) REFERENCES moneda(id_moneda);

-- ---------------------------------------------------------------
-- usuario.moneda_preferida (varchar) -> usuario.id_moneda_preferida (FK)
-- ---------------------------------------------------------------
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS id_moneda_preferida BIGINT;
UPDATE usuario u SET id_moneda_preferida = m.id_moneda
    FROM moneda m
    WHERE u.id_moneda_preferida IS NULL AND u.moneda_preferida = m.codigo;
UPDATE usuario u SET id_moneda_preferida = (SELECT id_moneda FROM moneda WHERE predeterminada LIMIT 1)
    WHERE u.id_moneda_preferida IS NULL;
ALTER TABLE usuario DROP COLUMN IF EXISTS moneda_preferida;
ALTER TABLE usuario DROP CONSTRAINT IF EXISTS fk_usuario_moneda_preferida;
ALTER TABLE usuario ADD CONSTRAINT fk_usuario_moneda_preferida
    FOREIGN KEY (id_moneda_preferida) REFERENCES moneda(id_moneda);

-- ---------------------------------------------------------------
-- conversion_moneda (varchar x2) -> FKs
-- ---------------------------------------------------------------
ALTER TABLE conversion_moneda ADD COLUMN IF NOT EXISTS id_moneda_entrada BIGINT;
ALTER TABLE conversion_moneda ADD COLUMN IF NOT EXISTS id_moneda_salida BIGINT;
UPDATE conversion_moneda c SET id_moneda_entrada = m.id_moneda
    FROM moneda m
    WHERE c.id_moneda_entrada IS NULL AND c.moneda_entrada = m.codigo;
UPDATE conversion_moneda c SET id_moneda_salida = m.id_moneda
    FROM moneda m
    WHERE c.id_moneda_salida IS NULL AND c.moneda_salida = m.codigo;
DELETE FROM conversion_moneda WHERE id_moneda_entrada IS NULL OR id_moneda_salida IS NULL;
ALTER TABLE conversion_moneda ALTER COLUMN id_moneda_entrada SET NOT NULL;
ALTER TABLE conversion_moneda ALTER COLUMN id_moneda_salida SET NOT NULL;
ALTER TABLE conversion_moneda DROP COLUMN IF EXISTS moneda_entrada;
ALTER TABLE conversion_moneda DROP COLUMN IF EXISTS moneda_salida;
ALTER TABLE conversion_moneda DROP CONSTRAINT IF EXISTS chk_conversion_monedas_distintas;
ALTER TABLE conversion_moneda ADD CONSTRAINT chk_conversion_monedas_distintas
    CHECK (id_moneda_entrada <> id_moneda_salida);
ALTER TABLE conversion_moneda DROP CONSTRAINT IF EXISTS fk_conversion_moneda_entrada;
ALTER TABLE conversion_moneda ADD CONSTRAINT fk_conversion_moneda_entrada
    FOREIGN KEY (id_moneda_entrada) REFERENCES moneda(id_moneda);
ALTER TABLE conversion_moneda DROP CONSTRAINT IF EXISTS fk_conversion_moneda_salida;
ALTER TABLE conversion_moneda ADD CONSTRAINT fk_conversion_moneda_salida
    FOREIGN KEY (id_moneda_salida) REFERENCES moneda(id_moneda);

CREATE INDEX IF NOT EXISTS idx_conversion_moneda_vigente
    ON conversion_moneda (id_moneda_entrada, id_moneda_salida, inicio_validez);

-- ---------------------------------------------------------------
-- configuracion_cotizacion (varchar x2) -> FKs
-- ---------------------------------------------------------------
ALTER TABLE configuracion_cotizacion ADD COLUMN IF NOT EXISTS id_moneda_origen BIGINT;
ALTER TABLE configuracion_cotizacion ADD COLUMN IF NOT EXISTS id_moneda_destino BIGINT;
UPDATE configuracion_cotizacion c SET id_moneda_origen = m.id_moneda
    FROM moneda m
    WHERE c.id_moneda_origen IS NULL AND c.moneda_origen = m.codigo;
UPDATE configuracion_cotizacion c SET id_moneda_destino = m.id_moneda
    FROM moneda m
    WHERE c.id_moneda_destino IS NULL AND c.moneda_destino = m.codigo;
UPDATE configuracion_cotizacion SET id_moneda_origen =
        (SELECT id_moneda FROM moneda WHERE codigo = 'USD' LIMIT 1)
    WHERE id_moneda_origen IS NULL;
UPDATE configuracion_cotizacion SET id_moneda_destino =
        (SELECT id_moneda FROM moneda WHERE predeterminada LIMIT 1)
    WHERE id_moneda_destino IS NULL;
ALTER TABLE configuracion_cotizacion ALTER COLUMN id_moneda_origen SET NOT NULL;
ALTER TABLE configuracion_cotizacion ALTER COLUMN id_moneda_destino SET NOT NULL;
ALTER TABLE configuracion_cotizacion DROP COLUMN IF EXISTS moneda_origen;
ALTER TABLE configuracion_cotizacion DROP COLUMN IF EXISTS moneda_destino;
ALTER TABLE configuracion_cotizacion DROP CONSTRAINT IF EXISTS fk_configuracion_moneda_origen;
ALTER TABLE configuracion_cotizacion ADD CONSTRAINT fk_configuracion_moneda_origen
    FOREIGN KEY (id_moneda_origen) REFERENCES moneda(id_moneda);
ALTER TABLE configuracion_cotizacion DROP CONSTRAINT IF EXISTS fk_configuracion_moneda_destino;
ALTER TABLE configuracion_cotizacion ADD CONSTRAINT fk_configuracion_moneda_destino
    FOREIGN KEY (id_moneda_destino) REFERENCES moneda(id_moneda);
