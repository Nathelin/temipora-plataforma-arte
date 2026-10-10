| UC–23  | Buscar artista |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas |  |
| **Descripción**  | El caso de uso inicia cuando un usuario desea buscar un artista en el sistema. |  |
| **Precondición**  | • El usuario debe haber iniciado sesión. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El usuario selecciona la opción “Buscar un artista”. |
|  | 2 | El sistema solicita que se seleccione un criterio de búsqueda: Nombre artístico Correo electrónico Si el usuario es un administrador, el sistema adicionalmente permite estos criterios de búsqueda: Nombre real DNI/CI |
|  | 3 | El usuario selecciona y completa los criterios de búsqueda que desea. |
|  | 4 | El sistema busca y muestra a los artistas que cumplen los criterios de búsqueda. |
|  | 5 | El usuario selecciona un artista. |
| **Salida** | El sistema retorna un artista |  |
| **Excepciones**  | **Paso**  |  |
|  |  |  |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 4 | 1 segundo  |
| **Frecuencia**  | 100 veces al día |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

