-- Preferencia del dueño para mostrar su nombre como propietario actual
-- de las piezas que compró. Por defecto se mantiene visible.
ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS mostrar_propietario BOOLEAN NOT NULL DEFAULT TRUE;
