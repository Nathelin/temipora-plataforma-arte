-- Columna base que la migración de seed V62 necesita para resolver el creador
-- de las etiquetas por nombre de usuario.
--
-- Igual que V0_1, este archivo existe porque Flyway corre ANTES que Hibernate:
-- en una base limpia (por ejemplo CI) la tabla `usuario` sólo tiene id_usuario
-- hasta que Hibernate completa el resto de columnas con ddl-auto=update. Sin
-- esta columna, V62 falla con: column "nombre_usuario" does not exist.
--
-- El tipo coincide con el mapeo de la entidad Usuario (nombreUsuario, máx. 25).
-- Idempotente: sobre una base ya inicializada (por ejemplo tu entorno local, que
-- ya tiene la columna) no hace nada.

ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS nombre_usuario VARCHAR(25);
