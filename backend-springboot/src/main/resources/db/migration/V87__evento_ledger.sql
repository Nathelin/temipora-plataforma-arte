-- V86: ledger de génesis para piezas únicas.
-- Cada variante con stock 1 registra un evento GENESIS encadenado por hash.
-- El anclaje externo (OpenTimestamps) se incorpora en una fase posterior.

CREATE TABLE evento_ledger (
    id_evento_ledger BIGSERIAL PRIMARY KEY,
    id_variante BIGINT NOT NULL,
    secuencia BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    id_venta BIGINT,
    id_actor_origen BIGINT,
    id_actor_destino BIGINT,
    monto NUMERIC(12, 2),
    id_moneda BIGINT,
    ocurrido_en TIMESTAMP NOT NULL,
    datos TEXT,
    hash_previo VARCHAR(64) NOT NULL,
    hash_imagen VARCHAR(64),
    hash_evento VARCHAR(64) NOT NULL,
    id_evento_auditoria BIGINT,
    CONSTRAINT fk_ledger_variante FOREIGN KEY (id_variante)
        REFERENCES producto_variante (id_variante) ON DELETE RESTRICT,
    CONSTRAINT fk_ledger_actor_origen FOREIGN KEY (id_actor_origen)
        REFERENCES usuario (id_usuario),
    CONSTRAINT fk_ledger_actor_destino FOREIGN KEY (id_actor_destino)
        REFERENCES usuario (id_usuario),
    CONSTRAINT fk_ledger_moneda FOREIGN KEY (id_moneda)
        REFERENCES moneda (id_moneda),
    CONSTRAINT fk_ledger_evento_auditoria FOREIGN KEY (id_evento_auditoria)
        REFERENCES evento_auditoria (id_evento_auditoria),
    CONSTRAINT ck_ledger_tipo
        CHECK (tipo IN ('GENESIS', 'VENTA', 'TRANSFERENCIA', 'REVENTA',
                        'ANULACION')),
    CONSTRAINT uk_ledger_variante_secuencia
        UNIQUE (id_variante, secuencia),
    CONSTRAINT uk_ledger_hash_evento
        UNIQUE (hash_evento)
);

CREATE INDEX idx_ledger_variante_secuencia
    ON evento_ledger (id_variante, secuencia);

CREATE INDEX idx_ledger_ocurrido_en
    ON evento_ledger (ocurrido_en);
