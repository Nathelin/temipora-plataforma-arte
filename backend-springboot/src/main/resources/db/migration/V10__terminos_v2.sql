-- Términos v2: derechos con % extra y disponibilidad, descripciones (para el modal),
-- temas personalizados y plazos (entrega/respuesta).

-- Derechos: nuevas columnas
ALTER TABLE derecho_termino ADD COLUMN IF NOT EXISTS codigo VARCHAR(30);
ALTER TABLE derecho_termino ADD COLUMN IF NOT EXISTS descripcion TEXT;
ALTER TABLE derecho_termino ADD COLUMN IF NOT EXISTS requiere_pieza_unica BOOLEAN NOT NULL DEFAULT FALSE;

-- Renombrar derechos existentes y dar de baja "Entrenamiento de IA"
UPDATE derecho_termino SET nombre = 'Uso comercial en producción de contenido' WHERE nombre = 'Uso comercial';
UPDATE derecho_termino SET nombre = 'Uso comercial como merchandising' WHERE nombre = 'Merchandising';
UPDATE derecho_termino SET activo = FALSE WHERE nombre = 'Entrenamiento de IA';

-- Reasegurar los 4 derechos base (idempotente)
INSERT INTO derecho_termino (nombre, orden, activo)
SELECT v.nombre, v.orden, TRUE
FROM (VALUES
    ('Uso personal', 1),
    ('Uso comercial en producción de contenido', 2),
    ('Uso comercial como merchandising', 3),
    ('Reventa', 4)
) AS v(nombre, orden)
WHERE NOT EXISTS (SELECT 1 FROM derecho_termino d WHERE d.nombre = v.nombre);

-- Códigos, descripciones y condición de pieza única
UPDATE derecho_termino SET
    codigo = 'PERSONAL',
    descripcion = 'Usás la obra para fines personales: fondo de pantalla, uso propio o regalo. No permite venderla, usarla en productos ni en contenido monetizado.',
    requiere_pieza_unica = FALSE
WHERE nombre = 'Uso personal';

UPDATE derecho_termino SET
    codigo = 'CONTENIDO',
    descripcion = 'Usás la obra en contenido (videos, streams, redes, thumbnails, etc.), incluso monetizado. No permite venderla como producto ni revenderla.',
    requiere_pieza_unica = FALSE
WHERE nombre = 'Uso comercial en producción de contenido';

UPDATE derecho_termino SET
    codigo = 'MERCHANDISING',
    descripcion = 'Usás la obra en productos físicos o digitales para vender (remeras, prints, stickers, etc.).',
    requiere_pieza_unica = FALSE
WHERE nombre = 'Uso comercial como merchandising';

UPDATE derecho_termino SET
    codigo = 'REVENTA',
    descripcion = 'Revendés la obra como pieza única (adoptables o pinturas tradicionales únicas). Otorga más derechos; aplica solo a piezas únicas.',
    requiere_pieza_unica = TRUE
WHERE nombre = 'Reventa';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_derecho_termino_codigo') THEN
        ALTER TABLE derecho_termino ADD CONSTRAINT uk_derecho_termino_codigo UNIQUE (codigo);
    END IF;
END $$;

-- Temas: descripción para el modal
ALTER TABLE tema_termino ADD COLUMN IF NOT EXISTS descripcion TEXT;

UPDATE tema_termino SET descripcion = 'Contenido sexualmente explícito o desnudos.' WHERE nombre = 'NSFW / contenido adulto' AND descripcion IS NULL;
UPDATE tema_termino SET descripcion = 'Sangre, heridas o violencia gráfica.' WHERE nombre = 'Gore / violencia' AND descripcion IS NULL;
UPDATE tema_termino SET descripcion = 'Robots, naves y máquinas.' WHERE nombre = 'Mecha' AND descripcion IS NULL;
UPDATE tema_termino SET descripcion = 'Personajes animales antropomórficos.' WHERE nombre = 'Furries / anthro' AND descripcion IS NULL;
UPDATE tema_termino SET descripcion = 'Dibujos de personajes que no son tuyos.' WHERE nombre = 'Fanart (personajes de otros)' AND descripcion IS NULL;
UPDATE tema_termino SET descripcion = 'Dibujos de personajes originales del cliente.' WHERE nombre = 'OCs (personajes originales)' AND descripcion IS NULL;
UPDATE tema_termino SET descripcion = 'Creación de un personaje desde cero.' WHERE nombre = 'Diseño de personajes' AND descripcion IS NULL;
UPDATE tema_termino SET descripcion = 'Ilustración de entornos y paisajes.' WHERE nombre = 'Fondos / escenarios' AND descripcion IS NULL;
UPDATE tema_termino SET descripcion = 'Estilo chibi (proporciones cabezonas).' WHERE nombre = 'Chibi' AND descripcion IS NULL;
UPDATE tema_termino SET descripcion = 'Animaciones o frames animados.' WHERE nombre = 'Animación' AND descripcion IS NULL;

-- Plazos
ALTER TABLE terminos ADD COLUMN IF NOT EXISTS tiempo_entrega VARCHAR(120);
ALTER TABLE terminos ADD COLUMN IF NOT EXISTS tiempo_respuesta VARCHAR(120);

-- termino_derecho: disponible + porcentaje extra
ALTER TABLE termino_derecho ADD COLUMN IF NOT EXISTS porcentaje_extra NUMERIC(6,2);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'termino_derecho' AND column_name = 'otorgado'
    ) THEN
        ALTER TABLE termino_derecho RENAME COLUMN otorgado TO disponible;
    END IF;
END $$;

-- termino_tema: id_tema_termino opcional + nombre personalizado (temas propios del artista)
ALTER TABLE termino_tema ALTER COLUMN id_tema_termino DROP NOT NULL;
ALTER TABLE termino_tema ADD COLUMN IF NOT EXISTS nombre_personalizado VARCHAR(120);
