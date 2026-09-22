| UC–08  | Buscar servicio |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ-04 Información sobre productos  |  |
| **Descripción**  | El caso de uso inicia cuando se necesita buscar un servicio en el sistema. |  |
| **Precondición**  | • El usuario debe haber iniciado sesión. • El servicio que el usuario desea buscar debe existir en el sistema. |  |
| **Secuencia**  | **Paso**  | **Acción**  |
| **normal**  | 1  | El usuario selecciona la opción “Buscar un servicio”. |
|  | 2 | El sistema solicita que se seleccionen y completen los datos de filtrado deseados: Artista Nombre del servicio Tipo Si es un servicio de carácter adulto o sensible Rango de precio base Tiempo máximo de entrega |
|  | 3 | El usuario selecciona y completa los datos de filtrado que desea. |
|  | 4 | El sistema busca y muestra los servicios que cumplen los criterios de búsqueda. |
|  | 5 | El usuario selecciona un servicio. |
| **Salida** | El sistema retorna un servicio. |  |
| **Excepciones**  | **Paso**  | **Acción**  |
|  | \- | \- |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  | **Cota de tiempo**  |
|  | 4 | 1 segundo  |
| **Frecuencia**  | 1 vez/día  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  | Si el sistema no encuentra coincidencias, simplemente se informa por pantalla. |  |

      5. 

