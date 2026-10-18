-- Tabla base de tipos de servicio.
--
-- V12 agrega la FK servicio.id_tipo_servicio -> tipo_servicio, pero esa tabla
-- solo era creada por Hibernate (ddl-auto=update), que corre DESPUÉS de Flyway.
-- En un entorno limpio (CI) todavía no existe y la migración falla.
-- Acá la creamos para que V12 pueda aplicar la FK; Hibernate completa el resto
-- de las columnas después.
--
-- Versión menor a V12 a propósito: en bases ya migradas se aplica como
-- "out of order" (spring.flyway.out-of-order=true) y, al ser IF NOT EXISTS,
-- no tiene efecto porque Hibernate ya la había creado.

CREATE TABLE IF NOT EXISTS tipo_servicio (
    id_tipo_servicio BIGSERIAL PRIMARY KEY
);
