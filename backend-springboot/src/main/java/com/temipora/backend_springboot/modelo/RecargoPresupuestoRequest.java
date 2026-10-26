package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecargoPresupuestoRequest {

    @NotBlank(message = "La descripción del recargo es obligatoria.")
    @Size(max = 100, message = "La descripción del recargo no puede superar los 100 caracteres.")
    private String descripcion;

    @NotNull(message = "El tipo de recargo es obligatorio.")
    private TipoRecargo tipo;

    @NotNull(message = "El valor del recargo es obligatorio.")
    @DecimalMin(value = "0.01", message = "El valor del recargo debe ser mayor a cero.")
    @Digits(integer = 10, fraction = 2, message = "El valor del recargo no es válido.")
    private BigDecimal valor;
}
