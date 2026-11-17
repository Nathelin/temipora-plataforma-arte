package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BajaCuentaRequest {

    @NotBlank(message = "Debés ingresar tu contraseña para dar de baja la cuenta.")
    private String contrasenia;
}
