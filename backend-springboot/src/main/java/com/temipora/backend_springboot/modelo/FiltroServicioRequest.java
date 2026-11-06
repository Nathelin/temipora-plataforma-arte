package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FiltroServicioRequest {

    private String nombre;
    private String nombreArtista;
    private Long idTipoServicio;
    private Boolean sensible;
    private BigDecimal precioMin;
    private BigDecimal precioMax;
    private Integer tiempoMaxDias;
    /**
     * Incluye en el catálogo los servicios que hoy no pueden recibir una
     * solicitud (sin cupos, solo espera o cerrados), siempre después de los
     * disponibles. Por defecto se incluyen.
     */
    private Boolean incluirSinCupos;
}
