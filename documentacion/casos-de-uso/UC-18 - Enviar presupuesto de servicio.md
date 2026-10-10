| UC–18  | Enviar presupuesto de servicio |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores • OBJ–02 Formalizar el seguimiento de arte a medida • OBJ–03 Mejorar la protección de productores y consumidores  |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes • IRQ–06 Información sobre pedidos. • IRQ–11 Información sobre solicitudes de servicio. |  |
| **Descripción**  | El caso de uso inicia cuando un artista desea presupuestar una solicitud de algún servicio y enviársela al potencial cliente. Esta solicitud presupuestada se convertirá en un pedido en caso de que el cliente la acepte. |  |
| **Precondición**  | • El usuario inició sesión en el sistema. • La solicitud de servicio que el artista desea presupuestar existe en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El artista selecciona la opción “Enviar presupuesto”. |
|  | 2 | El sistema solicita que seleccione la solicitud de servicio que desea presupuestar. |
|  | 3 | El artista elige la solicitud (Ver UC-20 “Buscar solicitud de servicio”) |
|  | 4 | El sistema despliega los detalles de la solicitud enviada por el cliente junto con el precio base del servicio. |
|  | 5 | El sistema solicita que se ingrese obligatoriamente: Tiempo máximo de entrega tras el pago. |
|  | 6 | El artista ingresa un tiempo máximo de entrega. |
|  | 7 | El sistema permite opcionalmente agregar un ítem de recargo. |
|  | 8 | Si es necesario agregar un ítem de recargo, el artista solicita ingresar un nuevo ítem de recargo. De no ser necesario, el caso de uso continúa en el paso 11\. |
|  | 9 | El sistema solicita que se ingrese los siguientes datos obligatorios para el nuevo ítem de recargo: Descripción Monto positivo fijo o porcentaje extra sobre el precio original. |
|  | 10 | El artista ingresa los datos solicitados. Se regresa al paso 7\. |
|  | 11 | El sistema muestra, calcula y actualiza dinámicamente el precio final sumando el precio base con los modificadores agregados en caso de existir alguno. |
|  | 12 | El sistema permite opcionalmente agregar un mensaje para el cliente. |
|  | 13 | El artista opcionalmente redacta un mensaje para el cliente y solicita al sistema que se envíe el presupuesto. |
|  | 14 | El sistema valida que no existan campos obligatorios vacíos. |
|  | 15 | El sistema guarda el presupuesto de la solicitud y notifica al cliente. |
|  | 16 | El sistema informa que el proceso fue un éxito. |
| **Postcondición**  | La solicitud de servicio cambió su Estado a “Presupuestado”. |  |
| **Excepciones**  | **Paso**  |  |
|  | 14.1 | Si el sistema detecta que no se definió tiempo máximo de entrega, se notifica al artista y se regresa al paso 5 sin borrar los datos ingresados. |
|  | 14.2 | Si el sistema detecta que se declararon ítems de recargo y estos tienen campos vacíos, se notifica al artista y se regresa al paso 9 sin borrar los datos ingresados. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 4  | 1 segundo |
|  | 14 | 1 segundo |
|  | 15 | 1 segundo |
| **Frecuencia**  | 30 veces al día |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

6. 

