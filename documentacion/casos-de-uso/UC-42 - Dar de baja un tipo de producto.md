| UC–42  | Dar de baja un tipo de producto |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ-04 Información sobre productos |  |
| **Descripción**  | El caso de uso inicia cuando un administrador desea descontinuar un tipo de producto que puede ofrecerse en la plataforma. No afecta a productos que ya tenían definido con anterioridad el tipo de producto que se quiere descontinuar. |  |
| **Precondición**  | • El administrador inició sesión en el sistema. • El tipo de producto a descontinuar existe en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El administrador selecciona la opción de “Dar de baja un tipo de producto”. |
|  | 2 | El sistema despliega los tipos de productos registrados en el sistema y solicita que se seleccione uno. |
|  | 3 | El administrador selecciona un tipo de producto. |
|  | 4 | El sistema muestra la información del tipo de producto. |
|  | 5 | El administrador solicita la baja del tipo de producto. |
|  | 6 | El sistema solicita la confirmación del administrador. |
|  | 7 | El administrador confirma su intención de dar de baja el tipo de producto. |
|  | 8 | El sistema realiza una baja lógica sobre el tipo de producto seleccionado, impidiendo que puedan crearse nuevos productos de este tipo. |
|  | 9 | El sistema comunica al administrador que el proceso fue exitoso. |
| **Postcondición**  | El tipo de producto es dado de baja del sistema cambiando su estado de “Activo” a “Inactivo”. |  |
| **Excepciones**  | **Paso**  |  |
|  | 7 | Si el administrador no confirma la baja del tipo de producto, se regresa al paso 2\. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 4 | 1 segundo  |
|  | 8 | 1 segundo  |
| **Frecuencia**  | 1 vez/mes  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

29.   
30. 

