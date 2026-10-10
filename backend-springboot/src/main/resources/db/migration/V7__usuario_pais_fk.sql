-- Usuario.pais (texto libre) -> referencia a pais (id_pais).
-- Backfill por nombre (case-insensitive, trim); los textos que no coincidan quedan NULL.

DO $$
BEGIN
    IF to_regclass('public.usuario') IS NULL THEN
        RETURN;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'usuario'
          AND column_name = 'id_pais'
    ) THEN
        ALTER TABLE usuario ADD COLUMN id_pais BIGINT;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'usuario'
          AND column_name = 'pais'
    ) THEN
        UPDATE usuario u
        SET id_pais = p.id_pais
        FROM pais p
        WHERE u.id_pais IS NULL
          AND lower(trim(u.pais)) = lower(p.nombre);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
            ON kcu.constraint_name = tc.constraint_name
           AND kcu.table_schema = tc.table_schema
        WHERE tc.table_schema = 'public'
          AND tc.table_name = 'usuario'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'id_pais'
    ) THEN
        ALTER TABLE usuario
            ADD CONSTRAINT fk_usuario_pais
            FOREIGN KEY (id_pais) REFERENCES pais (id_pais);
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'usuario'
          AND column_name = 'pais'
    ) THEN
        ALTER TABLE usuario DROP COLUMN pais;
    END IF;
END $$;
