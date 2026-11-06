-- El peso de los artistas que el cliente sigue pasa a ser una porción del
-- puntaje total (máximo 30%), no un bonus que podía superar el 100%.

UPDATE configuracion_sustitucion
   SET bonus_favorito = 0.300
 WHERE bonus_favorito > 0.300;

ALTER TABLE configuracion_sustitucion
    DROP CONSTRAINT IF EXISTS ck_configuracion_sustitucion_rango;
ALTER TABLE configuracion_sustitucion
    ADD CONSTRAINT ck_configuracion_sustitucion_rango
    CHECK (max_alternativas BETWEEN 1 AND 20
       AND max_por_artista BETWEEN 1 AND 5
       AND rango_precio_pct BETWEEN 0 AND 200
       AND bonus_favorito BETWEEN 0 AND 0.300
       AND factor_tiempo_max BETWEEN 1 AND 10
       AND minutos_dedupe BETWEEN 0 AND 1440
       AND dias_retencion >= 0
       AND email_cierre_minutos BETWEEN 0 AND 1440
       AND email_cierre_max BETWEEN 1 AND 20
       AND email_cierre_vigencia_horas BETWEEN 1 AND 720);
