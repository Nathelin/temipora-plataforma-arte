| UC–27  | Procesar pago |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes. • IRQ-04 Información sobre productos • IRQ–05 Información sobre servicios • IRQ–06 Información sobre pedidos. |  |
| **Descripción**  | El caso de uso inicia cuando un cliente solicita procesar el pago de un producto o servicio. |  |
| **Precondición**  | • Un cliente solicitó pagar un producto o servicio. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El caso de uso inicia cuando el sistema envía la solicitud de pago al sistema externo Mercado Pago luego de que un cliente confirme que desea pagar un producto o servicio. |
|  | 2 | Mercado Pago valida la operación de compra y retorna una respuesta. |
|  | 3 | El sistema comunica al usuario que el pago fue exitoso. |
| **Postcondición**  | Se registró el pago de un producto o servicio. |  |
| **Excepciones**  | **Paso**  |  |
|  | 2 | Si el pago es rechazado, se informa al cliente y el caso de uso finaliza. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 3 | 1 segundo |
| **Frecuencia**  | 30 veces al día. |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

13. 

