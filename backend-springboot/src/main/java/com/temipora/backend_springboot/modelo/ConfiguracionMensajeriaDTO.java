package com.temipora.backend_springboot.modelo;

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
public class ConfiguracionMensajeriaDTO {

    private Integer longitudMaximaMensaje;
    private Integer longitudMaximaMotivo;
    private Integer horasEsperaReenvio;
    private Integer maxSolicitudesPendientes;
    private Integer maxSolicitudesPorDia;
    private Integer maxMensajesPorDia;
    private Integer diasVencimientoSolicitud;
    private LocalDateTime fechaActualizacion;

    public static ConfiguracionMensajeriaDTO desdeConfiguracion(
            ConfiguracionMensajeria config) {
        return ConfiguracionMensajeriaDTO.builder()
            .longitudMaximaMensaje(config.getLongitudMaximaMensaje())
            .longitudMaximaMotivo(config.getLongitudMaximaMotivo())
            .horasEsperaReenvio(config.getHorasEsperaReenvio())
            .maxSolicitudesPendientes(config.getMaxSolicitudesPendientes())
            .maxSolicitudesPorDia(config.getMaxSolicitudesPorDia())
            .maxMensajesPorDia(config.getMaxMensajesPorDia())
            .diasVencimientoSolicitud(config.getDiasVencimientoSolicitud())
            .fechaActualizacion(config.getFechaActualizacion())
            .build();
    }
}
