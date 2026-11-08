-- Jerarquía de roles: CLIENTE <- ARTISTA <- ADMIN.
--
-- Flyway corre antes que Hibernate. En una base limpia la tabla `rol` sólo tiene
-- `id_rol` (V0_1), así que acá se crean las columnas mínimas que el resto de la
-- migración necesita. En una base ya inicializada, los ADD COLUMN son no-op.
--
-- La jerarquía se usa en DetallesUsuarioService para expandir autoridades:
--   ADMIN  -> ROLE_ADMIN, ROLE_ARTISTA, ROLE_CLIENTE
--   ARTISTA-> ROLE_ARTISTA, ROLE_CLIENTE
--   CLIENTE-> ROLE_CLIENTE

ALTER TABLE rol ADD COLUMN IF NOT EXISTS nombre VARCHAR(255);
ALTER TABLE rol ADD COLUMN IF NOT EXISTS estado BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE rol ADD COLUMN IF NOT EXISTS id_rol_padre BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_rol_padre'
    ) THEN
        ALTER TABLE rol
            ADD CONSTRAINT fk_rol_padre
            FOREIGN KEY (id_rol_padre) REFERENCES rol (id_rol);
    END IF;
END $$;

-- Backfill idempotente. En una base limpia no hay filas todavía; el RolSeeder
-- crea la jerarquía después de que Hibernate completa el esquema.
UPDATE rol
   SET id_rol_padre = (SELECT r.id_rol FROM rol r WHERE r.nombre = 'CLIENTE')
 WHERE nombre = 'ARTISTA'
   AND id_rol_padre IS NULL;

UPDATE rol
   SET id_rol_padre = (SELECT r.id_rol FROM rol r WHERE r.nombre = 'ARTISTA')
 WHERE nombre = 'ADMIN'
   AND id_rol_padre IS NULL;

-- Un solo rol por nombre.
CREATE UNIQUE INDEX IF NOT EXISTS uk_rol_nombre ON rol (nombre);

-- `Permiso` era una entidad huérfana (sin código, sin relaciones y sin uso).
-- La autorización se resuelve con la jerarquía de roles.
DROP TABLE IF EXISTS permiso;
