DO $$
BEGIN
    IF to_regclass('public.imagen') IS NULL THEN
        RETURN;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'imagen'
          AND column_name = 'id_usuario'
    ) THEN
        ALTER TABLE imagen ADD COLUMN id_usuario BIGINT;
    END IF;

    ALTER TABLE imagen ALTER COLUMN id_usuario SET NOT NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_imagen_usuario'
    ) THEN
        ALTER TABLE imagen
            ADD CONSTRAINT fk_imagen_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario);
    END IF;
END $$;