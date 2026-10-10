| UC–25  | Cancelar pedido |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores • OBJ–03 Mejorar la protección de productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes. • IRQ–05 Información sobre servicios • IRQ–06 Información sobre pedidos. |  |
| **Descripción**  | El caso de uso inicia cuando un cliente desea cancelar un pedido realizado hacia algún artista. |  |
| **Precondición**  | • El usuario inició sesión. • El pedido que desea cancelar existe y no se encuentra en estado “Finalizado”. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El cliente selecciona la opción “Cancelar pedido”. |
|  | 2 | El sistema solicita que se seleccione un pedido. |
|  | 3 | El cliente selecciona un pedido (Ver UC-17 “Buscar pedido”) |
|  | 4 | El cliente solicita su cancelación. |
|  | 5 | El sistema verifica si la política de cancelación definida por el artista para determinar si debe devolver o no el dinero pagado. |
|  | 6 | El sistema enseña al usuario los detalles del pedido y la política de cancelación del artista. |
|  | 7 | El cliente confirma su intención de cancelar el pedido. |
|  | 8 | El sistema da de baja el pedido e informa al cliente que el proceso fue un éxito. |
|  | 9 | El sistema notifica al artista de la cancelación del pedido. |
| **Postcondición**  | El estado de un pedido fue modificado a “Cancelado”. |  |
| **Excepciones**  | **Paso**  |  |
|  | 7 | Si el cliente decide no cancelar el pedido, el caso de uso finaliza. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 5 | 1 segundo |
|  | 8 | 1 segundo |
| **Frecuencia**  | 1 vez al día.  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

11. 

