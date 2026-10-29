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
public class OpcionCriterioDTO {

    private Long idOpcionCriterio;
    private String etiqueta;
    private PolaridadOpcion polaridad;
    private int orden;
    private boolean activo;

    public static OpcionCriterioDTO desdeOpcion(OpcionCriterio opcion) {
        return OpcionCriterioDTO.builder()
            .idOpcionCriterio(opcion.getIdOpcionCriterio())
            .etiqueta(opcion.getEtiqueta())
            .polaridad(opcion.getPolaridad())
            .orden(opcion.getOrden())
            .activo(opcion.isActivo())
            .build();
    }
}
