package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResolucionApelacionRequest {

    @NotNull(message = "Indicá si la apelación se acepta o se rechaza.")
    private Boolean aceptada;

    @Size(max = 1000, message = "La respuesta no puede superar los 1000 caracteres.")
    private String respuesta;
}
