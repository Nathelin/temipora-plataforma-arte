| UC–35 | Enviar mensaje privado |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores • OBJ–03 Mejorar la protección de productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes. |  |
| **Descripción**  | El caso de uso inicia cuando un usuario desea enviar un mensaje privado a otro usuario. |  |
| **Precondición**  | • El usuario inició sesión. • La solicitud de envío de mensaje privado asociada a ambos usuarios tiene Estado “Aceptada”. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El usuario solicita “Enviar un mensaje privado” |
|  | 2 | El sistema solicita que ingrese una de estas opciones: Enviar un mensaje privado a un artista Enviar un mensaje privado a un usuario no artista. |
|  | 3 | El usuario elige la opción que desea (Ver flujo de eventos alternativos) |
|  | 4 | El sistema muestra el historial de mensajes y el campo de texto para enviar un nuevo mensaje. |
|  | 5 | El usuario redacta el mensaje. |
|  | 6 | El usuario selecciona “Enviar”. |
|  | 7 | El sistema valida que el mensaje no esté vacío ni supere el límite de caracteres. |
|  | 8 | El sistema envía el mensaje, lo muestra en pantalla y notifica al usuario destinatario. |
| **Postcondición**  | El mensaje se guarda en el historial de la conversación. |  |
| **Flujo de eventos alternativos 1** | 3.1.1 | El usuario elige enviar un mensaje a un artista. |
|  | 3.1.2 | El sistema solicita que ingrese un artista. |
|  | 3.1.2 | El usuario elige un artista (Ver UC-23 “Buscar artista) |
| **Flujo de eventos alternativos 2** | 3.2.1 | El usuario elige enviar un mensaje a un usuario no artista. |
|  | 3.2.2 | El sistema solicita que ingrese un usuario. |
|  | 3.2.3 | El usuario elige un usuario (Ver UC-36 “Buscar usuario no artista”) |
| **Excepciones**  | **Paso**  |  |
|  | 7.1 | Si el mensaje está vacío, notifica al usuario y regresa al paso 4\. |
|  |  |  |
|  |  |  |
|  | 7.2 | Si el mensaje supera el límite de caracteres, notifica al usuario y regresa al paso 4 sin eliminar lo que ya está escrito. |
| **Rendimiento**  | **Paso**  |  |
|  | 7 | 1 segundo |
|  | 8 | 1 segundo |
| **Frecuencia**  | 100 al día |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  | Debo definir un límite de caracteres. Por el momento considero 300\. |  |

21. 

