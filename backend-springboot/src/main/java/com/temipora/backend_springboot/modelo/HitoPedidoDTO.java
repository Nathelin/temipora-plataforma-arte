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
    private Long idPedido;
    private Integer orden;
    private String nombre;
    private String descripcion;
    private BigDecimal porcentaje;
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal arancelMpEstimado;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal arancelMpEstimadoCobro;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal arancelMpEstimadoMostrado;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal montoNetoConArancel;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal montoNetoConArancelCobro;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal montoNetoConArancelMostrado;
    private String estadoPago;
    private String estadoEntrega;
    private LocalDateTime fechaLimitePago;
    private LocalDateTime fechaPago;
    private LocalDateTime fechaEntrega;
    private LocalDateTime fechaLimiteAprobacion;
    private LocalDateTime fechaAprobacion;
    private String entregaDescripcion;
    private String entregaImagen;
    private List<EntregaHitoDTO> entregas;
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
            List<EntregaHitoDTO> entregas,
            boolean puedePagar,
            boolean puedeEntregar,
            boolean puedeAprobar,
            boolean puedeSolicitarRevision,
            boolean muestraOrigen,
            boolean muestraCobro,
            BigDecimal factorCobroDestino,
            BigDecimal factorOrigenDestino) {
        HitoPresupuesto origen = hito.getHitoPresupuesto();
        BigDecimal montoMostrado = PresupuestoDTO.totalMostrado(
            origen.getMonto(),
            origen.getMontoCobro(),
            muestraOrigen,
            muestraCobro,
            factorCobroDestino,
            factorOrigenDestino);
        BigDecimal comisionMostrada = incluirComision
            ? PresupuestoDTO.totalMostrado(
                origen.getComisionMonto(),
                origen.getComisionMontoCobro(),
                muestraOrigen,
                muestraCobro,
                factorCobroDestino,
                factorOrigenDestino)
            : null;
        BigDecimal netoMostrado = incluirComision
            ? PresupuestoDTO.totalMostrado(
                origen.getMontoNeto(),
                origen.getMontoNetoCobro(),
                muestraOrigen,
                muestraCobro,
                factorCobroDestino,
                factorOrigenDestino)
            : null;

        return HitoPedidoDTO.builder()
            .idHitoPedido(hito.getIdHitoPedido())
            .idPedido(hito.getPedido() != null
                ? hito.getPedido().getIdPedido()
                : null)
            .orden(hito.getOrden())
            .nombre(origen.getNombre())
            .descripcion(origen.getDescripcion())
            .porcentaje(origen.getPorcentaje())
            .tipo(origen.getTipo() != null ? origen.getTipo().name() : null)
            .requiereEntrega(origen.isRequiereEntrega())
            .monto(origen.getMonto())
            .montoCobro(origen.getMontoCobro())
            .montoMostrado(montoMostrado)
            .comisionMonto(incluirComision ? origen.getComisionMonto() : null)
            .comisionMontoCobro(incluirComision
                ? origen.getComisionMontoCobro() : null)
            .comisionMontoMostrado(comisionMostrada)
            .montoNeto(incluirComision ? origen.getMontoNeto() : null)
            .montoNetoCobro(incluirComision ? origen.getMontoNetoCobro() : null)
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
            .entregas(entregas)
            .puedePagar(puedePagar)
            .puedeEntregar(puedeEntregar)
            .puedeAprobar(puedeAprobar)
            .puedeSolicitarRevision(puedeSolicitarRevision)
            .pagos(pagos)
            .build();
    }

    public void aplicarArancelMp(BigDecimal porcentajeTotal) {
        if (porcentajeTotal == null || porcentajeTotal.signum() < 0) {
            return;
        }
        this.arancelMpEstimado = PresupuestoDTO.parte(monto, porcentajeTotal);
        this.arancelMpEstimadoCobro = PresupuestoDTO.parte(
            montoCobro, porcentajeTotal);
        this.arancelMpEstimadoMostrado = PresupuestoDTO.parte(
            montoMostrado != null ? montoMostrado : monto, porcentajeTotal);
        this.montoNetoConArancel = PresupuestoDTO.restar(
            montoNeto, arancelMpEstimado);
        this.montoNetoConArancelCobro = PresupuestoDTO.restar(
            montoNetoCobro, arancelMpEstimadoCobro);
        this.montoNetoConArancelMostrado = PresupuestoDTO.restar(
            montoNetoMostrado, arancelMpEstimadoMostrado);
    }
}
