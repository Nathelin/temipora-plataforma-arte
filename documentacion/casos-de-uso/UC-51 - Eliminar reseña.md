| UC–51  | Eliminar reseña |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores • OBJ–02 Formalizar el seguimiento de arte a medida • OBJ–03 Mejorar la protección de productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes • IRQ-04 Información sobre productos • IRQ-05 Información sobre servicios • IRQ-06 Información sobre pedidos • IRQ-07 Información sobre ventas • IRQ-14 Información sobre reseñas |  |
| **Descripción**  | El caso de uso inicia cuando un cliente desea eliminar una reseña que dejó en un producto o pedido de servicio finalizado. |  |
| **Precondición**  | • El cliente debe haber iniciado sesión. • Debe existir la reseña que el cliente desea eliminar. |  |
| **Secuencia**  | **Paso**  | **Acción**  |
| **normal**  | 1  | El cliente selecciona la opción “Eliminar una reseña” |
|  | 2 | El sistema solicita que se indique la reseña a eliminar. |
|  | 3 | El usuario selecciona la reseña (Ver UC-52 “Buscar reseña”) |
|  | 4 | El sistema despliega la información de la reseña: Producto/Pedido asociado Título registrado Descripción registrada Calificación de una a cinco estrellas registrada |
|  | 5 | El cliente solicita eliminar la reseña. |
|  | 6 | El sistema solicita confirmación para realizar este proceso. |
|  | 7 | El cliente confirma su intención de eliminar la reseña. |
|  | 8 | El sistema elimina la reseña e informa que el proceso fue un éxito. |
| **Postcondición**  | Se realizó una baja lógica de la reseña. |  |
| **Excepciones**  | **Paso**  | **Acción**  |
|  |  |  |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  | **Cota de tiempo**  |
|  | 8  | 1 segundo  |
| **Frecuencia**  | 10 veces al mes  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

