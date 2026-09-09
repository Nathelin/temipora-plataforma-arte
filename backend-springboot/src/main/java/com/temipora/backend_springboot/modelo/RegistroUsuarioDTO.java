package com.temipora.backend_springboot.modelo;


import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistroUsuarioDTO {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String nombreUsuario;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String contrasenia;

    @NotBlank(message = "Debe confirmar la contraseña")
    private String confirmarContrasenia;

    @AssertTrue(message = "Debes aceptar los términos y condiciones")
    private boolean acceptaTOS;

    @Size(min = 2, max = 25, message = "El nombre debe tener entre 2 y 25 caracteres")
    private String nombre;

    @Size(min = 2, max = 25, message = "El apellido debe tener entre 2 y 25 caracteres")
    private String apellido;

    private LocalDate fechaNacimiento;

    @Size(max = 500, message = "La biografía no puede superar los 500 caracteres")
    private String biografia;

    @NotNull(message = "Debe seleccionar un rol")
    private ERol rol;

    private String urlFotoPerfil;
}