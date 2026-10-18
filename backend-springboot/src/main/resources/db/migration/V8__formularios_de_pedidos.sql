-- Módulo de formularios de pedidos (UC-10, UC-45, UC-46, UC-47).
-- Nomenclatura: Formulario -> Pregunta -> OpcionPregunta; los tipos de campo viven en un catálogo.

-- Catálogo de tipos de campo
CREATE TABLE IF NOT EXISTS tipo_campo (
    id_tipo_campo BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(60) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_tipo_campo_codigo') THEN
        ALTER TABLE tipo_campo ADD CONSTRAINT uk_tipo_campo_codigo UNIQUE (codigo);
    END IF;
END $$;

INSERT INTO tipo_campo (codigo, nombre, activo)
SELECT v.codigo, v.nombre, TRUE
FROM (VALUES
    ('TEXTO', 'Bloque de texto'),
    ('CASILLAS', 'Casillas de verificación'),
    ('OPCIONES', 'Botones de opción'),
    ('ENCABEZADO', 'Encabezado / título'),
    ('SUBIR_IMAGEN', 'Subir imagen')
) AS v(codigo, nombre)
WHERE NOT EXISTS (SELECT 1 FROM tipo_campo t WHERE t.codigo = v.codigo);

-- Formulario (global del artista, por servicio, o del sistema)
CREATE TABLE IF NOT EXISTS formulario (
    id_formulario BIGSERIAL PRIMARY KEY,
    id_artista BIGINT,
    id_servicio BIGINT,
    es_sistema BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_formulario_artista') THEN
        ALTER TABLE formulario
            ADD CONSTRAINT fk_formulario_artista
            FOREIGN KEY (id_artista) REFERENCES usuario (id_usuario);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_formulario_artista_global
    ON formulario (id_artista) WHERE es_sistema = FALSE AND id_servicio IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_formulario_servicio
    ON formulario (id_servicio) WHERE id_servicio IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_formulario_sistema
    ON formulario (es_sistema) WHERE es_sistema = TRUE;

-- Preguntas del formulario
CREATE TABLE IF NOT EXISTS pregunta (
    id_pregunta BIGSERIAL PRIMARY KEY,
    id_formulario BIGINT NOT NULL,
    id_tipo_campo BIGINT NOT NULL,
    enunciado VARCHAR(250) NOT NULL,
    ayuda VARCHAR(250),
    obligatoria BOOLEAN NOT NULL DEFAULT FALSE,
    orden INTEGER NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pregunta_formulario') THEN
        ALTER TABLE pregunta
            ADD CONSTRAINT fk_pregunta_formulario
            FOREIGN KEY (id_formulario) REFERENCES formulario (id_formulario) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pregunta_tipo_campo') THEN
        ALTER TABLE pregunta
            ADD CONSTRAINT fk_pregunta_tipo_campo
            FOREIGN KEY (id_tipo_campo) REFERENCES tipo_campo (id_tipo_campo);
    END IF;
END $$;

-- Opciones de preguntas de casillas / botones de opción
CREATE TABLE IF NOT EXISTS opcion_pregunta (
    id_opcion_pregunta BIGSERIAL PRIMARY KEY,
    id_pregunta BIGINT NOT NULL,
    texto VARCHAR(150) NOT NULL,
    orden INTEGER NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_opcion_pregunta_pregunta') THEN
        ALTER TABLE opcion_pregunta
            ADD CONSTRAINT fk_opcion_pregunta_pregunta
            FOREIGN KEY (id_pregunta) REFERENCES pregunta (id_pregunta) ON DELETE CASCADE;
    END IF;
END $$;

-- Plantilla del sistema: un formulario simple (encabezado + texto + subir imagen)
INSERT INTO formulario (es_sistema, fecha_creacion)
SELECT TRUE, now()
WHERE NOT EXISTS (SELECT 1 FROM formulario f WHERE f.es_sistema = TRUE);

DO $$
DECLARE
    v_form BIGINT;
    v_enc BIGINT;
    v_txt BIGINT;
    v_img BIGINT;
BEGIN
    SELECT id_formulario INTO v_form FROM formulario WHERE es_sistema = TRUE LIMIT 1;
    IF v_form IS NULL THEN
        RETURN;
    END IF;

    IF EXISTS (SELECT 1 FROM pregunta WHERE id_formulario = v_form) THEN
        RETURN;
    END IF;

    SELECT id_tipo_campo INTO v_enc FROM tipo_campo WHERE codigo = 'ENCABEZADO';
    SELECT id_tipo_campo INTO v_txt FROM tipo_campo WHERE codigo = 'TEXTO';
    SELECT id_tipo_campo INTO v_img FROM tipo_campo WHERE codigo = 'SUBIR_IMAGEN';

    INSERT INTO pregunta (id_formulario, id_tipo_campo, enunciado, obligatoria, orden)
    VALUES (v_form, v_enc, 'Datos de tu pedido', FALSE, 0);

    INSERT INTO pregunta (id_formulario, id_tipo_campo, enunciado, obligatoria, orden)
    VALUES (v_form, v_txt, 'Contame qué querés', TRUE, 1);

    INSERT INTO pregunta (id_formulario, id_tipo_campo, enunciado, ayuda, obligatoria, orden)
    VALUES (v_form, v_img, 'Subí imágenes de referencia', 'Adjuntá las referencias que te sirvan.', FALSE, 2);
END $$;
