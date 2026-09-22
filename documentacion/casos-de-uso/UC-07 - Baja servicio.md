| UC–07  | Baja servicio |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores • OBJ–02 Formalizar el seguimiento de arte a medida |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ-05 Información sobre servicios • IRQ-06 Información sobre pedidos |  |
| **Descripción**  | El caso de uso inicia cuando un artista desea dar de baja uno de sus servicios. |  |
| **Precondición**  | • El artista inició sesión en el sistema. • El servicio que el artista desea dar de baja debe existir en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El artista selecciona la opción de “Dar de baja un servicio”. |
|  | 2 | El sistema solicita que seleccione un servicio a dar de baja. |
|  | 3 | El artista selecciona el servicio que desea dar de baja (Ver UC-08 “Buscar servicio”). |
|  | 4 | El sistema recupera la información del servicio elegido y se la enseña al artista. |
|  | 5 | El artista solicita al sistema la baja del servicio. |
|  | 6 | El sistema solicita la confirmación del artista. |
|  | 7 | El artista confirma su intención de dar de baja el servicio. |
|  | 8 | El sistema realiza una baja lógica y da de baja el servicio seleccionado, impidiendo que aparezca en búsquedas y perfil del artista. |
|  | 9 | El sistema informa al artista que el proceso fue un éxito. |
| **Postcondición**  | El servicio es dado de baja del sistema cambiando su estado de “Activo” a “Inactivo”. |  |
| **Excepciones**  | **Paso**  |  |
|  | 7 | Si el artista no confirma la baja del servicio, se regresa al paso 2\. |
|  |  |  |
|  |  |  |
|  | 8 | Si el servicio corresponde al de un pedido activo, se informa al artista que no puede ser dado de baja y se regresa al paso 2\. |
| **Rendimiento**  | **Paso**  |  |
|  | 4 | 1 segundo  |
|  | 8 | 1 segundo  |
| **Frecuencia**  | 1 vez/mes  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

      4. 

