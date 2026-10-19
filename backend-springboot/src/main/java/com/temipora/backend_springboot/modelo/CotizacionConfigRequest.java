package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CotizacionConfigRequest {

    @NotNull(message = "Debe indicar si la actualización desde la API está habilitada.")
    private Boolean apiHabilitada;

    @NotNull(message = "La moneda de origen es obligatoria.")
    private Long idMonedaOrigen;

    @NotNull(message = "La moneda de destino es obligatoria.")
    private Long idMonedaDestino;

    @DecimalMin(value = "0", inclusive = false, message = "El factor manual debe ser mayor a cero.")
    private BigDecimal factorManual;
}
