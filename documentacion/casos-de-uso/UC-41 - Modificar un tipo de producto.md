| UC–41  | Modificar un tipo de producto |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ-04 Información sobre productos  |  |
| **Descripción**  | El caso de uso inicia cuando un administrador desea modificar un tipo de producto que puede ofrecerse en la plataforma. |  |
| **Precondición**  | • El administrador inició sesión en el sistema. • El tipo de producto a modificar existe en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El administrador selecciona la opción de “Modificar un tipo de producto”. |
|  | 2 | El sistema despliega los tipos de productos registrados en el sistema y solicita que se seleccione uno. |
|  | 3 | El administrador selecciona un tipo de producto. |
|  | 4 | El sistema muestra los datos obligatorios que puede modificar: Nombre Descripción Opcionalmente: Observaciones |
|  | 5 | El administrador modifica los campos que desea. |
|  | 6 | El administrador confirma los datos ingresados seleccionando “Guardar”. |
|  | 7 | El sistema valida que no existan campos obligatorios vacíos. |
|  | 8 | El sistema verifica que no existan otros tipos de productos con el mismo nombre. |
|  | 9 | El sistema guarda los cambios del tipo de producto. |
|  | 10 | El sistema comunica al administrador que el proceso fue exitoso. |
| **Postcondición**  | Se modificó un tipo de producto disponible en el sistema. |  |
| **Excepciones**  | **Paso**  |  |
|  | 7 | Si se encuentran campos obligatorios vacíos, el sistema los resalta e informa al administrador de que es necesario completarlos. Los datos ya ingresados no son borrados y se regresa al paso 2\. |
|  |  |  |
|  |  |  |
|  | 8 | Si el sistema encuentra que ya existe un tipo de producto con el mismo nombre, notifica al administrador y regresa al paso 2\. |
| **Rendimiento**  | **Paso**  |  |
|  | 7 | 1 segundo  |
|  | 8 | 1 segundo  |
| **Frecuencia**  | 1 vez/mes  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  | Los cambios se aplican a partir del momento de la modificación y no afectan a productos anteriores que se vendieron con las características previas del tipo. |  |

28. 

