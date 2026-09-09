package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarPerfilDTO {

    @Size(max = 25, message = "El nombre de display no puede superar los 25 caracteres")
    private String nombreDisplay;

    @Size(max = 500, message = "La biografía no puede superar los 500 caracteres")
    private String biografia;

    private String urlFotoPerfil;

    private String urlBanner;
}