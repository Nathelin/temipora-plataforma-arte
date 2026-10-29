-- Actualiza las etiquetas de descripción del catálogo de reseñas de clientes.

UPDATE opcion_criterio o
SET etiqueta = 'Descripciones poco claras'
FROM criterio_resenia c
WHERE o.id_criterio = c.id_criterio_resenia
  AND c.ambito = 'CLIENTE'
  AND c.nombre = 'Aspectos destacados'
  AND o.etiqueta = 'descripciones poco claras';
