-- La columna la usa V115 para indexar las cuentas dadas de baja.
-- En bases creadas con ddl-auto=update ya existe; en una base limpia hay que
-- crearla antes de que corra V115.
ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS estado_cuenta BOOLEAN NOT NULL DEFAULT TRUE;
