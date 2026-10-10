| UC–03  | Baja producto |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ-04 Información sobre productos  |  |
| **Descripción**  | El caso de uso inicia cuando un artista desea dar de baja uno de sus productos. |  |
| **Precondición**  | • El artista inició sesión en el sistema. • El producto que el artista desea dar de baja debe existir en el sistema. |  |
| **Secuencia**  | **Paso**  | **Acción**  |
| **normal**  | 1  | El artista selecciona la opción de “Dar de baja un producto”. |
|  | 2 | El sistema solicita que seleccione un producto a dar de baja. |
|  | 3 | El artista selecciona el producto que desea dar de baja (Ver UC-04 “Buscar producto”). |
|  | 4 | El sistema recupera la información del producto elegido y se la enseña al artista. |
|  | 5 | El artista solicita al sistema la baja del producto. |
|  | 6 | El sistema solicita la confirmación del artista. |
|  | 7 | El artista confirma su intención de dar de baja el producto. |
|  | 8 | El sistema realiza una baja lógica y da de baja el producto seleccionado, impidiendo que aparezca en búsquedas y perfil del artista. |
|  | 9 | El sistema informa al artista que el proceso fue un éxito. |
| **Postcondición**  | El producto es dado de baja del sistema cambiando su estado de “Activo” a “Inactivo”. |  |
| **Excepciones**  | **Paso**  | **Acción**  |
|  | 7 | Si el artista no confirma la baja del producto, se regresa al paso 2\. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  | **Cota de tiempo**  |
|  | 4 | 1 segundo  |
|  | 8 | 1 segundo  |
| **Frecuencia**  | 1 vez/mes  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

