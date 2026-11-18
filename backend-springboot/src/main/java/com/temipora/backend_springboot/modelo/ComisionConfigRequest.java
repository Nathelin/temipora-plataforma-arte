package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComisionConfigRequest {

    @NotNull(message = "La comisión de servicios es obligatoria.")
    @DecimalMin(value = "0.00", message = "La comisión de servicios no puede ser negativa.")
    @DecimalMax(value = "50.00", message = "La comisión de servicios no puede superar el 50%.")
    @Digits(integer = 3, fraction = 2,
        message = "La comisión de servicios admite hasta 2 decimales.")
    private BigDecimal porcentajeServicio;

    @NotNull(message = "La comisión de productos es obligatoria.")
    @DecimalMin(value = "0.00", message = "La comisión de productos no puede ser negativa.")
    @DecimalMax(value = "50.00", message = "La comisión de productos no puede superar el 50%.")
    @Digits(integer = 3, fraction = 2,
        message = "La comisión de productos admite hasta 2 decimales.")
    private BigDecimal porcentajeProducto;

    @NotNull(message = "La comisión de reventas es obligatoria.")
    @DecimalMin(value = "0.00", message = "La comisión de reventas no puede ser negativa.")
    @DecimalMax(value = "50.00", message = "La comisión de reventas no puede superar el 50%.")
    @Digits(integer = 3, fraction = 2,
        message = "La comisión de reventas admite hasta 2 decimales.")
    private BigDecimal porcentajeReventa;
}
