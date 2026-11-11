-- V102: repara los eventos VENTA que se crearon con el monto dentro del
-- hash pero sin persistir la columna `monto`.
--
-- El hash_evento ya se calculó con el monto de la venta. Al completar la
-- columna con el mismo valor, la verificación vuelve a coincidir.
--
-- El ledger es inmutable por trigger; para esta reparación puntual se
-- deshabilita el trigger dentro de la migración y se vuelve a habilitar.

ALTER TABLE evento_ledger
    DISABLE TRIGGER trg_evento_ledger_inmutable;

UPDATE evento_ledger e
SET monto = ROUND(ci.precio_cobro * ci.cantidad, 2)
FROM compra_item ci
WHERE e.id_compra_item = ci.id_compra_item
  AND e.tipo = 'VENTA'
  AND e.monto IS NULL;

ALTER TABLE evento_ledger
    ENABLE TRIGGER trg_evento_ledger_inmutable;
