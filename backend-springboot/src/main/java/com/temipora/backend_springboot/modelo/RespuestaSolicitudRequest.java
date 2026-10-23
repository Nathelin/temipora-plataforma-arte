package com.temipora.backend_springboot.modelo;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RespuestaSolicitudRequest {

    @NotNull(message = "La pregunta es obligatoria.")
    private Long idPregunta;

    @NotNull(message = "Los valores de la respuesta son obligatorios.")
    private List<String> valores;
}
