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
public class OpcionDestacadaDTO {

    private Long idOpcion;
    private String etiqueta;
    private PolaridadOpcion polaridad;
    private long cantidad;
    private BigDecimal porcentaje;
}
