| UC–11  | Alta imagen en portfolio |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–4 Centralizar manejo de portfolios |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ-03 Información sobre imágenes en portfolios • IRQ-06 Información sobre pedidos |  |
| **Descripción**  | El caso de uso inicia cuando un artista desea dar de alta una nueva imagen en su portfolio personal. Cada imagen debe tener un título, descripción, una fecha de subida asignada automáticamente, un medio utilizado, materiales y puede o no presentar contenido sensible. El sistema guardará los datos relevantes de la imagen de la forma que se detalla a continuación y permite que sea visualizado por cualquier usuario. |  |
| **Precondición**  | • El artista inició sesión en el sistema. • La imagen no existe previamente en el portfolio. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El artista selecciona la opción de “Subir imagen a portfolio”. |
|  | 2 | El sistema solicita que se ingresen los siguientes datos obligatorios: Título Descripción Imagen:  Subir nueva imagen o Seleccionar imagen de un pedido finalizado Si presenta o no contenido sensible ya sea por desnudez, violencia o contenido generalmente sensible. Si es tradicional o digital. Y los siguientes datos opcionales: Materiales |
|  | 3 | El artista ingresa los datos solicitados. Si agrega una imagen de un pedido finalizado, Ver UC-17 “Buscar pedido”. |
|  | 4 | El artista confirma los datos ingresados seleccionando “Guardar”. |
|  | 5 | El sistema valida que no existan campos obligatorios vacíos. |
|  | 6 | El sistema verifica que el artista no posea otras imágenes en su portfolio con igual título y medio (tradicional o digital). |
|  | 7 | El sistema da de alta la imagen en el portfolio del artista y lo vuelve visible para todo usuario que quiera verla. |
|  | 8 | El sistema comunica al artista de que el alta de la imagen fue exitosa. |
| **Postcondición**  | La imagen se encuentra registrada en el portfolio del usuario y se encuentra disponible para que otros usuarios la encuentren. |  |
| **Excepciones**  | **Paso**  |  |
|  | 5 | Si se encuentran campos obligatorios vacíos, el sistema los resalta e informa al artista de que es necesario completarlos. Los datos ya ingresados no son borrados y se regresa al paso 2\. |
|  |  |  |
|  |  |  |
|  | 6 | Si el sistema encuentra que el artista ya posee una imagen en su portfolio de iguales características, notifica al artista y regresa al paso 2\. |
| **Rendimiento**  | **Paso**  |  |
|  | 5  | 1 segundo  |
|  | 6 | 1 segundo  |
|  | 7 | 1 segundo  |
| **Frecuencia**  | 1 vez a la semana  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  | Si se selecciona de entre un pedido finalizado, habrá algún indicador de que es una comisión realizada en la plataforma.  |  |

      8. 

