package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HitoPresupuestoRequest {

    @Size(max = 60, message = "El nombre del hito no puede superar los 60 caracteres.")
    private String nombre;

    @Size(max = 250, message = "La descripción no puede superar los 250 caracteres.")
    private String descripcion;

    @NotNull(message = "Cada hito debe indicar su porcentaje.")
    @DecimalMin(value = "0.01", message = "El porcentaje del hito debe ser mayor a cero.")
    @DecimalMax(value = "100.00", message = "El porcentaje del hito no puede superar el 100%.")
    @Digits(integer = 3, fraction = 2,
        message = "El porcentaje del hito admite hasta 2 decimales.")
    private BigDecimal porcentaje;

    /**
     * Si el hito exige un entregable que el cliente debe aprobar. Por defecto
     * se exige en todos los hitos; la seña puede desactivarlo (encargo tipo
     * skeb, sin entregas intermedias).
     */
    private Boolean requiereEntrega;
}
