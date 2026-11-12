package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReventaRequest {

    private Long idCompraItem;

    @NotNull(message = "Indicá el precio de reventa.")
    @DecimalMin(value = "0.01", message = "El precio de reventa debe ser mayor a cero.")
    @Digits(integer = 10, fraction = 2,
        message = "El precio de reventa admite hasta 2 decimales.")
    private BigDecimal precio;
}
