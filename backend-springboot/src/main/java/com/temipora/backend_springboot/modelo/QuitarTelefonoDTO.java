package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuitarTelefonoDTO {

    @NotBlank(message = "Debés ingresar tu contraseña actual.")
    private String contraseniaActual;
}
