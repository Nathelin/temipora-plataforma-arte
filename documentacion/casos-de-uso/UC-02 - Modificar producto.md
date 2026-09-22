| UC–02  | Modificar producto  |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ-04 Información sobre productos  |  |
| **Descripción**  | El caso de uso inicia cuando un artista decide modificar un producto que tiene a la venta. Cada producto cuenta con un título, descripción, tipo, precio y términos de uso. |  |
| **Precondición**  | • El artista inició sesión en el sistema. • El producto que el artista desea modificar debe existir en el sistema. |  |
| **Secuencia**  | **Paso**  | **Acción**  |
| **normal**  | 1  | El artista selecciona la opción de “Modificar un producto”. |
|  | 2 | El sistema solicita que seleccione un producto a modificar. |
|  | 3 | El artista selecciona el producto que desea modificar (Ver UC-NN “Buscar producto”). |
|  | 4 | El sistema recupera la información del producto elegido y se la enseña al artista. |
|  | 5 | El artista elige el o los datos que desea modificar de entre los siguientes: \-            Título del producto. \-            Descripción. \-            Tipo. \-            Imagen/es. \-            Precio. \-            Si presenta o no contenido sensible. \-            Términos de uso. Y modifica su contenido. |
|  | 6 | El artista guarda los cambios. |
|  | 7 | El sistema valida que no existan campos obligatorios vacíos. |
|  | 8 | El sistema verifica que el artista no posea otros productos con igual título y tipo. |
|  | 9 | El sistema guarda los cambios y comunica al artista que el proceso fue exitoso. |
| **Postcondición**  | Se modificaron datos de un producto en el sistema. |  |
| **Excepciones**  | **Paso**  | **Acción**  |
|  | 7 | Si se encuentran campos obligatorios vacíos, el sistema los resalta e informa al artista de que es necesario completarlos. Los datos ya ingresados no son borrados y se regresa al paso 5\. |
|  |  |  |
|  |  |  |
|  | 8 | Si el sistema encuentra que el artista ya posee un producto de iguales características, notifica al artista y regresa al paso 5\. |
| **Rendimiento**  | **Paso**  | **Cota de tiempo**  |
|  | 7 | 1 segundo  |
|  | 8 | 1 segundo |
| **Frecuencia**  | 1 vez/mes  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  | Los cambios se aplican a partir del momento de la modificación y no afectan a unidades del producto que se hayan vendido con la información previa. |  |

