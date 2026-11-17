package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReactivarCuentaRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio.")
    private String nombreUsuario;

    @NotBlank(message = "La contraseña es obligatoria.")
    private String contrasenia;
}
