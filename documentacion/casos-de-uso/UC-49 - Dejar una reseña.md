| UC–49 | Dejar una reseña |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores • OBJ–02 Formalizar el seguimiento de arte a medida • OBJ–03 Mejorar la protección de productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes • IRQ-04 Información sobre productos • IRQ-05 Información sobre servicios • IRQ-06 Información sobre pedidos • IRQ-07 Información sobre ventas • IRQ-14 Información sobre reseñas |  |
| **Descripción**  | El caso de uso inicia cuando un cliente desea reseñar un pedido o producto que recibió. |  |
| **Precondición**  | • El cliente debe haber iniciado sesión. • Si el cliente reseña un servicio, el pedido asociado debe estar finalizado y se debe haber dado de alta la entrega final. • Si el cliente reseña un producto, este debe estar pagado y recibido. |  |
| **Secuencia**  | **Paso**  | **Acción**  |
| **normal**  | 1  | El cliente selecciona la opción “Dejar una reseña”. |
|  | 2 | El sistema solicita que se indique: El servicio de un pedido finalizado. Un producto comprado. A reseñar. |
|  | 3 | El cliente elige reseñar un producto (Ver UC-04 “Buscar producto”). |
|  | 4 | El sistema muestra los datos del producto: Título Descripción Tipo Precio Imagen Fecha de compra |
|  | 5 | El sistema solicita que se ingresen los siguientes datos obligatorios para dejar una reseña: Título Descripción Calificación de una a cinco estrellas. |
|  | 6 | El cliente ingresa los datos solicitados y confirma su intención de dejar una reseña. |
|  | 7 | El sistema valida que se hayan completado los campos obligatorios. |
|  | 8 | El sistema publica la reseña, notifica al artista, e informa que el proceso fue un éxito. |
| **Postcondición**  | Se ha publicado una reseña asociada a un producto o un servicio relacionado a un pedido finalizado. |  |
| **Flujo de eventos alternativos** | 3.1 | Si el cliente decide reseñar el servicio de un pedido finalizado, selecciona un pedido finalizado (Ver UC-17 “Buscar pedido”). Se obtendrá el servicio asociado al pedido. |
|  | 3.2 | El sistema muestra los datos del pedido: Artista asociado Servicio asociado: Nombre del servicio Descripción Precio Imagen de la entrega final Se continúa en el paso 5\. |
| **Excepciones**  | **Paso**  | **Acción**  |
|  | 7 | Si el sistema detecta que existen campos obligatorios vacíos, se notifica al cliente y se regresa al paso 4 si se encuentra reseñando un producto o al paso 3.2 si reseña un pedido de un servicio. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  | **Cota de tiempo**  |
|  | 7  | 1 segundo  |
|  | 8 | 1 segundo  |
| **Frecuencia**  | 10 veces al día |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

