package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonedaRequest {

    @NotBlank(message = "El código es obligatorio.")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "El código debe tener 3 letras (ISO 4217).")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
    private String nombre;

    @Size(max = 6, message = "El símbolo no puede superar los 6 caracteres")
    private String simbolo;

    private boolean predeterminada;
}
