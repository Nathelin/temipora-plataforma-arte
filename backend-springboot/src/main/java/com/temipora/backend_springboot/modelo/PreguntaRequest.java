package com.temipora.backend_springboot.modelo;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreguntaRequest {

    @NotNull(message = "El tipo de campo es obligatorio.")
    private Long idTipoCampo;

    @NotBlank(message = "El enunciado es obligatorio.")
    @Size(max = 250, message = "El enunciado no puede superar los 250 caracteres")
    private String enunciado;

    @Size(max = 250, message = "La ayuda no puede superar los 250 caracteres")
    private String ayuda;

    private boolean obligatoria;

    private List<String> opciones;
}
