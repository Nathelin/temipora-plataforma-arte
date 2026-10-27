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
public class ComisionConfigDTO {

    private BigDecimal porcentajeServicio;
    private BigDecimal porcentajeProducto;
    private LocalDateTime fechaActualizacion;

    public static ComisionConfigDTO desdeConfiguracion(ConfiguracionComision config) {
        return ComisionConfigDTO.builder()
            .porcentajeServicio(config.getPorcentajeServicio())
            .porcentajeProducto(config.getPorcentajeProducto())
            .fechaActualizacion(config.getFechaActualizacion())
            .build();
    }
}
