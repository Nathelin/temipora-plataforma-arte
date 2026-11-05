package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

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
public class HitoPresupuestoDTO {

    private Long idHitoPresupuesto;
    private Integer orden;
    private String nombre;
    private String descripcion;
    private String tipo;
    private BigDecimal porcentaje;
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
    private boolean requiereEntrega;

    public static HitoPresupuestoDTO desdeHito(
            HitoPresupuesto hito,
            boolean muestraOrigen,
            boolean muestraCobro,
            BigDecimal factorCobroDestino,
            BigDecimal factorOrigenDestino,
            boolean incluirComision) {
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

        return HitoPresupuestoDTO.builder()
            .idHitoPresupuesto(hito.getIdHitoPresupuesto())
            .orden(hito.getOrden())
            .nombre(hito.getNombre())
            .descripcion(hito.getDescripcion())
            .tipo(hito.getTipo() != null ? hito.getTipo().name() : null)
            .porcentaje(hito.getPorcentaje())
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
            .requiereEntrega(hito.isRequiereEntrega())
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
