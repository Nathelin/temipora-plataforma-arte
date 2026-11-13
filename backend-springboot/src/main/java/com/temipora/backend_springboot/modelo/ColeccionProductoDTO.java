package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Producto comprado tal como se muestra en la colección pública del perfil.
 * El título y la variante salen del snapshot de la compra, y la imagen es la
 * versión exacta que se compró, no la portada actual del producto.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ColeccionProductoDTO {

    private Long idCompraItem;
    private Long idCompra;
    private Long idProducto;
    private Long idVariante;

    private String tituloProducto;
    private String nombreVariante;
    private String imagenUrl;

    private LocalDateTime fechaCompra;
    private LocalDateTime fechaEntrega;

    private boolean esReventa;
    private boolean esPiezaUnica;

    private String estadoProducto;
    private boolean productoDisponible;

    private UsuarioResumenDTO artista;

    private boolean contenidoSensible;
    private List<String> contenidosSensibles;
    private boolean requiereAdvertencia;

    /** Solo para el dueño: permite administrar qué compras oculta. */
    private Boolean oculto;

    /** Reseña del dueño sobre el producto, si decidió publicarla con su nombre. */
    private ReseniaPublicaResumenDTO resenia;
}
