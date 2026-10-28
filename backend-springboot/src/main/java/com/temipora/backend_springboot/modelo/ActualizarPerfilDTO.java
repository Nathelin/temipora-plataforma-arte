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

    @Size(max = 15, message = "El sexo no puede superar los 15 caracteres")
    private String sexo;

    private Long idPais;

    private EPreferenciaContenidoSensible preferenciaSensible;

    private Boolean pedidosPublicos;

    private Long idMonedaPreferida;
}