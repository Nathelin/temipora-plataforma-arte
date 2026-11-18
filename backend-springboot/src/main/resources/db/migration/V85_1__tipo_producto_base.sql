-- La tabla tipo_producto se creaba solo por `spring.jpa.hibernate.ddl-auto=update`
-- en los entornos de desarrollo. En una base nueva Flyway corre antes que
-- Hibernate, y V86__productos_base.sql la referencia, así que hay que crearla
-- desde la migración. Idempotente para no chocar con bases existentes.
--
-- Versión 85.1: se aplica antes de V86 en bases nuevas y, gracias a
-- `spring.flyway.out-of-order=true`, también en bases ya migradas.

CREATE TABLE IF NOT EXISTS tipo_producto (
    id_tipo_producto BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(250) NOT NULL,
    observaciones VARCHAR(500),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);
