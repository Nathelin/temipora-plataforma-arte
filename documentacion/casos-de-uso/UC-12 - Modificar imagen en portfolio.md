| UC–12  | Modificar imagen en portfolio |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | OBJ–04 Centralizar manejo de portfolios. |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ-03 Información sobre imágenes en portfolios • IRQ-06 Información sobre pedidos |  |
| **Descripción**  | El caso de uso inicia cuando un artista decide modificar una imagen. Cada imagen tiene un título, descripción, una fecha de subida asignada automáticamente, un medio utilizado, materiales y puede o no presentar contenido sensible. |  |
| **Precondición**  | • El artista inició sesión en el sistema. • La imagen que el artista desea modificar debe existir en su portfolio. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El artista selecciona la opción de “Modificar imagen”. |
|  | 2 | El sistema solicita que seleccione una imagen a modificar. |
|  | 3 | El artista selecciona la imagen que desea modificar (Ver UC-14 “Buscar imagen en portfolio”). |
|  | 4 | El sistema recupera la información de la imagen elegida y se la enseña al artista. |
|  | 5 | El artista elige el o los datos que desea modificar. Los siguientes son datos obligatorios que no deben permanecer nulos: Título de la imagen. Descripción. Imagen: Subir nueva imagen o Seleccionar imagen de un pedido finalizado   Si presenta o no contenido sensible. Si es tradicional o digital. Los siguientes son datos opcionales: Materiales Y modifica su contenido. Si agrega una imagen de un pedido finalizado, Ver UC-17 “Buscar pedido”. |
|  | 6 | El artista guarda los cambios. |
|  | 7 | El sistema valida que no existan campos obligatorios vacíos. |
|  | 8 | El sistema verifica que el artista no posea otras imágenes en su portfolio con igual título y medio (tradicional o digital). |
|  | 9 | El sistema guarda los cambios y comunica al artista que el proceso fue exitoso. |
| **Postcondición**  | Se modificó una imagen en el portfolio. |  |
| **Excepciones**  | **Paso**  |  |
|  | 7 | Si se encuentran campos obligatorios vacíos, el sistema los resalta e informa al artista de que es necesario completarlos. Los datos ya ingresados no son borrados y se regresa al paso 4\. |
|  |  |  |
|  |  |  |
|  | 8 | Si el sistema encuentra que el artista ya posee una imagen en su portfolio de iguales características, notifica al artista y regresa al paso 4\. |
| **Rendimiento**  | **Paso**  |  |
|  | 4 | 1 segundo  |
|  | 7 | 1 segundo  |
|  | 8 | 1 segundo  |
| **Frecuencia**  | 1 vez/mes  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

      9. 

