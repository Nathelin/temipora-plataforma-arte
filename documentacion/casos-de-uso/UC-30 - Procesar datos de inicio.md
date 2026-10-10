| UC–30  | Procesar datos de inicio |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes |  |
| **Descripción**  | El caso de uso inicia cuando el sistema necesita validar la identidad de un nuevo usuario interactuando con Google OAuth para obtener sus datos y crear su cuenta. |  |
| **Precondición**  |  |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | Google OAuth recibe la solicitud de vinculación de cuenta. |
|  | 2 | Google OAuth solicita que el usuario le otorgue permisos para vincular sus datos. |
|  | 3 | El usuario autoriza el acceso a sus datos. |
|  | 4 | Google OAuth valida su identidad y retorna un token de autenticación. |
|  | 5 | El sistema recibe el token con los datos del usuario y crea una cuenta con estos datos. |
| **Postcondición**  | El usuario fue dado de alta en el sistema con los datos recibidos por el sistema externo Google OAuth. |  |
| **Flujo de eventos alternativos** | 5.1.1 | Si los datos ya están vinculados a una cuenta, el sistema inicia sesión con estos datos de inicio y no crea una cuenta nueva.  |
| **Excepciones**  | **Paso**  |  |
|  | 4 | Si Google OAuth no responde, el sistema informa al usuario y le sugiere intentar registrarse manualmente. El caso de uso finaliza. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 4 | 2 segundos |
|  | 5 | 1 segundo |
| **Frecuencia**  | 30 veces al día |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

16. 

