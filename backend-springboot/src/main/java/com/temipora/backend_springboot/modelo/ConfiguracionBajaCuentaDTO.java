package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class ConfiguracionBajaCuentaDTO {

    @NotNull(message = "Los días para reactivar son obligatorios.")
    @Min(value = 1, message = "El plazo para reactivar debe ser al menos 1 día.")
    @Max(value = 365, message = "El plazo para reactivar no puede superar los 365 días.")
    private Integer diasParaReactivar;

    private LocalDateTime fechaActualizacion;

    public static ConfiguracionBajaCuentaDTO desdeEntidad(ConfiguracionBajaCuenta config) {
        return ConfiguracionBajaCuentaDTO.builder()
            .diasParaReactivar(config.getDiasParaReactivar())
            .fechaActualizacion(config.getFechaActualizacion())
            .build();
    }
}
