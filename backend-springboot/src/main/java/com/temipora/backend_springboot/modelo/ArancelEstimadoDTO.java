package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

public record ArancelEstimadoDTO(
        BigDecimal porcentaje,
        BigDecimal ivaPorcentaje,
        BigDecimal porcentajeTotal) {
}
