package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TerminoTemaRequest {

    private Long idTemaTermino;

    @Size(max = 120, message = "El tema no puede superar los 120 caracteres")
    private String nombre;

    @NotNull(message = "El estado del tema es obligatorio.")
    private EEstadoTema estado;
}
