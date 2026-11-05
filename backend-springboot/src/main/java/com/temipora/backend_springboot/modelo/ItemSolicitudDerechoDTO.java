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
public class ItemSolicitudDerechoDTO {

    private Long idDerechoTermino;
    private String codigo;
    private String nombre;
    private BigDecimal porcentajeExtra;

    public static ItemSolicitudDerechoDTO desdeItemSolicitudDerecho(
            ItemSolicitudDerecho derecho) {
        return ItemSolicitudDerechoDTO.builder()
            .idDerechoTermino(derecho.getDerecho() != null
                ? derecho.getDerecho().getIdDerechoTermino()
                : null)
            .codigo(derecho.getCodigo())
            .nombre(derecho.getNombre())
            .porcentajeExtra(derecho.getPorcentajeExtra())
            .build();
    }
}
