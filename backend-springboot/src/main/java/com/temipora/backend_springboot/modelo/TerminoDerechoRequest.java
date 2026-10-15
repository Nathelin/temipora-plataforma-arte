package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TerminoDerechoRequest {

    @NotNull(message = "El derecho es obligatorio.")
    private Long idDerechoTermino;

    private boolean disponible;

    @DecimalMin(value = "0", message = "El porcentaje no puede ser negativo")
    @DecimalMax(value = "1000", message = "El porcentaje es demasiado alto")
    private BigDecimal porcentajeExtra;
}
