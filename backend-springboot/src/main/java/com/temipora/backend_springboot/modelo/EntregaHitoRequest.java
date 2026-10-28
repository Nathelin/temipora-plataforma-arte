package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntregaHitoRequest {

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.")
    private String descripcion;

    @NotBlank(message = "Tenés que subir una imagen del entregable.")
    @Size(max = 255, message = "La URL de la imagen no puede superar los 255 caracteres.")
    private String imagen;
}
