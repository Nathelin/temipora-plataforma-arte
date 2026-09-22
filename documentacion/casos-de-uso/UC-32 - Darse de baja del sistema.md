| UC–32  | Darse de baja del sistema |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes |  |
| **Descripción**  | El caso de uso inicia cuando un usuario desea darse de baja del sistema. |  |
| **Precondición**  | • El usuario debe haber iniciado sesión. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El usuario selecciona la opción “Darse de baja”. |
|  | 2 | El sistema solicita confirmación para la baja definitiva. |
|  | 3 | El usuario confirma su intención. |
|  | 4 | El sistema realiza una baja lógica y cierra la sesión activa del usuario. |
|  | 5 | El sistema informa al usuario de que el proceso fue realizado con éxito. |
| **Postcondición**  | El perfil del usuario pasa a estar en estado “inactivo” y ya no es visible en las búsquedas. Toda la información histórica se mantiene en el sistema y se permite la reactivación de la cuenta. |  |
| **Excepciones**  | **Paso**  |  |
|  | 4.1.1 | Si el usuario está asociado a un pedido activo, el sistema informa al usuario de que no puede darse de baja si tiene pedidos abiertos. El caso de uso finaliza. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 4 | 1 segundo |
| **Frecuencia**  | 1 vez/mes  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

18. 

