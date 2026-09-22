| UC–06  | Modificar servicio |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores • OBJ–02 Formalizar el seguimiento de arte a medida |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ-05 Información sobre servicios |  |
| **Descripción**  | El caso de uso inicia cuando un artista decide modificar algún servicio que ofrece. Cada servicio cuenta con un nombre, descripción, tipo, imagen de portada, imagen de ejemplo, precio base, declara si contiene o no contenido sensible, un tiempo estimado de entrega, cantidad de cupos y términos de uso y servicio. |  |
| **Precondición**  | • El artista inició sesión en el sistema. • El servicio que se desea modificar existe en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El artista selecciona la opción de “Modificar un servicio”. |
|  | 2 | El sistema solicita que seleccione un servicio a modificar. |
|  | 3 | El artista selecciona el servicio que desea modificar (Ver UC-08 “Buscar servicio”). |
|  | 4 | El sistema recupera la información del servicio elegido y se la enseña al artista. |
|  | 5 | El artista elige el o los datos que desea modificar. Los siguientes deben obligatoriamente contener información: Nombre del servicio Descripción Tipo Imagen de portada Imagen/es de ejemplo Puede subir una imagen nueva. O puede elegir una imagen de un pedido finalizado. Precio base  Si presenta o no contenido sensible. Tiempo estimado de entrega Cantidad de cupos disponibles\* Términos de uso Términos de servicio Los siguientes son datos opcionales: Habilitar cupos nuevos tras la entrega de un pedido. Formulario de pedidos. Y modifica su contenido.  Si agrega una imagen de un pedido finalizado, Ver UC-17 “Buscar pedido”. Ver UC-47 “Configurar formulario de pedidos” si el artista decide configurar el formulario para este servicio en específico. |
|  | 6 | El artista guarda los cambios. |
|  | 7 | El sistema valida que no existan campos obligatorios vacíos. |
|  | 8 | El sistema verifica que el artista no posea otros servicios con igual nombre y tipo. |
|  | 9 | El sistema guarda los cambios y comunica al artista que el proceso fue exitoso. |
| **Postcondición**  | Se modificaron datos asociados a un servicio. |  |
| **Excepciones**  | **Paso**  |  |
|  | 7 | Si se encuentran campos obligatorios vacíos, el sistema los resalta e informa al artista de que es necesario completarlos. Los datos ya ingresados no son borrados y se regresa al paso 5\. |
|  |  |  |
|  |  |  |
|  | 8 | Si el sistema encuentra que el artista ya posee un servicio de iguales características, notifica al artista y regresa al paso 5\. |
| **Rendimiento**  | **Paso**  |  |
|  | 7 | 1 segundo  |
|  | 8 | 1 segundo |
| **Frecuencia**  | 1 vez/mes  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  | \*Si por algún motivo decide reducir/aumentarlos manualmente. |  |

