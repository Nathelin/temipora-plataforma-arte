| UC–50  | Modificar una reseña |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores • OBJ–02 Formalizar el seguimiento de arte a medida • OBJ–03 Mejorar la protección de productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes • IRQ-04 Información sobre productos • IRQ-05 Información sobre servicios • IRQ-06 Información sobre pedidos • IRQ-07 Información sobre ventas • IRQ-14 Información sobre reseñas |  |
| **Descripción**  | El caso de uso inicia cuando un cliente desea modificar una reseña que dejó en un pedido o producto recibido. |  |
| **Precondición**  | • El cliente debe haber iniciado sesión. • Debe existir la reseña que el cliente desea modificar. |  |
| **Secuencia**  | **Paso**  | **Acción**  |
| **normal**  | 1  | El cliente selecciona la opción “Modificar una reseña” |
|  | 2 | El sistema solicita que se indique la reseña a modificar. |
|  | 3 | El usuario selecciona la reseña (Ver UC-52 “Buscar reseña”) |
|  | 4 | El sistema despliega la información de la reseña: Producto/Servicio asociado al pedido Título registrado Descripción registrada Calificación de una a cinco estrellas registrada |
|  | 5 | El sistema permite modificar los siguientes datos. Ninguno de estos campos puede permanecer vacío: Título  Descripción  Calificación de una a cinco estrellas  |
|  | 6 | El cliente modifica los datos deseados y confirma su intención de guardar la reseña. |
|  | 7 | El sistema valida que se hayan completado los campos obligatorios. |
|  | 8 | El sistema publica la reseña, notifica al artista, e informa que el proceso fue un éxito. |
| **Postcondición**  | Se modificó una reseña registrada en el sistema. |  |
| **Excepciones**  | **Paso**  | **Acción**  |
|  | 7 | Si el sistema detecta que existen campos obligatorios vacíos, se notifica al cliente y se regresa al paso 5 sin borrar los datos ingresados. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  | **Cota de tiempo**  |
|  | 7  | 1 segundo  |
|  | 8 | 1 segundo  |
| **Frecuencia**  | 10 veces al mes  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

