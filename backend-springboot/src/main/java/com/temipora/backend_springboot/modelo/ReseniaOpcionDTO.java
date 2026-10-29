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
public class ReseniaOpcionDTO {

    private Long idOpcion;
    private String etiqueta;
    private PolaridadOpcion polaridad;
    private Long idCriterio;
    private String nombreCriterio;

    public static ReseniaOpcionDTO desdeOpcion(ReseniaOpcion opcion) {
        CriterioResenia criterio = opcion.getOpcion().getCriterio();
        return ReseniaOpcionDTO.builder()
            .idOpcion(opcion.getOpcion().getIdOpcionCriterio())
            .etiqueta(opcion.getOpcion().getEtiqueta())
            .polaridad(opcion.getOpcion().getPolaridad())
            .idCriterio(criterio.getIdCriterioResenia())
            .nombreCriterio(criterio.getNombre())
            .build();
    }
}
