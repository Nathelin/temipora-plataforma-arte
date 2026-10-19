-- Moneda de los precios de servicios, preferencia de moneda del usuario y
-- tabla de conversión con ventanas de validez temporales.

-- Moneda en la que el artista carga el precio base del servicio.
ALTER TABLE servicio ADD COLUMN IF NOT EXISTS moneda VARCHAR(3) NOT NULL DEFAULT 'ARS';

-- Moneda en la que el usuario quiere ver los precios.
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS moneda_preferida VARCHAR(3) NOT NULL DEFAULT 'ARS';

-- Conversiones versionadas: cada fila vale para [inicio_validez, fin_validez).
-- Al cambiar el factor se cierra la ventana abierta (fin_validez) y se inserta una nueva.
CREATE TABLE IF NOT EXISTS conversion_moneda (
    id_conversion_moneda BIGSERIAL PRIMARY KEY,
    moneda_entrada VARCHAR(3) NOT NULL,
    moneda_salida VARCHAR(3) NOT NULL,
    factor NUMERIC(18,8) NOT NULL CHECK (factor > 0),
    inicio_validez TIMESTAMP NOT NULL,
    fin_validez TIMESTAMP,
    origen VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_conversion_monedas_distintas CHECK (moneda_entrada <> moneda_salida)
);

CREATE INDEX IF NOT EXISTS idx_conversion_moneda_vigente
    ON conversion_moneda (moneda_entrada, moneda_salida, inicio_validez);

-- Configuración (fila única) de la integración con la API de cotizaciones.
CREATE TABLE IF NOT EXISTS configuracion_cotizacion (
    id_configuracion BIGINT PRIMARY KEY,
    api_habilitada BOOLEAN NOT NULL DEFAULT TRUE,
    moneda_origen VARCHAR(3) NOT NULL DEFAULT 'USD',
    moneda_destino VARCHAR(3) NOT NULL DEFAULT 'ARS',
    factor_manual NUMERIC(18,8),
    fecha_actualizacion TIMESTAMP
);

INSERT INTO configuracion_cotizacion (id_configuracion, api_habilitada, moneda_origen, moneda_destino)
SELECT 1, TRUE, 'USD', 'ARS'
WHERE NOT EXISTS (SELECT 1 FROM configuracion_cotizacion WHERE id_configuracion = 1);
