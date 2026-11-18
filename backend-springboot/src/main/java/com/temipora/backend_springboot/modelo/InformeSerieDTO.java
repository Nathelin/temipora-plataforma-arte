package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

/**
 * Un punto de la serie temporal. {@code periodo} es {@code yyyy-MM-dd} para
 * agrupaciones diaria y semanal, y {@code yyyy-MM} para la mensual.
 */
public record InformeSerieDTO(
        String periodo,
        BigDecimal bruto,
        BigDecimal netoPercibido,
        long cobros) {
}
