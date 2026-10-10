package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaisRequest {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
    private String nombre;

    @NotBlank(message = "El código ISO es obligatorio.")
    @Pattern(regexp = "^[A-Za-z]{2}$", message = "El código ISO debe tener 2 letras.")
    private String codigoIso;

    @NotBlank(message = "El prefijo telefónico es obligatorio.")
    @Pattern(regexp = "^\\+\\d{1,4}$", message = "El prefijo telefónico debe tener el formato +NNN.")
    private String prefijoTelefonico;
}
