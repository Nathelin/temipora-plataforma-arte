| UC–54  | Dar de baja una etiqueta |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–08 Información sobre listas de favoritos. |  |
| **Descripción**  | El caso de uso inicia cuando un administrador decide dar de baja y dejar inactiva una etiqueta disponible en el sistema. |  |
| **Precondición**  | • El administrador debe haber iniciado sesión. • La etiqueta que desea dar de baja debe existir en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El administrador selecciona la opción de “Dar de baja una etiqueta”. |
|  | 2 | El sistema despliega las etiquetas disponibles en el sistema y solicita que se seleccione una. |
|  | 3 | El administrador selecciona una etiqueta. |
|  | 3 | El sistema muestra la información de la etiqueta. |
|  | 4 | El administrador solicita la baja de la etiqueta. |
|  | 5 | El sistema solicita la confirmación del administrador. |
|  | 6 | El administrador confirma su intención de dar de baja la etiqueta. |
|  | 7 | El sistema realiza una baja lógica sobre la etiqueta seleccionada, impidiendo que pueda asignarse a nuevas piezas de arte. |
|  | 8 | El sistema comunica al administrador que el proceso fue exitoso. |
| **Postcondición** | La etiqueta es dada de baja del sistema cambiando su estado de “Activo” a “Inactivo”. |  |
| **Excepciones**  | **Paso**  |  |
|  |  |  |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 4 | 1 segundo  |
|  | 8 | 1 segundo  |
| **Frecuencia**  | 1 vez/mes |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |
