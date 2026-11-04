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
public class VentaDTO {

    private Long idPago;
    private LocalDateTime fecha;
    private LocalDateTime fechaAcreditacion;
    private Long idPedido;
    private String nombreServicio;
    private String nombreUsuarioCliente;
    private String nombreHito;
    private Integer ordenHito;
    private String tipoHito;
    private String estado;
    private BigDecimal monto;
    private String moneda;
    private BigDecimal montoCobro;
    private String monedaCobro;
    private BigDecimal comisionMontoCobro;
    private BigDecimal montoNetoCobro;
    private BigDecimal netoMostrado;
    private String monedaMostrada;
    private BigDecimal arancelMp;
    private BigDecimal netoRecibidoMp;
    private LocalDateTime fechaAprobacion;
    private LocalDateTime fechaLiberacion;
    private String medioPago;
    private String tipoMedioPago;
    private Integer cuotas;
    private String ultimosCuatro;
    private boolean puedeSincronizar;
    private String proveedor;
}
