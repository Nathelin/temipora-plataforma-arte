| UC–43  | Sancionar usuario |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–03 Mejorar la protección de productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes |  |
| **Descripción**  | El caso de uso inicia cuando un administrador desea limitar las funcionalidades de un usuario debido a su conducta, falta de compromiso o violación de los términos de uso de la plataforma. |  |
| **Precondición**  | • El usuario a sancionar debe existir en el sistema. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El administrador selecciona la opción “Sancionar usuario” |
|  | 2 | El sistema solicita que se elija a un usuario. |
|  | 3 | El administrador selecciona un usuario (Ver UC-NN “Buscar artista”)  |
|  | 4 | El sistema enseña la información del usuario. |
|  | 5 | El administrador solicita aplicar una sanción. |
|  | 6 | El sistema despliega un formulario solicitando que se complete obligatoriamente: El tipo de sanción: Advertencia Suspensión temporal de ventas\* Bloqueo definitivo Duración Motivo Opcionalmente permite subir evidencia y justificaciones adicionales. |
|  | 7 | El administrador completa los datos y confirma la sanción. |
|  | 8 | El sistema verifica que no haya campos obligatorios vacíos. |
|  | 9 | El sistema procesa la solicitud de sanción y la aplica. |
|  | 10 | El sistema notifica al usuario de la sanción. |
|  | 11 | El sistema notifica al administrador que el proceso fue realizado con éxito. |
| **Postcondición**  | Se registró una sanción a un usuario. |  |
| **Flujo de eventos alternativos** | 3.1.1 | Si el administrador desea sancionar a un cliente, ver UC-NN “Buscar usuario no artista”. |
|  | 3.1.2 | El sistema enseña la información del usuario. |
|  | 3.1.3 | El administrador solicita aplicar una sanción. |
|  | 3.1.4 | El sistema despliega un formulario solicitando que se complete obligatoriamente: El tipo de sanción: Advertencia Suspensión temporal de compras Bloqueo definitivo Duración Motivo Opcionalmente permite subir evidencia y justificaciones adicionales. |
|  | 3.1.5 | El administrador completa los datos y confirma la sanción. Tras esto el flujo de eventos continúa desde el paso 8\. |
| **Excepciones**  | **Paso**  |  |
|  | 8 | Si se encuentran campos obligatorios vacíos, el sistema los resalta e informa al administrador de que es necesario completarlos. Los datos ya ingresados no son borrados y se regresa al paso 6 si es un usuario artista o 3.1.4 si no es artista. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 4 | 1 segundo  |
|  | 8 | 1 segundo  |
|  | 9 | 1 segundo  |
| **Frecuencia**  | 2 veces al mes  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  | \*Si se suspenden las ventas del artista, no puede tomar nuevos pedidos ni vender productos pero sí puede gestionar los pedidos que había tomado previamente. |  |

31. 

