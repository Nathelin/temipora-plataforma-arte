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
    private BigDecimal comisionMonto;
    private BigDecimal comisionMontoCobro;
    private BigDecimal comisionMontoMostrado;
    private BigDecimal montoNeto;
    private BigDecimal montoNetoCobro;
    private BigDecimal montoNetoMostrado;
    private boolean requiereEntrega;

    public static HitoPresupuestoDTO desdeHito(
            HitoPresupuesto hito,
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
        BigDecimal comisionMostrada = PresupuestoDTO.totalMostrado(
            hito.getComisionMonto(),
            hito.getComisionMontoCobro(),
            muestraOrigen,
            muestraCobro,
            factorCobroDestino,
            factorOrigenDestino);
        BigDecimal netoMostrado = PresupuestoDTO.totalMostrado(
            hito.getMontoNeto(),
            hito.getMontoNetoCobro(),
            muestraOrigen,
            muestraCobro,
            factorCobroDestino,
            factorOrigenDestino);

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
            .comisionMonto(hito.getComisionMonto())
            .comisionMontoCobro(hito.getComisionMontoCobro())
            .comisionMontoMostrado(comisionMostrada)
            .montoNeto(hito.getMontoNeto())
            .montoNetoCobro(hito.getMontoNetoCobro())
            .montoNetoMostrado(netoMostrado)
            .requiereEntrega(hito.isRequiereEntrega())
            .build();
    }
}
