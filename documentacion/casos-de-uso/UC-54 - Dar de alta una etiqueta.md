| UC–54  | Dar de alta una etiqueta |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–08 Información sobre listas de favoritos. |  |
| **Descripción**  | El caso de uso inicia cuando un administrador decide agregar una nueva etiqueta que pueda ser utilizada para identificar servicios y productos dentro de la plataforma. |  |
| **Precondición**  | • El administrador debe haber iniciado sesión. • La etiqueta que desea crear todavía no existe en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El administrador selecciona la opción de “Dar de alta una etiqueta”. |
|  | 2 | El sistema solicita que se ingresen los siguientes datos obligatorios: Nombre Descripción Opcionalmente: Observaciones |
|  | 3 | El administrador ingresa los datos solicitados. |
|  | 4 | El administrador confirma los datos ingresados seleccionando “Guardar”. |
|  | 5 | El sistema valida que no existan campos obligatorios vacíos. |
|  | 6 | El sistema verifica que no existan otras etiquetas con el mismo nombre. |
|  | 7 | El sistema da de alta la etiqueta. |
|  | 8 | El sistema comunica al administrador que el proceso fue exitoso. |
| **Postcondición** | La nueva etiqueta fue registrada en el sistema y está disponible para catalogar el arte de los artistas y para que pueda ser filtrado en la plataforma. |  |
| **Excepciones**  | **Paso**  |  |
|  | 5 | Si se encuentran campos obligatorios vacíos, el sistema los resalta e informa al administrador de que es necesario completarlos. Los datos ya ingresados no son borrados y se regresa al paso 2\. |
|  |  |  |
|  |  |  |
|  | 6 | Si el sistema encuentra que ya existe una etiqueta con el mismo nombre, notifica al administrador y regresa al paso 2\. |
| **Rendimiento**  | **Paso**  |  |
|  | 5 | 1 segundo  |
|  | 6 | 1 segundo  |
|  | 7 | 1 segundo  |
| **Frecuencia**  | 1 vez/mes |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

