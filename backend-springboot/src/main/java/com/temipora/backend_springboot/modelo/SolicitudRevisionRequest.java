package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolicitudRevisionRequest {

    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres.")
    private String motivo;
}
