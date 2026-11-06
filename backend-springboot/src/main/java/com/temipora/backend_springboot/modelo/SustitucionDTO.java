package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
import java.util.List;

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
public class SustitucionDTO {

    private Long idSustitucion;
    private String motivo;
    private String mensaje;
    private boolean puedeQuedarEnCola;
    private Integer cuposDisponibles;
    private Integer cuposTotales;
    private Long idSolicitud;
    private Long idPedido;
    private LocalDateTime fechaCreacion;
    private ServicioDTO servicioOrigen;
    private List<SustitucionOpcionDTO> opciones;
}
