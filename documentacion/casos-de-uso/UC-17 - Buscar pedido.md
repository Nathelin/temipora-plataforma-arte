| UC–17  | Buscar pedido |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–02 Formalizar el seguimiento de arte a medida. |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes • IRQ–06 Información sobre pedidos. |  |
| **Descripción**  | El caso de uso inicia cuando un usuario desea buscar un pedido de arte realizado en el sistema. |  |
| **Precondición**  | • El usuario inició sesión en el sistema. • El pedido que el usuario desea buscar existe en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El usuario selecciona la opción “Buscar un pedido”. |
|  | 2 | El sistema solicita que se seleccionen y completen los datos de filtrado deseados: Artista Cliente Nombre del servicio Tipo Si es un servicio de carácter adulto o sensible Rango de precio Fecha de inicio Estado |
|  | 3 | El usuario selecciona y completa los datos de filtrado que desea. |
|  | 4 | El sistema busca y muestra los pedidos que cumplen los criterios de búsqueda. |
|  | 5 | El usuario selecciona un pedido. |
| **Salida** | El sistema retorna un pedido |  |
| **Excepciones**  | **Paso**  |  |
|  |  |  |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 4 | 1 segundo. |
| **Frecuencia**  | 30 veces al día  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

