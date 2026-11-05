ALTER TABLE pago ADD COLUMN IF NOT EXISTS arancel_mp NUMERIC(12,2);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS neto_recibido_mp NUMERIC(12,2);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS fecha_aprobacion TIMESTAMP;
ALTER TABLE pago ADD COLUMN IF NOT EXISTS fecha_liberacion TIMESTAMP;
ALTER TABLE pago ADD COLUMN IF NOT EXISTS medio_pago VARCHAR(40);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS tipo_medio_pago VARCHAR(30);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS cuotas INTEGER;
ALTER TABLE pago ADD COLUMN IF NOT EXISTS ultimos_cuatro VARCHAR(4);

COMMENT ON COLUMN pago.arancel_mp IS
    'Arancel de MercadoPago, expresado en moneda_cobro.';
COMMENT ON COLUMN pago.neto_recibido_mp IS
    'net_received_amount informado por MercadoPago, en moneda_cobro.';
