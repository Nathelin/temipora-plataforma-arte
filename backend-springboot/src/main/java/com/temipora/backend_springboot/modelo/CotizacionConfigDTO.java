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
public class CotizacionConfigDTO {

    private boolean apiHabilitada;
    private Long idMonedaOrigen;
    private String monedaOrigen;
    private Long idMonedaDestino;
    private String monedaDestino;
    private BigDecimal factorManual;
    private BigDecimal factorVigente;
    private String origenFactor;
    private LocalDateTime inicioVigencia;
    private LocalDateTime fechaActualizacion;

    public static CotizacionConfigDTO desde(ConfiguracionCotizacion c, ConversionMoneda vigente) {
        Moneda origen = c.getMonedaOrigen();
        Moneda destino = c.getMonedaDestino();
        return CotizacionConfigDTO.builder()
            .apiHabilitada(c.isApiHabilitada())
            .idMonedaOrigen(origen != null ? origen.getIdMoneda() : null)
            .monedaOrigen(origen != null ? origen.getCodigo() : null)
            .idMonedaDestino(destino != null ? destino.getIdMoneda() : null)
            .monedaDestino(destino != null ? destino.getCodigo() : null)
            .factorManual(c.getFactorManual())
            .factorVigente(vigente != null ? vigente.getFactor() : null)
            .origenFactor(vigente != null ? vigente.getOrigen() : null)
            .inicioVigencia(vigente != null ? vigente.getInicioValidez() : null)
            .fechaActualizacion(c.getFechaActualizacion())
            .build();
    }
}
