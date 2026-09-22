| UC–15  | Alta avance de pedido |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–02 Formalizar el seguimiento de arte a medida • OBJ–03 Mejorar la protección de productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas. • IRQ–02 Información sobre clientes. • IRQ–05 Información sobre servicios. • IRQ–06 Información sobre pedidos. |  |
| **Descripción**  | El caso de uso inicia cuando un artista desea registrar un avance en el estado de uno de sus pedidos. |  |
| **Precondición**  | • El artista inició sesión en el sistema. • El pedido asociado debe existir en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El artista solicita dar de alta un avance de algún pedido. |
|  | 2 | El sistema solicita que seleccione el pedido. |
|  | 3 | El artista selecciona el pedido (Ver UC-17 “Buscar pedido) |
|  | 4 | El sistema muestra los datos del pedido y solicita que el artista ingrese los siguientes datos obligatorios: Imagen. Descripción. Estado: Sketch Lineart Color base Renderizado Otro (Ingresar) |
|  | 5 | El artista completa los datos solicitados y confirma su intención de dar de alta el avance. |
|  | 6 | El sistema valida que no hayan campos obligatorios vacíos. |
|  | 7 | El sistema da de alta el avance de pedido y notifica al cliente. |
| **Postcondición**  | Se dio de alta un avance de un pedido en el sistema. |  |
| **Excepciones**  | **Paso**  |  |
|  | 6 | Si se encuentran campos obligatorios vacíos, el sistema los resalta e informa al artista de que es necesario completarlos. Los datos ya ingresados no son borrados y se regresa al paso 4\. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 4 | 1 segundo |
|  | 6 | 1 segundo |
|  | 7 | 1 segundo |
| **Frecuencia**  | 10 veces al día. |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

4. 

