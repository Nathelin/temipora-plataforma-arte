| UC–05  | Alta servicio |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores • OBJ–02 Formalizar el seguimiento de arte a medida |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ-05 Información sobre servicios |  |
| **Descripción**  | El caso de uso inicia cuando un artista decide dar de alta un servicio en la plataforma. Cada servicio cuenta con un nombre, descripción, tipo, imagen de portada, imagen de ejemplo, precio base, declara si contiene o no contenido sensible, un tiempo estimado de entrega, cantidad de cupos y términos de uso y servicio. El sistema guardará los datos relevantes del servicio de la forma que se detalla a continuación y permite que los clientes lo encuentren y envíen una solicitud de pedido.  |  |
| **Precondición**  | • El artista inició sesión en el sistema. • El servicio no existe previamente en el sistema. |  |
| **Secuencia**  | **Paso**  | **Acción**  |
| **normal**  | 1  | El artista selecciona la opción de “Dar de alta un servicio”. |
|  | 2 | El sistema solicita que se ingresen los siguientes datos obligatorios: Nombre del servicio Descripción Tipo (YCH Ilustración digital Ilustración tradicional Diseño de personaje Hoja de referencia Recurso para streamers Recurso para videojuegos Otros) Imagen de portada Imagen/es de ejemplo Puede subir una imagen nueva. O puede elegir una imagen de un pedido finalizado. Precio base Si presenta o no contenido sensible ya sea por desnudez, violencia o contenido generalmente sensible. Tiempo estimado de entrega Cantidad de cupos disponibles Términos de uso y servicio El sistema presenta también los siguientes datos opcionales: Habilitar cupos nuevos tras la entrega de un pedido. |
|  | 3 | El artista ingresa los datos solicitados. Si agrega una imagen de un pedido finalizado, Ver UC-17 “Buscar pedido”. |
|  | 4 | El artista confirma los datos ingresados. |
|  | 5 | El sistema valida que no existan campos obligatorios vacíos. |
|  | 6 | El sistema verifica que el artista no posea otros servicios con igual nombre y tipo. |
|  | 7 | El sistema pregunta al artista si desea utilizar su formulario de pedidos por defecto o si desea configurar uno para este servicio. |
|  | 8 | El artista elige una opción. Ver UC-47 “Configurar formulario de pedidos” si el artista decide configurar el formulario para este servicio en específico. |
|  | 9 | El sistema da de alta el servicio en el sistema colocándolo como “Disponible”. |
| **Postcondición**  | El servicio se encuentra registrado en el sistema. |  |
| **Excepciones**  | **Paso**  | **Acción**  |
|  | 5 | Si se encuentran campos obligatorios vacíos, el sistema los resalta e informa al artista de que es necesario completarlos. Los datos ya ingresados no son borrados y se regresa al paso 2\. |
|  |  |  |
|  |  |  |
|  | 6 | Si el sistema encuentra que el artista ya posee un servicio de iguales características, notifica al artista y regresa al paso 2\. |
| **Rendimiento**  | **Paso**  | **Cota de tiempo**  |
|  | 5 | 1 segundo  |
|  | 6 | 1 segundo  |
|  | 9 | 1 segundo  |
| **Frecuencia**  | 10 veces al día  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

