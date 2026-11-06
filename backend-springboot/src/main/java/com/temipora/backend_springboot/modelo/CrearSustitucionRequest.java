package com.temipora.backend_springboot.modelo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearSustitucionRequest {

    private Long idServicioOrigen;

    /**
     * Superficie desde la que se pidió la búsqueda. Si no llega o no es
     * válida se asume DETALLE.
     */
    private String disparador;
}
