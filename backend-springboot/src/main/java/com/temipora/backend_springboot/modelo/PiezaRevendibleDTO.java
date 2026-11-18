package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
public class PiezaRevendibleDTO {

    private Long idCompraItem;
    private Long idProducto;
    private Long idVariante;
    private String titulo;
    private String nombreVariante;
    private String imagenUrl;
    private String moneda;
    private BigDecimal precioTope;
    private BigDecimal precioPagado;
    private BigDecimal ultimoPrecio;
    private Long idArtistaOriginal;
    private String nombreArtistaOriginal;
    private TipoEntrega tipoEntrega;
    private LocalDateTime fechaEntrega;
    private LicenciaCompraDTO licencia;
}
