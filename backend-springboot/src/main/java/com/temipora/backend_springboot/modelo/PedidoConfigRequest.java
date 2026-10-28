package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PedidoConfigRequest {

    @NotNull(message = "Los días de aprobación automática son obligatorios.")
    @Min(value = 1, message = "La aprobación automática debe ser al menos 1 día.")
    @Max(value = 60, message = "La aprobación automática no puede superar los 60 días.")
    private Integer diasAprobacionAutomatica;

    @NotNull(message = "Los días para pagar una cuota son obligatorios.")
    @Min(value = 1, message = "El plazo de pago debe ser al menos 1 día.")
    @Max(value = 90, message = "El plazo de pago no puede superar los 90 días.")
    private Integer diasPagoCuota;

    @NotNull(message = "El máximo de hitos es obligatorio.")
    @Min(value = 1, message = "Tiene que haber al menos 1 hito.")
    @Max(value = 6, message = "El máximo de hitos no puede superar 6.")
    private Integer maxHitos;

    @NotNull(message = "El porcentaje máximo del primer hito es obligatorio.")
    @DecimalMin(value = "1.00", message = "El primer hito debe permitir al menos el 1%.")
    @DecimalMax(value = "100.00", message = "El primer hito no puede superar el 100%.")
    @Digits(integer = 3, fraction = 2,
        message = "El porcentaje máximo admite hasta 2 decimales.")
    private BigDecimal porcentajeMaxPrimerHito;

    @NotNull(message = "El monto mínimo por cuota es obligatorio.")
    @DecimalMin(value = "0.00", message = "El monto mínimo no puede ser negativo.")
    @DecimalMax(value = "10000000.00",
        message = "El monto mínimo no puede superar los 10.000.000.")
    @Digits(integer = 10, fraction = 2,
        message = "El monto mínimo admite hasta 2 decimales.")
    private BigDecimal montoMinimoCuota;
}
