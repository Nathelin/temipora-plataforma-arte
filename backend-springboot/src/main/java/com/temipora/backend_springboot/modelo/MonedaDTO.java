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
public class MonedaDTO {

    private Long idMoneda;
    private String codigo;
    private String nombre;
    private String simbolo;
    private boolean predeterminada;
    private boolean activo;

    public static MonedaDTO desdeMoneda(Moneda moneda) {
        return MonedaDTO.builder()
            .idMoneda(moneda.getIdMoneda())
            .codigo(moneda.getCodigo())
            .nombre(moneda.getNombre())
            .simbolo(moneda.getSimbolo())
            .predeterminada(moneda.isPredeterminada())
            .activo(moneda.isEstado())
            .build();
    }
}
