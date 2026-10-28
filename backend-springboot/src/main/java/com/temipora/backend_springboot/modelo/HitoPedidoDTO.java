package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

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
public class HitoPedidoDTO {

    private Long idHitoPedido;
    private Integer orden;
    private String nombre;
    private String tipo;
    private boolean requiereEntrega;
    private BigDecimal monto;
    private BigDecimal montoCobro;
    private BigDecimal montoMostrado;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal comisionMonto;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal comisionMontoCobro;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal comisionMontoMostrado;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal montoNeto;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal montoNetoCobro;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal montoNetoMostrado;
    private String estadoPago;
    private String estadoEntrega;
    private LocalDateTime fechaLimitePago;
    private LocalDateTime fechaPago;
    private LocalDateTime fechaEntrega;
    private LocalDateTime fechaLimiteAprobacion;
    private LocalDateTime fechaAprobacion;
    private String entregaDescripcion;
    private String entregaImagen;
    private boolean puedePagar;
    private boolean puedeEntregar;
    private boolean puedeAprobar;
    private boolean puedeSolicitarRevision;
    private List<PagoDTO> pagos;

    public static HitoPedidoDTO desdeHito(
            HitoPedido hito,
            List<PagoDTO> pagos,
            boolean incluirComision,
            boolean incluirEntrega,
            boolean puedePagar,
            boolean puedeEntregar,
            boolean puedeAprobar,
            boolean puedeSolicitarRevision,
            boolean muestraOrigen,
            boolean muestraCobro,
            BigDecimal factorCobroDestino,
            BigDecimal factorOrigenDestino) {
        BigDecimal montoMostrado = PresupuestoDTO.totalMostrado(
            hito.getMonto(),
            hito.getMontoCobro(),
            muestraOrigen,
            muestraCobro,
            factorCobroDestino,
            factorOrigenDestino);
        BigDecimal comisionMostrada = incluirComision
            ? PresupuestoDTO.totalMostrado(
                hito.getComisionMonto(),
                hito.getComisionMontoCobro(),
                muestraOrigen,
                muestraCobro,
                factorCobroDestino,
                factorOrigenDestino)
            : null;
        BigDecimal netoMostrado = incluirComision
            ? PresupuestoDTO.totalMostrado(
                hito.getMontoNeto(),
                hito.getMontoNetoCobro(),
                muestraOrigen,
                muestraCobro,
                factorCobroDestino,
                factorOrigenDestino)
            : null;

        return HitoPedidoDTO.builder()
            .idHitoPedido(hito.getIdHitoPedido())
            .orden(hito.getOrden())
            .nombre(hito.getNombre())
            .tipo(hito.getTipo() != null ? hito.getTipo().name() : null)
            .requiereEntrega(hito.isRequiereEntrega())
            .monto(hito.getMonto())
            .montoCobro(hito.getMontoCobro())
            .montoMostrado(montoMostrado)
            .comisionMonto(incluirComision ? hito.getComisionMonto() : null)
            .comisionMontoCobro(incluirComision
                ? hito.getComisionMontoCobro() : null)
            .comisionMontoMostrado(comisionMostrada)
            .montoNeto(incluirComision ? hito.getMontoNeto() : null)
            .montoNetoCobro(incluirComision ? hito.getMontoNetoCobro() : null)
            .montoNetoMostrado(netoMostrado)
            .estadoPago(hito.getEstadoPago() != null
                ? hito.getEstadoPago().name()
                : null)
            .estadoEntrega(hito.getEstadoEntrega() != null
                ? hito.getEstadoEntrega().name()
                : null)
            .fechaLimitePago(hito.getFechaLimitePago())
            .fechaPago(hito.getFechaPago())
            .fechaEntrega(hito.getFechaEntrega())
            .fechaLimiteAprobacion(hito.getFechaLimiteAprobacion())
            .fechaAprobacion(hito.getFechaAprobacion())
            .entregaDescripcion(hito.getEntregaDescripcion())
            .entregaImagen(incluirEntrega ? hito.getEntregaImagen() : null)
            .puedePagar(puedePagar)
            .puedeEntregar(puedeEntregar)
            .puedeAprobar(puedeAprobar)
            .puedeSolicitarRevision(puedeSolicitarRevision)
            .pagos(pagos)
            .build();
    }
}
