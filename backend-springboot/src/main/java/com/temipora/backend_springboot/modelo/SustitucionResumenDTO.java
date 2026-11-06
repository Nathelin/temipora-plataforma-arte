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
public class SustitucionResumenDTO {

    private long totalEventos;
    private long resueltasConAlternativa;
    private long resueltasConCola;
    private long descartadas;
    private long abiertas;
    private double tasaSustitucion;
    private List<OrigenFrecuente> origenesFrecuentes;

    public record OrigenFrecuente(Long idServicio, String nombre, long cantidad) {
    }
}
