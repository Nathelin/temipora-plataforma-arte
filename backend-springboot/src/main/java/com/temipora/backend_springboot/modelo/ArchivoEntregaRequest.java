package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArchivoEntregaRequest {

    private Long idItemSolicitud;

    @Size(max = 255, message = "El nombre visible no puede superar los 255 caracteres.")
    private String nombreVisible;
}
