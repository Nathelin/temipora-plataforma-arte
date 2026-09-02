package com.temipora.backend_springboot.modelo;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistroUsuarioDTO {

    //private String nombre;

    //private String apellido;

    @NotBlank
    private String nombreUsuario;

    @NotBlank
    @Email
    private String correo;

    @NotBlank
    @Size(min = 8)
    private String contrasenia;

    @NotBlank
    private String confirmarContrasenia;

    @AssertTrue(message = "Debes aceptar los términos y condiciones")
    private boolean acceptaTOS;
}