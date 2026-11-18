package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos del período anterior de igual duración. La variación se calcula sobre
 * el neto percibido y queda en {@code null} cuando el período anterior no
 * tuvo ventas.
 */
public record InformeComparacionDTO(
        LocalDate desde,
        LocalDate hasta,
        BigDecimal bruto,
        BigDecimal netoPercibido,
        BigDecimal variacionNetoPorcentaje) {
}
