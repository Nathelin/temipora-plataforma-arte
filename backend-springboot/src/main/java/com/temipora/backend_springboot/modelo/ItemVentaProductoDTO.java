package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemVentaProductoDTO {

    private Long idCompraItem;
    private Long idProducto;
    private Long idVariante;
    private String tituloProducto;
    private String nombreVariante;
    private TipoEntrega tipoEntrega;
    private int cantidad;
    private BigDecimal precioCobro;
    private BigDecimal precioTotalCobro;
    private String imagenUrl;
}
