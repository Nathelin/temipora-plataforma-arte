package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una venta de producto desde el lado del artista. Cada fila representa un
 * {@link CompraGrupo} pagado: la unidad de liquidación por artista. Puede
 * contener uno o varios productos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaProductoDTO {

    private Long idCompraGrupo;
    private Long idCompra;
    private LocalDateTime fecha;
    private String nombreUsuarioCliente;
    private int cantidadItems;
    @Builder.Default
    private List<ItemVentaProductoDTO> items = new ArrayList<>();
    private BigDecimal montoCobro;
    private String monedaCobro;
    private BigDecimal comisionMontoCobro;
    private BigDecimal montoNetoCobro;
    private BigDecimal netoMostrado;
    private String monedaMostrada;
    private BigDecimal arancelMp;
    private BigDecimal netoRecibidoMp;
    private String estado;
    private String proveedor;
}
