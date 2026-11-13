-- V110: anclaje externo del registro de autenticidad (OpenTimestamps).
--
-- El historial interno (evento_ledger) es inmutable por trigger; por eso el
-- anclaje vive en tablas nuevas, append-only, que referencian las filas del
-- ledger sin modificarlas.
--
-- Cada "ancla" cubre un lote de eventos: se calcula una raiz de Merkle sobre
-- sus hashes y se sella con OpenTimestamps (que la agrupa en la blockchain de
-- Bitcoin). Guardamos la prueba (.ots) y, al confirmar, la altura del bloque.

CREATE TABLE IF NOT EXISTS anclaje_ledger (
    id_anclaje BIGSERIAL PRIMARY KEY,
    hash_raiz CHAR(64) NOT NULL,
    cantidad_eventos INTEGER NOT NULL,
    id_evento_desde BIGINT,
    id_evento_hasta BIGINT,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    prueba_ots BYTEA,
    prueba_confirmada BYTEA,
    calendario_url VARCHAR(255),
    hash_bitcoin CHAR(64),
    bitcoin_altura BIGINT,
    fecha_solicitud TIMESTAMP NOT NULL DEFAULT now(),
    fecha_confirmacion TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    id_evento_auditoria BIGINT,
    CONSTRAINT ck_anclaje_estado
        CHECK (estado IN ('PENDIENTE', 'CONFIRMADO', 'FALLIDO')),
    CONSTRAINT uk_anclaje_hash_raiz UNIQUE (hash_raiz)
);

CREATE INDEX IF NOT EXISTS idx_anclaje_estado
    ON anclaje_ledger (estado, id_anclaje);

CREATE TABLE IF NOT EXISTS anclaje_ledger_evento (
    id_anclaje_evento BIGSERIAL PRIMARY KEY,
    id_anclaje BIGINT NOT NULL,
    id_evento_ledger BIGINT NOT NULL,
    posicion INTEGER NOT NULL,
    CONSTRAINT fk_anclaje_evento_anclaje FOREIGN KEY (id_anclaje)
        REFERENCES anclaje_ledger (id_anclaje) ON DELETE CASCADE,
    CONSTRAINT fk_anclaje_evento_ledger FOREIGN KEY (id_evento_ledger)
        REFERENCES evento_ledger (id_evento_ledger) ON DELETE RESTRICT,
    CONSTRAINT uk_anclaje_evento_ledger UNIQUE (id_anclaje, id_evento_ledger),
    CONSTRAINT uk_anclaje_evento_posicion UNIQUE (id_anclaje, posicion)
);

CREATE INDEX IF NOT EXISTS idx_anclaje_evento_evento
    ON anclaje_ledger_evento (id_evento_ledger);
