package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

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
public class TerminoDerechoDTO {

    private Long idDerechoTermino;
    private String codigo;
    private String nombre;
    private String descripcion;
    private boolean requierePiezaUnica;
    private boolean disponible;
    private BigDecimal porcentajeExtra;

    public static TerminoDerechoDTO desdeTerminoDerecho(TerminoDerecho terminoDerecho) {
        DerechoTermino derecho = terminoDerecho.getDerecho();
        return TerminoDerechoDTO.builder()
            .idDerechoTermino(derecho != null ? derecho.getIdDerechoTermino() : null)
            .codigo(derecho != null ? derecho.getCodigo() : null)
            .nombre(derecho != null ? derecho.getNombre() : null)
            .descripcion(derecho != null ? derecho.getDescripcion() : null)
            .requierePiezaUnica(derecho != null && derecho.isRequierePiezaUnica())
            .disponible(terminoDerecho.isDisponible())
            .porcentajeExtra(terminoDerecho.getPorcentajeExtra())
            .build();
    }
}
