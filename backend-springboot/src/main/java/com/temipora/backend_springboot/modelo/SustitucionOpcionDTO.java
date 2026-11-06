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
public class SustitucionOpcionDTO {

    private Long idServicio;
    private int orden;
    private double score;
    private String motivo;
    private boolean disponible;
    private ServicioDTO servicio;
}
