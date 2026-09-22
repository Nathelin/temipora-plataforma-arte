| UC–37  | Dar de alta un tipo de servicio |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | IRQ-04 Información sobre servicios |  |
| **Descripción**  | El caso de uso inicia cuando un administrador desea agregar un nuevo tipo de servicio que puede ofrecerse en la plataforma. |  |
| **Precondición**  | • El administrador inició sesión en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El administrador selecciona la opción de “Dar de alta un tipo de servicio”. |
|  | 2 | El sistema solicita que se ingresen los siguientes datos obligatorios: Nombre Descripción Opcionalmente: Observaciones |
|  | 3 | El administrador ingresa los datos solicitados. |
|  | 4 | El administrador confirma los datos ingresados seleccionando “Guardar”. |
|  | 5 | El sistema valida que no existan campos obligatorios vacíos. |
|  | 6 | El sistema verifica que no existan otros tipos de servicio con el mismo nombre. |
|  | 7 | El sistema da de alta el tipo de servicio. |
|  | 8 | El sistema comunica al administrador que el proceso fue exitoso. |
| **Postcondición**  | El nuevo tipo de servicio fue registrado en el sistema y está disponible como un tipo de servicio que los artistas pueden elegir ofrecer y que puede ser buscado. |  |
| **Excepciones**  | **Paso**  |  |
|  | 5 | Si se encuentran campos obligatorios vacíos, el sistema los resalta e informa al administrador de que es necesario completarlos. Los datos ya ingresados no son borrados y se regresa al paso 2\. |
|  |  |  |
|  |  |  |
|  | 6 | Si el sistema encuentra que ya existe un tipo de servicio con el mismo nombre, notifica al administrador y regresa al paso 2\. |
| **Rendimiento**  | **Paso**  |  |
|  | 5 | 1 segundo  |
|  | 6 | 1 segundo  |
|  | 7 | 1 segundo  |
| **Frecuencia**  | 1 vez/mes  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  |  |  |

24. 

