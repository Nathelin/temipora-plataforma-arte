package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FiltroProductoRequest {

    private String titulo;
    private String nombreArtista;
    private Long idTipoProducto;
    private Long idMedio;
    private Boolean sensible;
    private BigDecimal precioMin;
    private BigDecimal precioMax;
    /** Deja fuera los productos sin stock disponible. */
    private Boolean soloConStock;
}
