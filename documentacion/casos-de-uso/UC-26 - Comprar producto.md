| UC–26  | Comprar producto |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ–02 Información sobre clientes. • IRQ-04 Información sobre productos |  |
| **Descripción**  | El caso de uso inicia cuando un cliente desea pagar un producto a un artista. |  |
| **Precondición**  | • El cliente inició sesión. • El producto que el cliente desea comprar debe existir en el sistema y estar disponible. |  |
| **Secuencia**  | **Paso**  |  |
| **normal**  | 1  | El cliente selecciona la opción “Comprar producto”. |
|  | 2 | El sistema solicita que se seleccione el producto a comprar. |
|  | 3 | El cliente selecciona el producto que desea comprar (Ver UC-04 “Buscar producto) |
|  | 4 | El sistema despliega los datos del producto: Artista asociado. Título Descripción. Tipo. Precio. |
|  | 5 | El usuario confirma su intención de pagar (Ver UC-27 “Procesar pago”) |
|  | 6 | El sistema registra el pago y notifica al cliente y artista de que se compró un producto. |
| **Postcondición**  | Se registró una venta de un producto. Si el producto vendido fue un adoptable, este adoptable cambia su estado a “Comprado”. |  |
| **Excepciones**  | **Paso**  |  |
|  | 5.1.1 | Si el caso de uso UC-27 retorna un estado de rechazo, se informa al cliente y el caso de uso finaliza. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  |  |
|  | 4  | 1 segundo |
|  | 6 | 1 segundo |
| **Frecuencia**  | 1 vez/mes  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  | Originalmente también abarcaba a pagar un pedido, pero en realidad ahora eso es parte de lo que originalmente era “aceptar presupuesto de servicio”, porque si acepto es porque pago. |  |

12. 

