-- V97: elimina las tablas huérfanas del boceto original de compra.
--
-- `Compra`, `DetalleCompra`, `CarritoCompra` y `MetodoPago` son entidades JPA
-- de 2-4 campos, sin relaciones, sin repositorios, sin servicios ni endpoints.
-- Nunca tuvieron migración Flyway: las creó `ddl-auto=update`. No las usa
-- ninguna consulta del código.
--
-- Se eliminan ahora para que el esquema sea reconstruible desde las
-- migraciones. El flujo real de compra de productos se modelará cuando exista
-- (ver fase-3 y la sección de bundles), y esas tablas van a tener otra forma.

DROP TABLE IF EXISTS detalle_compra CASCADE;
DROP TABLE IF EXISTS carrito_compra CASCADE;
DROP TABLE IF EXISTS metodo_pago CASCADE;
DROP TABLE IF EXISTS compra CASCADE;
