-- Los formularios se versionan para que editarlos no altere las solicitudes
-- ya creadas. Cada solicitud conserva el formulario exacto que usó.

ALTER TABLE formulario
    ADD COLUMN IF NOT EXISTS vigente BOOLEAN NOT NULL DEFAULT TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_formulario_servicio_vigente
    ON formulario (id_artista, id_servicio)
    WHERE vigente AND es_sistema = false AND id_servicio IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_formulario_global_vigente
    ON formulario (id_artista)
    WHERE vigente AND es_sistema = false AND id_servicio IS NULL;
