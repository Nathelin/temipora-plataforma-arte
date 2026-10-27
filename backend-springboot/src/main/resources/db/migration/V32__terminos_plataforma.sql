-- Términos y condiciones de la plataforma, versionados.
-- Cada publicación crea una versión nueva e inmutable con la comisión vigente
-- congelada. El huérfano "AcceptacionTOS" (entidad TOSTemipora) se descarta:
-- la aceptación por usuario se registra junto al pedido en la fase de pagos.

DROP TABLE IF EXISTS "AcceptacionTOS";

CREATE TABLE IF NOT EXISTS terminos_plataforma (
    id_terminos_plataforma BIGSERIAL PRIMARY KEY,
    version INTEGER NOT NULL,
    titulo VARCHAR(120) NOT NULL,
    contenido TEXT NOT NULL,
    comision_servicio NUMERIC(5,2) NOT NULL,
    comision_producto NUMERIC(5,2) NOT NULL,
    fecha_publicacion TIMESTAMP NOT NULL DEFAULT now(),
    vigente BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_terminos_plataforma_version
    ON terminos_plataforma (version);
CREATE INDEX IF NOT EXISTS idx_terminos_plataforma_vigente
    ON terminos_plataforma (vigente);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_terminos_plataforma_comisiones'
    ) THEN
        ALTER TABLE terminos_plataforma ADD CONSTRAINT ck_terminos_plataforma_comisiones
            CHECK (comision_servicio BETWEEN 0 AND 50
               AND comision_producto BETWEEN 0 AND 50);
    END IF;
END $$;

INSERT INTO terminos_plataforma (
    version, titulo, contenido, comision_servicio, comision_producto,
    fecha_publicacion, vigente
)
SELECT
    1,
    'Términos y condiciones de Temipora',
    $tos$1. Aceptación de los términos
Al utilizar Temipora aceptás estos términos. Si no estás de acuerdo, no utilices la plataforma.

2. Requisitos de edad
Para crear una cuenta y usar Temipora tenés que ser mayor de 18 años. Al registrarte declarás que cumplís este requisito y que la fecha de nacimiento que informás es correcta.

3. Comisión de la plataforma
Temipora descuenta una comisión sobre el precio que recibe el artista. El porcentaje vigente se informa en el presupuesto antes del pago y en esta página. El cliente paga el precio final del presupuesto sin recargos adicionales por comisión.

4. Hitos y pagos
El artista puede dividir el trabajo en hitos. Cada hito se paga antes de que el artista comience esa etapa. El cliente aprueba cada entregable; si no responde dentro del plazo informado, el hito se aprueba automáticamente. Si el cliente no paga la cuota siguiente dentro del plazo, el pedido se cierra y el artista conserva los importes ya cobrados por el trabajo entregado.

5. Cancelaciones y devoluciones
Las devoluciones se evalúan según la política de cancelación informada por el artista y se procesan por la plataforma.

6. Contenido y propiedad intelectual
Los artistas conservan los derechos sobre sus obras. Al subir contenido, garantizás que tenés los derechos necesarios para publicarlo.

7. Responsabilidad
La plataforma se ofrece tal cual. No nos hacemos responsables por daños derivados del uso de la misma.$tos$,
    COALESCE((SELECT porcentaje_servicio FROM configuracion_comision WHERE id_configuracion = 1), 10.00),
    COALESCE((SELECT porcentaje_producto FROM configuracion_comision WHERE id_configuracion = 1), 10.00),
    now(),
    TRUE
WHERE NOT EXISTS (SELECT 1 FROM terminos_plataforma);
