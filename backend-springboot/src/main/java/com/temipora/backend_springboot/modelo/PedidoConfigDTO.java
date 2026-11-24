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
public class PedidoConfigDTO {

    private Integer diasAprobacionAutomatica;
    private Integer diasPagoCuota;
    private Integer revisionesMaximasPorHito;
    private Integer diasReentrega;
    private Integer maxHitos;
    private BigDecimal porcentajeMaxPrimerHito;
    private BigDecimal montoMinimoCuota;
    private Integer diasGraciaEntrega;
    private Integer diasSancionVentas;
    private Integer diasReapelacion;
    private Integer diasCierreTrasBloqueo;
    private Integer diasGraciaPago;
    private Integer diasSancionCompras;
    private LocalDateTime sancionesAutomaticasDesde;
    private LocalDateTime fechaActualizacion;

    public static PedidoConfigDTO desdeConfiguracion(ConfiguracionPedido config) {
        return PedidoConfigDTO.builder()
            .diasAprobacionAutomatica(config.getDiasAprobacionAutomatica())
            .diasPagoCuota(config.getDiasPagoCuota())
            .revisionesMaximasPorHito(config.getRevisionesMaximasPorHito())
            .diasReentrega(config.getDiasReentrega())
            .maxHitos(config.getMaxHitos())
            .porcentajeMaxPrimerHito(config.getPorcentajeMaxPrimerHito())
            .montoMinimoCuota(config.getMontoMinimoCuota())
            .diasGraciaEntrega(config.getDiasGraciaEntrega())
            .diasSancionVentas(config.getDiasSancionVentas())
            .diasReapelacion(config.getDiasReapelacion())
            .diasCierreTrasBloqueo(config.getDiasCierreTrasBloqueo())
            .diasGraciaPago(config.getDiasGraciaPago())
            .diasSancionCompras(config.getDiasSancionCompras())
            .sancionesAutomaticasDesde(config.getSancionesAutomaticasDesde())
            .fechaActualizacion(config.getFechaActualizacion())
            .build();
    }
}
