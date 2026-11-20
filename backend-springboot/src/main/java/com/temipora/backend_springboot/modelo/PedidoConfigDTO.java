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
            .fechaActualizacion(config.getFechaActualizacion())
            .build();
    }
}
