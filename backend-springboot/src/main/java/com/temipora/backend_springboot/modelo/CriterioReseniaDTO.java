package com.temipora.backend_springboot.modelo;

import java.util.ArrayList;
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
public class CriterioReseniaDTO {

    private Long idCriterioResenia;
    private String nombre;
    private String descripcion;
    private String observaciones;
    private AmbitoResenia ambito;
    private EscalaCriterio escala;
    private boolean obligatorio;
    private int orden;
    private boolean activo;
    private long reseniasAsociadas;

    @Builder.Default
    private List<OpcionCriterioDTO> opciones = new ArrayList<>();

    public static CriterioReseniaDTO desdeCriterio(
            CriterioResenia criterio, long reseniasAsociadas) {
        return desdeCriterio(criterio, reseniasAsociadas, false);
    }

    public static CriterioReseniaDTO desdeCriterio(
            CriterioResenia criterio, long reseniasAsociadas,
            boolean soloOpcionesActivas) {
        return CriterioReseniaDTO.builder()
            .idCriterioResenia(criterio.getIdCriterioResenia())
            .nombre(criterio.getNombre())
            .descripcion(criterio.getDescripcion())
            .observaciones(criterio.getObservaciones())
            .ambito(criterio.getAmbito())
            .escala(criterio.getEscala())
            .obligatorio(criterio.isObligatorio())
            .orden(criterio.getOrden())
            .activo(criterio.isActivo())
            .reseniasAsociadas(reseniasAsociadas)
            .opciones(criterio.getOpciones().stream()
                .filter(opcion -> !soloOpcionesActivas || opcion.isActivo())
                .map(OpcionCriterioDTO::desdeOpcion)
                .toList())
            .build();
    }
}
