package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LicenciaProductoRequest {

    /**
     * En un borrador puede quedar vacío; al publicar se exige al menos uno.
     */
    private Set<@NotNull(message = "El derecho no puede ser nulo.") Long> idsDerechos;

    private boolean permiteModificaciones;

    private boolean requiereCredito;

    @DecimalMin(value = "0", message = "El porcentaje de regalía no puede ser negativo")
    @DecimalMax(value = "10", message = "El porcentaje de regalía no puede superar el 10%")
    private BigDecimal porcentajeRegalia;

    @Size(max = 2000, message = "El texto adicional no puede superar los 2000 caracteres")
    private String textoAdicional;
}
