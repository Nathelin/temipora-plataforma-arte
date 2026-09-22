| UC–01  | Alta producto |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | IRQ–01 Información sobre artistas IRQ-04 Información sobre productos  |  |
| **Descripción**  | El caso de uso inicia cuando un artista decide dar de alta un producto para su venta en la plataforma. Cada producto cuenta con un título, descripción, tipo, precio y términos de uso. El sistema guardará los datos relevantes del producto de la forma que se detalla a continuación y permite que sea comprado por algún cliente. |  |
| **Precondición**  | • El artista inició sesión en el sistema. • El producto no existe previamente en el sistema. |  |
| **Secuencia**  | **Paso**  | **Acción**  |
| **normal**  | 1  | El artista selecciona la opción de “Dar de alta un producto”. |
|  | 2 | El sistema solicita que se ingresen los siguientes datos obligatorios: Título del producto. Descripción. Tipo: Adoptable\* Fondo de pantalla PC Fondo de pantalla móvil Íconos de PC/móvil Print digital Foto de perfil  Recurso para streamers Recurso para videojuegos Arte tradicional\*\* Otros Imagen/es del producto. Precio. Si presenta o no contenido sensible ya sea por desnudez, violencia o contenido generalmente sensible. Términos de uso.\*\*\* |
|  | 3 | El artista ingresa los datos solicitados. Ver flujo de eventos alternativos en caso de que se seleccione “Adoptable”.  |
|  | 4 | El artista confirma los datos ingresados seleccionando “Guardar”. |
|  | 5 | El sistema valida que no existan campos obligatorios vacíos. |
|  | 6 | El sistema verifica que el artista no posea otros productos con igual título y tipo. |
|  | 7 | El sistema da de alta el producto en el sistema colocándolo como “Disponible”. El producto será de stock limitado si fue definido como “Adoptable” o “Arte tradicional”. |
|  | 8 | El sistema comunica al artista de que el alta del producto fue exitoso. |
| **Postcondición**  | El producto se encuentra registrado en el sistema y se encuentra a la venta, disponible para que otros usuarios lo encuentren. |  |
| **Flujo de eventos alternativos** | 3.1.1 | El artista marca el tipo como “Adoptable”. |
|  | 3.1.2 | El sistema solicita que se ingrese si es un único adoptable o es una plancha de adoptables. |
|  | 3.1.3 | El artista selecciona una opción. |
|  | 3.1.4 | Si el artista selecciona que es un único adoptable, se continúa desde el paso 4\. |
|  | 3.1.5 | Si el artista selecciona que es una plancha de adoptables, el sistema solicita que se ingrese obligatoriamente: La cantidad de adoptables disponibles en la plancha. Un nombre por cada adoptable disponible. Una imagen por adoptable disponible. |
|  | 3.1.6 | El artista ingresa los datos solicitados y el flujo continua desde el paso 4\. |
| **Excepciones**  | **Paso**  | **Acción**  |
|  | 5 | Si se encuentran campos obligatorios vacíos, el sistema los resalta e informa al artista de que es necesario completarlos. Los datos ya ingresados no son borrados y se regresa al paso 2\. |
|  |  |  |
|  |  |  |
|  | 6 | Si el sistema encuentra que el artista ya posee un producto de iguales características, notifica al artista y regresa al paso 2\. |
| **Rendimiento**  | **Paso**  | **Cota de tiempo**  |
|  | 5 | 1 segundo  |
|  | 6 | 1 segundo  |
|  | 7 | 1 segundo  |
| **Frecuencia**  | 1 vez por semana |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  | \*Al ser de uso exclusivo, solamente se tendrá una unidad en stock por cada adoptable individual. Es decir, en una plancha de adoptables se tomarán como unidad los adoptables en la plancha y no la plancha en sí. \*\*A todo producto del tipo “Arte tradicional” se le asigna una unidad de stock. El envío de la pieza deberá en primera instancia ser acordado entre artista y cliente por fuera de la plataforma. \*\*\*El artista puede elegir utilizar los términos de uso globales asociados a su cuenta y definidos en el UC-NN “Configurar términos globales de uso” o definir términos de uso exclusivos a un cierto producto durante su alta en el paso 3\. |  |

2.   
   3. 

