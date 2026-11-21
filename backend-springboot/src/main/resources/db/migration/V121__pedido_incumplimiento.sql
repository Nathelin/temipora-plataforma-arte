-- Cierre de pedidos por incumplimiento del artista (fase 7).
-- V36 creó ck_pedido_estado con cinco estados; se amplía con INCUMPLIMIENTO.

ALTER TABLE pedido DROP CONSTRAINT IF EXISTS ck_pedido_estado;

ALTER TABLE pedido ADD CONSTRAINT ck_pedido_estado
    CHECK (estado IN (
        'PENDIENTE_PAGO',
        'EN_CURSO',
        'FINALIZADO',
        'CANCELADO',
        'IMPAGO',
        'INCUMPLIMIENTO'));
