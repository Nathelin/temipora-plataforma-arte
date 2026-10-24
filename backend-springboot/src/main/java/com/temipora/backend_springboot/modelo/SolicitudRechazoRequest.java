package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SolicitudRechazoRequest {

    @Size(max = 200, message = "El motivo no puede superar los 200 caracteres.")
    private String motivo;
}
