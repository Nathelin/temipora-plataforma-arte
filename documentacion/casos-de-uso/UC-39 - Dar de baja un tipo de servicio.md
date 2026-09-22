| UC–39 | Dar de baja un tipo de servicio |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ-04 Información sobre productos |  |
| **Descripción**  | El caso de uso inicia cuando un administrador desea descontinuar un tipo de servicio que puede ofrecerse en la plataforma. No afecta a productos que ya tenían definido con anterioridad el tipo de producto que se quiere descontinuar. |  |
| **Precondición**  | • El administrador inició sesión en el sistema. • El tipo de servicio a descontinuar existe en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El administrador selecciona la opción de “Dar de baja un tipo de servicio”. |
|  | 2 | El sistema despliega los tipos de servicios registrados en el sistema y solicita que se seleccione uno. |
|  | 3 | El administrador selecciona un tipo de servicio. |
|  | 4 | El sistema muestra la información del tipo de servicio. |
|  | 5 | El administrador solicita la baja del tipo de servicio. |
|  | 6 | El sistema solicita la confirmación del administrador. |
|  | 7 | El administrador confirma su intención de dar de baja el tipo de servicio. |
|  | 8 | El sistema realiza una baja lógica sobre el tipo de servicio seleccionado, impidiendo que puedan ofrecerse nuevos servicios de este tipo. |
|  | 9 | El sistema comunica al administrador que el proceso fue exitoso. |
| **Postcondición**  | El tipo de servicio es dado de baja del sistema cambiando su estado de “Activo” a “Inactivo”. |  |
| **Excepciones**  | **Paso**  |  |
|  | 7 | Si el administrador no confirma la baja del tipo de servicio, se regresa al paso 2\. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 4 | 1 segundo  |
|  | 8 | 1 segundo  |
| **Frecuencia**  | 1 vez/mes  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

26. 

