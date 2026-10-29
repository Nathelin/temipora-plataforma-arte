package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpcionCriterioRequest {

    @NotBlank(message = "La etiqueta es obligatoria.")
    @Size(max = 50, message = "La etiqueta no puede superar los 50 caracteres")
    private String etiqueta;

    private PolaridadOpcion polaridad;

    @Min(value = 0, message = "El orden no puede ser negativo.")
    @Max(value = 9999, message = "El orden no puede superar 9999.")
    private Integer orden;
}
