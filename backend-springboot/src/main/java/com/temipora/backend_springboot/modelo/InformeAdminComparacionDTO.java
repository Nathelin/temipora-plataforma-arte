package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InformeAdminComparacionDTO(
        LocalDate desde,
        LocalDate hasta,
        BigDecimal volumen,
        BigDecimal comision,
        BigDecimal variacionVolumenPorcentaje,
        BigDecimal variacionComisionPorcentaje) {
}
