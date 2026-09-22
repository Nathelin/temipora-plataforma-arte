| UC–48  | Configurar formulario de pedidos |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores • OBJ–02 Formalizar el seguimiento de arte a medida |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ-05 Información sobre servicios • IRQ-06 Información sobre pedidos |  |
| **Descripción**  | El caso de uso inicia cuando un artista desea configurar un formulario de pedidos. Este será utilizado por defecto a la hora de dar de alta nuevos servicios a menos que el artista elija definir uno particular durante el alta de un servicio. |  |
| **Precondición**  | • El artista debe haber iniciado sesión. • El artista tiene un alta o modificación de un servicio en curso y decidió configurar el formulario para el mismo. |  |
| **Secuencia**  | **Paso**  | **Acción**  |
| **normal**  | 1  | El caso de uso inicia cuando un artista solicita configurar un formulario de pedidos durante el alta o modificación de un servicio. |
|  | 2 | El sistema enseña la información que contiene el formulario por defecto del artista, ya sea el global propio o el del sistema en caso de que no haya configurado uno global. |
|  | 3 | El sistema ofrece la opción de: Agregar campo a formulario Modificar campo de formulario Eliminar campo de formulario |
|  | 4 | De ser necesario, el artista solicita realizar una de esas opciones. De no ser necesario, el caso de uso continúa en el paso 6\. |
|  | 5 | El artista realiza la opción seleccionada: Ver UC-45 “Agregar campo de formulario” si solicita realizar esta acción. Ver UC-46 “Modificar campo de formulario” si solicita realizar esta acción. Ver UC-47 “Eliminar campo de formulario” si solicita realizar esta acción. Una vez realizada una acción, el caso de uso continúa en el paso 3\. |
|  | 6 | El artista solicita guardar los cambios. |
|  | 7 | El sistema verifica que exista al menos un campo en el formulario. |
|  | 8 | El sistema guarda los cambios del formulario a asociar a un servicio. |
| **Postcondición**  | El formulario de pedidos que será asociado a un servicio fue modificado. |  |
| **Excepciones**  | **Paso**  | **Acción**  |
|  | 7 | Si el sistema detecta que el artista borró todo los campos, informa al artista y sugiere que coloque al menos un cuadro de texto que pueda completar el potencial cliente. Se regresa al paso 3\. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  | **Cota de tiempo**  |
|  | 7  | 1 segundo  |
| **Frecuencia**  | 10 veces al mes  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

