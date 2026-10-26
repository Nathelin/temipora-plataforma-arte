-- UC-18: fecha estimada de inicio del trabajo.
-- La fecha límite de entrega no se guarda: se calcula al confirmarse el pago
-- como max(fecha de pago, fecha_inicio_estimada) + plazo.

ALTER TABLE presupuesto
    ADD COLUMN IF NOT EXISTS fecha_inicio_estimada DATE;
