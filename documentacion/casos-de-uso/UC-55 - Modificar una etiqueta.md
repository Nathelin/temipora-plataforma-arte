| UC–54  | Modificar una etiqueta |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–08 Información sobre listas de favoritos. |  |
| **Descripción**  | El caso de uso inicia cuando un administrador decide modificar una etiqueta disponible en el sistema. |  |
| **Precondición**  | • El administrador debe haber iniciado sesión. • La etiqueta a modificar debe existir en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El administrador selecciona la opción de “Modificar una etiqueta”. |
|  | 2 | El sistema despliega las etiquetas registradas en el sistema y solicita que se seleccione una. |
|  | 3 | El administrador elige una etiqueta. |
|  | 4 | El sistema muestra los datos obligatorios que se pueden modificar: Nombre Descripción Opcionalmente: Observaciones |
|  | 5 | El administrador modifica los campos que desea. |
|  | 6 | El administrador confirma los datos ingresados seleccionando “Guardar”. |
|  | 7 | El sistema valida que no existan campos obligatorios vacíos. |
|  | 8 | El sistema verifica que no existan otras etiquetas con el mismo nombre. |
|  | 9 | El sistema guarda los cambios de la etiqueta. |
|  | 10 | El sistema comunica al administrador que el proceso fue exitoso. |
| **Postcondición** | Se modificó una etiqueta del sistema. |  |
| **Excepciones**  | **Paso**  |  |
|  | 5 | Si se encuentran campos obligatorios vacíos, el sistema los resalta e informa al administrador de que es necesario completarlos. Los datos ya ingresados no son borrados y se regresa al paso 2\. |
|  |  |  |
|  |  |  |
|  | 6 | Si el sistema encuentra que ya existe una etiqueta con el mismo nombre, notifica al administrador y regresa al paso 2\. |
| **Rendimiento**  | **Paso**  |  |
|  | 7 | 1 segundo  |
|  | 8 | 1 segundo  |
| **Frecuencia**  | 1 vez/mes |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

