| UC–13  | Baja imagen en portfolio |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–04 Centralizar manejo de portfolios. |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–03 Información sobre imágenes en portfolios. |  |
| **Descripción**  | El caso de uso inicia cuando un artista desea dar de baja una de las imágenes en su portfolio. |  |
| **Precondición**  | • El artista inició sesión en el sistema. • La imagen que el artista desea dar de baja debe existir en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El artista selecciona la opción de “Eliminar imagen” |
|  | 2 | El sistema solicita que seleccione una imagen a dar de baja. |
|  | 3 | El artista selecciona la imagen que desea dar de baja (Ver UC-NN “Buscar imagen en portfolio”). |
|  | 4 | El sistema recupera la información de la imagen elegida y se la enseña al artista. |
|  | 5 | El artista solicita al sistema la baja de la imagen. |
|  | 6 | El sistema solicita la confirmación del artista. |
|  | 7 | El artista confirma su intención de dar de baja la imagen. |
|  | 8 | El sistema realiza una baja lógica y da de baja la imagen seleccionada, impidiendo que aparezca en búsquedas y el portfolio del artista. |
| **Postcondición**  | La imagen es dada de baja del sistema cambiando su estado de “Activo” a “Inactivo”. |  |
| **Excepciones**  | **Paso**  |  |
|  | 7 | Si el artista no confirma la baja de la imagen, se regresa al paso 2\. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 4 | 1 segundo  |
|  | 8 | 1 segundo  |
| **Frecuencia**  | 1 vez/mes  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

      10.   
2. 

