package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TerminosPlataformaAceptacionRequest {

    @NotNull(message = "La versión de los términos es obligatoria.")
    @Min(value = 1, message = "La versión de los términos no es válida.")
    private Integer version;
}
