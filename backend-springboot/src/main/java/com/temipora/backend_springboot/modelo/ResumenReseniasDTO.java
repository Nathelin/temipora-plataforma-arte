package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
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
public class ResumenReseniasDTO {

    private BigDecimal promedio;
    private long cantidad;
    private boolean hayDatos;

    @Builder.Default
    private List<DistribucionEstrellasDTO> distribucion = new ArrayList<>();

    @Builder.Default
    private List<CriterioPromedioDTO> criterios = new ArrayList<>();

    @Builder.Default
    private List<OpcionDestacadaDTO> destacados = new ArrayList<>();

    public static ResumenReseniasDTO vacio() {
        return ResumenReseniasDTO.builder()
            .cantidad(0)
            .hayDatos(false)
            .build();
    }
}
