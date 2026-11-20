package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class PedidoResumenDTO {

    private Long idPedido;
    private Long idSolicitud;
    private String nombreServicio;
    private UsuarioResumenDTO contraparte;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDate limiteEntrega;
    private LocalDate fechaEntregaPrevista;
    private int diasDemoraCliente;
    private boolean entregaAtrasada;
    private BigDecimal precioFinalCobro;
    private String monedaCobro;
    private String proximoHitoNombre;
    private String proximoHitoTipo;
    private String proximoHitoEstado;
    private BigDecimal proximoHitoMontoMostrado;
    private String monedaMostrada;
    private String rolVista;
    private int hitosTotales;
    private int hitosPagados;
    private int porcentajeAvance;
    private boolean puedePagar;
    private boolean puedeEntregar;
    private boolean puedeAprobar;
    private Long idConversacion;
    private String estadoConversacion;
    private long noLeidos;
}
