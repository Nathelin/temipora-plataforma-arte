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
public class SustitucionOpcionDTO {

    private Long idServicio;
    private int orden;
    private double score;
    private String motivo;
    private List<String> caracteristicas;
    private boolean disponible;
    private ServicioDTO servicio;
}
