package com.temipora.backend_springboot.modelo;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FormularioRequest {

    @NotEmpty(message = "El formulario debe tener al menos una pregunta.")
    private List<@Valid PreguntaRequest> preguntas;
}
