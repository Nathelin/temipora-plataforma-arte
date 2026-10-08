DO $$
BEGIN
    IF to_regclass('public.imagen') IS NULL THEN
        RETURN;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'imagen'
          AND column_name = 'materiales'
    ) THEN
        ALTER TABLE imagen DROP COLUMN materiales;
    END IF;
END $$;
