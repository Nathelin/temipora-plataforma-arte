| UC–33  | Modificar datos personales y preferencias |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores • OBJ–03 Mejorar la protección de productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes. |  |
| **Descripción**  | El caso de uso inicia cuando un usuario desea modificar sus datos personales y preferencias. |  |
| **Precondición**  | • El usuario inició sesión. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El usuario selecciona la opción “Modificar mis datos personales y preferencias”. |
|  | 2 | El sistema despliega los siguientes datos del usuario: Nombre artístico (si es un artista) Nombre Apellido Número de teléfono Correo electrónico Adicionalmente ofrece una opción para modificar la contraseña del usuario. El sistema también despliega las preferencias de la visualización del contenido sensible: Ocultar todo el contenido sensible. Mostrar advertencias ante contenido sensible (opción por defecto). Mostrar todo el contenido sensible sin advertencias. |
|  | 3 | El usuario modifica los datos que desea y solicita guardar los cambios. Ver flujo de eventos alternativos si el usuario modifica su contraseña. |
|  | 4 | Si el usuario modificó su correo electrónico o teléfono, el sistema verifica que no haya otro usuario con el mismo correo o teléfono. |
|  | 5 | El sistema verifica que no haya ningún campo de datos personales vacío. |
|  | 6 | El sistema guarda las modificaciones del usuario. |
| **Postcondición**  | Se modificaron los datos personales y/o preferencias del usuario. |  |
| **Flujo de eventos alternativos** | 3.1.1 | El sistema solicita que el usuario ingrese su contraseña actual y una contraseña nueva. |
|  | 3.1.2 | El usuario ingresa su contraseña actual y una nueva contraseña. |
|  | 3.1.3 | El sistema verifica que la contraseña actual sea correcta y que la nueva contraseña ingresada no sea la misma que la anterior. Se regresa al paso 4 de la secuencia normal. |
| **Excepciones**  | **Paso**  |  |
|  | 3.1.3 | Si la contraseña no es correcta o la nueva contraseña es repetida, se informa al usuario y se regresa al paso 3.1.1. |
|  |  |  |
|  |  |  |
|  | 4 | Si existe otro usuario con el mismo correo electrónico o teléfono, se comunica al usuario y se regresa al paso 2\. |
|  | 5 | Si existe algún campo vacío, se comunica al usuario y se regresa al paso 2\. |
| **Rendimiento**  | **Paso**  |  |
|  |  2 | 1 segundo |
|  | 3.1.3 | 1 segundo |
|  | 4 | 1 segundo |
|  | 5 | 1 segundo |
| **Frecuencia**  | 1 vez al día.  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

19. 

