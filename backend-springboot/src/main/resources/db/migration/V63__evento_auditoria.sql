-- Registro de acciones administrativas. Append-only: la aplicación solo inserta.
CREATE TABLE evento_auditoria (
    id_evento_auditoria BIGSERIAL PRIMARY KEY,
    ocurrido_en TIMESTAMP NOT NULL DEFAULT now(),
    actor_id BIGINT,
    actor_rol VARCHAR(20),
    actor_nombre VARCHAR(60),
    accion VARCHAR(60) NOT NULL,
    entidad VARCHAR(60),
    entidad_id VARCHAR(60),
    resumen VARCHAR(500) NOT NULL,
    cambios TEXT NOT NULL DEFAULT '[]',
    motivo VARCHAR(500),
    ip VARCHAR(45),
    user_agent VARCHAR(300),
    request_id VARCHAR(36),
    origen VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    resultado VARCHAR(20) NOT NULL DEFAULT 'OK'
);

CREATE INDEX idx_evento_auditoria_fecha
    ON evento_auditoria (ocurrido_en DESC);

CREATE INDEX idx_evento_auditoria_actor
    ON evento_auditoria (actor_id, ocurrido_en DESC);

CREATE INDEX idx_evento_auditoria_entidad
    ON evento_auditoria (entidad, entidad_id);

CREATE INDEX idx_evento_auditoria_accion
    ON evento_auditoria (accion, ocurrido_en DESC);
