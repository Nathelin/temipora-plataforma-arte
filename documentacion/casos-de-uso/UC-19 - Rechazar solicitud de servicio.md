| UC–19  | Rechazar solicitud de servicio |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores • OBJ–02 Formalizar el seguimiento de arte a medida • OBJ–03 Mejorar la protección de productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes • IRQ–06 Información sobre pedidos. • IRQ–11 Información sobre solicitudes de servicio. |  |
| **Descripción**  | El caso de uso inicia cuando un artista desea rechazar una solicitud de servicio enviada por un cliente potencial. |  |
| **Precondición**  | • El usuario inició sesión en el sistema. • La solicitud de servicio que el artista desea rechazar existe en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El artista solicita rechazar una solicitud de servicio. |
|  | 2 | El sistema solicita que seleccione la solicitud de servicio que desea presupuestar. |
|  | 3 | El artista elige la solicitud (Ver UC-20 “Buscar solicitud de servicio”) |
|  | 4 | El sistema despliega los detalles de la solicitud enviada por el cliente junto con el precio base del servicio. |
|  | 5 | El artista selecciona rechazar la solicitud de servicio y opcionalmente redacta un mensaje con el motivo. |
|  | 6 | El sistema solicita que el artista confirme su intención de rechazar la solicitud de servicio. |
|  | 7 | El artista confirma su intención. |
|  | 8 | El sistema cambia el estado de la solicitud de servicio a “Rechazada” e informa al cliente. |
|  | 9 | El sistema informa que el proceso fue un éxito. |
| **Postcondición**  | La solicitud de servicio cambió su Estado a “Rechazada”. |  |
| **Excepciones**  | **Paso**  |  |
|  |  |  |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 4  | 1 segundo |
|  | 8 | 1 segundo |
|  | 9 | 1 segundo |
| **Frecuencia**  | 3 veces al día  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

7. 

