package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PresupuestoConfigRequest {

    @NotNull(message = "La vigencia es obligatoria.")
    @Min(value = 1, message = "La vigencia debe ser al menos 1 hora.")
    @Max(value = 8760, message = "La vigencia no puede superar las 8760 horas.")
    private Integer vigenciaHoras;
}
