package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MensajeRequest {

    @NotBlank(message = "El mensaje no puede estar vacío.")
    @Size(max = 4000, message = "El mensaje no puede superar los 4000 caracteres.")
    private String contenido;
}
