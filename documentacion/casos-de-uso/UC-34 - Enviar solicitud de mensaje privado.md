| UC–34  | Enviar solicitud de mensaje privado |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores • OBJ–03 Mejorar la protección de productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes. |  |
| **Descripción**  | El caso de uso inicia cuando un usuario desea enviar una solicitud para mandar un mensaje privado a otro usuario. |  |
| **Precondición**  | • El usuario inició sesión. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El usuario selecciona la opción “Enviar solicitud de mensaje privado”. |
|  | 2 | El sistema solicita que elija una de estas opciones: Enviar solicitud de mensaje a un artista. Enviar solicitud de mensaje a un usuario no artista. |
|  | 3 | El usuario elige la opción que desea (Ver flujo de eventos alternativos) |
|  | 4 | El sistema muestra la información pública del usuario. |
|  | 5 | El sistema solicita que se ingrese obligatoriamente: Motivo de contacto. Un primer mensaje. |
|  | 6 | El usuario completa los datos solicitados y confirma su intención de enviar una solicitud de mensaje. |
|  | 7 | El sistema valida que no hayan campos obligatorios vacíos. |
|  | 8 | El sistema guarda la solicitud y notifica al destinatario. |
|  | 9 | El sistema informa que el proceso fue un éxito. |
| **Postcondición**  | Se registró una solicitud de mensaje con estado “Pendiente” |  |
| **Flujo de eventos alternativos 1** | 3.1.1 | El usuario elige enviar un mensaje a un artista. |
|  | 3.1.2 | El sistema solicita que ingrese un artista. |
|  | 3.1.2 | El usuario elige un artista (Ver UC-23 “Buscar artista) |
| **Flujo de eventos alternativos 2** | 3.2.1 | El usuario elige enviar un mensaje a un usuario no artista. |
|  | 3.2.2 | El sistema solicita que ingrese un usuario. |
|  | 3.2.3 | El usuario elige un usuario (Ver UC-36 “Buscar usuario no artista”) |
| **Excepciones**  | **Paso**  |  |
|  | 7 | Si hay campos obligatorios vacíos, el sistema notifica al usuario y regresa al paso 5 sin borrar los datos ya ingresados. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 1 | 1 segundo |
|  | 7 | 1 segundo |
| **Frecuencia**  | 10 veces al día |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

20. 

