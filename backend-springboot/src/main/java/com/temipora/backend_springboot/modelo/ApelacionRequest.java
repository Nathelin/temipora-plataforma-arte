package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApelacionRequest {

    @NotBlank(message = "Escribí el motivo de tu apelación.")
    @Size(max = 2000, message = "La apelación no puede superar los 2000 caracteres.")
    private String mensaje;
}
