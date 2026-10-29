-- Actualiza las etiquetas de descripción del catálogo de reseñas de clientes.
UPDATE opcion_criterio o
SET etiqueta = 'Descripciones claras'
FROM criterio_resenia c
WHERE o.id_criterio = c.id_criterio_resenia
  AND c.ambito = 'CLIENTE'
  AND c.nombre = 'Aspectos destacados'
  AND o.etiqueta = 'Brief claro';

UPDATE opcion_criterio o
SET etiqueta = 'descripciones poco claras'
FROM criterio_resenia c
WHERE o.id_criterio = c.id_criterio_resenia
  AND c.ambito = 'CLIENTE'
  AND c.nombre = 'Aspectos destacados'
  AND o.etiqueta = 'Brief poco claro';
