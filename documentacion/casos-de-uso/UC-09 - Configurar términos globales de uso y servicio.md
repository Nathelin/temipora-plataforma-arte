| UC–09  | Configurar términos globales de uso y servicio |  |
| :---- | :---- | :---- |
| **Objetivos**  **asociados**  | • OBJ–01 Facilitar la conexión entre productores y consumidores • OBJ–02 Formalizar el seguimiento de arte a medida |  |
| **Requisitos**  **asociados**  | • IRQ–01 Información sobre artistas • IRQ-04 Información sobre productos • IRQ-05 Información sobre servicios |  |
| **Descripción**  | El caso de uso inicia cuando un artista desea configurar sus términos globales de uso y servicio. Estos serán utilizados por defecto a la hora de dar de alta nuevos productos y servicios a menos que el artista elija definir otros durante el alta particular de un producto o servicio. |  |
| **Precondición**  | • El artista debe haber iniciado sesión. |  |
| **Secuencia**  | **Paso**  | **Acción**  |
| **normal**  | 1  | El artista selecciona la opción “Configurar términos globales de uso y servicio”. |
|  | 2 | El sistema enseña la información que contienen los términos globales de uso y servicio actuales del artista. Si no cuenta con términos globales, se enseña un formulario que sugiere se establezcan los siguientes datos: Información general. Política de revisiones. Política de cancelaciones. Uso del artista. Uso del cliente. |
|  | 3 | El artista describe sus términos de uso y servicio. |
|  | 4 | El artista confirma los datos ingresados seleccionando “Guardar”. |
|  | 5 | El sistema revisa si el artista dejó el formulario nulo. |
|  | 6 | El sistema guarda los términos de uso y servicio del artista y lo deja disponible para usarse por defecto en el alta y modificación de productos y servicios. |
|  | 7 | El sistema informa al artista que el proceso fue un éxito. |
| **Postcondición**  | Los términos globales de uso y servicios del artista fueron modificados. |  |
| **Excepciones**  | **Paso**  | **Acción**  |
|  | 5.1.1 | Si el sistema detecta que el formulario está vacío, solicita al artista que ingrese algo de información y regresa al paso 2\. |
|  |  |  |
|  |  |  |
| **Rendimiento**  | **Paso**  | **Cota de tiempo**  |
|  | 6 | 1 segundo  |
| **Frecuencia**  | 1 vez/mes  |  |
| **Estabilidad**  | alta  |  |
| **Comentarios**  | Es posible que el artista simplemente escriba que no tiene términos de uso y servicio en el formulario, si lo desea. |  |

      6. 

