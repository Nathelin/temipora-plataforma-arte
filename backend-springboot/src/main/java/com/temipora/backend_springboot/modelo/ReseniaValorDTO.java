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
public class ReseniaValorDTO {

    private Long idCriterio;
    private String nombreCriterio;
    private EscalaCriterio escala;
    private int valor;

    public static ReseniaValorDTO desdeValor(ReseniaValor valor) {
        CriterioResenia criterio = valor.getCriterio();
        return ReseniaValorDTO.builder()
            .idCriterio(criterio.getIdCriterioResenia())
            .nombreCriterio(criterio.getNombre())
            .escala(criterio.getEscala())
            .valor(valor.getValor())
            .build();
    }
}
