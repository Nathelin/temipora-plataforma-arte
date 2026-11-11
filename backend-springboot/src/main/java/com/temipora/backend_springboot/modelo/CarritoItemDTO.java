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
public class CarritoItemDTO {

    private Long idCarritoItem;
    private Long idVariante;
    private Long idProducto;
    private Long idArtista;
    private String titulo;
    private String nombreVariante;
    private String imagenUrl;
    private BigDecimal precioOriginal;
    private String monedaOriginal;
    private BigDecimal precioCobro;
    private String monedaCobro;
    private Integer cantidad;
    private boolean cantidadEditable;
    private String motivoCantidadFija;
    private Integer stockDisponible;
    private boolean esUnica;
    private TipoEntrega tipoEntrega;
    private boolean disponible;
    private String motivoNoDisponible;
}
