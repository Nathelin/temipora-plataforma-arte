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
public class PresupuestoConfigDTO {

    private Integer vigenciaHoras;
    private LocalDateTime fechaActualizacion;

    public static PresupuestoConfigDTO desdeConfiguracion(ConfiguracionPresupuesto config) {
        return PresupuestoConfigDTO.builder()
            .vigenciaHoras(config.getVigenciaHoras())
            .fechaActualizacion(config.getFechaActualizacion())
            .build();
    }
}
