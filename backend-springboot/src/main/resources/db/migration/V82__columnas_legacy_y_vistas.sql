-- Fase 4.4: la aplicación deja de leer y escribir los duplicados legacy.
-- Las columnas se renombran a *_legacy para que el DROP definitivo de 4.5
-- sea explícito. Se dejan vistas de compatibilidad con los nombres anteriores
-- para reportes/ERStudio u otros consumidores externos.

ALTER TABLE pedido RENAME COLUMN precio_final TO precio_final_legacy;
ALTER TABLE pedido RENAME COLUMN precio_final_cobro TO precio_final_cobro_legacy;
ALTER TABLE pedido RENAME COLUMN id_moneda TO id_moneda_legacy;
ALTER TABLE pedido RENAME COLUMN id_moneda_cobro TO id_moneda_cobro_legacy;
ALTER TABLE pedido RENAME COLUMN cotizacion_cobro TO cotizacion_cobro_legacy;
ALTER TABLE pedido RENAME COLUMN comision_porcentaje TO comision_porcentaje_legacy;
ALTER TABLE pedido RENAME COLUMN id_oferta_reactivacion TO id_oferta_reactivacion_legacy;
ALTER TABLE pedido RENAME COLUMN descuento_porcentaje TO descuento_porcentaje_legacy;
ALTER TABLE pedido RENAME COLUMN descuento_monto TO descuento_monto_legacy;
ALTER TABLE pedido RENAME COLUMN descuento_monto_cobro TO descuento_monto_cobro_legacy;
ALTER TABLE pedido RENAME COLUMN precio_final_cliente TO precio_final_cliente_legacy;
ALTER TABLE pedido RENAME COLUMN precio_final_cliente_cobro TO precio_final_cliente_cobro_legacy;
ALTER TABLE pedido RENAME COLUMN terminos_plataforma_version TO terminos_plataforma_version_legacy;
ALTER TABLE pedido RENAME COLUMN terminos_plataforma_snapshot TO terminos_plataforma_snapshot_legacy;
ALTER TABLE pedido RENAME COLUMN terminos_plataforma_aceptados_en TO terminos_plataforma_aceptados_en_legacy;

ALTER TABLE hito_pedido RENAME COLUMN nombre TO nombre_legacy;
ALTER TABLE hito_pedido RENAME COLUMN descripcion TO descripcion_legacy;
ALTER TABLE hito_pedido RENAME COLUMN porcentaje TO porcentaje_legacy;
ALTER TABLE hito_pedido RENAME COLUMN tipo TO tipo_legacy;
ALTER TABLE hito_pedido RENAME COLUMN requiere_entrega TO requiere_entrega_legacy;
ALTER TABLE hito_pedido RENAME COLUMN monto TO monto_legacy;
ALTER TABLE hito_pedido RENAME COLUMN monto_cobro TO monto_cobro_legacy;
ALTER TABLE hito_pedido RENAME COLUMN comision_monto TO comision_monto_legacy;
ALTER TABLE hito_pedido RENAME COLUMN comision_monto_cobro TO comision_monto_cobro_legacy;
ALTER TABLE hito_pedido RENAME COLUMN monto_neto TO monto_neto_legacy;
ALTER TABLE hito_pedido RENAME COLUMN monto_neto_cobro TO monto_neto_cobro_legacy;
ALTER TABLE hito_pedido RENAME COLUMN id_pago_aprobado TO id_pago_aprobado_legacy;

CREATE OR REPLACE VIEW v_pedido_compat AS
SELECT p.*,
       p.precio_final_legacy AS precio_final,
       p.precio_final_cobro_legacy AS precio_final_cobro,
       p.id_moneda_legacy AS id_moneda,
       p.id_moneda_cobro_legacy AS id_moneda_cobro,
       p.cotizacion_cobro_legacy AS cotizacion_cobro,
       p.comision_porcentaje_legacy AS comision_porcentaje,
       p.id_oferta_reactivacion_legacy AS id_oferta_reactivacion,
       p.descuento_porcentaje_legacy AS descuento_porcentaje,
       p.descuento_monto_legacy AS descuento_monto,
       p.descuento_monto_cobro_legacy AS descuento_monto_cobro,
       p.precio_final_cliente_legacy AS precio_final_cliente,
       p.precio_final_cliente_cobro_legacy AS precio_final_cliente_cobro,
       p.terminos_plataforma_version_legacy AS terminos_plataforma_version,
       p.terminos_plataforma_snapshot_legacy AS terminos_plataforma_snapshot,
       p.terminos_plataforma_aceptados_en_legacy AS terminos_plataforma_aceptados_en
FROM pedido p;

CREATE OR REPLACE VIEW v_hito_pedido_compat AS
SELECT h.*,
       h.nombre_legacy AS nombre,
       h.descripcion_legacy AS descripcion,
       h.porcentaje_legacy AS porcentaje,
       h.tipo_legacy AS tipo,
       h.requiere_entrega_legacy AS requiere_entrega,
       h.monto_legacy AS monto,
       h.monto_cobro_legacy AS monto_cobro,
       h.comision_monto_legacy AS comision_monto,
       h.comision_monto_cobro_legacy AS comision_monto_cobro,
       h.monto_neto_legacy AS monto_neto,
       h.monto_neto_cobro_legacy AS monto_neto_cobro,
       h.id_pago_aprobado_legacy AS id_pago_aprobado
FROM hito_pedido h;
