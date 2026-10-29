package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CriterioReseniaRequest {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 250, message = "La descripción no puede superar los 250 caracteres")
    private String descripcion;

    @Size(max = 500, message = "Las observaciones no pueden superar los 500 caracteres")
    private String observaciones;

    @NotNull(message = "El ámbito es obligatorio.")
    private AmbitoResenia ambito;

    @NotNull(message = "La escala es obligatoria.")
    private EscalaCriterio escala;

    private boolean obligatorio;

    @Min(value = 0, message = "El orden no puede ser negativo.")
    @Max(value = 9999, message = "El orden no puede superar 9999.")
    private Integer orden;
}
