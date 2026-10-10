| UC–16  | Alta entrega final |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–02 Formalizar el seguimiento de arte a medida • OBJ–03 Mejorar la protección de productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas. • IRQ–02 Información sobre clientes. • IRQ–05 Información sobre servicios. • IRQ–06 Información sobre pedidos. |  |
| **Descripción**  | El caso de uso inicia cuando un artista desea dar de alta la entrega final de un pedido. |  |
| **Precondición**  | • El artista inició sesión en el sistema. • El pedido asociado debe existir en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El artista solicita dar de alta un pedido finalizado. |
|  | 2 | El sistema solicita que seleccione el pedido. |
|  | 3 | El artista selecciona el pedido (Ver UC-17 “Buscar pedido) |
|  | 4 | El sistema muestra los datos del pedido y solicita que suba una imagen. |
|  | 5 | El artista sube una imagen. |
|  | 6 | El sistema solicita que el artista confirme su intención de subir la versión final del pedido. |
|  | 7 | El artista confirma. |
|  | 8 | El sistema da de alta la versión final del pedido y notifica al cliente. |
| **Postcondición**  | Se finalizó un pedido y se dio de alta la imagen resultante. |  |
| **Excepciones**  | **Paso**  |  |
|  | 6 | Si el artista no confirma, se regresa al paso 4\. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 4 | 1 segundo |
|  | 8 | 1 segundo |
| **Frecuencia**  | 10 veces al día. |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

5. 

