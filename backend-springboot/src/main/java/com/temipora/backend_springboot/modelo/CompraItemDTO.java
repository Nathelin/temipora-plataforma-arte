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
public class CompraItemDTO {

    private Long idCompraItem;
    private Long idProducto;
    private Long idVariante;
    private String tituloProducto;
    private String nombreVariante;
    private TipoEntrega tipoEntrega;
    private int cantidad;
    private BigDecimal precioOriginal;
    private String monedaOriginal;
    private BigDecimal precioCobro;
    private BigDecimal comisionMonto;
    private EstadoEntregaItem estadoEntrega;
    private boolean puedeDescargar;
    private String motivoNoDescarga;
}
