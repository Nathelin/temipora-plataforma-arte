| UC–45  | Agregar campo a formulario |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–05 Información sobre servicios • IRQ–06 Información sobre pedidos. • IRQ–10 Información sobre formularios de pedidos. • IRQ–11 Información sobre solicitudes de pedidos. |  |
| **Descripción**  | El caso de uso inicia cuando un artista desea añadir un nuevo campo a su formulario de pedidos. |  |
| **Precondición**  | • El artista inició sesión en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El usuario solicita añadir un nuevo campo a su formulario de pedidos. |
|  | 2 | El sistema solicita que seleccione el tipo de campo que desea: Bloque de texto Casillas de verificación Botones de opción Lista desplegable Imágen Campo para subir imagen |
|  | 3 | El artista selecciona el tipo de campo que desea. |
|  | 4 | El sistema solicita que se complete información obligatoria dependiendo del tipo de campo seleccionado: Bloque de texto: Agregar un título. Casillas de verificación: Texto a mostrar en la casilla. Botones de opción: Texto a mostrar en la opción. Lista desplegable: Opciones. Imagen: Título de la imagen. Campo para subir imagen: Descripción de la imagen a subir. |
|  | 5 | El artista confirma su intención de guardar y agregar el campo. |
|  | 6 | El sistema verifica que no haya información obligatoria vacía según el tipo de campo que desea agregar. |
|  | 7 | El sistema agrega el tipo de campo al formulario del artista. |
| **Postcondición**  | Se agregó un campo a un formulario de pedidos. |  |
| **Excepciones**  | **Paso**  |  |
|  | 6 | Si se encuentran campos obligatorios vacíos, el sistema los resalta e informa al cliente de que es necesario completarlos. Los datos ya ingresados no son borrados y se regresa al paso 4\. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 6 | 1 segundo  |
| **Frecuencia**  | 5 veces al día |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

33. 

