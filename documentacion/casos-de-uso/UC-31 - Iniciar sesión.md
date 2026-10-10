| UC–31  | Iniciar sesión |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes |  |
| **Descripción**  | El caso de uso inicia cuando un usuario desea iniciar sesión en el sistema. |  |
| **Precondición**  | • La cuenta del usuario debe existir en el sistema y debe estar activa. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El usuario solicita iniciar sesión en el sistema. |
|  | 2 | El sistema solicita que ingrese sus datos de inicio de sesión: Correo electrónico. Contraseña. |
|  | 3 | El usuario completa los datos solicitados. |
|  | 4 | El sistema valida que no haya campos vacíos. |
|  | 5 | El sistema valida que el correo electrónico exista y que la contraseña le corresponda. |
|  | 6 | El sistema valida que la cuenta esté activa. |
|  | 7 | El sistema inicia sesión al usuario y le da acceso a la plataforma. |
| **Postcondición**  | El usuario obtuvo acceso a su cuenta. |  |
| **Flujo de eventos alternativo** | 2.1.1 | Si el usuario desea iniciar sesión con su cuenta de Google, ver UC-30 “Procesar datos de inicio”. |
| **Excepciones**  | **Paso**  |  |
|  | 4 | Si se encuentran campos obligatorios vacíos, el sistema los resalta e informa al usuario de que es necesario completarlos. Los datos ya ingresados no son borrados y se regresa al paso 2\. |
|  |  |  |
|  |  |  |
|  | 5.1.1 | Si la cuenta no existe, se informa al usuario y se le sugiere registrarse en el sistema. El caso de uso finaliza. |
|  | 5.2.1 | Si la contraseña es incorrecta, se informa al usuario y se solicita que intente nuevamente. Se regresa al paso 2\. |
| **Rendimiento**  | **Paso**  |  |
|  | 4 | 1 segundo  |
|  | 5 | 1 segundo  |
|  | 6 | 1 segundo  |
| **Frecuencia**  | 50 veces al día  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

17. 

