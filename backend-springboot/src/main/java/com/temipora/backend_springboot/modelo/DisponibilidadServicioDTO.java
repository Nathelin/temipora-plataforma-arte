package com.temipora.backend_springboot.modelo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisponibilidadServicioDTO {

    private Long idServicio;
    private Integer cuposTotales;
    private Integer cuposOcupados;
    private Integer cuposDisponibles;
    private String modoRecepcion;
    private Integer maxDibujosPorSolicitud;
    private boolean permiteListaEspera;
    private boolean tieneSolicitudActiva;
    private boolean tieneSolicitudEnEspera;
    private boolean puedeSolicitarCupo;
    private boolean puedeListaEspera;
    private boolean puedeSolicitar;
    private String mensaje;
}
