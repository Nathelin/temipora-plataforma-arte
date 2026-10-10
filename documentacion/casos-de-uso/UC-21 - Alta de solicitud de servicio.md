| UC–21  | Alta de solicitud de servicio |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores • OBJ–02 Formalizar el seguimiento de arte a medida • OBJ–03 Mejorar la protección de productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes • IRQ–06 Información sobre pedidos. • IRQ–11 Información sobre solicitudes de servicio. |  |
| **Descripción**  | El caso de uso inicia cuando un cliente desea enviar una solicitud de servicio a algún artista porque le interesa alguno de los servicios que ofrece. |  |
| **Precondición**  | • El cliente inició sesión en el sistema. • El servicio que desea incluir en su solicitud debe existir en el sistema. • El artista que ofrece ese servicio en particular debe existir en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El cliente selecciona la opción “Crear nueva solicitud de servicio”. |
|  | 2 | El sistema solicita que seleccione el servicio que desea consultar. |
|  | 3 | El cliente selecciona un servicio (Ver UC-08 “Buscar servicio”) |
|  | 4 | El sistema evalúa si el servicio tiene cupos abiertos. |
|  | 5 | El sistema muestra el formulario de pedidos definido para el servicio. |
|  | 6 | El cliente completa los datos solicitados en el formulario y solicita enviarlos. |
|  | 7 | El sistema evalúa que no haya campos obligatorios vacíos. |
|  | 8 | El sistema guarda la solicitud de servicio y notifica al artista. |
|  | 9 | El sistema informa que el proceso fue exitoso. |
| **Postcondición**  | Se dio de alta una nueva solicitud de servicio con Estado “Pendiente”. Si el servicio no cuenta con cupos abiertos, el sistema guarda la solicitud en la lista de espera del artista. |  |
| **Flujo de eventos alternativos** | 3.1.1 | El cliente opcionalmente puede escoger enviar una solicitud a un artista. Para ello selecciona un artista (Ver UC-23 “Buscar artista”). |
|  | 3.1.2 | El sistema enseña los servicios activos del artista. |
|  | 3.1.3 | El cliente elige uno de los servicios del artista (Ver UC-08 “Buscar servicio”). |
|  | 3.1.4 | Si el servicio tiene cupos disponibles, continúa desde el paso 5\. Si el servicio no tiene cupos disponibles, continúa desde el paso 5.1.1. |
|  | 5.1.1 | Si el servicio no presenta cupos abiertos, el sistema sugiere al cliente ingresar a la lista de espera. |
|  | 5.1.2 | El cliente acepta la sugerencia. |
|  | 5.1.3 | El sistema muestra el formulario de pedidos definido para el servicio. |
|  | 5.1.4 | El cliente completa los datos solicitados en el formulario y solicita enviarlos. |
|  | 5.1.5 | El sistema evalúa que no haya campos obligatorios vacíos. |
|  | 5.1.6 | El sistema guarda la solicitud de servicio en la lista de espera y notifica al artista. |
|  | 5.1.7 | El sistema informa que el proceso fue exitoso. |
| **Excepciones**  | **Paso**  |  |
|  | 7 | Si se encuentran campos obligatorios vacíos, el sistema los resalta e informa al cliente de que es necesario completarlos. Los datos ya ingresados no son borrados y se regresa al paso 5\. |
|  |  |  |
|  |  |  |
|  | 5.1.5 | Si se encuentran campos obligatorios vacíos, el sistema los resalta e informa al cliente de que es necesario completarlos. Los datos ya ingresados no son borrados y se regresa al paso 5.1.3. |
| **Rendimiento**  | **Paso**  |  |
|  | 3.1.2 | \< 1 segundo |
|  | 4 | 1 segundo |
|  | 5.1.3 | \< 1 segundo |
|  | 5.1.5 | \< 1 segundo |
|  | 7 | \< 1 segundo |
|  | 8 | 1 segundo |
|  | 9 | \< 1 segundo |
| **Frecuencia**  | 1 vez/mes  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  | El flujo alternativo ocurrirá normalmente cuando no inicia el recorrido desde el perfil de un artista. |  |

8. 

