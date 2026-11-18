-- V96: una imagen por variante y esa imagen es el entregable.
--
-- Decisiones:
--  * El entregable de una variante es SIEMPRE su imagen. No se elige de la
--    galería ni se puede quitar, así el hash del ledger (hash_imagen) es
--    exactamente el hash del archivo que recibe el comprador.
--  * Se elimina `producto_entregable`: era una copia derivable de
--    producto_variante. El espacio para multi-archivo se deja en el código
--    (un único método que hoy devuelve el original de la variante).
--  * Dos variantes del mismo producto no pueden compartir imagen: cerraba un
--    agujero donde una pieza única podía entregarse también como ilimitada.
--  * Variantes de más de un stock guardan historial de imágenes; las únicas
--    no cambian (el ledger ya lo impide).

DROP TABLE IF EXISTS producto_entregable;

CREATE UNIQUE INDEX IF NOT EXISTS uk_variante_producto_sha
    ON producto_variante (id_producto, sha256)
    WHERE estado <> 'ARCHIVADO'
      AND sha256 IS NOT NULL;

CREATE TABLE IF NOT EXISTS producto_variante_imagen (
    id_imagen_variante BIGSERIAL PRIMARY KEY,
    id_variante BIGINT NOT NULL,
    numero INTEGER NOT NULL,
    url VARCHAR(255) NOT NULL,
    sha256 CHAR(64) NOT NULL,
    clave_archivo_original VARCHAR(255),
    vigente BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_desde TIMESTAMP NOT NULL DEFAULT now(),
    fecha_hasta TIMESTAMP,
    id_usuario_cambio BIGINT,
    CONSTRAINT fk_imagen_variante_variante FOREIGN KEY (id_variante)
        REFERENCES producto_variante (id_variante) ON DELETE CASCADE,
    CONSTRAINT fk_imagen_variante_usuario FOREIGN KEY (id_usuario_cambio)
        REFERENCES usuario (id_usuario),
    CONSTRAINT uk_imagen_variante_numero UNIQUE (id_variante, numero),
    CONSTRAINT ck_imagen_variante_numero CHECK (numero > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_imagen_variante_vigente
    ON producto_variante_imagen (id_variante)
    WHERE vigente = TRUE;

CREATE INDEX IF NOT EXISTS idx_imagen_variante
    ON producto_variante_imagen (id_variante, vigente);

COMMENT ON TABLE producto_variante_imagen IS
    'Historial de imágenes de una variante. Cada cambio crea una versión nueva.';
COMMENT ON COLUMN producto_variante_imagen.vigente IS
    'Solo una fila vigente por variante (índice único parcial).';

-- Backfill: cada variante existente arranca su historial con su imagen actual.
INSERT INTO producto_variante_imagen (
    id_variante, numero, url, sha256, clave_archivo_original, vigente, fecha_desde)
SELECT v.id_variante,
       1,
       v.imagen_url,
       v.sha256,
       v.clave_archivo_original,
       TRUE,
       COALESCE(v.fecha_creacion, now())
FROM producto_variante v
WHERE v.imagen_url IS NOT NULL
  AND v.sha256 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM producto_variante_imagen i WHERE i.id_variante = v.id_variante
  );
