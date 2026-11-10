package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Producto tal como se muestra en el catálogo público. Es deliberadamente
 * liviano: no incluye variantes completas, entregables ni datos de gestión.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoCatalogoDTO {

    private Long idProducto;
    private String titulo;
    private String descripcion;
    private String imagenPortada;

    private Long idArtista;
    private String nombreArtista;
    private String nombreDisplayArtista;
    private String urlFotoArtista;

    private Long idTipoProducto;
    private String nombreTipoProducto;
    private Long idMedio;
    private String nombreMedio;

    /** Precio de la variante más barata disponible. */
    private BigDecimal precioDesde;
    private String moneda;

    private TipoEntrega tipoEntrega;

    /** Stock total sumando variantes; null si alguna es ilimitada. */
    private Integer stockTotal;
    private boolean tieneStockIlimitado;
    private boolean esPiezaUnica;
    private boolean disponible;
    private int cantidadVariantes;

    private boolean contenidoSensible;
    private List<String> contenidosSensibles;

    private BigDecimal calificacionPromedio;
    private int cantidadResenias;
}
