package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarContraseniaDTO {

    @NotBlank(message = "Debés ingresar tu contraseña actual.")
    private String contraseniaActual;

    @NotBlank(message = "Debés ingresar la nueva contraseña.")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).*$", message = "La contraseña debe incluir al menos una mayúscula y un número")
    private String contraseniaNueva;

    @NotBlank(message = "Debés confirmar la nueva contraseña.")
    private String confirmarContrasenia;
}
