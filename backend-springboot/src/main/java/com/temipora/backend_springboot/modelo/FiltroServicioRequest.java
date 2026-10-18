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
}
