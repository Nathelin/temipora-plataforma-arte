package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TerminosPlataformaRequest {

    @NotBlank(message = "El título es obligatorio.")
    @Size(max = 120, message = "El título no puede superar los 120 caracteres.")
    private String titulo;

    @NotBlank(message = "El contenido es obligatorio.")
    private String contenido;
}
