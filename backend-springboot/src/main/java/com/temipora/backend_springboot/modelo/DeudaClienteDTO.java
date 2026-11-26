package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pago de entrega final pendiente de un pedido. Es la unidad con la que el
 * cliente regulariza su deuda desde la pantalla restringida.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeudaClienteDTO {

    private Long idPedido;
    private String nombreServicio;
    private Long idArtista;
    private String nombreUsuarioArtista;
    private Long idHitoPedido;
    private String nombreHito;
    private BigDecimal monto;
    private BigDecimal montoCobro;
    private String moneda;
    private String monedaCobro;
    private LocalDateTime fechaEntrega;
    private LocalDateTime fechaLimitePago;
    private LocalDateTime impagoDesde;
    private long diasAtraso;
    private boolean puedePagar;
}
