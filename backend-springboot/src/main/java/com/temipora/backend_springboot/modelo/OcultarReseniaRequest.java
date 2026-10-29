package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OcultarReseniaRequest {

    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
    private String motivo;
}
