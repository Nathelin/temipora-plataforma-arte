-- V94: endurece el modelo de entregables.
--
-- 1. Índice de apoyo sobre la FK id_variante (Trabajo 92 no lo creó y las
--    consultas por variante lo necesitan; además PostgreSQL no indexa FKs solo).
-- 2. Un mismo archivo no puede entregarse dos veces dentro del alcance COMUN
--    del mismo producto: sería entregar la obra duplicada.
--
-- No se toca la unicidad por variante: la misma imagen de la galería puede
-- entregarse junto a varias variantes sin ser un error.

CREATE INDEX IF NOT EXISTS idx_entregable_variante
    ON producto_entregable (id_variante)
    WHERE id_variante IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_entregable_comun_archivo
    ON producto_entregable (id_producto, clave_archivo)
    WHERE alcance = 'COMUN';
