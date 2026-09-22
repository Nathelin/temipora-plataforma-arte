| UC–47  | Modificar campo de formulario |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | IRQ-04 Información sobre servicios |  |
| **Descripción**  | El caso de uso inicia cuando un artista desea modificar un campo en específico de un formulario de pedidos. |  |
| **Precondición**  | • El administrador inició sesión en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El usuario solicita modificar un campo de su formulario de pedidos. |
|  | 2 | El sistema ofrece la opción de modificar el tipo de campo del formulario y/o la información obligatoria que contiene: Bloque de texto: Agregar un título. Casillas de verificación: Texto a mostrar en la casilla. Botones de opción: Texto a mostrar en la opción. Lista desplegable: Opciones. Imágen: Título de la imagen. Campo para subir imagen: Descripción de la imagen a subir. |
|  | 3 | El artista modifica la información que desea.. |
|  | 4 | El artista confirma su intención de guardar y modificar el campo. |
|  | 5 | El sistema verifica que no haya información obligatoria vacía según el tipo de campo que desea modificar. |
|  | 6 | El sistema actualiza el campo en el formulario del artista. |
| **Postcondición**  | Se modificó un campo de un formulario de pedidos. |  |
| **Excepciones**  | **Paso**  |  |
|  | 5 | Si se encuentran campos obligatorios vacíos, el sistema los resalta e informa al cliente de que es necesario completarlos. Los datos ya ingresados no son borrados y se regresa al paso 2\. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 5 | 1 segundo |
| **Frecuencia**  | 5 veces al día |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

