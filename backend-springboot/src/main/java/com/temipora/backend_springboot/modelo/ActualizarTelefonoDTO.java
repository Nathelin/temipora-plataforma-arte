package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarTelefonoDTO {

    @Size(max = 30, message = "El teléfono actual no puede superar los 30 caracteres")
    private String telefonoActual;

    @NotBlank(message = "Ingresá el nuevo teléfono.")
    @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
    private String telefonoNuevo;

    @NotBlank(message = "Debés ingresar tu contraseña actual.")
    private String contraseniaActual;
}
