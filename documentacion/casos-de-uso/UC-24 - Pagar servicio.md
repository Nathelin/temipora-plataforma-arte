| UC–24  | Pagar servicio |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes. • IRQ-04 Información sobre productos • IRQ–05 Información sobre servicios • IRQ–06 Información sobre pedidos. • IRQ–11 Información sobre solicitudes de servicio. |  |
| **Descripción**  | El caso de uso inicia cuando un cliente desea pagar un servicio luego de que un artista envíe el presupuesto de la solicitud de servicio que realizó. |  |
| **Precondición**  | • El cliente inició sesión. • La solicitud de servicio que desea abonar se encuentra en estado “Presupuestado” |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El cliente selecciona la opción “Pagar servicio”. |
|  | 2 | El sistema solicita que se seleccione la solicitud de servicio a pagar. |
|  | 3 | El cliente selecciona la solicitud de servicio (Ver UC-20 “Buscar solicitud de servicio) |
|  | 4 | El sistema despliega los datos de la solicitud de servicio: Artista asociado. Servicio asociado. Precio final. Tiempo estimado de entrega. |
|  | 5 | El usuario confirma su intención de pagar (Ver UC-27 “Procesar pago”) |
|  | 6 | El sistema registra el pago y notifica al cliente y artista de que se realizó un pago por el servicio. |
| **Postcondición**  | Se registra un pedido en el sistema. La solicitud de servicio cambia su estado a “Pagada”. |  |
| **Excepciones**  | **Paso**  |  |
|  | 5.1 | Si el cliente cancela el proceso, el caso de uso finaliza. |
|  |  |  |
|  |  |  |
|  | 5.2 | Si el caso de uso UC-27 retorna un estado de rechazo, se informa al cliente y el caso de uso finaliza. |
| **Rendimiento** | **Paso** |  |
|  | 4 | 1 segundo |
|  | 6  | 1 segundo |
| **Frecuencia**  | 30 veces al día. |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  | En el futuro se planea añadir métodos alternativos de pago. Por el momento solo se acepta Mercado Pago. |  |

10. 

