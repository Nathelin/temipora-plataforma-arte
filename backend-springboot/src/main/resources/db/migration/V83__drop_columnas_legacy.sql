-- Fase 4.5: cierre definitivo de los duplicados legacy.
--
-- La aplicación ya no lee ni escribe estas columnas y las fuentes canónicas
-- son presupuesto, hito_presupuesto y aceptacion_terminos_plataforma.
-- Backup previo: temipora_pre_4.5_20261107_230551.dump

DROP VIEW IF EXISTS v_pedido_compat;
DROP VIEW IF EXISTS v_hito_pedido_compat;

ALTER TABLE pedido
    DROP COLUMN IF EXISTS precio_final_legacy,
    DROP COLUMN IF EXISTS precio_final_cobro_legacy,
    DROP COLUMN IF EXISTS id_moneda_legacy,
    DROP COLUMN IF EXISTS id_moneda_cobro_legacy,
    DROP COLUMN IF EXISTS cotizacion_cobro_legacy,
    DROP COLUMN IF EXISTS comision_porcentaje_legacy,
    DROP COLUMN IF EXISTS id_oferta_reactivacion_legacy,
    DROP COLUMN IF EXISTS descuento_porcentaje_legacy,
    DROP COLUMN IF EXISTS descuento_monto_legacy,
    DROP COLUMN IF EXISTS descuento_monto_cobro_legacy,
    DROP COLUMN IF EXISTS precio_final_cliente_legacy,
    DROP COLUMN IF EXISTS precio_final_cliente_cobro_legacy,
    DROP COLUMN IF EXISTS terminos_plataforma_version_legacy,
    DROP COLUMN IF EXISTS terminos_plataforma_snapshot_legacy,
    DROP COLUMN IF EXISTS terminos_plataforma_aceptados_en_legacy;

ALTER TABLE hito_pedido
    DROP COLUMN IF EXISTS nombre_legacy,
    DROP COLUMN IF EXISTS descripcion_legacy,
    DROP COLUMN IF EXISTS porcentaje_legacy,
    DROP COLUMN IF EXISTS tipo_legacy,
    DROP COLUMN IF EXISTS requiere_entrega_legacy,
    DROP COLUMN IF EXISTS monto_legacy,
    DROP COLUMN IF EXISTS monto_cobro_legacy,
    DROP COLUMN IF EXISTS comision_monto_legacy,
    DROP COLUMN IF EXISTS comision_monto_cobro_legacy,
    DROP COLUMN IF EXISTS monto_neto_legacy,
    DROP COLUMN IF EXISTS monto_neto_cobro_legacy,
    DROP COLUMN IF EXISTS id_pago_aprobado_legacy;
