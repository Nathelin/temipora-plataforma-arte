package com.temipora.backend_springboot.modelo;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReseniaRequest {

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
    private String descripcion;

    @NotNull(message = "La calificación es obligatoria.")
    @Min(value = 1, message = "La calificación debe ser de 1 a 5 estrellas.")
    @Max(value = 5, message = "La calificación debe ser de 1 a 5 estrellas.")
    private Integer calificacionGeneral;

    private List<@Valid ReseniaValorRequest> valores;

    private List<Long> opciones;
}
