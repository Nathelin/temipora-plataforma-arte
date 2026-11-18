-- Fase 6 (parcial): integridad de enums, unicidad y normalización de
-- preferencia_sensible.
--
-- Datos auditados antes de aplicar sobre la base actual:
--   - usuario.preferencia_sensible: 9 filas en 0 (ADVERTIR) y 2 en NULL.
--   - sin duplicados en usuario.nombre_usuario/correo, rol.nombre,
--     tipo_servicio.nombre, notificacion_usuario ni conversacion_participante.
--   - sin correos ni nombres de tipo de servicio con espacios.
--   - todos los valores de estado/tipo caen dentro de los enums de la app.
--
-- Las columnas que sólo crea Hibernate se agregan acá con IF NOT EXISTS para
-- que la migración también funcione en una base limpia (Flyway corre antes).

-- ---------------------------------------------------------------------------
-- 1) usuario.preferencia_sensible: smallint (ordinal) -> varchar (STRING)
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'usuario'
          AND column_name = 'preferencia_sensible'
          AND data_type IN ('smallint', 'integer', 'bigint')
    ) THEN
        ALTER TABLE usuario DROP CONSTRAINT IF EXISTS usuario_preferencia_sensible_check;
        ALTER TABLE usuario
            ALTER COLUMN preferencia_sensible TYPE VARCHAR(30)
            USING CASE
                WHEN preferencia_sensible IS NULL THEN 'ADVERTIR'
                WHEN preferencia_sensible = 0 THEN 'ADVERTIR'
                WHEN preferencia_sensible = 1 THEN 'MOSTRAR_SIN_ADVERTENCIA'
                WHEN preferencia_sensible = 2 THEN 'OCULTAR'
                ELSE 'ADVERTIR'
            END;
        ALTER TABLE usuario ALTER COLUMN preferencia_sensible SET DEFAULT 'ADVERTIR';
        ALTER TABLE usuario ALTER COLUMN preferencia_sensible SET NOT NULL;
    ELSIF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'usuario'
          AND column_name = 'preferencia_sensible'
    ) THEN
        ALTER TABLE usuario
            ADD COLUMN preferencia_sensible VARCHAR(30) NOT NULL DEFAULT 'ADVERTIR';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_usuario_preferencia_sensible'
    ) THEN
        ALTER TABLE usuario ADD CONSTRAINT ck_usuario_preferencia_sensible
            CHECK (preferencia_sensible IN ('ADVERTIR', 'MOSTRAR_SIN_ADVERTENCIA', 'OCULTAR'));
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 2) Unicidad de correo (case-insensitive)
-- ---------------------------------------------------------------------------
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS correo VARCHAR(40);
ALTER TABLE usuario ALTER COLUMN correo SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_usuario_correo_lower
    ON usuario (lower(correo));

-- ---------------------------------------------------------------------------
-- 3) Unicidad de tipo_servicio.nombre (case-insensitive)
-- ---------------------------------------------------------------------------
ALTER TABLE tipo_servicio ADD COLUMN IF NOT EXISTS nombre VARCHAR(50);
ALTER TABLE tipo_servicio ALTER COLUMN nombre SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_tipo_servicio_nombre_lower
    ON tipo_servicio (lower(trim(nombre)));

-- ---------------------------------------------------------------------------
-- 4) CHECKs de enums que faltaban (el resto ya existe en sus migraciones)
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_conversacion_estado') THEN
        ALTER TABLE conversacion ADD CONSTRAINT ck_conversacion_estado
            CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'CANCELADA', 'VENCIDA'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_conversacion_origen') THEN
        ALTER TABLE conversacion ADD CONSTRAINT ck_conversacion_origen
            CHECK (origen IN ('SOLICITUD_MENSAJE', 'SOLICITUD_SERVICIO'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_mensaje_estado') THEN
        ALTER TABLE mensaje ADD CONSTRAINT ck_mensaje_estado
            CHECK (estado IN ('ENVIADO', 'OCULTO'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_etiqueta_estado') THEN
        ALTER TABLE etiqueta ADD CONSTRAINT ck_etiqueta_estado
            CHECK (estado IN ('PENDIENTE', 'VALIDA', 'RECHAZADA', 'INACTIVA'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_solicitud_estado') THEN
        ALTER TABLE solicitud ADD CONSTRAINT ck_solicitud_estado
            CHECK (estado IN ('PENDIENTE', 'EN_LISTA_ESPERA', 'PRESUPUESTADA', 'PAGADA',
                              'FINALIZADA', 'RECHAZADA', 'CANCELADA', 'VENCIDA'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_oferta_reactivacion_estado') THEN
        ALTER TABLE oferta_reactivacion ADD CONSTRAINT ck_oferta_reactivacion_estado
            CHECK (estado IN ('OFRECIDA', 'RESERVADA', 'UTILIZADA', 'VENCIDA', 'CANCELADA'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_campana_reactivacion_estado') THEN
        ALTER TABLE campana_reactivacion ADD CONSTRAINT ck_campana_reactivacion_estado
            CHECK (estado IN ('ACTIVA', 'UTILIZADA', 'VENCIDA', 'CANCELADA'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_envio_email_estado') THEN
        ALTER TABLE envio_email ADD CONSTRAINT ck_envio_email_estado
            CHECK (estado IN ('PENDIENTE', 'ENVIADO', 'FALLIDO'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_conversacion_accion_tipo') THEN
        ALTER TABLE conversacion_accion ADD CONSTRAINT ck_conversacion_accion_tipo
            CHECK (tipo_accion IN ('SOLICITUD', 'ACEPTACION', 'RECHAZO', 'CANCELACION',
                                   'REENVIO', 'APERTURA_AUTOMATICA'));
    END IF;
END $$;
