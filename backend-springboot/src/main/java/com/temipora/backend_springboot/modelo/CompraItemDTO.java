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
public class CompraItemDTO {

    private Long idCompraItem;
    private Long idProducto;
    private Long idVariante;
    private String tituloProducto;
    private String nombreVariante;
    private TipoEntrega tipoEntrega;
    private OrigenCompraItem origen;
    private Long idPublicacionReventa;
    private int cantidad;
    private BigDecimal precioOriginal;
    private String monedaOriginal;
    private BigDecimal precioCobro;
    private BigDecimal precioTotalCobro;
    private EstadoEntregaItem estadoEntrega;
    private boolean puedeDescargar;
    private String motivoNoDescarga;

    private Long idArtistaOriginal;
    private String nombreArtistaOriginal;
    private String nombreDisplayArtistaOriginal;
    private String urlFotoArtistaOriginal;
    private String imagenUrl;
    private String sha256Archivo;
    private LocalDateTime fechaEntrega;
    private boolean tieneLedger;
    private LicenciaCompraDTO licencia;
}
