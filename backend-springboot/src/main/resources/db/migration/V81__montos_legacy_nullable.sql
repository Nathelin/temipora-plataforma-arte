-- Fase 4.3: el pedido deja de copiar los montos del presupuesto y el hito
-- deja de copiar los campos de hito_presupuesto. Las columnas legacy quedan
-- como respaldo temporal (se renombrarán/eliminarán después del ciclo) y por
-- eso pasan a ser nullable.

ALTER TABLE pedido
    ALTER COLUMN precio_final DROP NOT NULL,
    ALTER COLUMN precio_final_cobro DROP NOT NULL,
    ALTER COLUMN id_moneda DROP NOT NULL,
    ALTER COLUMN id_moneda_cobro DROP NOT NULL,
    ALTER COLUMN cotizacion_cobro DROP NOT NULL,
    ALTER COLUMN comision_porcentaje DROP NOT NULL,
    ALTER COLUMN descuento_porcentaje DROP NOT NULL,
    ALTER COLUMN descuento_monto DROP NOT NULL,
    ALTER COLUMN descuento_monto_cobro DROP NOT NULL,
    ALTER COLUMN precio_final_cliente DROP NOT NULL,
    ALTER COLUMN precio_final_cliente_cobro DROP NOT NULL;

ALTER TABLE hito_pedido
    ALTER COLUMN nombre DROP NOT NULL,
    ALTER COLUMN tipo DROP NOT NULL,
    ALTER COLUMN monto DROP NOT NULL,
    ALTER COLUMN monto_cobro DROP NOT NULL,
    ALTER COLUMN comision_monto DROP NOT NULL,
    ALTER COLUMN comision_monto_cobro DROP NOT NULL,
    ALTER COLUMN monto_neto DROP NOT NULL,
    ALTER COLUMN monto_neto_cobro DROP NOT NULL;
