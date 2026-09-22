| UC–29  | Registrarse |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes |  |
| **Descripción**  | El caso de uso inicia cuando un usuario desea registrarse en el sistema. |  |
| **Precondición**  |  |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El usuario solicita registrarse en el sistema eligiendo si desea ser artista o cliente. |
|  | 2 | El sistema solicita que elija una opción: Registrarse manualmente Registrarse con Google |
|  | 3 | El usuario elige registrarse manualmente. |
|  | 4 | El sistema solicita los siguientes datos obligatorios: Nombre Apellido Nacionalidad DNI o CI Correo Fecha de nacimiento Contraseña Opcionalmente: Número de teléfono. |
|  | 5 | El usuario ingresa los datos solicitados. |
|  | 6 | El usuario confirma los datos. |
|  | 7 | El sistema valida que no hayan campos obligatorios vacíos. |
|  | 8 | El sistema valida que no haya otro usuario registrado con el mismo DNI o CI. |
|  | 9 | El sistema da de alta al usuario y le informa que lo registró correctamente. |
| **Postcondición**  | El usuario fue dado de alta en el sistema |  |
| **Flujo de eventos alternativo** | 3.1.1 | Si el usuario elige registrarse con Google, ver UC-30 “Procesar datos de inicio”. |
| **Excepciones**  | **Paso**  |  |
|  | 7 | Si se encuentran campos obligatorios vacíos, el sistema los resalta e informa al artista de que es necesario completarlos. Los datos ya ingresados no son borrados y se regresa al paso 4\. |
|  |  |  |
|  |  |  |
|  | 8 | Si el sistema encuentra que ya existe un usuario con el mismo DNI o CI, se informa al usuario y se le sugiere iniciar sesión. De aceptar, se inicia el UC-NN “Iniciar sesión”. De lo contrario el caso de uso finaliza. |
| **Rendimiento**  | **Paso**  |  |
|  | 7 | 1 segundo |
|  | 8 | 1 segundo |
|  | 9 | 1 segundo |
| **Frecuencia**  | 30 veces al día |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

15. 

