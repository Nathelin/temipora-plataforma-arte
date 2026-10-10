package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarCorreoDTO {

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "El correo no es válido.")
    @Size(max = 40, message = "El correo no puede superar los 40 caracteres")
    private String correo;

    @NotBlank(message = "Debés ingresar tu contraseña actual.")
    private String contraseniaActual;
}
