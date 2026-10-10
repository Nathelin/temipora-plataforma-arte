-- Esquema base (pre-migraciones).
--
-- Flyway corre ANTES que Hibernate (ddl-auto=update), por lo que las migraciones
-- siguientes (V1..) asumen tablas base que, en un entorno limpio (CI), todavía no
-- existen. Acá se crean de forma idempotente para que esas migraciones puedan correr.
-- Hibernate completa el resto de columnas y tablas después.
--
-- Idempotente: sobre una base ya inicializada (por ejemplo, tu entorno local) no hace nada.

CREATE TABLE IF NOT EXISTS rol (
    id_rol BIGSERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS usuario (
    id_usuario BIGSERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS portfolio (
    id_portfolio BIGSERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS imagen (
    id_imagen BIGSERIAL PRIMARY KEY
);
