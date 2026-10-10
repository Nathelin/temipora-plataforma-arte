| UC–22  | Cancelar solicitud de servicio |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores • OBJ–03 Mejorar la protección de productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes. • IRQ–05 Información sobre servicios • IRQ–06 Información sobre pedidos. |  |
| **Descripción**  | El caso de uso inicia cuando un cliente desea anular una solicitud de servicio que envió a algún artista. |  |
| **Precondición**  | • El usuario inició sesión. • La solicitud que desea cancelar existe. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El cliente selecciona la opción “Anular solicitud de servicio”. |
|  | 2 | El sistema solicita que se seleccione la solicitud de servicio que desea anular. |
|  | 3 | El cliente selecciona la solicitud de servicio (Ver UC-20 “Buscar solicitud de servicio”) |
|  | 4 | El cliente solicita su anulación. |
|  | 5 | El sistema bloquea temporalmente el acceso del artista a la solicitud de servicio para evitar que lo acepte. |
|  | 6 | El sistema solicita la confirmación del cliente. |
|  | 7 | El cliente confirma su intención de anular la solicitud de servicio. |
|  | 8 | El sistema da de baja la solicitud de servicio e informa al cliente que el proceso fue exitoso. |
| **Postcondición**  | La solicitud de servicio cambia su estado a “Cancelada”. |  |
| **Excepciones**  | **Paso**  |  |
|  | 7 | Si el cliente decide no cancelar la solicitud, el caso de uso finaliza. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 8  | 1 segundo |
| **Frecuencia**  | 1 vez al día. |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  | Una solicitud de servicio no es un pedido. Estas solicitudes aún no han sido aceptadas por el artista, por ende, no se menciona la política de cancelaciones. |  |

9. 

