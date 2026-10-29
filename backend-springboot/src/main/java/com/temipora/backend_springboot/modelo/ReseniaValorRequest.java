package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReseniaValorRequest {

    @NotNull(message = "El criterio es obligatorio.")
    private Long idCriterio;

    @NotNull(message = "El valor del criterio es obligatorio.")
    @Min(value = 1, message = "El valor del criterio debe ser de 1 a 5 estrellas.")
    @Max(value = 5, message = "El valor del criterio debe ser de 1 a 5 estrellas.")
    private Integer valor;
}
