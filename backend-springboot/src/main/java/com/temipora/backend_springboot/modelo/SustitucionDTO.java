package com.temipora.backend_springboot.modelo;

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
    private List<SustitucionOpcionDTO> opciones;
}
